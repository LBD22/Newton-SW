# Conventions

These are enforced in review. `ktlint` handles the obvious formatting; humans (and Claude Code) handle the rest.

## Package structure

```
ru.newton.fieldapp
  .<layer>                       # app | core | gnss | domain | data | features
    .<module>                    # common | bluetooth | ui | data | command | ntrip | ...
      .<feature or unit>         # auth | rover | stakeout | ...
        ScreenFiles.kt
        ViewModelFiles.kt
        StateFiles.kt
```

Example full path: `ru.newton.fieldapp.features.settings.rover.RoverSettingsViewModel`.

## Naming

- **Screens**: `<ScreenName>Screen.kt`, top-level Composable named `<ScreenName>Screen`.
- **ViewModels**: `<ScreenName>ViewModel`, one per screen.
- **States**: `<ScreenName>State` as `sealed interface` in `<ScreenName>State.kt` (same file or separate is fine).
- **Use cases**: verb-first: `SurveyPointUseCase`, `ApplyReceiverConfigUseCase`. Not `Interactor`, not `Manager`.
- **Repositories**: `<Noun>Repository` interface in `:domain`, `<Noun>RepositoryImpl` in `:data`.
- **Entities**: `<Noun>Entity` in `:data`.
- **Domain models**: just `<Noun>` in `:domain` — `Point`, `Project`, `Observation`.
- **Parsers**: `<MessageType>Parser` — `GgaParser`, `GstParser`.
- **Hilt qualifiers**: as annotations with `@Qualifier`, not strings. `@DataSpp` / `@CommandSpp`.

## File layout inside a module

```
<module>/
  src/main/kotlin/ru/newton/fieldapp/<path>/
    <feature folders>
  src/main/res/                  # only for modules that have resources (app, features)
  src/test/kotlin/ru/newton/fieldapp/<path>/
  src/test/resources/fixtures/   # for parsers especially
  src/androidTest/kotlin/...     # only for :data and others with instrumented tests
  build.gradle.kts
```

## Kotlin idioms

### State modelling
Use sealed hierarchies, not boolean flags:

```kotlin
// BAD
data class UiState(
    val isLoading: Boolean,
    val isError: Boolean,
    val errorMessage: String?,
    val data: List<Point>,
)

// GOOD
sealed interface UiState {
    data object Loading : UiState
    data class Content(val data: List<Point>) : UiState
    data class Error(val message: String) : UiState
}
```

### Data classes for values, data objects for singletons

```kotlin
data object Idle : LinkState
data class Connecting(val attempt: Int) : LinkState
```

### Immutable over mutable

Fields are `val` unless genuinely must change. `var` is acceptable inside ViewModel private state, never in domain models.

### Nulls
`null` is fine for "this value genuinely doesn't exist yet". Use `sealed interface` for "which case are we in".

### Coroutines

- `suspend fun` for one-shot async.
- `Flow<T>` for streams of values.
- `StateFlow<T>` for state (has current + subscriptions).
- `SharedFlow<T>` for events (fire-and-forget, no current).
- `viewModelScope` in ViewModels. Never `GlobalScope`.
- `Dispatchers.Default` for CPU-heavy work, `Dispatchers.IO` for disk/network. Don't guess — mark explicitly with `withContext(...)` at the boundary.

## Compose

### One screen, one Composable
The top-level `@Composable fun <Name>Screen(...)` takes `ViewModel` and navigation lambdas, nothing else. Everything below is `@Composable private fun` or shared components from `:core:ui`.

### Previews
Every screen has at least one preview, ideally three (loading, content, error). Preview uses sample data, not a real ViewModel.

```kotlin
@Preview(showBackground = true)
@Composable
private fun RoverSettingsPreview_Content() {
    NewtonTheme {
        RoverSettingsContent(
            state = RoverSettingsState.Content(/* sample */),
            onAction = {},
        )
    }
}
```

### State hoisting
ViewModel owns state → screen reads it → child composables receive data + lambdas. Children do not receive the ViewModel.

### Modifier order
Receiver first, then:
1. size/width/height
2. padding
3. border/background
4. clickable/other behaviour

## Hilt

- One `@Module` per module (one per Gradle module), installed in `SingletonComponent` unless there's a specific scope reason.
- Qualifiers for duplicated types: `@Qualifier @Retention(BINARY) annotation class DataSpp`.
- `@HiltViewModel` requires `@Inject constructor`. No setter injection.
- `@Provides` functions short; if it needs logic, extract to a helper.

## Logging

- Use `AppLog` from `:core:logging`, categorized.
- Never `Log.d` directly.
- Never `println` or `System.out.println`.
- Levels: `v/d/i/w/e`. `e` only for things that need user attention.
- No sensitive data (passwords, coordinates of personal sites) in logs.

## Error handling

- Domain errors: `sealed interface <X>Error` with specific cases.
- Result type: `kotlin.Result` or our own `Either<Error, Value>` from `:core:common`. Don't use `null` to signal errors.
- Exceptions thrown only for truly exceptional situations (out of memory, framework bugs). Business failures are not exceptions.

## Comments & KDoc

- KDoc on `internal`/`public` APIs of `:domain`, `:gnss:*`, `:data`. Not on private helpers.
- KDoc answers "why" or "what consumers need to know", not "what the code does" (which the code shows).
- Protocol-specific constants link to `docs/protocol-newton.md` by section.
- `// TODO: <ticket>` not `// TODO`. Tickets prevent TODOs from rotting.

## Git

- Branch: `feat/<ticket>-<short-description>` e.g. `feat/SET-010-rover-settings`.
- Commit: `<type>(<scope>): <message>` e.g. `feat(settings): rover PPP config`.
  - Types: `feat`, `fix`, `refactor`, `test`, `docs`, `chore`, `infra`.
- One logical change per commit.
- Rebase-update before merging.
- PR descriptions answer: what, why, how tested, screenshots for UI.
