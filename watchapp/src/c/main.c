#include <pebble.h>
#include "navstate.h"
#include "keys.h"
#include "views.h"

static Window *s_window;
static Layer *s_root;

// ---- rendering ----

static void root_update_proc(Layer *layer, GContext *ctx) {
  GRect bounds = layer_get_bounds(layer);
  graphics_context_set_fill_color(ctx, ui_bg());
  graphics_fill_rect(ctx, bounds, 0, GCornerNone);
  view_nav_draw(ctx, bounds, navstate_get());
}

// ---- AppMessage ----

static void inbox_received(DictionaryIterator *iter, void *context) {
  if (navstate_update_from_dict(iter)) {
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
