package ru.newton.fieldapp.features.settings.about

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ru.newton.fieldapp.core.ui.components.NewtonCard
import ru.newton.fieldapp.core.ui.components.NewtonSectionLabel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(onBack: () -> Unit) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("О приложении") },
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
                    NewtonSectionLabel("Newton Field App")
                    Text("MVP-сборка", style = MaterialTheme.typography.headlineMedium)
                    Text(
                        "Сборка для полевых испытаний с приёмником «Ньютон». Журнал, " +
                            "профили NTRIP и параметры съёмки сохраняются на устройстве.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            NewtonCard {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    NewtonSectionLabel("Возможности")
                    Text(
                        "• Системы координат: WGS-84, ГСК-2011 GK, СК-42 GK, СК-95 GK, UTM 35–37N\n" +
                            "• Импорт/экспорт CSV\n" +
                            "• Импорт/экспорт DXF (POINT/LINE/LWPOLYLINE)\n" +
                            "• Съёмка точки с усреднением по эпохам\n" +
                            "• Вынос точки и линии в реальном времени\n" +
                            "• NTRIP-клиент с переподключением",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
            Text(
                "Документация и план разработки находятся в папке docs/ репозитория.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
