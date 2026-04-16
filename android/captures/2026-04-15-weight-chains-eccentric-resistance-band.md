# 2026-04-15 iPad Capture: Weight, Chains, Eccentric, Resistance Band

Source: `/Users/ticnitsi/Downloads/sysdiagnose_2026.04.15_09-15-04-0400_iPhone-OS_iPad_22F76.tar.gz`

Primary PacketLogger file: `logs/Bluetooth/bluetoothd-hci-latest.pklg`

## Summary

- Official app writes were on ATT handle `0x0035`, the confirmed VOLTRA transport characteristic.
- VOLTRA notifications were on ATT handle `0x002A`, the confirmed command/response characteristic.
- This capture confirms direct `cmd=0x11` writes for Chains and Eccentric strength features.
- Eccentric values are stored as signed 16-bit pounds even though `paramInfo.csv` labels `BP_ECCENTRIC_WEIGHT` as `uint16`.
- Resistance Band mode was observed, but the safe end-to-end command sequence is not isolated enough for active Android controls yet.

## Confirmed Strength Feature Writes

| Feature | Param | Observed values | Example official app frame |
| --- | --- | --- | --- |
| Chains | `0x3E87 BP_CHAINS_WEIGHT` | `0`, `24`, `29`, `30` lb | `55130403AA1020002000110100873E1E0042CA` (`30 lb`) |
| Eccentric | `0x3E88 BP_ECCENTRIC_WEIGHT` | `0`, `-15`, `-20` lb | `55130403AA1023002000110100883EECFFC8C6` (`-20 lb`) |
| Inverse chains | `0x53B0 FITNESS_INVERSE_CHAIN` | `0` only | `551204C7AA1027002000110100B05300ED37` |

The device echoed the Chains and Eccentric writes in `cmd=0x10` async state frames. Examples:

- `BP_CHAINS_WEIGHT=30`: `5513040310AA26002000100100873E1E00FF37`
- `BP_ECCENTRIC_WEIGHT=-20`: `5513040310AA32002000100100883EECFFD6EA`

## Load/Unload Observation

During the official app's feature flow, the app wrote `BP_SET_FITNESS_MODE=1` before the device reported loaded state `5`, and wrote `BP_SET_FITNESS_MODE=0` before the device reported ready state `4`.

Android keeps using the previously hardware-tested strength controls:

- Load: write `BP_SET_FITNESS_MODE=5`
- Unload: write `BP_SET_FITNESS_MODE=4`

Reason: those writes were already tested successfully from Android. The `1/0` form may be a UI-level start/stop shortcut or mode-dependent command, so it needs a clean isolated capture before replacing the known-good load/unload path.

## Resistance Band Clues

The end of the capture looks like a Resistance Band flow:

- `FITNESS_WORKOUT_STATE=0`: leaves the weight-training session.
- `FITNESS_WORKOUT_STATE=2`: likely enters Resistance Band mode.
- `cmd=0x0F` read request for `0x506A MC_DEFAULT_OFFLEN_CM` and `0x3E82 BP_RUNTIME_POSITION_CM`.
- `BP_SET_FITNESS_MODE=1`: likely load/start for the current mode; the device then reports `BP_SET_FITNESS_MODE=5` while `FITNESS_WORKOUT_STATE=2`.

This is enough to track the mode passively, but not enough to add active Resistance Band controls.

## Android Changes From This Capture

- Decode `BP_ECCENTRIC_WEIGHT` as signed 16-bit pounds.
- Add guarded experimental Chains and Eccentric setting writes in the Control screen.
- Later Android diagnostics expanded the UI caps from fixed captured values to dynamic ranges: Chains `0..min(baseWeightLb, 200 - baseWeightLb)`, Eccentric signed load `-baseWeightLb..+baseWeightLb`.
- Inverse chains now has a guarded standalone toggle using `FITNESS_INVERSE_CHAIN`; inverse-off was captured here, inverse-on still needs a clean official-app capture.
- Keep custom curve and Resistance Band active writes as placeholders until isolated captures confirm their full sequences.
