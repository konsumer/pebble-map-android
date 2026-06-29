#include "views.h"
#include "arrows.h"

// Join the available trip fields ("18 min", "4.2 mi", "10:45") into one line, separated
// by spaces (font-safe — no reliance on a middot glyph). Empty fields are skipped.
static void build_trip(const NavState *s, char *out, int n) {
  out[0] = '\0';
  const char *parts[3] = {s->time_remain, s->dist_remain, s->eta};
  for (int i = 0; i < 3; i++) {
    if (parts[i][0]) {
      if (out[0]) {
        strncat(out, "  ", n - strlen(out) - 1);
      }
      strncat(out, parts[i], n - strlen(out) - 1);
    }
  }
}

// Single combined screen: maneuver arrow, distance to the turn, street, and a trip summary
// line. Everything that is available is shown at once — no page switching.
void view_nav_draw(GContext *ctx, GRect bounds, const NavState *s) {
  if (!s->active) {
    GFont f = fonts_get_system_font(FONT_KEY_GOTHIC_24_BOLD);
    GRect r = GRect(bounds.origin.x + 6, bounds.origin.y + bounds.size.h / 2 - 30,
                    bounds.size.w - 12, 60);
    ui_text(ctx, "Waiting for\ndirections…", f, r, GTextAlignmentCenter);
    return;
  }

  int x = bounds.origin.x;
  int w = bounds.size.w;
  int h = bounds.size.h;

  // Arrow — top ~38% of the screen.
  GRect arrow_box = GRect(x, bounds.origin.y + h * 3 / 100, w, h * 38 / 100);
  graphics_context_set_stroke_color(ctx, ui_fg());
  graphics_context_set_fill_color(ctx, ui_fg());
  if (s->has_arrow) {
    ui_draw_packed_arrow(ctx, arrow_box, s->arrow);
  } else {
    arrows_draw(ctx, arrow_box, s->maneuver);
  }

  // Distance to the turn — the most glanceable number, so the biggest text.
  GFont df = fonts_get_system_font(FONT_KEY_BITHAM_30_BLACK);
  GRect dr = GRect(x + 4, bounds.origin.y + h * 41 / 100, w - 8, h * 22 / 100);
  ui_text(ctx, s->distance[0] ? s->distance : "", df, dr, GTextAlignmentCenter);

  // Street / headline instruction.
  GFont sf = fonts_get_system_font(FONT_KEY_GOTHIC_24_BOLD);
  GRect sr = GRect(x + 4, bounds.origin.y + h * 62 / 100, w - 8, h * 20 / 100);
  ui_text(ctx, s->street[0] ? s->street : s->instruction, sf, sr, GTextAlignmentCenter);

  // Trip summary line — time left, distance left, ETA (whatever is available).
  char trip[NAV_STR_LEN * 2];
  build_trip(s, trip, sizeof(trip));
  if (trip[0]) {
    GFont tf = fonts_get_system_font(FONT_KEY_GOTHIC_18);
    GRect tr = GRect(x + 2, bounds.origin.y + h * 83 / 100, w - 4, h * 17 / 100);
    ui_text(ctx, trip, tf, tr, GTextAlignmentCenter);
  }
}
