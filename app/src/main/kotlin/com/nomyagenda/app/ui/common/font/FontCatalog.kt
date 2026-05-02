package com.nomyagenda.app.ui.common.font

import android.content.Context
import android.graphics.Typeface
import androidx.core.content.res.ResourcesCompat
import com.nomyagenda.app.R

data class FontItem(
    val id: String,
    val displayName: String,
    val fontResId: Int
)

object FontCatalog {

    const val DEFAULT_ID = ""

    val fonts: List<FontItem> = listOf(
        FontItem("",               "Predeterminada",  0),
        FontItem("nunito",         "Nunito",          R.font.nunito),
        FontItem("pacifico",       "Pacifico",        R.font.pacifico),
        FontItem("lato",           "Lato",            R.font.lato),
        FontItem("merriweather",   "Merriweather",    R.font.merriweather),
        FontItem("dancing_script", "Dancing Script",  R.font.dancing_script),
        FontItem("roboto_mono",    "Roboto Mono",     R.font.roboto_mono),
        FontItem("lobster",        "Lobster",         R.font.lobster)
    )

    /** Returns the [Typeface] for [fontId], or null for the system default. */
    fun resolve(context: Context, fontId: String): Typeface? {
        val item = fonts.firstOrNull { it.id == fontId } ?: return null
        if (item.fontResId == 0) return null
        return try {
            ResourcesCompat.getFont(context, item.fontResId)
        } catch (_: Exception) {
            null
        }
    }
}
