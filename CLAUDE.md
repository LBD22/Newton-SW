# Newton Field App — Claude Code Context

You are working on **Newton Field App** — an Android application for professional GNSS surveying with the Russian «Ньютон» receiver. The product is targeted at Russian surveyors. Full product context lives in `docs/` (short versions) and in the sibling documents `Newton_FieldApp_DevHandbook_v1.docx` and `Newton_FieldApp_ProductReview_v1.docx`.

This file is loaded at the start of every Claude Code session. It encodes **how we build**, not what we build — scope lives in docs and the PRD.

---

## Stack — non-negotiable

- **Kotlin 2.3.x**, **JDK 21**, **AGP 9.2.0**, **Gradle 9.4.x**
- **minSdk 31**, **compileSdk / targetSdk 36**
- **Jetpack Compose** (Material 3) via Compose BOM 2026.04.01
- **Hilt** for DI — not Koin, not manual DI
- **Room** with **KSP** — never KAPT (slow on Kotlin 2.x)
- **Coroutines + Flow** (StateFlow for state, SharedFlow for events, Turbine in tests)
- **OkHttp 5** for NTRIP and future network
- **OSMDroid** for maps, **kabeja** for DXF
- **kotlinx.serialization** (not Moshi, not Gson) for JSON
- **Timber** + custom `AppLog` for logging

Compose compiler is configured via the `org.jetbrains.kotlin.plugin.compose` plugin — **never** use the legacy `kotlinCompilerExtensionVersion` property.

Rule of thumb: **if it's in `gradle/libs.versions.toml`, you can use it. If it's not, don't add it without asking.**

---

## Architecture — module layout

15 Gradle modules, 5 layers. See `docs/architecture.md` for the diagram. Key invariants:

- `:features:*` never see each other. Shared UI goes to `:core:ui`, shared domain to `:domain`.
- `:domain` has **no Android imports**. Pure Kotlin + coroutines.
- `:gnss:*` modules don't know about `:domain` or `:data`. They expose low-level APIs.
- `:core:bluetooth` knows nothing about GNSS or Newton commands. One RFCOMM socket; `@DataSpp` / `@CommandSpp` are role tags over the same singleton.

Dependency rule enforcement is manual in MVP — we lint architecture during code review, not via Konsist (yet).

---

## How to run things

```bash
./gradlew :app:assembleDebug           # build app
./gradlew :core:common:test            # run unit tests in a module
./gradlew :gnss:data:test              # run parser tests
./gradlew testDebugUnitTest            # all unit tests
./gradlew ktlintCheck                  # style check
./gradlew lint                         # Android Lint
./gradlew :app:installDebug            # install to connected device
```

**Before pushing:** run `./gradlew ktlintCheck testDebugUnitTest lint` and make sure all green.

---

## Conventions

### Naming

- `ViewModel` classes: `<Screen>ViewModel` — e.g. `RoverSettingsViewModel`
- Use cases: verb-first, `<Action>UseCase` — e.g. `SurveyPointUseCase`, not `SurveyPointManager`
- Sealed states: suffixed with `State` — `LinkState`, `CommandSessionState`, `StakeoutState`
- Hilt qualifiers for duplicated types: `@DataSpp` / `@CommandSpp`, not strings
- Room entities: `<Entity>Entity` — `PointEntity`, `ProjectEntity`

### Files

- One ViewModel per file.
- One `@Composable` screen-level function per file. Small helper composables in the same file are fine.
- Parsers per message type, one file each: `GgaParser.kt`, `GstParser.kt`, etc.
- Sealed state declarations live in `:domain` unless they are purely hardware-level (then in `:gnss:*`).

### Kotlin style

- Prefer `data class` / `data object` for states, not `class`.
- Prefer `sealed interface` over `sealed class` when no shared behaviour.
- `@Serializable` on any model that crosses a boundary (file, network, IPC).
- No `!!` in production code. `requireNotNull` with a message, or handle the null.
- No `GlobalScope`. Use the appropriate `CoroutineScope` from DI or `viewModelScope`.
- Prefer `buildList { }` / `buildString { }` over manual accumulation.

### Comments

- Write **why**, not **what**. "Tagging this write @CommandSpp because the receiver only accepts RTCM after `input set bluetooth`" — yes. "Sets the value" — no.
- Protocol-specific constants must link to `docs/protocol-newton.md` section.

---

## Newton protocol — the critical bits

Full reference: `docs/protocol-newton.md`.

1. **One Bluetooth SPP channel**. The Newton receiver advertises a single RFCOMM record; NMEA, RTCM, command replies and our outgoing commands/RTCM all share that one socket. `@DataSpp` and `@CommandSpp` are call-site role tags resolving to the same transport instance — don't try to open a second socket, you'll race yourself.

2. **Four OK responses**: `OK` (AT only), `OK+` (command mode on), `OK-` (command mode off), `OK!` (queued/applied). **Do not collapse them**. The parser must distinguish.

3. **Source separator is U+2502 (│), not ASCII pipe (|)**. Always use `NewtonCommandBuilder.SRC_SEP`. Never hard-code `|` between sources. If you see `|` in a command string — fix it.

4. **Commands are case-sensitive**. `Mode set rover` ≠ `mode set rover`. Don't capitalize for "readability".

5. **RTCM from NTRIP is written via `@CommandSpp.write(bytes)`** — role tag for the upstream/command side of the shared transport. See `docs/protocol-newton.md` § RTCM flow.

6. **Nothing is applied until `system save`**. Every setting change goes into the `command_queue` table (Room) and is flushed only on explicit user Apply. This is a user-visible guarantee — do not add "auto-apply" shortcuts.

7. **`config reset` is immediate**, not queued. After it, reconnect and re-handshake.

---

## Data — Room conventions

- Point IDs are `Long autoincrement`, never UUID. `externalRef: String?` stores imported identifiers.
- `(project_id, name, revision)` is unique on `points`. Re-measurements are `revision + 1`, not overwrites.
- Migrations: always write them from day 1, even for MVP. Use `AutoMigration` where possible.
- DataStore only for UI preferences (theme, units). Project data always goes to Room.

---

## Testing

- Unit tests in `src/test` — no Android framework. JUnit 5.
- Flow tests use **Turbine**.
- `FakeSppTransport` lives in `:core:bluetooth/src/test`. Reads pre-recorded NMEA sessions from `src/test/resources/fixtures/`.
- When adding a new NMEA parser: `src/test/resources/fixtures/<sentence>.txt` + unit test. No exception.
- Aim for ≥80% line coverage on `:core:*`, `:gnss:*`, `:domain`. UI coverage is opportunistic.

---

## Feature flag & branch hygiene

- Branch naming: `feat/<ticket>-short-description`, `fix/<ticket>`, `chore/<ticket>`.
- Ticket IDs map to screens in ScreenMap v6 (e.g. `SET-010`, `SUR-010`) or cross-cutting concerns (`INFRA-001`).
- Squash-merge to `main`. Rebase-update the branch before merge.
- Write PR descriptions with: what, why, how tested, screenshots (if UI).

---

## What NOT to do (autopilot rejections)

Claude Code should refuse / push back if asked to:

- Add KAPT instead of KSP.
- Use Moshi or Gson instead of kotlinx.serialization.
- Hard-code the source separator `|` in Newton commands.
- Open a second Bluetooth socket against the Newton receiver, or treat `@DataSpp` and `@CommandSpp` as independent transports.
- Return `StateFlow<Any>` or use untyped maps for state.
- Commit secrets (NTRIP passwords, API keys) to git.
- Skip writing a test for a new NMEA parser.
- Use `@HiltViewModel` without a corresponding `@Inject constructor`.
- Add a `:features:<x>` dependency on another `:features:<y>`.
- Introduce a new library without updating `libs.versions.toml`.

---

## Current sprint

Sprint 1 — Foundation. See `docs/sprint-plan.md` for the task list and definition of done.

When starting work, read `docs/sprint-plan.md` first, pick the top unfinished task, and update the status after completion.

---

## Slash commands

See `.claude/commands/` for project-specific commands:
- `/new-screen <ID>` — scaffold a new feature screen
- `/gen-nmea-parser <sentence>` — scaffold a parser + test fixture
- `/check-protocol` — audit Newton command usage in the codebase
- `/sprint-status` — show current sprint progress
- `/add-migration <from> <to>` — create a Room migration skeleton
