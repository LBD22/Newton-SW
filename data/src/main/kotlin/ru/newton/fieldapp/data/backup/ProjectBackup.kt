package ru.newton.fieldapp.data.backup

import kotlinx.serialization.Serializable

/**
 * Wire format for the per-project backup ZIP. Versioned so we can evolve
 * fields without breaking older archives — restore must check [version] and
 * refuse anything it doesn't understand. The archive layout is:
 *
 * ```
 * backup.json        ← serialised [ProjectBackup]
 * photos/<file>.jpg  ← photo blobs referenced by photoFile fields
 * ```
 *
 * We deliberately keep wire types orthogonal to Room entities so a schema
 * migration doesn't silently break old backups. Mappers live alongside.
 */
@Serializable
data class ProjectBackup(
    val version: Int = CURRENT_VERSION,
    val exportedAtUtc: Long,
    val project: BackupProject,
    val points: List<BackupPoint>,
    val stakeoutResults: List<BackupStakeoutResult>,
    val tracks: List<BackupTrackSession>,
) {
    companion object { const val CURRENT_VERSION = 1 }
}

@Serializable
data class BackupProject(
    val name: String,
    val comment: String?,
    val crsConfigJson: String,
    val createdAtUtc: Long,
    val updatedAtUtc: Long,
)

@Serializable
data class BackupPoint(
    val name: String,
    val code: String?,
    val layerId: Long?,
    val revision: Int,
    val n: Double,
    val e: Double,
    val h: Double,
    val source: String,
    val externalRef: String?,
    val note: String,
    /** File name inside the archive's `photos/` directory, or null. */
    val photoFile: String?,
    val createdAtUtc: Long,
)

@Serializable
data class BackupStakeoutResult(
    val targetLabel: String,
    val mode: String,
    val targetN: Double,
    val targetE: Double,
    val targetH: Double,
    val actualN: Double,
    val actualE: Double,
    val actualH: Double,
    val deltaHorizontalM: Double,
    val deltaVerticalM: Double,
    val savedAtUtc: Long,
)

@Serializable
data class BackupTrackSession(
    val name: String,
    val startedAtUtc: Long,
    val stoppedAtUtc: Long?,
    val points: List<BackupTrackPoint>,
)

@Serializable
data class BackupTrackPoint(
    val n: Double,
    val e: Double,
    val h: Double,
    val fixQuality: String,
    val timestampUtc: Long,
)
