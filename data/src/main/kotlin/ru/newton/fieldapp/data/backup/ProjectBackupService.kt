package ru.newton.fieldapp.data.backup

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import ru.newton.fieldapp.data.db.dao.PointDao
import ru.newton.fieldapp.data.db.dao.ProjectDao
import ru.newton.fieldapp.data.db.dao.StakeoutResultDao
import ru.newton.fieldapp.data.db.dao.TrackDao
import ru.newton.fieldapp.data.db.entity.PointEntity
import ru.newton.fieldapp.data.db.entity.ProjectEntity
import ru.newton.fieldapp.data.db.entity.StakeoutResultEntity
import ru.newton.fieldapp.data.db.entity.TrackPointEntity
import ru.newton.fieldapp.data.db.entity.TrackSessionEntity
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Project-level export/import as a ZIP `.newton-backup` archive.
 *
 * Why a ZIP and not a single JSON: photos are binary and the office uses
 * AutoCAD/NanoCAD/Excel where attached binary blobs in JSON are awkward.
 * A ZIP keeps photos as separate JPEGs while the metadata stays human-readable
 * for ops who want to peek inside.
 *
 * Restore always creates a NEW project (never overwrites) — the surveyor's
 * working copy is sacred. Names get a "(копия)" suffix on collision.
 */
@Singleton
class ProjectBackupService
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
        private val projectDao: ProjectDao,
        private val pointDao: PointDao,
        private val stakeoutResultDao: StakeoutResultDao,
        private val trackDao: TrackDao,
        private val json: Json,
    ) {
        /**
         * Serialise project [projectId] into [output]. Caller is responsible
         * for closing the stream (typically a `ContentResolver.openOutputStream`).
         */
        suspend fun export(projectId: Long, output: OutputStream): ExportResult = withContext(Dispatchers.IO) {
            val project = projectDao.byId(projectId)
                ?: error("Project id=$projectId not found")
            val points = pointDao.observeByProject(projectId).first()
            val stakeouts = stakeoutResultDao.observeByProject(projectId).first()
            val sessions = trackDao.observeSessionsByProject(projectId).first()
            val sessionsWithPoints = sessions.map { session ->
                session to trackDao.pointsForSession(session.sessionId)
            }

            val photoFiles = mutableMapOf<String, File>() // archiveName → real file
            val backupPoints = points.map { p -> p.toBackup(photoFiles) }

            val backup = ProjectBackup(
                exportedAtUtc = System.currentTimeMillis(),
                project = project.toBackup(),
                points = backupPoints,
                stakeoutResults = stakeouts.map { it.toBackup() },
                tracks = sessionsWithPoints.map { (s, pts) -> s.toBackup(pts) },
            )

            ZipOutputStream(output.buffered()).use { zip ->
                zip.putNextEntry(ZipEntry("backup.json"))
                zip.write(json.encodeToString(ProjectBackup.serializer(), backup).toByteArray())
                zip.closeEntry()
                photoFiles.forEach { (name, file) ->
                    if (!file.exists()) return@forEach
                    zip.putNextEntry(ZipEntry("photos/$name"))
                    file.inputStream().use { it.copyTo(zip) }
                    zip.closeEntry()
                }
            }
            ExportResult(
                pointCount = backup.points.size,
                photoCount = photoFiles.size,
                stakeoutCount = backup.stakeoutResults.size,
                trackCount = backup.tracks.size,
            )
        }

        /**
         * Restore a backup ZIP from [input] as a NEW project. Returns the new
         * project's id so callers can navigate to it. Photos are unpacked into
         * `filesDir/photos/`; track sessions and stakeout history are recreated
         * with fresh ids pointing at the new project.
         */
        suspend fun import(input: InputStream): ImportResult = withContext(Dispatchers.IO) {
            // Stream the ZIP twice would mean buffering — instead, drain to
            // a temp file and re-read. Simpler than juggling rewinds.
            val tmp = File.createTempFile("newton_backup_", ".zip", context.cacheDir)
            try {
                tmp.outputStream().use { out -> input.copyTo(out) }

                val backupJson = readEntry(tmp, "backup.json")
                    ?: error("Архив не содержит backup.json — это не Newton-бэкап")
                val backup = json.decodeFromString(ProjectBackup.serializer(), backupJson.decodeToString())
                require(backup.version <= ProjectBackup.CURRENT_VERSION) {
                    "Версия бэкапа ${backup.version} новее, чем поддерживает приложение"
                }

                // Insert new project (with a uniqueness-friendly suffix if name collides).
                val originalName = backup.project.name
                val name = nextAvailableName(originalName)
                val now = System.currentTimeMillis()
                val newProjectId = projectDao.insert(
                    ProjectEntity(
                        name = name,
                        comment = backup.project.comment,
                        crsConfigJson = backup.project.crsConfigJson,
                        createdAtUtc = backup.project.createdAtUtc,
                        updatedAtUtc = now,
                    ),
                )

                // Unpack photos into our private dir, remembering the new
                // relative paths so the inserted points reference real files.
                val photosDir = File(context.filesDir, "photos").apply { mkdirs() }
                val photoMap = mutableMapOf<String, String>() // archive name → new relative path
                ZipInputStream(tmp.inputStream()).use { zip ->
                    var entry = zip.nextEntry
                    while (entry != null) {
                        if (!entry.isDirectory && entry.name.startsWith("photos/")) {
                            val src = entry.name.removePrefix("photos/")
                            val out = File(photosDir, "${newProjectId}_$src")
                            out.outputStream().use { o -> zip.copyTo(o) }
                            photoMap[src] = "photos/${out.name}"
                        }
                        entry = zip.nextEntry
                    }
                }

                backup.points.forEach { bp ->
                    pointDao.insert(
                        PointEntity(
                            projectId = newProjectId,
                            name = bp.name,
                            code = bp.code,
                            layerId = bp.layerId,
                            revision = bp.revision,
                            northingM = bp.n,
                            eastingM = bp.e,
                            heightM = bp.h,
                            sourceName = bp.source,
                            externalRef = bp.externalRef,
                            note = bp.note,
                            photoPath = bp.photoFile?.let { photoMap[it] },
                            createdAtUtc = bp.createdAtUtc,
                        ),
                    )
                }
                backup.stakeoutResults.forEach { bs ->
                    stakeoutResultDao.insert(
                        StakeoutResultEntity(
                            projectId = newProjectId,
                            targetPointId = null,
                            targetLabel = bs.targetLabel,
                            mode = bs.mode,
                            targetN = bs.targetN,
                            targetE = bs.targetE,
                            targetH = bs.targetH,
                            actualN = bs.actualN,
                            actualE = bs.actualE,
                            actualH = bs.actualH,
                            deltaHorizontalM = bs.deltaHorizontalM,
                            deltaVerticalM = bs.deltaVerticalM,
                            savedAtUtc = bs.savedAtUtc,
                        ),
                    )
                }
                backup.tracks.forEach { bt ->
                    val sessionId = trackDao.insertSession(
                        TrackSessionEntity(
                            projectId = newProjectId,
                            name = bt.name,
                            startedAtUtc = bt.startedAtUtc,
                            stoppedAtUtc = bt.stoppedAtUtc,
                        ),
                    )
                    bt.points.forEach { tp ->
                        trackDao.insertPoint(
                            TrackPointEntity(
                                sessionId = sessionId,
                                northingM = tp.n,
                                eastingM = tp.e,
                                heightM = tp.h,
                                fixQuality = tp.fixQuality,
                                timestampUtc = tp.timestampUtc,
                            ),
                        )
                    }
                }

                ImportResult(
                    newProjectId = newProjectId,
                    pointCount = backup.points.size,
                    photoCount = photoMap.size,
                    stakeoutCount = backup.stakeoutResults.size,
                    trackCount = backup.tracks.size,
                )
            } finally {
                runCatching { tmp.delete() }
            }
        }

        private fun readEntry(zipFile: File, name: String): ByteArray? {
            ZipInputStream(zipFile.inputStream()).use { zip ->
                var entry = zip.nextEntry
                while (entry != null) {
                    if (entry.name == name) return zip.readBytes()
                    entry = zip.nextEntry
                }
            }
            return null
        }

        private suspend fun nextAvailableName(base: String): String {
            val existing = projectDao.observeAll().first().map { it.name }.toSet()
            if (base !in existing) return base
            var i = 2
            while ("$base ($i)" in existing) i++
            return "$base ($i)"
        }

        private fun ProjectEntity.toBackup() = BackupProject(
            name = name,
            comment = comment,
            crsConfigJson = crsConfigJson,
            createdAtUtc = createdAtUtc,
            updatedAtUtc = updatedAtUtc,
        )

        private fun PointEntity.toBackup(photoFiles: MutableMap<String, File>): BackupPoint {
            val photoArchiveName = photoPath?.let { rel ->
                val src = File(context.filesDir, rel)
                if (src.exists()) {
                    val archiveName = "${pointId}_${src.name}"
                    photoFiles[archiveName] = src
                    archiveName
                } else {
                    null
                }
            }
            return BackupPoint(
                name = name,
                code = code,
                layerId = layerId,
                revision = revision,
                n = northingM,
                e = eastingM,
                h = heightM,
                source = sourceName,
                externalRef = externalRef,
                note = note,
                photoFile = photoArchiveName,
                createdAtUtc = createdAtUtc,
            )
        }

        private fun StakeoutResultEntity.toBackup() = BackupStakeoutResult(
            targetLabel = targetLabel,
            mode = mode,
            targetN = targetN,
            targetE = targetE,
            targetH = targetH,
            actualN = actualN,
            actualE = actualE,
            actualH = actualH,
            deltaHorizontalM = deltaHorizontalM,
            deltaVerticalM = deltaVerticalM,
            savedAtUtc = savedAtUtc,
        )

        private fun TrackSessionEntity.toBackup(points: List<TrackPointEntity>) = BackupTrackSession(
            name = name,
            startedAtUtc = startedAtUtc,
            stoppedAtUtc = stoppedAtUtc,
            points = points.map {
                BackupTrackPoint(
                    n = it.northingM,
                    e = it.eastingM,
                    h = it.heightM,
                    fixQuality = it.fixQuality,
                    timestampUtc = it.timestampUtc,
                )
            },
        )

        data class ExportResult(
            val pointCount: Int,
            val photoCount: Int,
            val stakeoutCount: Int,
            val trackCount: Int,
        )

        data class ImportResult(
            val newProjectId: Long,
            val pointCount: Int,
            val photoCount: Int,
            val stakeoutCount: Int,
            val trackCount: Int,
        )
    }
