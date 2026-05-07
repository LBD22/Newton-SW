# Architecture — quick reference

**For the full version, read `Newton_FieldApp_DevHandbook_v1.docx`.** This file is a tl;dr for quick lookup during coding.

## Module layout (15 modules, 5 layers)

```
                         :app
                          │
         ┌────────────────┼─────────────────┐
         ▼                ▼                 ▼
  :features:project  :features:settings  :features:survey  :features:cad  :features:export
         │                │                 │
         └────────┬───────┴──────┬──────────┘
                  ▼              ▼
              :domain         :data
                  │              │
                  ▼              ▼
              :crs    :core:common   :core:ui
                  │
         ┌────────┴─────────┬─────────────┐
         ▼                  ▼             ▼
    :gnss:data      :gnss:command   :gnss:ntrip
         │                  │             │
         └────────┬─────────┘             │
                  ▼                       │
           :core:bluetooth  ◄─────────────┘
                  │
                  ▼
            :core:logging
```

## Module responsibilities

| Module | What it does | What it MUST NOT do |
|---|---|---|
| `:app` | Application shell, top-level navigation, Hilt wiring | Contain feature-specific logic |
| `:core:common` | Result types, time utils, formatters | Depend on anything else |
| `:core:ui` | Design system, reusable composables | Know about specific features |
| `:core:logging` | AppLog, file export | Anything Android-framework-heavy |
| `:core:bluetooth` | SPP transport abstraction | Know about GNSS or Newton commands |
| `:gnss:data` | NMEA parsers, GnssStatusStore | Know about UI or use cases |
| `:gnss:command` | Newton command protocol | Depend on :domain or :data |
| `:gnss:ntrip` | NTRIP client | Depend on :domain or :data |
| `:crs` | Coordinate system math, geoids | Know about Android |
| `:data` | Room, DAOs, repositories | Contain business logic |
| `:domain` | Use cases, sealed states, repo interfaces | Import android.* or androidx.* |
| `:features:*` | UI screens for one feature vertical | Depend on another `:features:*` |

## Key data flows

### GNSS data → UI

```
BluetoothSocket bytes
     │
     ▼ (DataSPP)
LineAggregator (split by CRLF)
     │
     ▼
NmeaDispatcher (by sentence tag)
     │
     ▼
Typed parsers (Gga, Gst, Gsa, Tra, ...)
     │
     ▼
GnssStatusStore (StateFlow<GnssStatus>)
     │
     ▼
UI (any @Composable that collects)
```

### Command flow

```
User action on settings screen
     │
     ▼
ViewModel → PendingChangesService.update(patch)
     │
     ▼ (stored in Room command_queue)
...
User taps "Apply" on SET-080
     │
     ▼
ApplyReceiverConfigUseCase.apply() : Flow<ApplyProgress>
     │
     ▼ (via CommandSession, via CommandSPP)
Newton receiver
     │
     ▼ (one of OK / OK+ / OK- / OK!)
Parser → ApplyProgress emissions → UI
     │
     ▼ (final: system save → OK!)
Queue cleared, persisted, UI shows success
```

### NTRIP → receiver

```
NTRIP caster (HTTP/1.1)
     │
     ▼ (OkHttp ResponseBody stream)
NtripClient.streamLoop
     │
     ▼ write to CommandSPP (NOT DataSPP — see protocol-newton.md)
     │
     ▼
Newton receiver (input set bluetooth mode, consumes RTCM from CommandSPP)
```

## State ownership

- `GnssStatusStore` — **singleton**, owns `StateFlow<GnssStatus>`. All UI showing fix/satellites reads from here.
- `CommandSession` — singleton, owns `StateFlow<CommandSessionState>`.
- `NtripClient` — singleton, owns `StateFlow<NtripState>`.
- `BluetoothSppTransport` (x2) — singletons (qualified `@DataSpp` / `@CommandSpp`), each owns `StateFlow<LinkState>`.
- `PendingChangesService` — singleton, owns `StateFlow<ReceiverConfigPatch>`.
- ViewModel-owned state — only for screen-local concerns (form input, loading flags for this screen).

## Rules to enforce in review

1. No `:features:*` → `:features:*` edges.
2. No Android imports in `:domain`.
3. All command strings come from `NewtonCommandBuilder`.
4. All RTCM writes go to `@CommandSpp`.
5. No library added without `libs.versions.toml` entry.
6. Every new parser has a fixture + test.
7. Every schema change has a migration + migration test.
