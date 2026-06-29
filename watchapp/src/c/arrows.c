#include "arrows.h"

// Vector maneuver arrows, drawn in a normalized 0..100 box mapped onto `frame`.
// Shafts are thick polylines; heads are filled triangles. Colour comes from the
// caller's current stroke/fill colour.

// Map a normalized point (0..100) into the frame.
static GPoint np(GRect f, int nx, int ny) {
  return GPoint(f.origin.x + (f.size.w * nx) / 100,
                f.origin.y + (f.size.h * ny) / 100);
}

// Filled triangular arrowhead at `tip`, pointing along unit-ish vector (dx,dy).
// `size` is in pixels (length of the head along its axis).
static void head(GContext *ctx, GPoint tip, int dx, int dy, int size) {
  // Cheap vector-length approximation (max + 0.41*min) so we avoid libm's sqrtf, which
  // pulls in newlib __errno that the Pebble link environment doesn't provide.
  int adx = dx < 0 ? -dx : dx;
  int ady = dy < 0 ? -dy : dy;
  float len = (adx > ady) ? (adx + 0.41f * ady) : (ady + 0.41f * adx);
  if (len < 0.001f) {
    return;
  }
  float ux = dx / len, uy = dy / len;   // axis (direction of travel)
  float px = -uy, py = ux;              // perpendicular
  float wing = size * 0.62f;

  GPoint base = GPoint((int)(tip.x - ux * size), (int)(tip.y - uy * size));
  GPoint a = GPoint((int)(base.x + px * wing), (int)(base.y + py * wing));
  GPoint b = GPoint((int)(base.x - px * wing), (int)(base.y - py * wing));

  GPathInfo info = {.num_points = 3, .points = (GPoint[]){tip, a, b}};
  GPath *path = gpath_create(&info);
  gpath_draw_filled(ctx, path);
  gpath_destroy(path);
}

// Thick polyline through `pts` (already in screen coordinates). `joint` is the
// radius of the filled dot drawn at interior corners so thick segments connect
// cleanly (draw_line has no round joins). Fill colour must match stroke colour.
static void shaft(GContext *ctx, const GPoint *pts, int n, int joint) {
  for (int i = 0; i < n - 1; i++) {
    graphics_draw_line(ctx, pts[i], pts[i + 1]);
    if (i > 0 && joint > 0) {
      graphics_fill_circle(ctx, pts[i], joint);
    }
  }
}

void arrows_draw(GContext *ctx, GRect frame, ManeuverType type) {
  // Make a square drawing box centered in the frame.
  int side = frame.size.w < frame.size.h ? frame.size.w : frame.size.h;
  GRect f = GRect(frame.origin.x + (frame.size.w - side) / 2,
                  frame.origin.y + (frame.size.h - side) / 2, side, side);

  int sw = side / 8;             // stroke width scales with size
  if (sw < 3) sw = 3;
  graphics_context_set_stroke_width(ctx, sw);
  int hs = side / 4;             // arrowhead size
  int jt = sw / 2;               // corner joint radius

  switch (type) {
    case MANEUVER_TURN_LEFT: {
      GPoint pts[] = {np(f, 70, 90), np(f, 70, 45), np(f, 22, 45)};
      shaft(ctx, pts, 3, jt);
      head(ctx, np(f, 12, 45), -1, 0, hs);
      break;
    }
    case MANEUVER_TURN_RIGHT: {
      GPoint pts[] = {np(f, 30, 90), np(f, 30, 45), np(f, 78, 45)};
      shaft(ctx, pts, 3, jt);
      head(ctx, np(f, 88, 45), 1, 0, hs);
      break;
    }
    case MANEUVER_SLIGHT_LEFT: {
      GPoint pts[] = {np(f, 60, 92), np(f, 60, 55), np(f, 32, 28)};
      shaft(ctx, pts, 3, jt);
      head(ctx, np(f, 24, 20), -1, -1, hs);
      break;
    }
    case MANEUVER_SLIGHT_RIGHT: {
      GPoint pts[] = {np(f, 40, 92), np(f, 40, 55), np(f, 68, 28)};
      shaft(ctx, pts, 3, jt);
      head(ctx, np(f, 76, 20), 1, -1, hs);
      break;
    }
    case MANEUVER_SHARP_LEFT: {
      GPoint pts[] = {np(f, 70, 90), np(f, 70, 50), np(f, 40, 50), np(f, 55, 22)};
      shaft(ctx, pts, 4, jt);
      head(ctx, np(f, 60, 12), 1, -2, hs);
      break;
    }
    case MANEUVER_SHARP_RIGHT: {
      GPoint pts[] = {np(f, 30, 90), np(f, 30, 50), np(f, 60, 50), np(f, 45, 22)};
      shaft(ctx, pts, 4, jt);
      head(ctx, np(f, 40, 12), -1, -2, hs);
      break;
    }
    case MANEUVER_UTURN: {
      GPoint pts[] = {np(f, 65, 92), np(f, 65, 45), np(f, 35, 45), np(f, 35, 78)};
      shaft(ctx, pts, 4, jt);
      head(ctx, np(f, 35, 90), 0, 1, hs);
      break;
    }
    case MANEUVER_ROUNDABOUT: {
      graphics_draw_circle(ctx, np(f, 50, 55), side / 5);
      GPoint pts[] = {np(f, 50, 92), np(f, 50, 75)};
      shaft(ctx, pts, 2, jt);
      head(ctx, np(f, 78, 30), 1, -1, hs);
      break;
    }
    case MANEUVER_DESTINATION: {
      // A simple map-pin: circle head over a point.
      graphics_fill_circle(ctx, np(f, 50, 40), side / 5);
      GPoint pts[] = {np(f, 50, 55), np(f, 50, 90)};
      shaft(ctx, pts, 2, jt);
      break;
    }
    case MANEUVER_MERGE:
    case MANEUVER_RAMP:
    case MANEUVER_STRAIGHT:
    case MANEUVER_UNKNOWN:
    default: {
      GPoint pts[] = {np(f, 50, 92), np(f, 50, 28)};
      shaft(ctx, pts, 2, jt);
      head(ctx, np(f, 50, 14), 0, -1, hs);
      break;
    }
  }
}
