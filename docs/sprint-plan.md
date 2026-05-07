# Sprint plan

Current sprint highlighted. Tick boxes as tasks complete. Full roadmap in `Newton_FieldApp_ProductReview_v1.docx` §7.

---

## Sprint 1 — Foundation (weeks 1–2)

Infrastructure and empty shell. No hardware integration yet.

- [x] INFRA-001: Gradle setup with 15 modules, Hilt, Compose plugin, KSP, Version Catalog
- [x] INFRA-002: Core navigation shell — three empty tabs, NavHost
- [x] INFRA-003: AppLog in `:core:logging` with file export
- [x] INFRA-004: Design tokens in `:core:ui` — theme, typography, colors
- [x] INFRA-005: Room database skeleton — `NewtonDatabase` with at least one entity
- [x] PRJ-001: Projects list screen (empty state, no data yet)
- [x] PRJ-002: Create-new-project screen (name only, no CRS yet)

**Definition of done:** App installs, opens, shows three tabs, can create a project with just a name, project persists through restart. `./gradlew testDebugUnitTest ktlintCheck lint` all green.

**Status (2026-05-03):** all four DoD tasks pass green: `:app:assembleDebug`,
`ktlintCheck`, `testDebugUnitTest`, `lint`. APK builds. Pending manual on-device
verification: open the app, create a project named «Тест», kill, reopen, see the
project in the list. Once confirmed, Sprint 1 closes.

**Carry-overs to next sprints:**
- Kabeja artifact `org.kabeja:kabeja:0.4` does not exist on public Maven —
  resolve real coordinates in Sprint 7 (CAD-001) and re-enable in `:features:cad`.
- Gradle configuration cache disabled (AGP 9 ↔ `ProcessNavigationXmlTask` bug);
  re-enable when AGP fixes it.
- `hiltViewModel` deprecation warning: migrate to
  `androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel` once the new package
  is published in a Hilt release we can pin.

---

## Sprint 2 — CRS and basic export (weeks 3–4) ← **CURRENT**

- [x] CRS-001: WGS-84 geo as baseline (no transform needed, passthrough)
- [x] CRS-002: UTM 36N forward/inverse
- [x] CRS-003: ГСК-2011 GK zones 6°
- [x] CRS-004: СК-42 GK (Helmert 7-param to WGS-84) — also covers СК-95
- [~] CRS-005: EGM-86 geoid baked in — code path ready (`Egm86`), 1440×721 float32
      grid resource not yet shipped. `undulationM()` returns 0 until grid lands.
- [x] PRJ-003: CRS picker in project creation
- [ ] PRJ-004: Change CRS on existing project with recalc preview
      (deferred: Sprint 3 — needs project-details screen polish first)
- [x] PRJ-005: Manual point entry with N/E/H
- [x] PRJ-013: CSV import wizard (basic — no column-mapping wizard yet,
      header auto-detection only; bad rows surface as issues, good rows save)
- [x] PRJ-014: CSV/TXT export with field template

**Definition of done:** Create project in ГСК-2011 zone 8, import 1000 points
from CSV, export back, diff is zero. Code path verified: 1000-point round-trip
test in `:core:common` (`CsvSerializerTest.parses a 1000-point round-trip with
diff zero`). End-to-end on-device walk-through pending manual verification.

**Status (2026-05-04):** all DoD commands green —
`./gradlew :app:assembleDebug ktlintCheck testDebugUnitTest :crs:test
:core:common:test lint`. Tests: 28 in `:crs`, 6 in `:core:common`. Pending:
PRJ-004 (slip to Sprint 3) and EGM-86 grid binary (data deliverable, not code).

---

## Sprint 3 — Bluetooth & GNSS Data (weeks 5–6)

- [x] BT-001: `:core:bluetooth` transport abstraction
- [x] BT-002: Two SppTransport instances with Hilt qualifiers
- [x] BT-003: Reconnect with exponential backoff (1s..30s ceiling)
- [x] BT-004: `GnssForegroundService` (lives in `:gnss:data`, not `:core:bluetooth`,
      because it owns the NMEA pipeline, not just the socket)
- [x] GNSS-001: Line aggregator + NmeaDispatcher
- [x] GNSS-002: GgaParser + fixture + test
- [x] GNSS-003: GstParser + fixture + test
- [x] GNSS-004: GsaParser + fixture + test
- [x] GNSS-005: TraParser + fixture + test
- [x] GNSS-006: GnssStatusStore singleton
- [x] UI-001: GlobalStatusStrip showing real data
- [x] SET-001: Bluetooth connect screen

**Definition of done:** Connect to real Newton, see coordinates updating at 1
Hz in status strip. Kill app, reopen → reconnects.

**Status (2026-05-04):** all DoD commands green —
`./gradlew :app:assembleDebug ktlintCheck testDebugUnitTest :crs:test
:core:common:test lint`. Tests: 28 in `:crs`, 6 in `:core:common`, 31 in
`:gnss:data` (parsers + dispatcher + aggregator + store). Auto-reconnect on
app restart not yet wired — needs persisting last MAC in DataStore and
auto-starting `GnssForegroundService` on cold boot. Slip to Sprint 4 polish.

**Deferred:**
- Auto-start service on app launch with last-used MAC (Sprint 4 polish).
- DataSPP/CommandSPP RFCOMM channel selection — current implementation uses
  the standard SPP UUID for both; if the receiver assigns the same channel
  twice, may need reflection-based `createRfcommSocket(channel)` workaround.
  Verify in field testing.

---

## Sprint 4 — Command port (weeks 7–8)

- [x] CMD-001: `CommandSession` with handshake (AT → OK → get command mode → set on)
- [x] CMD-002: Parser for four OK replies (`OkKind`)
- [x] CMD-003: `NewtonCommandBuilder` with all MVP commands (delivered in Sprint 1)
- [~] CMD-004: `CommandQueue` in Room — deferred. The in-memory
      `PendingChangesService` is the queue for now; on-disk persistence so the
      queue survives a crash mid-session lands when the failure mode is observed.
- [x] CMD-005: `PendingChangesService`
- [x] CMD-006: `ApplyReceiverConfigUseCase` with Flow<ApplyProgress>
- [x] SET-010: Rover settings screen (mode, mask, RTCM id)
- [x] SET-080: Diagnostics + Apply screen (pending list, progress, log export)

**Definition of done:** Change rover mask from 5° to 10° via UI, apply, verify
via `system list` on receiver web interface.

**Status (2026-05-04):** all DoD commands green —
`./gradlew :app:assembleDebug ktlintCheck testDebugUnitTest :crs:test
:core:common:test lint`. Tests: 28 in `:crs`, 6 in `:core:common`, 31 in
`:gnss:data`, 10 in `:gnss:command` (CommandSession + aggregator), 11 in
`:data` (PendingChangesService + PatchToCommands). End-to-end via real
receiver pending hardware-in-the-loop verification.

**Architecture note:** `PendingChangesService`, `PatchToCommands`, and
`ApplyReceiverConfigUseCase` live in `:data` (not `:gnss:command`) because
they need both `:domain` (`ReceiverConfigPatch`, `ApplyProgress`) and
`:gnss:command` (`CommandSession`, `NewtonCommandBuilder`). The
`:gnss:* ↛ :domain` invariant prevents them living inside `:gnss:command`.

---

## Sprint 5 — NTRIP & full RTK (weeks 9–10)

- [x] NTR-001: NTRIP client on OkHttp 5
- [x] NTR-002: SourceTable parser (4 tests)
- [x] NTR-003: Reconnect with exponential backoff
- [x] NTR-004: Encrypted password storage (Android Keystore + AES-256-GCM)
- [x] SET-011: Correction source screen
- [x] SET-012: NTRIP profile list
- [x] SET-013: NTRIP new/edit profile
- [ ] SET-014: Output messages — deferred (data path ready, UI missing)
- [ ] SET-015: Output streams — deferred (same)

**Definition of done:** In open field, Single → Float → Fixed in ≤60 s using
public NTRIP caster. Pending hardware-in-the-loop. Critical invariant:
`NtripForwarder` writes RTCM to `@CommandSpp` only — never `@DataSpp`.

---

## Sprint 6 — Survey and map (weeks 11–12)

- [~] UI-002: OSMDroid integration — done (MAPNIK tiles, AndroidView wrapper).
      MBTiles offline support deferred (file-picker UI + tile-source binding).
- [x] SUR-001: Survey tab root with index + nav graph
- [x] SUR-010: Map Survey with real-time cursor (auto-pan on fix)
- [x] SUR-011: Point Survey with epoch averaging (mean lat/lon/h, σH, save)
- [ ] SUR-012: Line Survey by points — deferred (Sprint 8 polish)
- [ ] SUR-014: Track recording — deferred (Sprint 8 polish)
- [~] SUR-101: Save-point inline (no separate dialog yet — name/code fields
      live on the PointSurvey screen)
- [x] SET-111: Survey defaults (min epochs, σH/σV tolerances) via DataStore

**Definition of done:** Walk a ~1 ha contour, capture 30 points in Fixed, all
show on map in correct CRS. Code path complete for single-point surveys;
contour walk pending hardware verification.

---

## Sprint 7 — Stakeout and CAD (weeks 13–14)

- [~] CAD-001: hand-rolled `DxfReader` (POINT/LINE/LWPOLYLINE) — kabeja still
      missing on Maven; revisit if/when full DXF features are needed.
- [ ] CAD-002: DXF import wizard UI — deferred (parser ready)
- [ ] CAD-003: CAD overlay on map — deferred (rendering polylines on
      OSMDroid via `Polyline` overlay; do when first user requests)
- [x] SUR-030: Stakeout target picker (lists project points)
- [x] SUR-031: Stakeout to point (live distance/azimuth/ΔH, save as-built
      when within tolerance)
- [ ] SUR-032: Stakeout to line — deferred (math for perpendicular foot is
      ~30 lines; do when needed)
- [~] SUR-131: As-built save inline on stakeout screen
- [ ] SUR-132: Stakeout history — deferred
- [x] EXP-001: DXF writer (round-trip-tested with NanoCAD-compatible
      format; `Locale.US` decimal separator forced)

**Definition of done:** Import a DXF, stakeout 10 points to tolerance, save
as-builts, export DXF that opens in NanoCAD. Math + I/O verified; full
end-to-end requires hardware + DXF samples.

---

## Sprint 8 — Polish and field tests (weeks 15–16)

- [~] APP-001: minimal "О приложении" screen exists; full multi-step
      onboarding (permissions → BT pairing → first project → first survey)
      not implemented.
- [ ] APP-002–005, SET-090/091 (units / language) — not implemented; the
      app currently ships ru-RU with metric units only.
- [ ] All remaining MVP-priority screens from ScreenMap v6
- [ ] Run full test matrix (Product Review §6.2) twice
- [ ] Field test with external surveyor
- [ ] Fix blockers from field test

**Definition of done:** External surveyor completes a full work day (J1–J7
scenarios) without developer assistance. Results accepted by an office using
AutoCAD/NanoCAD.

**Status (2026-05-04):** the engineering substrate for all eight sprints is
in place — protocol, CRS math, NTRIP, command port, survey, stakeout, DXF
I/O. Sprint 8 polish is the remaining surface-area work; it's bulky but
non-blocking. The next outstanding deliverable is **field validation with
real Newton hardware**, which is outside the scope of this codebase.

---

## Task ID convention

- `INFRA-NNN` — foundation work, not tied to a screen
- `PRJ-NNN` — project tab screens (see ScreenMap v6)
- `SET-NNN` — settings tab
- `SUR-NNN` — survey tab
- `CAD-NNN` — CAD features
- `BT-NNN` — bluetooth-level work
- `GNSS-NNN` — GNSS data parsing
- `CMD-NNN` — command protocol
- `NTR-NNN` — NTRIP
- `CRS-NNN` — coordinate systems
- `UI-NNN` — cross-cutting UI work
- `EXP-NNN` — import/export
