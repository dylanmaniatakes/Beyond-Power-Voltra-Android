# Isokinetic Target Speed 1.0 -> 0.5

Source: `sysdiagnose_2026.04.15_19-25-55-0400_iPhone-OS_iPad_22F76`

User sequence:
- Open Isokinetic
- Set main-card speed to `1.0 m/s`
- Load weight
- Perform 3 reps
- Set main-card speed to `0.5 m/s`
- Perform 3 reps

Confirmed writes:
- Enter Isokinetic: `551204C7AA1013002000110100B04F0718B1`
- Set main target speed to `1.0 m/s`: `551504A9AA10140020001101005053E8030000D59B`
- Load: `55130403AA1016002000110100893E05008173`
- Set main target speed to `0.5 m/s`: `551504A9AA104A0020001101005053F4010000FF73`

Confirmed echoes / state:
- After entry, device reports `FITNESS_WORKOUT_STATE=7`
- During the first working block, `0x5350` echoes back as `E8030000` (`1000 mm/s`)
- During the second working block, `0x5350` echoes back as `F4010000` (`500 mm/s`)
- Loaded state continues to use `BP_SET_FITNESS_MODE=5`

Takeaway:
- `0x5350 EP_ISOKINETIC_TARGET_SPEED_MM_S` is the main-card Isokinetic speed control.
- `0x5411 ISOKINETIC_ECC_SPEED_LIMIT` should stay reserved for the eccentric settings sheet rather than the primary workout speed dial.
