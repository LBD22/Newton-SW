package ru.newton.fieldapp.core.common

/**
 * Centralised error codes for user-facing messages and support requests.
 *
 * Every code is stable: once published it never changes its meaning so a
 * surveyor reading a code over the phone gives the support engineer
 * unambiguous context. New conditions get new codes — never reuse old ones.
 *
 * Pattern: `ERR_<DOMAIN>_<NNN>` where DOMAIN ∈ {BT, NTR, CMD, GNSS, IMP, EXP, PROJ}.
 * Russian text is for the user; English code is for telemetry / logs.
 */
enum class ErrorCode(
    val code: String,
    val userText: String,
    /** Optional one-line action hint for the surveyor. Null = no action. */
    val recoveryHint: String? = null,
) {
    // ---- Bluetooth ----
    BT_PERMISSION_DENIED(
        "ERR_BT_001",
        "Нет разрешения на Bluetooth.",
        "Разрешите в системных настройках приложения.",
    ),
    BT_NOT_AVAILABLE(
        "ERR_BT_002",
        "Bluetooth недоступен на устройстве.",
    ),
    BT_DISABLED(
        "ERR_BT_003",
        "Bluetooth выключен.",
        "Включите его в шторке или в системных настройках.",
    ),
    BT_NOT_PAIRED(
        "ERR_BT_004",
        "Приёмник не сопряжён с телефоном.",
        "Сопрягите в системных настройках Bluetooth, затем нажмите «Обновить».",
    ),
    BT_CONNECT_FAILED(
        "ERR_BT_005",
        "Не удалось подключиться к приёмнику.",
        "Проверьте, что приёмник включён и в зоне действия Bluetooth.",
    ),
    BT_LINK_LOST(
        "ERR_BT_006",
        "Связь с приёмником потеряна.",
        "Приложение пробует переподключиться автоматически.",
    ),

    // ---- NTRIP ----
    NTR_AUTH_FAILED(
        "ERR_NTR_001",
        "Неверный логин или пароль NTRIP.",
        "Проверьте профиль в «Настройки → Источник коррекций».",
    ),
    NTR_MOUNTPOINT_MISSING(
        "ERR_NTR_002",
        "Точка монтирования не найдена на касте́ре.",
        "Запросите список доступных точек кнопкой «Source table».",
    ),
    NTR_NETWORK_DOWN(
        "ERR_NTR_003",
        "Нет сети для подключения к NTRIP.",
        "Проверьте мобильный интернет или Wi-Fi.",
    ),
    NTR_TIMEOUT(
        "ERR_NTR_004",
        "Касте́р не отвечает.",
        "Проверьте host и port профиля; иногда помогает переподключение.",
    ),
    NTR_HTTP_ERROR(
        "ERR_NTR_005",
        "Ошибка касте́ра.",
        "Попробуйте через минуту.",
    ),

    // ---- Newton command port ----
    CMD_AT_TIMEOUT(
        "ERR_CMD_001",
        "Приёмник не ответил на AT-команду.",
        "Переподключите CommandSPP в «Настройки → Подключение».",
    ),
    CMD_MODE_OFF(
        "ERR_CMD_002",
        "Командный режим приёмника выключен.",
        "Приложение пытается включить его автоматически; если не получается — переподключитесь.",
    ),
    CMD_REJECTED(
        "ERR_CMD_003",
        "Приёмник отклонил команду.",
        "Полный текст ответа смотрите в журнале команд.",
    ),
    CMD_QUEUE_FULL(
        "ERR_CMD_004",
        "Очередь pending-команд переполнена.",
        "Откройте «Применение и диагностика» и примените или очистите очередь.",
    ),
    CMD_SAVE_FAILED(
        "ERR_CMD_005",
        "Команда `system save` не выполнилась.",
        "Изменения не сохранены во flash приёмника.",
    ),

    // ---- GNSS data ----
    GNSS_NO_FIX(
        "ERR_GNSS_001",
        "Решение не получено.",
        "Подождите 30–60 секунд на открытом небе.",
    ),
    GNSS_STALE_FIX(
        "ERR_GNSS_002",
        "Возраст поправок > 30 секунд.",
        "Стрим RTCM прервался; проверьте NTRIP или базу.",
    ),
    GNSS_NO_DATA(
        "ERR_GNSS_003",
        "Приёмник не отдаёт NMEA.",
        "Проверьте Bluetooth и убедитесь, что NMEA-вывод включён в настройках сообщений.",
    ),

    // ---- Import / Export ----
    IMP_PARSE_FAILED(
        "ERR_IMP_001",
        "Не удалось разобрать файл.",
        "Проверьте формат и кодировку (UTF-8 или Windows-1251).",
    ),
    IMP_COLUMN_MISSING(
        "ERR_IMP_002",
        "В CSV не найдены обязательные колонки.",
        "Откройте мастер импорта и укажите соответствие колонок.",
    ),
    EXP_WRITE_FAILED(
        "ERR_EXP_001",
        "Не удалось записать файл.",
        "Проверьте свободное место и разрешения на хранилище.",
    ),

    // ---- Project ----
    PROJ_NOT_ACTIVE(
        "ERR_PROJ_001",
        "Активный проект не выбран.",
        "Откройте «Проект», создайте или выберите существующий.",
    ),
    PROJ_NOT_FOUND(
        "ERR_PROJ_002",
        "Проект не найден.",
    ),
    ;

    /** "ERR_BT_001 · Bluetooth выключен. Включите его..." */
    fun toUserMessage(): String = buildString {
        append(code).append(" · ").append(userText)
        recoveryHint?.let { append(' ').append(it) }
    }
}

/**
 * Map an exception to a user-friendly [ErrorCode] best-effort. Unknown
 * exception types fall through to a generic message that still carries the
 * original `cause.message` so it can be copy-pasted into a support ticket.
 */
fun Throwable.toUserMessage(): String = when (this) {
    is SecurityException -> ErrorCode.BT_PERMISSION_DENIED.toUserMessage()
    is java.net.SocketTimeoutException -> ErrorCode.NTR_TIMEOUT.toUserMessage()
    is java.net.UnknownHostException -> ErrorCode.NTR_NETWORK_DOWN.toUserMessage()
    is java.net.ConnectException -> ErrorCode.NTR_NETWORK_DOWN.toUserMessage()
    else -> message ?: "Неизвестная ошибка (${this::class.simpleName})"
}
