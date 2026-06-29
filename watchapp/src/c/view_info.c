#include "views.h"

// Extended info page (DOWN button): full instruction text plus lane / extended
// guidance when Google Maps provides it.
void view_info_draw(GContext *ctx, GRect bounds, const NavState *s) {
  int pad = 8;
  int x = bounds.origin.x + pad;
  int w = bounds.size.w - pad * 2;

  GFont title = fonts_get_system_font(FONT_KEY_GOTHIC_18_BOLD);
  ui_text(ctx, "DETAILS", title, GRect(x, bounds.origin.y + 2, w, 18), GTextAlignmentCenter);

  int y = bounds.origin.y + bounds.size.h * 14 / 100;

  GFont body = fonts_get_system_font(FONT_KEY_GOTHIC_24_BOLD);
  const char *instr = s->instruction[0] ? s->instruction : s->street;
  ui_text(ctx, instr[0] ? instr : "—", body, GRect(x, y, w, 72), GTextAlignmentLeft);

  if (s->lanes[0]) {
    GFont lf = fonts_get_system_font(FONT_KEY_GOTHIC_18);
    ui_text(ctx, s->lanes, lf,
            GRect(x, bounds.origin.y + bounds.size.h * 62 / 100, w, bounds.size.h * 36 / 100),
            GTextAlignmentLeft);
  }
}
