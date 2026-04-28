package com.nomyagenda.app.core.datetime

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

private val DATE_KEY_PARSE_FORMAT = SimpleDateFormat("yyyy-MM-dd", Locale.US)
private val DIARY_DATE_DISPLAY_FORMAT = SimpleDateFormat("EEEE, d 'de' MMMM 'de' yyyy", Locale("es"))

fun Long.toDateKey(): String {
    val cal = Calendar.getInstance()
    cal.timeInMillis = this
    return "%04d-%02d-%02d".format(
        cal.get(Calendar.YEAR),
        cal.get(Calendar.MONTH) + 1,
        cal.get(Calendar.DAY_OF_MONTH)
    )
}

fun formatDiaryDateKey(dateKey: String): String {
    return try {
        val date = DATE_KEY_PARSE_FORMAT.parse(dateKey) ?: return dateKey
        DIARY_DATE_DISPLAY_FORMAT.format(date).replaceFirstChar { it.uppercase() }
    } catch (_: Exception) {
        dateKey
    }
}
