package ru.newton.fieldapp.features.settings.gsm

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import ru.newton.fieldapp.core.ui.components.NewtonCard
import ru.newton.fieldapp.core.ui.components.NewtonPrimaryButton
import ru.newton.fieldapp.core.ui.components.NewtonSectionLabel
import ru.newton.fieldapp.data.receiver.PendingChangesService
import ru.newton.fieldapp.domain.receiver.GsmSetting
import javax.inject.Inject

data class GsmDraft(
    val enabled: Boolean = false,
    val apn1: String = "",
    val apn2: String = "",
)

@HiltViewModel
class GsmViewModel
    @Inject
    constructor(private val pendingChanges: PendingChangesService) : ViewModel() {
        private val _draft = MutableStateFlow(
            pendingChanges.patch.value.gsm?.let {
                GsmDraft(it.enabled, it.apn1, it.apn2 ?: "")
            } ?: GsmDraft(),
        )
        val draft: StateFlow<GsmDraft> = _draft.asStateFlow()

        fun setEnabled(v: Boolean) { _draft.value = _draft.value.copy(enabled = v) }
        fun setApn1(v: String) { _draft.value = _draft.value.copy(apn1 = v) }
        fun setApn2(v: String) { _draft.value = _draft.value.copy(apn2 = v) }

        fun queue() {
            val d = _draft.value
            viewModelScope.launch {
                pendingChanges.update {
                    it.copy(
                        gsm = GsmSetting(
                            enabled = d.enabled,
                            apn1 = d.apn1.trim(),
                            apn2 = d.apn2.trim().ifEmpty { null },
                        ),
                    )
                }
            }
        }
    }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GsmScreen(
    onBack: () -> Unit,
    viewModel: GsmViewModel = hiltViewModel(),
) {
    val draft by viewModel.draft.collectAsStateWithLifecycle()
    val canSave = draft.apn1.isNotBlank() || !draft.enabled

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("GSM модем") },
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
                    onClick = {
                        viewModel.queue()
                        onBack()
                    },
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
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            NewtonCard {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            NewtonSectionLabel("Модем")
                            Text(
                                "Встроенный GSM модем приёмника. Включается перед использованием " +
                                    "источника `gsmtcpclient` или `gsmntripclient`.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Switch(checked = draft.enabled, onCheckedChange = viewModel::setEnabled)
                    }
                }
            }
            NewtonCard {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    NewtonSectionLabel("APN")
                    OutlinedTextField(
                        value = draft.apn1,
                        onValueChange = viewModel::setApn1,
                        label = { Text("Основной APN") },
                        singleLine = true,
                        enabled = draft.enabled,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    OutlinedTextField(
                        value = draft.apn2,
                        onValueChange = viewModel::setApn2,
                        label = { Text("Резервный APN (необязательно)") },
                        singleLine = true,
                        enabled = draft.enabled,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
    }
}
