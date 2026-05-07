package ru.newton.fieldapp.core.logging

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import javax.inject.Inject
import javax.inject.Singleton

/**
 * File-backed implementation of [AppLog].
 *
 * Each category writes to its own file under `<filesDir>/logs/<category>.log`.
 * When a file exceeds [MAX_FILE_BYTES], it is rotated to `<category>.log.1` and
 * a fresh file is started — exactly one rolled copy is kept.
 *
 * Why per-category files: SET-080 (Diagnostics) lets the user export a subset
 * (e.g. only `bt` + `cmd` for a Bluetooth incident report) without dragging in
 * megabytes of unrelated `gnss` traffic.
 */
@Singleton
class AppLogImpl
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
    ) : AppLog {
        private val logsDir: File by lazy {
            File(context.filesDir, "logs").apply { if (!exists()) mkdirs() }
        }

        private val timestampFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US)

        override fun bt(
            message: String,
            throwable: Throwable?,
        ) = log(Category.BT, message, throwable)

        override fun gnss(
            message: String,
            throwable: Throwable?,
        ) = log(Category.GNSS, message, throwable)

        override fun cmd(
            message: String,
            throwable: Throwable?,
        ) = log(Category.CMD, message, throwable)

        override fun ntrip(
            message: String,
            throwable: Throwable?,
        ) = log(Category.NTRIP, message, throwable)

        override fun ui(
            message: String,
            throwable: Throwable?,
        ) = log(Category.UI, message, throwable)

        override fun general(
            message: String,
            throwable: Throwable?,
        ) = log(Category.GENERAL, message, throwable)

        override suspend fun exportArchive(daysBack: Int): String =
            withContext(Dispatchers.IO) {
                val cutoffMs = System.currentTimeMillis() - daysBack * MS_PER_DAY
                val outFile =
                    File(
                        context.cacheDir,
                        "newton-logs-${SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US).format(Date())}.zip",
                    )
                ZipOutputStream(FileOutputStream(outFile)).use { zip ->
                    logsDir.listFiles()?.filter { it.isFile && it.lastModified() >= cutoffMs }?.forEach { f ->
                        zip.putNextEntry(ZipEntry(f.name))
                        f.inputStream().use { it.copyTo(zip) }
                        zip.closeEntry()
                    }
                }
                outFile.absolutePath
            }

        private fun log(
            category: Category,
            message: String,
            throwable: Throwable?,
        ) {
            Timber.tag(category.tag)
            if (throwable != null) Timber.w(throwable, message) else Timber.d(message)
            appendToFile(category, message, throwable)
        }

        private fun appendToFile(
            category: Category,
            message: String,
            throwable: Throwable?,
        ) {
            runCatching {
                val file = File(logsDir, category.fileName)
                if (file.exists() && file.length() > MAX_FILE_BYTES) {
                    val rolled = File(logsDir, category.fileName + ".1")
                    if (rolled.exists()) rolled.delete()
                    file.renameTo(rolled)
                }
                FileOutputStream(file, true).bufferedWriter().use { w ->
                    w.append(timestampFormat.format(Date()))
                    w.append(' ').append(category.tag).append(": ")
                    w.append(message)
                    if (throwable != null) {
                        w.append(" | ").append(throwable.javaClass.simpleName)
                        throwable.message?.let { w.append(": ").append(it) }
                    }
                    w.append('\n')
                }
            }
            // Intentionally swallow IO failures: logging must never crash the app.
        }

        private enum class Category(
            val tag: String,
            val fileName: String,
        ) {
            BT("BT", "bt.log"),
            GNSS("GNSS", "gnss.log"),
            CMD("CMD", "cmd.log"),
            NTRIP("NTRIP", "ntrip.log"),
            UI("UI", "ui.log"),
            GENERAL("APP", "general.log"),
        }

        private companion object {
            const val MAX_FILE_BYTES = 20L * 1024 * 1024
            const val MS_PER_DAY = 24L * 60 * 60 * 1000
        }
    }
