# Shared contract

Both the watchapp and the Android companion must agree on the **UUID**, the **AppMessage
keys**, and the **companion whitelist**. This file is the source of truth.

## UUID

```
c83491e7-5048-4959-aeef-fe05d7a2edd1
```

- Watchapp: `watchapp/package.json` → `pebble.uuid`
- Android: `PebbleForwarder.APP_UUID`

If you regenerate the UUID (`uuidgen`), change it in **both** places or the companion can't
reach the watchapp.

## AppMessage keys

PebbleKit Android 2 dictionaries are `Map<UInt, PebbleDictionaryItem>` — keys are **raw
integers**, not names. We use explicit integer keys on both sides (no `messageKeys`
auto-numbering). Defined once in `watchapp/src/c/keys.h`, mirrored in
`PebbleForwarder.Keys` (Kotlin). Keep the two in sync.

### phone → watch

| #  | Key            | Type    | Meaning                                                  |
|----|----------------|---------|----------------------------------------------------------|
| 1  | `NAV_ACTIVE`   | uint8   | 1 = navigating, 0 = stopped (watch shows idle screen)    |
| 2  | `MANEUVER`     | uint8   | Maneuver enum (below) — drives the vector arrow fallback |
| 3  | `ARROW_BITMAP` | bytes   | Packed 1-bpp 26×26 arrow rendered from Google's own icon |
| 4  | `DISTANCE`     | cstring | Distance to next turn, e.g. `"200 m"`                    |
| 5  | `STREET`       | cstring | Next road / headline instruction, e.g. `"Main St"`      |
| 6  | `INSTRUCTION`  | cstring | Full instruction text (info view)                        |
| 7  | `ETA`          | cstring | Arrival clock time, e.g. `"10:45"`                      |
| 8  | `DIST_REMAIN`  | cstring | Remaining trip distance, e.g. `"4.2 mi"`                |
| 9  | `TIME_REMAIN`  | cstring | Remaining trip time, e.g. `"18 min"`                    |
| 10 | `LANES`        | cstring | Lane / extended guidance text (info view, optional)      |

### watch → phone

| #  | Key             | Type  | Meaning                                                |
|----|-----------------|-------|--------------------------------------------------------|
| 11 | `REQUEST_STATE` | uint8 | Sent on watchapp launch; phone re-pushes last NavState |

The companion also re-pushes state from `onAppOpened()` (BasePebbleListenerService), so the
watch is populated even if `REQUEST_STATE` is missed.

## Companion whitelist (required)

PebbleKit Android 2 refuses to deliver AppMessages (`FailedNoPermissions`) unless the Android
package is whitelisted in the watchapp's `package.json` under `companionApp`. This repo uses
package **`com.jetboystudio.pebblenav`**, which must match `android/app/build.gradle.kts`
`applicationId` and `companionApp.android.apps[].package` in `watchapp/package.json`.

## Maneuver enum (`MANEUVER`)

Matches `ManeuverType` in `watchapp/src/c/navstate.h` (C) and `Maneuver` in `NavState.kt`.

| Value | Name         | Value | Name        |
|-------|--------------|-------|-------------|
| 0     | UNKNOWN      | 7     | SHARP_RIGHT |
| 1     | STRAIGHT     | 8     | UTURN       |
| 2     | TURN_LEFT    | 9     | MERGE       |
| 3     | TURN_RIGHT   | 10    | ROUNDABOUT  |
| 4     | SLIGHT_LEFT  | 11    | RAMP / EXIT |
| 5     | SLIGHT_RIGHT | 12    | DESTINATION |
| 6     | SHARP_LEFT   |       |             |

## ARROW_BITMAP packing

- 26×26 pixels, 1 bit per pixel, MSB-first within each byte.
- Rows are byte-aligned: each row is `ceil(26/8) = 4` bytes → `4 × 26 = 104` bytes total.
- Bit = 1 means "ink" (drawn). The watch renders ink in the foreground colour, rest
  transparent.
- Produced by `ManeuverIcon.kt`, consumed by `view_common.c` (`ui_draw_packed_arrow`).
