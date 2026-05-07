package ru.newton.fieldapp.gnss.command

import ru.newton.fieldapp.core.logging.AppLog

internal class NoopAppLog : AppLog {
    override fun bt(message: String, throwable: Throwable?) = Unit
    override fun gnss(message: String, throwable: Throwable?) = Unit
    override fun cmd(message: String, throwable: Throwable?) = Unit
    override fun ntrip(message: String, throwable: Throwable?) = Unit
    override fun ui(message: String, throwable: Throwable?) = Unit
    override fun general(message: String, throwable: Throwable?) = Unit
    override suspend fun exportArchive(daysBack: Int): String = ""
}
