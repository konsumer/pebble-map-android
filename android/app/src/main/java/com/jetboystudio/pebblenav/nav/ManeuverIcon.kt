package com.jetboystudio.pebblenav.nav

import android.graphics.Bitmap
import android.graphics.Color

/**
 * Packs the Google Maps maneuver icon into the watch's arrow format: 26×26, 1 bit per
 * pixel, MSB-first, 4 bytes/row = 104 bytes (see "ARROW_BITMAP packing" in shared-contract.md).
 *
 * "Ink" = a pixel that differs from the background. We sample the top-left corner as the
 * background colour (covers both transparent and solid-colour icons) and mark a bit when a
 * pixel is sufficiently different — robust whether the arrow is white-on-transparent or
 * white-on-colour.
 */
object ManeuverIcon {
    const val W = 26
    const val H = 26
    const val ROW_BYTES = 4
    const val SIZE = ROW_BYTES * H // 104

    private const val DIFF_THRESHOLD = 64 * 64 // squared distance over ARGB

    fun pack(src: Bitmap?): ByteArray? {
        if (src == null) return null
        return try {
            // getPixel() throws on Config.HARDWARE bitmaps (common for notification icons),
            // so always work from a software ARGB_8888 copy.
            val software =
                if (src.config == Bitmap.Config.HARDWARE) src.copy(Bitmap.Config.ARGB_8888, false)
                else src
            val scaled = Bitmap.createScaledBitmap(software, W, H, true)
            val out = ByteArray(SIZE)
            val bg = scaled.getPixel(0, 0)
            for (y in 0 until H) {
                for (x in 0 until W) {
                    if (isInk(scaled.getPixel(x, y), bg)) {
                        out[y * ROW_BYTES + (x ushr 3)] =
                            (out[y * ROW_BYTES + (x ushr 3)].toInt() or (1 shl (7 - (x and 7)))).toByte()
                    }
                }
            }
            if (scaled !== software) scaled.recycle()
            if (software !== src) software.recycle()
            out
        } catch (e: Throwable) {
            null // arrow is best-effort; the vector fallback still renders on the watch
        }
    }

    private fun isInk(px: Int, bg: Int): Boolean {
        val da = Color.alpha(px) - Color.alpha(bg)
        val dr = Color.red(px) - Color.red(bg)
        val dg = Color.green(px) - Color.green(bg)
        val db = Color.blue(px) - Color.blue(bg)
        // Mostly-transparent pixels are never ink, regardless of colour difference.
        if (Color.alpha(px) < 24) return false
        return (da * da + dr * dr + dg * dg + db * db) > DIFF_THRESHOLD
    }
}
