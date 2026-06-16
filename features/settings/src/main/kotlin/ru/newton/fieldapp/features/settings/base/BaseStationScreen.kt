package ru.newton.fieldapp.features.settings.base

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import ru.newton.fieldapp.core.ui.components.NewtonCard
import ru.newton.fieldapp.core.ui.components.NewtonPrimaryButton
import ru.newton.fieldapp.core.ui.components.NewtonSecondaryButton
import ru.newton.fieldapp.core.ui.components.NewtonSectionLabel
import ru.newton.fieldapp.data.receiver.PendingChangesService
import ru.newton.fieldapp.domain.receiver.BaseMode
import ru.newton.fieldapp.gnss.command.NewtonCorrection
import ru.newton.fieldapp.gnss.data.GnssStatusStore
import javax.inject.Inject

data class BaseDraft(
    val latText: String = "",
    val lonText: String = "",
    val heightText: String = "",
    val rtcmIdText: String = "",
    val cmrIdText: String = "",
    val correctionId: Int? = null,
)

@HiltViewModel
class BaseStationViewModel
    @Inject
    constructor(
        private val pendingChanges: PendingChangesService,
        private val store: GnssStatusStore,
    ) : ViewModel() {
        private val _draft = MutableStateFlow(initialDraft())
        val draft: StateFlow<BaseDraft> = _draft.asStateFlow()

        private fun initialDraft(): BaseDraft {
            val p = pendingChanges.patch.value
            val b = p.baseMode
            return BaseDraft(
                latText = b?.latitude?.toString().orEmpty(),
                lonText = b?.longitude?.toString().orEmpty(),
                heightText = b?.heightM?.toString().orEmpty(),
                rtcmIdText = p.rtcmId?.toString().orEmpty(),
                cmrIdText = p.cmrId?.toString().orEmpty(),
                correctionId = p.correctionType,
            )
        }

        fun setLat(v: String) = _draft.update { it.copy(latText = v) }
        fun setLon(v: String) = _draft.update { it.copy(lonText = v) }
        fun setHeight(v: String) = _draft.update { it.copy(heightText = v) }
        fun setRtcmId(v: String) = _draft.update { it.copy(rtcmIdText = v) }
        fun setCmrId(v: String) = _draft.update { it.copy(cmrIdText = v) }
        fun setCorrection(id: Int?) = _draft.update { it.copy(correctionId = id) }

        /** Fill the base coordinates from the live WGS-84 fix (lat/lon/ellipsoidal h). */
        fun useCurrentPosition() {
            val s = store.status.value
            val lat = s.latitude ?: return
            val lon = s.longitude ?: return
            val h = s.ellipsoidalHeight ?: 0.0
            _draft.update { it.copy(latText = lat.toString(), lonText = lon.toString(), heightText = h.toString()) }
        }

        fun queue() {
            val d = _draft.value
            val lat = d.latText.trim().replace(',', '.').toDoubleOrNull()
            val lon = d.lonText.trim().replace(',', '.').toDoubleOrNull()
            val h = d.heightText.trim().replace(',', '.').toDoubleOrNull()
            viewModelScope.launch {
                val base = if (lat != null && lon != null && h != null) {
                    BaseMode(latitude = lat, longitude = lon, heightM = h)
                } else {
                    pendingChanges.patch.value.baseMode
                }
                pendingChanges.update { patch ->
                    patch.copy(
                        baseMode = base,
                        // Base and rover are mutually exclusive receiver modes —
                        // drop any queued rover mode so Apply doesn't send both.
                        roverMode = if (base != null) null else patch.roverMode,
                        rtcmId = d.rtcmIdText.trim().toIntOrNull() ?: patch.rtcmId,
                        cmrId = d.cmrIdText.trim().toIntOrNull() ?: patch.cmrId,
                        correctionType = d.correctionId ?: patch.correctionType,
                    )
                }
            }
        }
    }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BaseStationScreen(
    onBack: () -> Unit,
    viewModel: BaseStationViewModel = hiltViewModel(),
) {
    val draft by viewModel.draft.collectAsStateWithLifecycle()
    val canSave = draft.latText.isNotBlank() && draft.lonText.isNotBlank() && draft.heightText.isNotBlank()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("Базовая станция") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                ),
            )
        },
        bottomBar = {
            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp)) {
                NewtonPrimaryButton(
                    onClick = { viewModel.queue(); onBack() },
                    text = "В очередь команд",
                    enabled = canSave,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .imePadding()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            NewtonCard {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    NewtonSectionLabel("Координаты базы (WGS-84)")
                    Text(
                        "Геодезические координаты антенны базы: широта, долгота (град.) " +
                            "и эллипсоидальная высота (м). Команда: mode set base <B> <L> <H>.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    OutlinedTextField(
                        value = draft.latText,
                        onValueChange = viewModel::setLat,
                        label = { Text("Широта B, град.") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    OutlinedTextField(
                        value = draft.lonText,
                        onValueChange = viewModel::setLon,
                        label = { Text("Долгота L, град.") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    OutlinedTextField(
                        value = draft.heightText,
                        onValueChange = viewModel::setHeight,
                        label = { Text("Высота H, м (эллипсоидальная)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    NewtonSecondaryButton(
                        onClick = viewModel::useCurrentPosition,
                        text = "Взять текущую позицию",
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }

            NewtonCard {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    NewtonSectionLabel("Идентификаторы потока")
                    OutlinedTextField(
                        value = draft.rtcmIdText,
                        onValueChange = viewModel::setRtcmId,
                        label = { Text("RTCM id (необязательно)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    OutlinedTextField(
                        value = draft.cmrIdText,
                        onValueChange = viewModel::setCmrId,
                        label = { Text("CMR id (необязательно)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }

            NewtonCard {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    NewtonSectionLabel("Тип поправок (output set correction)")
                    Row(
                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        NewtonCorrection.entries.forEach { c ->
                            FilterChip(
                                selected = draft.correctionId == c.id,
                                onClick = { viewModel.setCorrection(c.id) },
                                label = { Text(c.label) },
                            )
                        }
                    }
                }
            }
        }
    }
}
