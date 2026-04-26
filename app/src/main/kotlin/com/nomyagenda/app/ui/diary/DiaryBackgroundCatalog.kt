package com.nomyagenda.app.ui.diary

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.nomyagenda.app.R

data class DiaryBackgroundItem(
    val key: String,
    @DrawableRes val drawableRes: Int,
    @StringRes val labelRes: Int
)

object DiaryBackgroundCatalog {

    val basicBackgrounds: List<DiaryBackgroundItem> = listOf(
        DiaryBackgroundItem("", 0, R.string.diary_bg_none),
        DiaryBackgroundItem("basic_cream", R.drawable.bg_basic_cream, R.string.diary_bg_basic_cream),
        DiaryBackgroundItem("basic_sky", R.drawable.bg_basic_sky, R.string.diary_bg_basic_sky),
        DiaryBackgroundItem("basic_mint", R.drawable.bg_basic_mint, R.string.diary_bg_basic_mint),
        DiaryBackgroundItem("basic_lavender", R.drawable.bg_basic_lavender, R.string.diary_bg_basic_lavender),
        DiaryBackgroundItem("basic_rose", R.drawable.bg_basic_rose, R.string.diary_bg_basic_rose)
    )

    val thematicBackgrounds: List<DiaryBackgroundItem> = listOf(
        DiaryBackgroundItem("floral", R.drawable.bg_illustration_floral, R.string.settings_bg_floral),
        DiaryBackgroundItem("stars", R.drawable.bg_illustration_stars, R.string.settings_bg_stars),
        DiaryBackgroundItem("geometric", R.drawable.bg_illustration_geometric, R.string.settings_bg_geometric),
        DiaryBackgroundItem("dots", R.drawable.bg_illustration_dots, R.string.settings_bg_dots),
        DiaryBackgroundItem("leaves", R.drawable.bg_illustration_leaves, R.string.settings_bg_leaves),
        DiaryBackgroundItem("butterfly", R.drawable.bg_illustration_butterfly, R.string.settings_bg_butterfly),
        DiaryBackgroundItem("mandala", R.drawable.bg_illustration_mandala, R.string.settings_bg_mandala),
        DiaryBackgroundItem("waves", R.drawable.bg_illustration_waves, R.string.settings_bg_waves),
        DiaryBackgroundItem("mountains", R.drawable.bg_illustration_mountains, R.string.settings_bg_mountains)
    )

    val festiveBackgrounds: List<DiaryBackgroundItem> = listOf(
        DiaryBackgroundItem("christmas", R.drawable.bg_festivity_christmas, R.string.diary_bg_christmas),
        DiaryBackgroundItem("halloween", R.drawable.bg_festivity_halloween, R.string.diary_bg_halloween),
        DiaryBackgroundItem("valentines", R.drawable.bg_festivity_valentines, R.string.diary_bg_valentines),
        DiaryBackgroundItem("valentines_roses", R.drawable.bg_festivity_valentines_roses, R.string.diary_bg_valentines_roses),
        DiaryBackgroundItem("valentines_pastel", R.drawable.bg_festivity_valentines_pastel, R.string.diary_bg_valentines_pastel),
        DiaryBackgroundItem("easter", R.drawable.bg_festivity_easter, R.string.diary_bg_easter),
        DiaryBackgroundItem("birthday", R.drawable.bg_festivity_birthday, R.string.diary_bg_birthday),
        DiaryBackgroundItem("birthday_pastel", R.drawable.bg_festivity_birthday_pastel, R.string.diary_bg_birthday_pastel),
        DiaryBackgroundItem("new_year", R.drawable.bg_festivity_new_year, R.string.diary_bg_new_year),
        DiaryBackgroundItem("spring", R.drawable.bg_festivity_spring, R.string.diary_bg_spring)
    )

    private val allBackgrounds: List<DiaryBackgroundItem> =
        basicBackgrounds + thematicBackgrounds + festiveBackgrounds

    @DrawableRes
    fun resolveDrawable(key: String): Int =
        allBackgrounds.firstOrNull { it.key == key }?.drawableRes ?: 0
}
