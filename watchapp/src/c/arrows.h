#pragma once
#include <pebble.h>
#include "navstate.h"

// Draw a vector maneuver arrow for `type`, centered in `frame`, in the current
// stroke/fill colour. Used as a fallback when no ARROW_BITMAP has been received.
void arrows_draw(GContext *ctx, GRect frame, ManeuverType type);
