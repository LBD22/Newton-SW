# Newton Command Port Protocol — working reference

Derived from `OSdoc_command_port_2025_11_13` (official firmware manual). For anything not covered here, consult the manual and update this file.

## Bluetooth channel

Newton exposes **one** Bluetooth Classic SPP channel — a single RFCOMM record under the standard SPP UUID `00001101-0000-1000-8000-00805F9B34FB`. Verified with `sdptool` against the T31 firmware: only one RFCOMM channel is advertised.

The receiver multiplexes everything over that one pipe:

- Downstream (receiver → phone): continuous NMEA / ORIENT / COMNAV / RTCM frames at the configured rate (1–20 Hz), **and** ASCII reply lines (`OK`, `OK+`, `OK-`, `OK!`, plus the info lines for `get …` queries).
- Upstream (phone → receiver): text commands, **and** RTCM bytes once `input set bluetooth` is configured.

Implications for our codebase:

- `:core:bluetooth` opens exactly one `BluetoothSocket`. Opening two against the same RFCOMM endpoint produces `read failed, socket might closed or timeout, read ret: -1` on whichever loses the race.
- `@DataSpp` and `@CommandSpp` are **role tags** for call-site readability, not separate transports. Both qualifiers resolve to the same singleton.
- Parsers above the transport demux by frame shape: `NmeaLineAggregator` keeps NMEA, `CommandLineAggregator` keeps reply lines, each ignoring the other's frames at the line level.
- UI shows **one** BT indicator, not two — the previous BT-D / BT-C split was a doc/UI artefact of the false two-channel assumption.

## Command format

```
section command parameters
```

- Commands are **case-sensitive**.
- Parameters with spaces must be in double quotes: `"my mountpoint"`.
- Lines terminated with `\r\n`.
- Source separator is **U+2502 (│)**, not ASCII `|`.

## OK replies — four variants

| Reply | Emitted after |
|---|---|
| `OK` | `AT` (handshake only) |
| `OK+` | `set command mode on` |
| `OK-` | `set command mode off` |
| `OK!` | any queued command, plus `system save` when applied |

**Do not collapse these.** `response == "OK"` checking a config command is a bug.

## Handshake sequence

```
Send:    AT
Expect:  OK
Send:    get command mode
Expect:  on or off
If off:
  Send:    set command mode on
  Expect:  OK+
```

Only after this is `CommandSession` in `Ready` state and commands may be sent.

## Command categories (from Tab 2.1)

### system

| Command | Purpose |
|---|---|
| `system help` | Built-in help |
| `system list` | Show pending command queue |
| `system remove <N>` | Remove command #N from queue |
| `system clear` | Clear queue |
| `system save` | Apply queue + persist to flash |

### mode

| Command | Purpose |
|---|---|
| `mode set rover` | Rover mode |
| `mode set roverbase` | Rover relative to base |
| `mode set rovermaster` | Rover relative to master |
| `mode set base <B> <L> <H>` | Base at fixed coords |
| `mode rtcmid <value>` | RTCM ID for base |
| `mode cmrid <value>` | CMR ID |
| `mode set ppp <TYPE> <on\|off>` | PPP mode |
| `mode set ppp sbas <on\|off> <SBAS>` | SBAS sub-mode |

### survey

| Command | Purpose |
|---|---|
| `survey set mask <deg>` | Elevation mask |

### coordsystem

| Command | Purpose |
|---|---|
| `coordsystem set geoid <wgs84\|egm86\|user>` | Geoid |
| `coordsystem set undulation <value>` | Custom undulation |

### bluetooth / gsm / eth0

| Command | Purpose |
|---|---|
| `bluetooth set <on\|off>` | BT enable |
| `gsm set <on\|off> <apn1> <apn2>` | GSM modem + APN |
| `eth0 set dhcp` | Ethernet DHCP |
| `eth0 set static <HOST> <GW> <MASK> <DNS1> <DNS2> <MTU>` | Ethernet static |

### input (data sources for rover)

| Command | Purpose |
|---|---|
| `input set tcpclient <HOST> <PORT>` | TCP client |
| `input set tcpserver <PORT>` | TCP server |
| `input set ntripclient <HOST> <PORT> <ENDPOINT> <LOGIN> <PASS> gppga` | NTRIP via receiver's network |
| `input set com1 <BAUD>` | COM1 |
| `input set com2 <BAUD>` | COM2 |
| `input set uhf <FREQ> <PROTOCOL> <BAUD>` | UHF radio |
| `input set gsmtcpclient <HOST> <PORT>` | TCP via receiver's GSM modem |
| `input set gsmntripclient <HOST> <PORT> <ENDPOINT> <LOGIN> <PASS> gppga` | NTRIP via receiver's GSM |
| `input set bluetooth` | Accept RTCM over Bluetooth |
| `input bridge <on\|off> [target] [params]` | Bridge between interfaces |

### output (streams / messages out)

Manage two collections: `message` (individual sentences) and `stream` (routes).

| Command | Purpose |
|---|---|
| `output list message` | Show configured messages |
| `output list stream` | Show configured streams |
| `output clear message` | Clear all |
| `output clear stream` | Clear all |
| `output remove message <N>` | Remove by index |
| `output remove stream <N>` | Remove by index |
| `output add message <SRC> <TYPE> <RATE> <FORMAT>` | Add message |
| `output add stream <SOURCES> <target> <params>` | Add stream |
| `output set correction <N>` | Correction type for base (see "Corrections") |

Targets for `add stream`: `tcpclient <HOST> <PORT>`, `tcpserver <PORT>`, `com1 <BAUD>`, `com2 <BAUD>`, `uhf <F> <P> <B> <PW>`, `bluetooth`, `gsmtcpclient <HOST> <PORT>`.

### config

| Command | Purpose |
|---|---|
| `config reset` | Factory reset. **Immediate, not queued.** Reconnect + re-handshake after. |

## Sources (Tab 2.6)

| Code | Meaning |
|---|---|
| `M` | Master |
| `R1` | Rover (or Rover 1) |
| `R2` | Rover 2 |
| `PPP` | PPP |
| `IMU` | Inertial |

Multiple sources joined by `│` (U+2502). Example: `M│R1│IMU`.

## Rates (Tab 2.4)

| Code | Meaning |
|---|---|
| `NONE` | Disabled |
| `20HZ`, `10HZ`, `5HZ`, `2HZ`, `1HZ` | Frequency |
| `5S`, `10S`, `30S`, `60S`, `120S` | Interval |
| `ONNEW` | On availability |
| `ONCHANGE` | On change |

## Correction types (Tab 2.3) — for `output set correction <N>`

| N | Type |
|---|---|
| 0 | None |
| 1 | DGNSS |
| 2 | RTCM 2.3 |
| 3 | RTCM 3.0 |
| 4 | RTCM 3.2 MSM4 |
| 5 | RTCM 3.2 MSM5 |
| 6 | RTCM 3.2 MSM6 |
| 7 | RTCM 3.2 MSM7 |
| 8 | RTCM ComNav |
| 9 | CMR |
| 10 | ComNav |
| 11 | RTCM COMPASS |
| 12–15 | MSM4–7 UHF variants |

## PPP types (Tab 2.7)

`rtcmssr`, `orient`, `pass2pass`, `sino`, `sbas`, `rtk`.

## SBAS systems (Tab 2.8)

`egnos`, `waas`, `msas`, `gagan`.

## UHF protocols (Tab 2.4 — UHF)

`trimtalk`, `trimmk3`, `transeot`, `mac`, `tt450s`, `transparent`, `south`, `satel`, `lora`.

## UHF power (Tab 2.5)

`high`, `low`, `0.5w`, `1w`, `2w`, `medium`.

## Messages (Tab 2.2) — MVP subset

For full list of 56, see official manual. For MVP, configure these on startup:

| Sentence | TYPE (for `add message`) | Format | Sources |
|---|---|---|---|
| GPGGA | 1 | A | M│R1│R2│PPP |
| GPRMC | 2 | A | M│R1│R2│PPP |
| GPGSA | 5 | A | M│R1│R2│PPP |
| GPGSV | 6 | A | M│R1│R2│PPP |
| GPGST | 9 | A | M│R1│R2│PPP |
| GPTRA | 7 | A | M│R1│R2 |
| PTNLVHD | 46 | A | M│R1│R2 |
| PTNLAVR | 23 | A | M│R1│R2 |

## RTCM flow (MVP)

**Mechanism**: RTCM from NTRIP is written to the shared SPP transport via `@CommandSpp.write(bytes)`. Since the receiver has a single RFCOMM channel, the byte stream physically lands on the same socket as text commands — the firmware demuxes by frame shape (RTCM messages start with `0xD3`, commands are ASCII text terminated by `\r\n`).

**Activation**: The receiver only consumes incoming Bluetooth bytes as RTCM once `input set bluetooth` has been applied. Before that, any bytes we write that aren't recognised commands are silently dropped.

**Why `@CommandSpp` and not `@DataSpp`** in code: it's a semantic role tag — RTCM is part of the "upstream" role that also handles commands. The qualifier choice signals intent at the call site; both resolve to the same transport.

## Canonical command sequences

### Initial rover setup for NTRIP from phone

```
set command mode on                        → OK+
mode set rover                              → OK!
input set bluetooth                         → OK!
survey set mask 10                          → OK!
output clear message                        → OK!
output clear stream                         → OK!
output add message M GPGGA 1HZ A            → OK!
output add message M GPGST 1HZ A            → OK!
output add message M GPGSA 1HZ A            → OK!
output add message M GPGSV 1HZ A            → OK!
output add message M GPTRA 1HZ A            → OK!
output add stream M bluetooth               → OK!   (send NMEA out over SPP)
system save                                 → OK!   (persist)
```

After `system save`, RTCM writes to the shared SPP transport will be accepted by the receiver as RTK corrections.

### Factory reset flow

```
config reset        → immediate, not queued
                    → receiver reboots, BT may drop
(wait 10–15s, reconnect Bluetooth)
AT                  → OK
get command mode    → off (factory default)
set command mode on → OK+
(clean slate, re-apply desired config)
```
