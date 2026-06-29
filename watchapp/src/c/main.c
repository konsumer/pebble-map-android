#include <pebble.h>
#include "navstate.h"
#include "keys.h"
#include "views.h"

typedef enum {
  VIEW_NAV = 0,
  VIEW_MAP = 1,
  VIEW_INFO = 2,
  VIEW_COUNT = 3,
} ViewId;

static Window *s_window;
static Layer *s_root;
static ViewId s_view = VIEW_NAV;

// ---- rendering ----

// Three small dots showing the active page, drawn at the bottom edge.
static void draw_page_dots(GContext *ctx, GRect bounds) {
  int gap = 12;
  int total = gap * (VIEW_COUNT - 1);
  int cx = bounds.origin.x + bounds.size.w / 2 - total / 2;
  int cy = bounds.origin.y + bounds.size.h - 8;
  for (int i = 0; i < VIEW_COUNT; i++) {
    GPoint p = GPoint(cx + i * gap, cy);
    if (i == s_view) {
      graphics_context_set_fill_color(ctx, ui_fg());
      graphics_fill_circle(ctx, p, 3);
    } else {
      graphics_context_set_stroke_color(ctx, ui_fg());
      graphics_draw_circle(ctx, p, 3);
    }
  }
}

static void root_update_proc(Layer *layer, GContext *ctx) {
  GRect bounds = layer_get_bounds(layer);
  graphics_context_set_fill_color(ctx, ui_bg());
  graphics_fill_rect(ctx, bounds, 0, GCornerNone);

  const NavState *s = navstate_get();
  switch (s_view) {
    case VIEW_MAP:  view_map_draw(ctx, bounds, s); break;
    case VIEW_INFO: view_info_draw(ctx, bounds, s); break;
    case VIEW_NAV:
    default:        view_nav_draw(ctx, bounds, s); break;
  }
  draw_page_dots(ctx, bounds);
}

static void set_view(ViewId v) {
  s_view = v;
  layer_mark_dirty(s_root);
}

// ---- buttons ----  UP = overview, DOWN = details, SELECT = back to nav.

static void up_click(ClickRecognizerRef r, void *ctx) { set_view(VIEW_MAP); }
static void down_click(ClickRecognizerRef r, void *ctx) { set_view(VIEW_INFO); }
static void select_click(ClickRecognizerRef r, void *ctx) { set_view(VIEW_NAV); }

static void click_config(void *context) {
  window_single_click_subscribe(BUTTON_ID_UP, up_click);
  window_single_click_subscribe(BUTTON_ID_DOWN, down_click);
  window_single_click_subscribe(BUTTON_ID_SELECT, select_click);
}

// ---- AppMessage ----

static void inbox_received(DictionaryIterator *iter, void *context) {
  bool changed = navstate_update_from_dict(iter);

  // A fresh maneuver (phone always includes MANEUVER on a turn update) snaps the
  // driver back to the nav page even if they were browsing the overview/details.
  if (dict_find(iter, KEY_MANEUVER) || dict_find(iter, KEY_NAV_ACTIVE)) {
    s_view = VIEW_NAV;
    changed = true;
  }
  if (changed) {
    layer_mark_dirty(s_root);
  }
}

static void inbox_dropped(AppMessageResult reason, void *context) {
  APP_LOG(APP_LOG_LEVEL_WARNING, "inbox dropped: %d", reason);
}

// On launch, ask the phone to re-push the current trip state (handles opening the
// watchapp after navigation has already started).
static void request_state(void) {
  DictionaryIterator *out;
  if (app_message_outbox_begin(&out) == APP_MSG_OK) {
    dict_write_uint8(out, KEY_REQUEST_STATE, 1);
    app_message_outbox_send();
  }
}

// ---- lifecycle ----

static void window_load(Window *window) {
  Layer *wl = window_get_root_layer(window);
  s_root = layer_create(layer_get_bounds(wl));
  layer_set_update_proc(s_root, root_update_proc);
  layer_add_child(wl, s_root);
}

static void window_unload(Window *window) {
  layer_destroy(s_root);
}

static void init(void) {
  s_window = window_create();
  window_set_background_color(s_window, ui_bg());
  window_set_click_config_provider(s_window, click_config);
  window_set_window_handlers(s_window, (WindowHandlers){
    .load = window_load,
    .unload = window_unload,
  });

  app_message_register_inbox_received(inbox_received);
  app_message_register_inbox_dropped(inbox_dropped);
  // Inbox must hold the 104-byte arrow plus several short strings in one message;
  // 512 leaves comfortable headroom for tuple overhead.
  app_message_open(512, 128);

  window_stack_push(s_window, true);
  request_state();
}

static void deinit(void) {
  window_destroy(s_window);
}

int main(void) {
  init();
  app_event_loop();
  deinit();
}
