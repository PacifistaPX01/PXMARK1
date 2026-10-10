package com.josue.systeme30

import android.content.Context
import android.graphics.Color

object Theme {
    // 10 teintes qui se succèdent : une par jour, en boucle
    private val HUES = floatArrayOf(215f, 265f, 320f, 28f, 48f, 95f, 145f, 170f, 190f, 245f)

    fun hue(ctx: Context): Float {
        val i = Store.dayIndex(ctx)
        return HUES[((i % 10) + 10) % 10]
    }

    fun c(h: Float, s: Float, v: Float): Int =
        Color.HSVToColor(floatArrayOf(((h % 360f) + 360f) % 360f, s, v))
}
