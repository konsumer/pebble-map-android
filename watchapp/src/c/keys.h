#pragma once

// AppMessage dictionary keys. These are RAW integer keys used verbatim on both
// sides (no messageKeys auto-numbering), so the Android companion must use the
// exact same numbers — see Keys in PebbleForwarder.kt and shared-contract.md.

// phone -> watch
#define KEY_NAV_ACTIVE    1   // uint8: 1 navigating, 0 stopped
#define KEY_MANEUVER      2   // uint8: ManeuverType enum
#define KEY_ARROW_BITMAP  3   // bytes: 104-byte packed 1bpp 26x26 arrow
#define KEY_DISTANCE      4   // cstring: "200 m"
#define KEY_STREET        5   // cstring: "Main St"
#define KEY_INSTRUCTION   6   // cstring
#define KEY_ETA           7   // cstring: "10:45"
#define KEY_DIST_REMAIN   8   // cstring: "4.2 mi"
#define KEY_TIME_REMAIN   9   // cstring: "18 min"
#define KEY_LANES         10  // cstring

// watch -> phone
#define KEY_REQUEST_STATE 11  // uint8: 1 = re-push current state
