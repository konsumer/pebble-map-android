#pragma once
#include <pebble.h>
#include "navstate.h"

// Each view renders the whole screen for one of the three pages. `bounds` is the
// root layer's bounds (already platform-correct: rect or round, any size).
void view_nav_draw(GContext *ctx, GRect bounds, const NavState *s);
void view_map_draw(GContext *ctx, GRect bounds, const NavState *s);
void view_info_draw(GContext *ctx, GRect bounds, const NavState *s);

// ---- shared helpers (view_common.c) ----

// Foreground (ink) / background colours. Background is black so the bright arrow
// and white text read well at a glance while driving; on B&W this is just black/white.
GColor ui_bg(void);
GColor ui_fg(void);

// Draw the 26x26 packed 1bpp arrow (see shared-contract.md) scaled to fill `box`,
// in the foreground colour. Used when NavState.has_arrow is true.
void ui_draw_packed_arrow(GContext *ctx, GRect box, const uint8_t *data);

// Draw `text` centered horizontally within `frame` using `font`, foreground colour.
// Returns the height actually used so callers can stack rows.
void ui_text(GContext *ctx, const char *text, GFont font, GRect frame, GTextAlignment align);

// A small all-caps label above a larger value, both left-aligned within `frame`.
// Used by the map/info overview rows.
void ui_label_value(GContext *ctx, GRect frame, const char *label, const char *value);
