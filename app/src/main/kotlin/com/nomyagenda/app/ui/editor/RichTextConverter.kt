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

    // Matches <font color> tags whose content may span multiple lines.
    // Used only by normalizeMultilineFontTags() to repair legacy stored data.
    private val MULTILINE_FONT_REGEX = Regex(
        """<font color="(#[0-9A-Fa-f]{6})">(.*?)</font>""",
        RegexOption.DOT_MATCHES_ALL
    )

    // ---------- Markdown → SpannableStringBuilder ----------

    /**
     * Parses inline Markdown in [markdown] and returns a [SpannableStringBuilder]
     * with StyleSpans / ForegroundColorSpans applied.  Line breaks are preserved.
     */
    fun markdownInlineToSpannable(markdown: String): SpannableStringBuilder {
        val normalized = normalizeMultilineFontTags(markdown)
        val result = SpannableStringBuilder()
        val lines = normalized.split('\n')
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
     *
     * Spans that cross a newline character are closed before the `\n` and
     * re-opened immediately after, so every output line is self-contained and
     * the line-by-line parser can handle it without seeing raw tag fragments.
     */
    fun spannableToMarkdown(text: Editable): String {
        val length = text.length
        if (length == 0) return ""

        val raw = text.toString()
        val sb = StringBuilder()
        val boldRanges = mergeRanges(
            text.getSpans(0, length, StyleSpan::class.java)
                .filter { it.style == Typeface.BOLD }
                .map { text.getSpanStart(it) to text.getSpanEnd(it) }
        )
        val italicRanges = mergeRanges(
            text.getSpans(0, length, StyleSpan::class.java)
                .filter { it.style == Typeface.ITALIC }
                .map { text.getSpanStart(it) to text.getSpanEnd(it) }
        )

        var activeBold = false
        var activeItalic = false
        var activeColorHex: String? = null

        for (i in 0 until length) {
            val ch = raw[i]

            if (ch == '\n') {
                if (activeColorHex != null) {
                    sb.append("</font>")
                    activeColorHex = null
                }
                if (activeItalic) {
                    sb.append("*")
                    activeItalic = false
                }
                if (activeBold) {
                    sb.append("**")
                    activeBold = false
                }
                sb.append('\n')
                continue
            }

            val hasBold = isInAnyRange(i, boldRanges)
            val hasItalic = isInAnyRange(i, italicRanges)
            val colorHex = text.getSpans(i, i + 1, ForegroundColorSpan::class.java)
                .firstOrNull { text.getSpanStart(it) <= i && text.getSpanEnd(it) > i }
                ?.let { "#%06X".format(it.foregroundColor and 0xFFFFFF) }

            if (activeColorHex != null && activeColorHex != colorHex) {
                sb.append("</font>")
                activeColorHex = null
            }
            if (activeItalic && !hasItalic) {
                sb.append("*")
                activeItalic = false
            }
            if (activeBold && !hasBold) {
                sb.append("**")
                activeBold = false
            }

            if (!activeBold && hasBold) {
                sb.append("**")
                activeBold = true
            }
            if (!activeItalic && hasItalic) {
                sb.append("*")
                activeItalic = true
            }
            if (activeColorHex == null && colorHex != null) {
                sb.append("<font color=\"$colorHex\">")
                activeColorHex = colorHex
            }

            sb.append(ch)
        }

        if (activeColorHex != null) sb.append("</font>")
        if (activeItalic) sb.append("*")
        if (activeBold) sb.append("**")

        return sb.toString()
    }

    private fun isInAnyRange(index: Int, ranges: List<Pair<Int, Int>>): Boolean =
        ranges.any { (s, e) -> index in s until e }

    // ---------- Plain-text extraction ----------

    /**
     * Strips all inline Markdown/HTML formatting (bold `**`, italic `*`, `<font color>`)
     * and returns the plain text content.  Useful for truncated previews where cutting
     * the raw markdown string mid-tag would leave visible tag fragments.
     */
    fun stripInlineMarkdown(markdown: String): String {
        val normalized = normalizeMultilineFontTags(markdown)
        val sb = StringBuilder()
        val lines = normalized.split('\n')
        lines.forEachIndexed { idx, line ->
            if (idx > 0) sb.append('\n')
            var lastEnd = 0
            INLINE_PATTERN.findAll(line).forEach { match ->
                if (match.range.first > lastEnd) {
                    sb.append(line.substring(lastEnd, match.range.first))
                }
                sb.append(
                    match.groups["boldContent"]?.value
                        ?: match.groups["italicContent"]?.value
                        ?: match.groups["colorContent"]?.value
                        ?: ""
                )
                lastEnd = match.range.last + 1
            }
            if (lastEnd < line.length) sb.append(line.substring(lastEnd))
        }
        return sb.toString()
    }

    // ---------- helpers ----------

    /**
     * Splits any `<font color>` tags whose content spans multiple lines into
     * equivalent per-line tags.  This lets the line-by-line parser handle text
     * that was previously serialised while a colour span crossed a newline
     * (e.g. the user selected two lines and applied a colour, or typed with a
     * colour active and pressed Enter).
     *
     * Example:
     *   `<font color="#FF0000">hello\nworld</font>`
     *   → `<font color="#FF0000">hello</font>\n<font color="#FF0000">world</font>`
     *
     * Empty lines inside the tag produce a bare newline with no surrounding tags,
     * which is safe for the parser.
     */
    private fun normalizeMultilineFontTags(text: String): String {
        if ('<' !in text) return text   // fast path: no HTML tags at all
        return MULTILINE_FONT_REGEX.replace(text) { match ->
            val color   = match.groupValues[1]
            val content = match.groupValues[2]
            if ('\n' !in content) {
                match.value // single-line — no change needed
            } else {
                content.split('\n').joinToString("\n") { line ->
                    if (line.isEmpty()) "" else "<font color=\"$color\">$line</font>"
                }
            }
        }
    }

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
