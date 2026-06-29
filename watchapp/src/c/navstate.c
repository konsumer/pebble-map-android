#include "navstate.h"
#include "keys.h"

static NavState s_state;

NavState *navstate_get(void) {
  return &s_state;
}

// Copy a cstring tuple into dest (NUL-terminated, bounded). Returns true if changed.
static bool copy_str(char *dest, const Tuple *t) {
  if (!t) {
    return false;
  }
  if (strncmp(dest, t->value->cstring, NAV_STR_LEN) == 0) {
    return false;
  }
  strncpy(dest, t->value->cstring, NAV_STR_LEN - 1);
  dest[NAV_STR_LEN - 1] = '\0';
  return true;
}

bool navstate_update_from_dict(DictionaryIterator *iter) {
  bool changed = false;

  Tuple *t = dict_find(iter, KEY_NAV_ACTIVE);
  if (t) {
    bool active = t->value->uint8 != 0;
    if (active != s_state.active) {
      s_state.active = active;
      changed = true;
    }
    // A fresh "stop" should drop the stale arrow so the next trip starts clean.
    if (!active) {
      s_state.has_arrow = false;
    }
  }

  t = dict_find(iter, KEY_MANEUVER);
  if (t && t->value->uint8 != s_state.maneuver) {
    s_state.maneuver = (ManeuverType)t->value->uint8;
    changed = true;
  }

  t = dict_find(iter, KEY_ARROW_BITMAP);
  if (t && t->length >= ARROW_BYTES) {
    if (!s_state.has_arrow || memcmp(s_state.arrow, t->value->data, ARROW_BYTES) != 0) {
      memcpy(s_state.arrow, t->value->data, ARROW_BYTES);
      s_state.has_arrow = true;
      changed = true;
    }
  }

  changed |= copy_str(s_state.distance, dict_find(iter, KEY_DISTANCE));
  changed |= copy_str(s_state.street, dict_find(iter, KEY_STREET));
  changed |= copy_str(s_state.instruction, dict_find(iter, KEY_INSTRUCTION));
  changed |= copy_str(s_state.eta, dict_find(iter, KEY_ETA));
  changed |= copy_str(s_state.dist_remain, dict_find(iter, KEY_DIST_REMAIN));
  changed |= copy_str(s_state.time_remain, dict_find(iter, KEY_TIME_REMAIN));
  changed |= copy_str(s_state.lanes, dict_find(iter, KEY_LANES));

  return changed;
}
