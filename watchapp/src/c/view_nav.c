#include "views.h"
#include "arrows.h"

// Core navigation page: big maneuver arrow on top, distance-to-turn, then street.
void view_nav_draw(GContext *ctx, GRect bounds, const NavState *s) {
  if (!s->active) {
    GFont f = fonts_get_system_font(FONT_KEY_GOTHIC_24_BOLD);
    GRect r = GRect(bounds.origin.x + 6, bounds.origin.y + bounds.size.h / 2 - 30,
                    bounds.size.w - 12, 60);
    ui_text(ctx, "Waiting for\ndirections…", f, r, GTextAlignmentCenter);
    return;
  }

  int w = bounds.size.w;
  int h = bounds.size.h;

  // Arrow occupies the top ~52% of the screen.
  GRect arrow_box = GRect(bounds.origin.x, bounds.origin.y + h * 4 / 100, w, h * 50 / 100);
  graphics_context_set_stroke_color(ctx, ui_fg());
  graphics_context_set_fill_color(ctx, ui_fg());
  if (s->has_arrow) {
    ui_draw_packed_arrow(ctx, arrow_box, s->arrow);
  } else {
    arrows_draw(ctx, arrow_box, s->maneuver);
  }

  // Distance to the turn — the single most glanceable number, so make it big.
  GFont df = fonts_get_system_font(FONT_KEY_BITHAM_30_BLACK);
  GRect dr = GRect(bounds.origin.x + 4, bounds.origin.y + h * 54 / 100, w - 8, 36);
  ui_text(ctx, s->distance[0] ? s->distance : "", df, dr, GTextAlignmentCenter);

  // Street / headline instruction.
  GFont sf = fonts_get_system_font(FONT_KEY_GOTHIC_24_BOLD);
  GRect sr = GRect(bounds.origin.x + 4, bounds.origin.y + h * 76 / 100, w - 8, h * 24 / 100);
  ui_text(ctx, s->street[0] ? s->street : s->instruction, sf, sr, GTextAlignmentCenter);
}
