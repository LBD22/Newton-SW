package ru.newton.fieldapp.features.settings.units

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import ru.newton.fieldapp.data.preferences.AngleFormat
import ru.newton.fieldapp.data.preferences.LengthUnit
import ru.newton.fieldapp.data.preferences.UnitsConfig
import ru.newton.fieldapp.data.preferences.UnitsPreferences
import javax.inject.Inject

@HiltViewModel
class UnitsSettingsViewModel
    @Inject
    constructor(
        private val preferences: UnitsPreferences,
    ) : ViewModel() {
        val config: StateFlow<UnitsConfig> = preferences.config
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UnitsConfig())

        fun setLength(unit: LengthUnit) {
            viewModelScope.launch { preferences.setLength(unit) }
        }

        fun setAngle(format: AngleFormat) {
            viewModelScope.launch { preferences.setAngle(format) }
        }
    }
