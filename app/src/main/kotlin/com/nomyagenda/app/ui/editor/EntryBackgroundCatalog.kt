package com.nomyagenda.app.ui.editor

import androidx.annotation.DrawableRes
import com.nomyagenda.app.R
import com.nomyagenda.app.ui.diary.DiaryBackgroundItem

object EntryBackgroundCatalog {

    val thematicBackgrounds: List<DiaryBackgroundItem> = listOf(
        DiaryBackgroundItem("", 0, R.string.diary_bg_none),
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
        DiaryBackgroundItem("spring", R.drawable.bg_festivity_spring, R.string.diary_bg_spring),
        DiaryBackgroundItem("birthday_illustrated", R.drawable.bg_festivity_birthday_illustrated, R.string.diary_bg_birthday_illustrated),
        DiaryBackgroundItem("christmas_illustrated", R.drawable.bg_festivity_christmas_illustrated, R.string.diary_bg_christmas_illustrated),
        DiaryBackgroundItem("halloween_illustrated", R.drawable.bg_festivity_halloween_illustrated, R.string.diary_bg_halloween_illustrated)
    )

    val premiumBackgrounds: List<DiaryBackgroundItem> = listOf(
        DiaryBackgroundItem("premium_paper_lavender", R.drawable.bg_premium_paper_lavender, R.string.bg_premium_paper_lavender),
        DiaryBackgroundItem("premium_botanical_soft", R.drawable.bg_premium_botanical_soft, R.string.bg_premium_botanical_soft),
        DiaryBackgroundItem("premium_dreamy_sky", R.drawable.bg_premium_dreamy_sky, R.string.bg_premium_dreamy_sky),
        DiaryBackgroundItem("premium_vintage_journal", R.drawable.bg_premium_vintage_journal, R.string.bg_premium_vintage_journal),
        DiaryBackgroundItem("premium_rose_romantic", R.drawable.bg_premium_rose_romantic, R.string.bg_premium_rose_romantic),
        DiaryBackgroundItem("premium_ocean_mist", R.drawable.bg_premium_ocean_mist, R.string.bg_premium_ocean_mist),
        DiaryBackgroundItem("premium_fox_autumn_illustrated", R.drawable.bg_premium_fox_autumn_illustrated, R.string.bg_premium_fox_autumn_illustrated)
    )

    val photoBackgrounds: List<DiaryBackgroundItem> = listOf(
        DiaryBackgroundItem("photo_fox", R.drawable.fox, R.string.diary_bg_photo_fox),
        DiaryBackgroundItem("photo_cumpleanos", R.drawable.cumpleanos, R.string.diary_bg_photo_cumpleanos),
        DiaryBackgroundItem("photo_navidad", R.drawable.navidad, R.string.diary_bg_photo_navidad),
        DiaryBackgroundItem("photo_halloween", R.drawable.halloween, R.string.diary_bg_photo_halloween),
        DiaryBackgroundItem("photo_ardilla", R.drawable.ardilla, R.string.diary_bg_photo_ardilla),
        DiaryBackgroundItem("photo_primavera", R.drawable.primavera, R.string.diary_bg_photo_primavera),
        DiaryBackgroundItem("photo_koala", R.drawable.koala, R.string.diary_bg_photo_koala),
        DiaryBackgroundItem("photo_hamster", R.drawable.hamster, R.string.diary_bg_photo_hamster),
        DiaryBackgroundItem("photo_invierno", R.drawable.invierno, R.string.diary_bg_photo_invierno),
        DiaryBackgroundItem("photo_otono", R.drawable.otono, R.string.diary_bg_photo_otono),
        DiaryBackgroundItem("photo_verano", R.drawable.verano, R.string.diary_bg_photo_verano)
    )

    private val allBackgrounds = thematicBackgrounds + festiveBackgrounds + premiumBackgrounds + photoBackgrounds

    @DrawableRes
    fun resolveDrawable(key: String): Int =
        allBackgrounds.firstOrNull { it.key == key }?.drawableRes ?: 0
}
