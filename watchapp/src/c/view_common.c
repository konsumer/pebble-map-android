#include "views.h"

GColor ui_bg(void) {
  return GColorBlack;
}

GColor ui_fg(void) {
  return GColorWhite;
}

void ui_draw_packed_arrow(GContext *ctx, GRect box, const uint8_t *data) {
  int side = box.size.w < box.size.h ? box.size.w : box.size.h;
  int scale = side / ARROW_W;
  if (scale < 1) {
    scale = 1;
  }
  int draw = scale * ARROW_W;
  int ox = box.origin.x + (box.size.w - draw) / 2;
  int oy = box.origin.y + (box.size.h - draw) / 2;

  graphics_context_set_fill_color(ctx, ui_fg());
  for (int y = 0; y < ARROW_H; y++) {
    for (int x = 0; x < ARROW_W; x++) {
      // MSB-first within each byte; row stride is ARROW_ROW_BYTES.
      uint8_t byte = data[y * ARROW_ROW_BYTES + (x >> 3)];
      if ((byte >> (7 - (x & 7))) & 1) {
        graphics_fill_rect(ctx,
                           GRect(ox + x * scale, oy + y * scale, scale, scale),
                           0, GCornerNone);
      }
    }
  }
}

void ui_text(GContext *ctx, const char *text, GFont font, GRect frame, GTextAlignment align) {
  graphics_context_set_text_color(ctx, ui_fg());
  graphics_draw_text(ctx, text, font, frame, GTextOverflowModeTrailingEllipsis, align, NULL);
}
