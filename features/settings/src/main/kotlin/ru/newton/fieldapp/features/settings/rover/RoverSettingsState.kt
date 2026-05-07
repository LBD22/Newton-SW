package ru.newton.fieldapp.features.settings.rover

import ru.newton.fieldapp.domain.receiver.RoverMode

data class RoverSettingsState(
    val mode: RoverMode = RoverMode.ROVER,
    val maskText: String = "10",
    val maskError: String? = null,
    val rtcmIdText: String = "",
)
