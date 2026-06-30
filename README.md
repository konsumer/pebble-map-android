# Pebble Maps Nav

[![CI](https://github.com/konsumer/pebble-map-android/actions/workflows/ci.yml/badge.svg)](https://github.com/konsumer/pebble-map-android/actions/workflows/ci.yml)

Turn-by-turn Google Maps directions on your Pebble. Start navigation in Google Maps on your
Android phone and the next turn — arrow, distance, and street — pops up on the watch
automatically. Built for the relaunched, open-source Pebble (primary target **Pebble Time 2**,
works across Pebble platforms).

I wanted a simple & open-source turn-direction (and eventually map) app for pebble. This is not fancy, but you can evaluate how it works (for security.) I tested on latest Graphene on a Pixel 10 Pro.

## Usage

### Install

Two parts: the **watchapp** (on the watch) and the **Android companion** (on the phone).

**Watchapp** — install **Maps Nav** from the [Rebble appstore](https://apps.rebble.io/) (or
open the `.pbw` from a [release](https://github.com/konsumer/pebble-map-android/releases) in
the Pebble app).

**Android companion** — no Google Play needed:

- **Obtainium (easiest, auto-updates):** install
  [Obtainium](https://github.com/ImranR98/Obtainium), then *Add App* with this URL:
  `https://github.com/konsumer/pebble-map-android`. It installs the latest signed APK and
  keeps it updated.
- **IzzyOnDroid (F-Droid repo):** add the IzzyOnDroid repo
  (`https://apt.izzysoft.de/fdroid/repo`) to the [F-Droid](https://f-droid.org) client, then
  search for **Pebble Maps Nav**.
- **Manual:** download `pebble-maps-nav.apk` from the
  [latest release](https://github.com/konsumer/pebble-map-android/releases/latest).

Then grant notification access (see [Permissions](#permissions)) and start driving directions
in Google Maps.

### Files

I wanted this to be very source-tracable, so you can see exactly how the file was built in Github CI. This allows you to verify I didn't do something sneaky, but also not require you to build it yourself. The exact process used to build them is [here](https://github.com/konsumer/pebble-map-android/blob/main/.github/workflows/ci.yml)

- Click on latest green action-run [here](https://github.com/konsumer/pebble-map-android/actions)
- scroll to bottom (after inspecting run, etc) and download `app-debug-apk` and `watchapp-pbw` to your phone. they are zips, so extract them in your file-browser. Install the APK, and open the pbw in Pebble.

You can also find the APK on any [release](https://github.com/konsumer/pebble-map-android/releases) for easier download.


### Permissions

On Graphene/Android, permissions are tricky for self-installed APKs. I did this:

1. Settings → Apps → Pebble Maps Nav (the app's info page).
2. Tap the ⋮ three‑dot menu in the top‑right corner.
3. Tap "Allow restricted settings."
4. Go back to "notifications read, reply & control" → now the toggle for Pebble Maps Nav will enable. Confirm the warning dialog.


At some point, I may publish to play-store, but for now, this is basically just for me & my friends that have Pebbles.


## How it works

Google has **no public live-navigation API** — the Directions API only returns a static
route, not the GPS-synced "in 200 m, turn left" stream. The only way to mirror live Google
Maps guidance is to read the **navigation notification** Maps posts while navigating. So:

```
Google Maps (phone)
   │  posts ongoing navigation notification (custom RemoteViews)
   ▼
NotificationListenerService  ──►  parse turn / distance / street / ETA + maneuver icon
   │
   ▼
PebbleKit Android 2  ──►  launches the watchapp + sends an AppMessage each update
   ▼
Watchapp (C)  ──►  one screen: arrow, distance, street, trip summary
```

The maneuver **arrow** is the actual icon Google draws (downsampled and forwarded), so it
stays correct regardless of language or new maneuver types. The text (distance/street/ETA) is
parsed best-effort; if a field is ever missed, the arrow still shows.

## Repository layout

| Path                | What                                                              |
|---------------------|------------------------------------------------------------------|
| `shared-contract.md`| UUID, AppMessage keys, maneuver enum, arrow packing — read first  |
| `watchapp/`         | Pebble watchapp in C (adaptive across aplite/basalt/chalk/diorite/emery) |
| `android/`          | Android companion (Kotlin)                                        |

## Build & install — watchapp

The watchapp builds with the modern Rebble toolchain (the original SDK was Python 2).

**Option A — rebbletool (local SDK):**
```bash
pip install rebbletool          # provides the `pebble` command on Python 3
cd watchapp
pebble build
pebble install --phone <PHONE_IP>   # or sideload the .pbw via the new Pebble app
```

**Option B — Docker:**
```bash
cd watchapp
docker run --rm -v "$PWD":/app rebble/pebble-sdk pebble build
```

The build produces `watchapp/build/pebble-gmaps-nav.pbw`. Install it on the watch with the
new Pebble mobile app (Settings → sideload / "Install app from file").

## Build & install — Android companion

Requires Android Studio or a local Gradle + Android SDK. PebbleKit Android 2 is pulled from
Maven Central (`io.rebble.pebblekit2:client`).

```bash
cd android
# If you don't have the gradle wrapper jar, generate it once: `gradle wrapper`
./gradlew assembleDebug          # build the APK
./gradlew test                   # run NavParser unit tests
adb install app/build/outputs/apk/debug/app-debug.apk
```

Then on the phone:
1. Open **Pebble Maps Nav**.
2. Tap **Grant Notification Access** and enable the app (this lets it read the Maps
   navigation notification).
3. Tap **Test send to watch** to confirm the watch link works.
4. Start driving directions in Google Maps — the watchapp launches and updates itself.

## On the watch

A single screen shows everything that's available at once: the maneuver arrow, distance to
the turn, the street, and a trip-summary line (time left · distance left · ETA). Press
**Back** to exit.

## Verifying it works

- `cd android && ./gradlew test` — parser tests over real-notification text fixtures.
- Watchapp compiles clean for every target platform (`pebble build`).
- **Test send** button exercises phone→watch delivery and all three views without driving.
- On-device: start Google Maps navigation → watch shows and updates each turn; ending
  navigation returns the watch to its watchface.

**Continuous integration:** `.github/workflows/ci.yml` builds both halves on every push/PR —
the Android job runs `assembleDebug` + `test` (and uploads the debug APK), and the watchapp
job runs `pebble build` inside the `rebble/pebble-sdk` Docker image. The badge above is your
green/red signal.

## Limitations & notes

- **iOS is not supported** for this feature. iOS sandboxes notifications, so no app can read
  Google Maps' navigation notification. There is no equivalent path; this is Android-only by
  platform design, not by choice.
- **Notification scraping is inherently fragile.** If Google changes the navigation
  notification layout, text parsing may need updating (resource names live in
  `GMapsNotificationReader.kt`). The arrow icon is the most robust signal and is forwarded
  directly.
- **The "map" screen is a textual overview, not real map tiles.** The notification carries no
  route geometry to draw, and a real static-map render would need an API key and your live
  location, which we don't have.
- Keep the watchapp UUID and the AppMessage key numbers in sync across
  `watchapp/src/c/keys.h`, `watchapp/package.json`, and `PebbleForwarder.kt` — see
  `shared-contract.md`.

## Credits

- Google Maps notification parsing approach and resource names are modelled on
  [3v1n0/GMapsParser](https://github.com/3v1n0/GMapsParser) (LGPL-3.0).
- [PebbleKit Android 2](https://github.com/pebble-dev/PebbleKitAndroid2) for phone↔watch
  communication, and the [Rebble](https://developer.rebble.io/) project for the open-source
  Pebble SDK.
