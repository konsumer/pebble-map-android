#pragma once
#include <pebble.h>

// Maneuver enum — must match the table in shared-contract.md and NavState.kt (Android).
typedef enum {
  MANEUVER_UNKNOWN = 0,
  MANEUVER_STRAIGHT = 1,
  MANEUVER_TURN_LEFT = 2,
  MANEUVER_TURN_RIGHT = 3,
  MANEUVER_SLIGHT_LEFT = 4,
  MANEUVER_SLIGHT_RIGHT = 5,
  MANEUVER_SHARP_LEFT = 6,
  MANEUVER_SHARP_RIGHT = 7,
  MANEUVER_UTURN = 8,
  MANEUVER_MERGE = 9,
  MANEUVER_ROUNDABOUT = 10,
  MANEUVER_RAMP = 11,
  MANEUVER_DESTINATION = 12,
} ManeuverType;

#define NAV_STR_LEN 48

// 26x26, 1bpp, 4 bytes/row -> 104 bytes. See "ARROW_BITMAP packing" in shared-contract.md.
#define ARROW_W 26
#define ARROW_H 26
#define ARROW_ROW_BYTES 4
#define ARROW_BYTES (ARROW_ROW_BYTES * ARROW_H)

typedef struct {
  bool active;          // NAV_ACTIVE
  ManeuverType maneuver;
  bool has_arrow;       // true once an ARROW_BITMAP has been received this trip
  uint8_t arrow[ARROW_BYTES];
  char distance[NAV_STR_LEN];     // "200 m"
  char street[NAV_STR_LEN];       // "Main St"
  char instruction[NAV_STR_LEN];  // full headline
  char eta[NAV_STR_LEN];          // "10:45"
  char dist_remain[NAV_STR_LEN];  // "4.2 mi"
  char time_remain[NAV_STR_LEN];  // "18 min"
  char lanes[NAV_STR_LEN];        // extended / lane text
} NavState;

// Singleton accessor for the current navigation state.
NavState *navstate_get(void);

// Apply an incoming AppMessage dictionary to the singleton state.
// Only keys present in the dict are modified. Returns true if anything changed.
bool navstate_update_from_dict(DictionaryIterator *iter);

// Seed the state with a sample turn, for emulator screenshots (see DEMO_MODE in main.c).
void navstate_load_demo(void);
