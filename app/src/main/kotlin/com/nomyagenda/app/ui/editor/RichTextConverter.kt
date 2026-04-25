package com.nomyagenda.app.ui.editor

import android.graphics.Color
import android.graphics.Typeface
import android.text.Editable
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan

/**
 * Converts between inline Markdown (bold `**`, italic `*`, `<font color>`) and
 * Android span objects so the editor can work in WYSIWYG toggle mode.
 *
 * Block-level prefixes (`# `, `- `, `1. `, `> `) are left as plain text in
 * the EditText and are therefore round-tripped transparently.
 */
object RichTextConverter {

    // Regex that matches the three inline formats the app uses.
    // Bold must come before italic so `**` is not accidentally consumed as two `*`.
    private val INLINE_PATTERN = Regex(
        """(?<bold>\*\*(?<boldContent>.+?)\*\*)|(?<italic>(?<!\*)\*(?!\*)(?<italicContent>.+?)(?<!\*)\*(?!\*))|(?<color><font color="(?<colorHex>#[0-9A-Fa-f]{6})">(?<colorContent>.+?)</font>)"""
    )

    // ---------- Markdown → SpannableStringBuilder ----------

    /**
     * Parses inline Markdown in [markdown] and returns a [SpannableStringBuilder]
     * with StyleSpans / ForegroundColorSpans applied.  Line breaks are preserved.
     */
    fun markdownInlineToSpannable(markdown: String): SpannableStringBuilder {
        val result = SpannableStringBuilder()
        val lines = markdown.split('\n')
        lines.forEachIndexed { idx, line ->
            if (idx > 0) result.append('\n')
            appendParsedLine(result, line)
        }
        return result
    }

    private fun appendParsedLine(sb: SpannableStringBuilder, line: String) {
        var lastEnd = 0
        INLINE_PATTERN.findAll(line).forEach { match ->
            // Plain text before the match
            if (match.range.first > lastEnd) {
                sb.append(line.substring(lastEnd, match.range.first))
            }
            val spanStart = sb.length
            val boldContent   = match.groups["boldContent"]?.value
            val italicContent = match.groups["italicContent"]?.value
            val colorContent  = match.groups["colorContent"]?.value
            val colorHex      = match.groups["colorHex"]?.value

            when {
                boldContent != null -> {
                    // **bold**
                    sb.append(boldContent)
                    sb.setSpan(
                        StyleSpan(Typeface.BOLD),
                        spanStart, sb.length,
                        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                    )
                }
                italicContent != null -> {
                    // *italic*
                    sb.append(italicContent)
                    sb.setSpan(
                        StyleSpan(Typeface.ITALIC),
                        spanStart, sb.length,
                        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                    )
                }
                colorContent != null && colorHex != null -> {
                    // <font color="...">text</font>
                    sb.append(colorContent)
                    try {
                        sb.setSpan(
                            ForegroundColorSpan(Color.parseColor(colorHex)),
                            spanStart, sb.length,
                            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                        )
                    } catch (_: IllegalArgumentException) { /* ignore bad color */ }
                }
            }
            lastEnd = match.range.last + 1
        }
        // Remaining plain text
        if (lastEnd < line.length) {
            sb.append(line.substring(lastEnd))
        }
    }

    // ---------- SpannableStringBuilder → Markdown ----------

    /**
     * Serialises an [Editable] that may contain [StyleSpan] (bold/italic) and
     * [ForegroundColorSpan] back to a Markdown string.  Plain text (including
     * block-level prefixes inserted by the toolbar) is written verbatim.
     */
    fun spannableToMarkdown(text: Editable): String {
        val length = text.length
        if (length == 0) return ""

        data class Event(val pos: Int, val close: Boolean, val marker: String)

        val events = mutableListOf<Event>()

        // Bold spans
        mergeRanges(
            text.getSpans(0, length, StyleSpan::class.java)
                .filter { it.style == Typeface.BOLD }
                .map { text.getSpanStart(it) to text.getSpanEnd(it) }
        ).forEach { (s, e) ->
            events += Event(s, false, "**")
            events += Event(e, true, "**")
        }

        // Italic spans
        mergeRanges(
            text.getSpans(0, length, StyleSpan::class.java)
                .filter { it.style == Typeface.ITALIC }
                .map { text.getSpanStart(it) to text.getSpanEnd(it) }
        ).forEach { (s, e) ->
            events += Event(s, false, "*")
            events += Event(e, true, "*")
        }

        // ForegroundColorSpan → <font color="..."> ... </font>
        text.getSpans(0, length, ForegroundColorSpan::class.java).forEach { span ->
            val s = text.getSpanStart(span)
            val e = text.getSpanEnd(span)
            if (s < e) {
                val hex = "#%06X".format(span.foregroundColor and 0xFFFFFF)
                events += Event(s, false, "<font color=\"$hex\">")
                events += Event(e, true, "</font>")
            }
        }

        // Sort: ascending position; at same position, opens before closes
        events.sortWith(compareBy({ it.pos }, { if (it.close) 1 else 0 }))

        val raw = text.toString()
        val sb = StringBuilder(length + events.size * 4)
        var lastPos = 0
        events.forEach { ev ->
            if (ev.pos > lastPos) sb.append(raw, lastPos, ev.pos)
            sb.append(ev.marker)
            lastPos = ev.pos
        }
        if (lastPos < length) sb.append(raw, lastPos, length)

        return sb.toString()
    }

    // ---------- helpers ----------

    /** Merges overlapping or adjacent [ranges] and returns a sorted, disjoint list. */
    private fun mergeRanges(ranges: List<Pair<Int, Int>>): List<Pair<Int, Int>> {
        val sorted = ranges.filter { (s, e) -> s < e }.sortedBy { it.first }
        if (sorted.isEmpty()) return emptyList()
        val result = mutableListOf<Pair<Int, Int>>()
        var (curS, curE) = sorted.first()
        sorted.drop(1).forEach { (s, e) ->
            if (s <= curE) curE = maxOf(curE, e)
            else { result += curS to curE; curS = s; curE = e }
        }
        result += curS to curE
        return result
    }
}
