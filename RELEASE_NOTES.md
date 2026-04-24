# Release Notes

## Alpha 1.4.5

Alpha 1.4.5 is a bigger step forward for real VOLTRA control from Android. Several areas that were previously rough, missing, or experimental are now usable enough for regular testing.

### What's New

- Added Custom Curve support.
- Added a visual curve builder with four editable points.
- Added drag-to-edit graph controls, optional fine-tune sliders, resistance range, and range-of-motion controls.
- Added local Custom Curve presets so good setups can be saved and loaded later.
- Added live Custom Curve stats for force, reps, and phase while the mode is loaded.

### Improved

- Isometric Test is now much closer to the VOLTRA's own results and is usable for strength-test pulls.
- Custom Curve behavior now tracks the iPad flow more closely, including range of motion and resistance range handling.
- Mode switching is cleaner across Custom Curve, Isometric Test, and the existing workout modes.
- Device name changes are implemented and working.
- Diagnostics are cleaner, with less cross-talk between Custom Curve and Isometric Test data.

### Still In Progress

- Startup image upload is still under development. The app now labels it that way and includes the newer crop workflow, but the VOLTRA still does not reliably apply the image.
- Custom Curve is functional, but still considered alpha while more iPad behavior is compared against Android.
- Row Mode has fresh capture notes, but it is not ready as a user-facing workout mode yet.

### Notes

This release includes a lot of protocol cleanup under the hood, but the important part is simple: Isometric Test is finally useful, Custom Curve is now testable, and personalization is moving forward.
