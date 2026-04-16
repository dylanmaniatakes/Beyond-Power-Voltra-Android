# 2026-04-15 Assist, Isometric, and Custom Curve

Sources:

- `sysdiagnose_2026.04.15_20-25-36-0400_iPhone-OS_iPad_22F76`
- `sysdiagnose_2026.04.15_20-29-37-0400_iPhone-OS_iPad_22F76`
- `sysdiagnose_2026.04.15_20-36-03-0400_iPhone-OS_iPad_22F76`
- `sysdiagnose_2026.04.15_20-44-11-0400_iPhone-OS_iPad_22F76`

## Assist

- Weight Training Assist is confirmed on `0x5106 FITNESS_ASSIST_MODE`.
- Official app write observed for On:
  - `551204C7AA1014002000110100065101C143`
- Official app write observed for Off:
  - `551204C7AA101E0020001101000651002F63`
- Follow-up async update for On echoed `0x5106 = 1`.
- Follow-up async update after the Off write reported `0x5106 = 8`.
- Working rule for Android: treat only `1` as On; treat any other observed Assist value as Off until more states are captured.

## Isometric Test

- Official app enters the page with:
  - `551204C7AA1014002000110100B04F081A8D`
- Passive state while the page is open reports:
  - `FITNESS_WORKOUT_STATE = 8`
  - `BP_SET_FITNESS_MODE = 0x0085`
  - `ISOMETRIC_MAX_FORCE (0x5431) = 400 lb`
  - `ISOMETRIC_MAX_DURATION (0x53D2) = 15 s`
- No dedicated Isometric control writes were seen in this capture beyond mode entry and exit, so the page currently looks read-only from BLE.

## Custom Curve

- Custom Curve create/apply traffic is not using the normal `cmd=0x11 param write` flow.
- The main families seen in the iPad traffic are:
  - repeated app writes `cmd=0xAA`, subcommand `0x13`
  - larger responses under `0xAA 0x80`
  - larger responses under `0xAA 0x86`
  - larger responses under `0xAA 0x92`
- The earlier CSV candidates `0x539C CUSTOM_CURVE_ID`, `0x3E90 APP_STORAGE_SETTINGS_FORCE_CURVE_CHOOSE`, and `0x3E8F FORCE_CURVE_UPDATE` did not appear directly in these captures.
- Practical conclusion: keep Custom Curve behind Developer Mode and do not guess writes yet.
