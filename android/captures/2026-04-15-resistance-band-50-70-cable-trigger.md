# 2026-04-15 iPad Capture: Resistance Band 50/70 and Cable Trigger

Source: `/Users/ticnitsi/Downloads/sysdiagnose_2026.04.15_15-35-05-0400_iPhone-OS_iPad_22F76.tar.gz`

Primary PacketLogger file: `logs/Bluetooth/bluetoothd-hci-latest.pklg`

## Summary

- Official app writes stayed on ATT handle `0x0035`, and VOLTRA notifications stayed on ATT handle `0x002A`.
- This capture cleanly confirms `FITNESS_WORKOUT_STATE=2` enters Resistance Band mode.
- Resistance Band max force is parameter `0x5362` / `RESISTANCE_BAND_MAX_FORCE`.
- Resistance Band load/start used the same confirmed loaded value as Weight Training: `BP_SET_FITNESS_MODE=5`.
- Cable trigger adjustment updates `BP_RUNTIME_POSITION_CM` (`0x3E82`) as the cable moves and then updates `MC_DEFAULT_OFFLEN_CM` (`0x506A`) when the new offset is saved.
- Android now passively decodes Resistance Band max force and the saved cable offset, but active Resistance Band controls remain blocked until the force slider mapping is captured one more time without mixed UI actions.

## Confirmed Frames

| Action | Parameter | Official app write or VOLTRA update |
| --- | --- | --- |
| Enter Resistance Band | `0x4FB0 FITNESS_WORKOUT_STATE = 2` | `551204C7AA1013002000110100B04F02B5E6` |
| Resistance max force intermediate | `0x5362 RESISTANCE_BAND_MAX_FORCE = 51` | `55130403AA1014002000110100625333007CB4` |
| Resistance max force 50 lb | `0x5362 RESISTANCE_BAND_MAX_FORCE = 50` | `55130403AA1017002000110100625332001753` |
| Force echo from device | `0x5362 RESISTANCE_BAND_MAX_FORCE = 50` | `5513040310AA4510200010010062533200ED9E` |
| Read cable offset/runtime position | `0x506A MC_DEFAULT_OFFLEN_CM`, `0x3E82 BP_RUNTIME_POSITION_CM` | `55130403AA10180020000F02006A50823EE198` |
| Cable read response | `0x506A = 34 cm`, `0x3E82 = 34 cm` | `5518088310AA180020000F0002006A502200823E2200BA1E` |
| Load/start Resistance Band | `0x3E89 BP_SET_FITNESS_MODE = 5` | `55130403AA1019002000110100893E0500CD6F` |
| Loaded echo from device | `0x3E89 BP_SET_FITNESS_MODE = 5` | `5513040310AA4B102000100100893E050037A2` |
| Later force write | `0x5362 RESISTANCE_BAND_MAX_FORCE = 100` | `55130403AA102B0020001101006253640000A5` |
| Saved cable offset after trigger | `0x506A MC_DEFAULT_OFFLEN_CM = 39 cm` | `5513040310AA601020001001006A5027003696` |
| Return ready/unloaded | `0x3E89 BP_SET_FITNESS_MODE = 4`, `0x5467 FITNESS_ONGOING_UI = 0` | `5517043810AA55102000100200893E0400675400005E3C` |
| Exit Resistance Band | `0x4FB0 FITNESS_WORKOUT_STATE = 0` | `551204C7AA103E002000110100B04F00809C` |

## Cable Trigger Sequence

After the load/start write, the cable trigger produced runtime position updates:

- `0x3E82 = 35 cm`: `5513040310AA59102000100100823E2300A6CF`
- `0x3E82 = 36 cm`: `5513040310AA5A102000100100823E24001D7C`
- `0x3E82 = 37 cm`, `0x506A = 0`: `5517043810AA5C102000100200823E25006A500000CFEB`
- `0x3E82 = 38 cm`: `5513040310AA5D102000100100823E26004BEF`
- `0x3E82 = 39 cm`: `5513040310AA5E102000100100823E27002008`
- `0x506A = 39 cm`: `5513040310AA601020001001006A5027003696`

## Open Questions

- The user-visible second target was described as 70 lb, but the cleanest later force write in this capture is `0x5362 = 100`. This might be an intermediate UI state, a different Resistance Band control, or a unit/slider mapping issue. Do not ship active force setting from this capture alone.
- `0x5165 EP_SCR_SWITCH` used two captured uint32 payloads around the cable-adjustment UI: enter with bytes `00 10 00 01`, then later close/return with bytes `00 00 00 02`. Treat it as a screen/control handoff marker, not as a direct cable-length command.
- `0x5467 FITNESS_ONGOING_UI` changes with the Resistance Band ongoing screen and should be tracked in later captures if we want a richer mode UI.

## Android Changes From This Capture

- Decode `RESISTANCE_BAND_MAX_FORCE` (`0x5362`) into passive readings.
- Decode `MC_DEFAULT_OFFLEN_CM` (`0x506A`) into passive readings.
- Read Resistance Band max force, runtime cable position, and saved cable offset during the read-only bootstrap.
