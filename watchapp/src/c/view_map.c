#include "views.h"

// "Map"/overview page (UP button): trip-level info — ETA, remaining time/distance.
// Note: this is a textual overview, not real map tiles; the Google Maps notification
// gives us no route geometry to draw (see README "Out of scope").
void view_map_draw(GContext *ctx, GRect bounds, const NavState *s) {
  int pad = 8;
  int x = bounds.origin.x + pad;
  int w = bounds.size.w - pad * 2;
  int y = bounds.origin.y + bounds.size.h * 8 / 100;
  int row = (bounds.size.h * 84 / 100) / 3;

  GFont title = fonts_get_system_font(FONT_KEY_GOTHIC_18_BOLD);
  ui_text(ctx, "OVERVIEW", title, GRect(x, bounds.origin.y + 2, w, 18), GTextAlignmentCenter);

  ui_label_value(ctx, GRect(x, y, w, row), "ARRIVAL",
                 s->eta[0] ? s->eta : "—");
  ui_label_value(ctx, GRect(x, y + row, w, row), "TIME LEFT",
                 s->time_remain[0] ? s->time_remain : "—");
  ui_label_value(ctx, GRect(x, y + row * 2, w, row), "DISTANCE LEFT",
                 s->dist_remain[0] ? s->dist_remain : "—");
}
