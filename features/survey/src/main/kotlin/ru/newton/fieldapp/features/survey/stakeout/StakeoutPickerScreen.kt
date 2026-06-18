package ru.newton.fieldapp.features.survey.stakeout

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import ru.newton.fieldapp.core.ui.components.EmptyState
import ru.newton.fieldapp.core.ui.components.NewtonCard
import ru.newton.fieldapp.data.preferences.ActiveProjectStore
import ru.newton.fieldapp.domain.model.Point
import ru.newton.fieldapp.domain.repository.PointRepository
import javax.inject.Inject

@HiltViewModel
class StakeoutPickerViewModel
    @Inject
    constructor(
        activeProject: ActiveProjectStore,
        pointRepository: PointRepository,
    ) : ViewModel() {
        @OptIn(ExperimentalCoroutinesApi::class)
        val points: StateFlow<List<Point>> = activeProject.activeId
            .flatMapLatest { id ->
                if (id == null) flowOf(emptyList()) else pointRepository.observePoints(id)
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    }

@Composable
fun StakeoutPickerScreen(
    onBack: () -> Unit,
    onPick: (Long) -> Unit,
    viewModel: StakeoutPickerViewModel = hiltViewModel(),
) {
    val points by viewModel.points.collectAsStateWithLifecycle()
    StakeoutPickerContent(points = points, onBack = onBack, onPick = onPick)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StakeoutPickerContent(
    points: List<Point>,
    onBack: () -> Unit,
    onPick: (Long) -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Выбор цели") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                    }
                },
            )
        },
    ) { padding ->
        Box(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentAlignment = Alignment.Center,
        ) {
            if (points.isEmpty()) {
                EmptyState(
                    icon = Icons.Default.Flag,
                    title = "Нет проектных точек",
                    message = "Создайте проект и импортируйте/снимите точки, " +
                        "чтобы вынести их на местность.",
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(points, key = Point::id) { point ->
                        NewtonCard(
                            onClick = { onPick(point.id) },
                            modifier = Modifier.fillMaxWidth(),
                            contentPadding = PaddingValues(12.dp),
                        ) {
                            Column {
                                Text(point.name, style = MaterialTheme.typography.titleMedium)
                                Text(
                                    "N=${"%.3f".format(point.n)}  E=${"%.3f".format(point.e)}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
