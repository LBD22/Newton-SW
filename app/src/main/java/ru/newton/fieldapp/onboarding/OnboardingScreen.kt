package ru.newton.fieldapp.onboarding

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.Map
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import ru.newton.fieldapp.core.ui.components.NewtonCard
import ru.newton.fieldapp.core.ui.components.NewtonPrimaryButton
import ru.newton.fieldapp.core.ui.components.NewtonSecondaryButton
import ru.newton.fieldapp.core.ui.components.NewtonSectionLabel
import ru.newton.fieldapp.data.preferences.OnboardingPreferences
import javax.inject.Inject

@HiltViewModel
class OnboardingViewModel
    @Inject
    constructor(
        private val preferences: OnboardingPreferences,
    ) : ViewModel() {
        fun complete() {
            viewModelScope.launch { preferences.setCompleted() }
        }
    }

/**
 * APP-002–005 — first-run wizard. Walks the user through the four prerequisites
 * before they can record a fix:
 *   1. Permissions (BT + location).
 *   2. Receiver pairing nudge — they finish in the system Bluetooth settings,
 *      then connect on the in-app screen.
 *   3. Project creation hint.
 *   4. First survey hint.
 *
 * Steps don't enforce completion of the underlying action — the wizard is a
 * scaffold for the surveyor to follow, not a gating mechanism. Skipping
 * jumps straight to [onFinished], same as completing all steps.
 */
@Composable
fun OnboardingScreen(
    onFinished: () -> Unit,
    viewModel: OnboardingViewModel = hiltViewModel(),
) {
    var step by remember { mutableStateOf(0) }
    val total = 5
    val finishAndPersist: () -> Unit = {
        viewModel.complete()
        onFinished()
    }

    Scaffold(containerColor = MaterialTheme.colorScheme.background) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                "Шаг ${step + 1} из $total",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            when (step) {
                0 -> PermissionsStep(onNext = { step++ })
                1 -> StaticStep(
                    icon = Icons.Default.Bluetooth,
                    title = "Подключите приёмник",
                    body = "Включите «Ньютон», убедитесь, что он спарен по Bluetooth в " +
                        "системных настройках. После этого зайдите в раздел «Настройки → " +
                        "Подключение Bluetooth» и выберите устройство.",
                    onNext = { step++ },
                )
                2 -> StaticStep(
                    icon = Icons.Default.Folder,
                    title = "Создайте проект",
                    body = "На вкладке «Проект» нажмите «Новый» и задайте имя, систему " +
                        "координат и геоид. Все измерения и импорт DXF/CSV сохраняются " +
                        "относительно активного проекта.",
                    onNext = { step++ },
                )
                3 -> StaticStep(
                    icon = Icons.Default.GpsFixed,
                    title = "Настройте источник поправок",
                    body = "В «Настройки → Источник коррекций» добавьте профиль NTRIP " +
                        "(host/port/login/пароль). После Apply приёмник перейдёт " +
                        "Single → Float → Fixed обычно за 30–60 секунд.",
                    onNext = { step++ },
                )
                else -> StaticStep(
                    icon = Icons.Default.Map,
                    title = "Готово к съёмке",
                    body = "Перейдите на вкладку «Съёмка» — там доступны точка, линия, " +
                        "непрерывная съёмка, вынос. Полоса состояния сверху показывает " +
                        "фикс и количество видимых спутников в реальном времени.",
                    nextLabel = "Завершить",
                    onNext = finishAndPersist,
                    nextIcon = Icons.Default.CheckCircle,
                )
            }
            Spacer(Modifier.height(8.dp))
            NewtonSecondaryButton(
                onClick = finishAndPersist,
                text = "Пропустить",
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun PermissionsStep(onNext: () -> Unit) {
    val permissions = remember {
        buildList {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                add(Manifest.permission.BLUETOOTH_CONNECT)
                add(Manifest.permission.BLUETOOTH_SCAN)
            }
            add(Manifest.permission.ACCESS_FINE_LOCATION)
        }.toTypedArray()
    }
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { _ -> onNext() }

    NewtonCard {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            NewtonSectionLabel("Разрешения")
            Text("Добро пожаловать в Newton Field App", style = MaterialTheme.typography.headlineMedium)
            Text(
                "Для работы с приёмником нам нужен доступ к Bluetooth и точному " +
                    "местоположению — без него Android не позволит подключиться к " +
                    "GNSS-приёмнику и получать NMEA-данные.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            NewtonPrimaryButton(
                onClick = { launcher.launch(permissions) },
                text = "Запросить разрешения",
                icon = Icons.Default.GpsFixed,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun StaticStep(
    icon: ImageVector,
    title: String,
    body: String,
    onNext: () -> Unit,
    nextLabel: String = "Дальше",
    nextIcon: ImageVector? = null,
) {
    NewtonCard {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            NewtonSectionLabel(title)
            Text(title, style = MaterialTheme.typography.headlineMedium)
            Text(
                body,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            NewtonPrimaryButton(
                onClick = onNext,
                text = nextLabel,
                icon = nextIcon ?: icon,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
