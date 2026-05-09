package com.nomyagenda.app.ui.editor

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.text.Editable
import android.text.Spannable
import android.text.TextWatcher
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import android.view.Gravity
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import com.google.android.material.button.MaterialButton
import com.nomyagenda.app.R
import com.nomyagenda.app.databinding.IncludeEditorContentSectionBinding
import com.nomyagenda.app.databinding.IncludeEditorCustomizationSectionBinding
import com.nomyagenda.app.databinding.IncludeEditorTitleSectionBinding
import com.nomyagenda.app.ui.common.color.ColorPalette
import com.nomyagenda.app.ui.common.font.FontCatalog
import com.nomyagenda.app.ui.diary.DiaryBackgroundItem
import com.nomyagenda.app.ui.resolveThemeColor
import com.google.android.material.R as MaterialR

/**
 * Shared helper that wires up all WYSIWYG rich-text editing logic:
 * title/content format toolbars, colour swatches, font picker, and a
 * dynamically-configured background picker.
 *
 * Both [EntryEditorFragment] and [DiaryEntryEditorFragment] create an instance
 * of this class and delegate all shared editor work to it, keeping only their
 * domain-specific UI (type chips, checklist, mood, photos, …) in their own
 * fragment code.
 *
 * @param context       Fragment context (used for dialogs, resources, ContextCompat).
 * @param titleBinding  Binding for `include_editor_title_section.xml`.
 * @param contentBinding Binding for `include_editor_content_section.xml`.
 * @param customBinding  Binding for `include_editor_customization_section.xml`.
 */
class RichTextEditorHelper(
    private val context: Context,
    private val titleBinding: IncludeEditorTitleSectionBinding,
    private val contentBinding: IncludeEditorContentSectionBinding,
    private val customBinding: IncludeEditorCustomizationSectionBinding
) {

    // ──────────────────────────────────────────────────────────────────────
    // WYSIWYG state – title
    // ──────────────────────────────────────────────────────────────────────

    private var isTitleBoldActive   = false
    private var isTitleItalicActive = false
    private var titleLastInsertStart = 0
    private var titleLastInsertCount = 0
    private var isTitleApplyingSpans = false

    // ──────────────────────────────────────────────────────────────────────
    // WYSIWYG state – content
    // ──────────────────────────────────────────────────────────────────────

    private var isBoldActive   = false
    private var isItalicActive = false
    private var activeTextColor: Int? = null
    private var lastInsertStart = 0
    private var lastInsertCount = 0
    private var isApplyingSpans = false

    // ──────────────────────────────────────────────────────────────────────
    // Font-size button lists (populated in setup())
    // ──────────────────────────────────────────────────────────────────────

    private var defaultTitleTextSize   = 0f
    private var defaultContentTextSize = 0f
    private lateinit var titleSizeButtons:   List<Pair<MaterialButton, Float>>
    private lateinit var contentSizeButtons: List<Pair<MaterialButton, Float>>

    // All dynamically-created background swatch containers (for selection sync)
    private val allBgContainers = mutableListOf<LinearLayout>()

    // ──────────────────────────────────────────────────────────────────────
    // Callbacks – invoked when the user changes a style property
    // ──────────────────────────────────────────────────────────────────────

    var onTitleColorChanged:       (String) -> Unit = {}
    var onContentColorChanged:     (String) -> Unit = {}
    var onFontFamilyChanged:       (String) -> Unit = {}
    var onBackgroundChanged:       (String) -> Unit = {}
    var onTitleFontSizeChanged:    (Float)  -> Unit = {}
    var onContentFontSizeChanged:  (Float)  -> Unit = {}

    // ──────────────────────────────────────────────────────────────────────
    // TextWatcher – title
    // ──────────────────────────────────────────────────────────────────────

    private val titleFormatWatcher = object : TextWatcher {
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            if (!isTitleApplyingSpans) {
                titleLastInsertStart = start
                titleLastInsertCount = count
            }
        }
        override fun afterTextChanged(s: Editable?) {
            if (s == null || titleLastInsertCount == 0 || isTitleApplyingSpans) return
            isTitleApplyingSpans = true
            try {
                val end = titleLastInsertStart + titleLastInsertCount
                if (isTitleBoldActive)
                    s.setSpan(StyleSpan(Typeface.BOLD), titleLastInsertStart, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                if (isTitleItalicActive)
                    s.setSpan(StyleSpan(Typeface.ITALIC), titleLastInsertStart, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
            } finally {
                isTitleApplyingSpans = false
            }
        }
    }

    // ──────────────────────────────────────────────────────────────────────
    // TextWatcher – content
    // ──────────────────────────────────────────────────────────────────────

    private val contentFormatWatcher = object : TextWatcher {
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            if (!isApplyingSpans) {
                lastInsertStart = start
                lastInsertCount = count
            }
        }
        override fun afterTextChanged(s: Editable?) {
            if (s == null || lastInsertCount == 0 || isApplyingSpans) return
            isApplyingSpans = true
            try {
                val end = lastInsertStart + lastInsertCount
                if (isBoldActive)
                    s.setSpan(StyleSpan(Typeface.BOLD), lastInsertStart, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                if (isItalicActive)
                    s.setSpan(StyleSpan(Typeface.ITALIC), lastInsertStart, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                activeTextColor?.let { color ->
                    s.setSpan(ForegroundColorSpan(color), lastInsertStart, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                }
            } finally {
                isApplyingSpans = false
            }
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    // Setup
    // ══════════════════════════════════════════════════════════════════════

    /**
     * Wires up all format toolbars, pickers, and watchers.
     * Call once from [Fragment.onViewCreated] before accessing any other method.
     */
    fun setup() {
        defaultTitleTextSize   = titleBinding.editorEditTitle.textSize   / context.resources.displayMetrics.scaledDensity
        defaultContentTextSize = contentBinding.editorEditContent.textSize / context.resources.displayMetrics.scaledDensity

        titleSizeButtons = listOf(
            titleBinding.editorBtnTitleSize12 to 12f,
            titleBinding.editorBtnTitleSize14 to 14f,
            titleBinding.editorBtnTitleSize16 to 16f,
            titleBinding.editorBtnTitleSize18 to 18f,
            titleBinding.editorBtnTitleSize20 to 20f,
            titleBinding.editorBtnTitleSize24 to 24f
        )
        contentSizeButtons = listOf(
            contentBinding.editorBtnContentSize12 to 12f,
            contentBinding.editorBtnContentSize14 to 14f,
            contentBinding.editorBtnContentSize16 to 16f,
            contentBinding.editorBtnContentSize18 to 18f,
            contentBinding.editorBtnContentSize20 to 20f,
            contentBinding.editorBtnContentSize24 to 24f
        )

        setupTitleToolbar()
        setupContentToolbar()
        setupColorSwatches()
        setupFontPicker()
    }

    // ──────────────────────────────────────────────────────────────────────
    // Private – toolbar wiring
    // ──────────────────────────────────────────────────────────────────────

    private fun setupTitleToolbar() {
        titleBinding.editorEditTitle.addTextChangedListener(titleFormatWatcher)

        titleBinding.editorBtnTitleBold.setOnClickListener {
            toggleTitleInlineFormat(Typeface.BOLD)
        }
        titleBinding.editorBtnTitleItalic.setOnClickListener {
            toggleTitleInlineFormat(Typeface.ITALIC)
        }

        setupFontSizeButtons(titleSizeButtons, isTitle = true)
    }

    private fun setupContentToolbar() {
        contentBinding.editorEditContent.addTextChangedListener(contentFormatWatcher)

        contentBinding.editorBtnContentBold.setOnClickListener {
            toggleContentInlineFormat(Typeface.BOLD)
        }
        contentBinding.editorBtnContentItalic.setOnClickListener {
            toggleContentInlineFormat(Typeface.ITALIC)
        }
        contentBinding.editorBtnContentColor.setOnClickListener {
            showContentColorPicker()
        }

        setupFontSizeButtons(contentSizeButtons, isTitle = false)
    }

    private fun setupFontSizeButtons(buttons: List<Pair<MaterialButton, Float>>, isTitle: Boolean) {
        buttons.forEach { (btn, size) ->
            btn.setOnClickListener {
                val nowChecked = btn.isChecked
                buttons.forEach { (b, _) -> b.isChecked = false }
                if (nowChecked) {
                    btn.isChecked = true
                    if (isTitle) {
                        onTitleFontSizeChanged(size)
                        titleBinding.editorEditTitle.textSize = size
                    } else {
                        onContentFontSizeChanged(size)
                        contentBinding.editorEditContent.textSize = size
                    }
                } else {
                    if (isTitle) {
                        onTitleFontSizeChanged(0f)
                        titleBinding.editorEditTitle.textSize = defaultTitleTextSize
                    } else {
                        onContentFontSizeChanged(0f)
                        contentBinding.editorEditContent.textSize = defaultContentTextSize
                    }
                }
            }
        }
    }

    // ──────────────────────────────────────────────────────────────────────
    // Private – WYSIWYG toggle helpers
    // ──────────────────────────────────────────────────────────────────────

    private fun toggleTitleInlineFormat(style: Int) {
        val editText = titleBinding.editorEditTitle
        val text = editText.text ?: return
        val selStart = editText.selectionStart.coerceAtLeast(0)
        val selEnd   = editText.selectionEnd.coerceAtLeast(0)

        if (selStart != selEnd) {
            val existing = text.getSpans(selStart, selEnd, StyleSpan::class.java)
                .filter { it.style == style
                        && text.getSpanStart(it) >= selStart
                        && text.getSpanEnd(it) <= selEnd }
            if (existing.isNotEmpty()) existing.forEach { text.removeSpan(it) }
            else text.setSpan(StyleSpan(style), selStart, selEnd, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
            // Restore button state to reflect typing-mode, not this selection op
            when (style) {
                Typeface.BOLD   -> titleBinding.editorBtnTitleBold.isChecked   = isTitleBoldActive
                Typeface.ITALIC -> titleBinding.editorBtnTitleItalic.isChecked = isTitleItalicActive
            }
        } else {
            when (style) {
                Typeface.BOLD -> {
                    isTitleBoldActive = !isTitleBoldActive
                    titleBinding.editorBtnTitleBold.isChecked = isTitleBoldActive
                }
                Typeface.ITALIC -> {
                    isTitleItalicActive = !isTitleItalicActive
                    titleBinding.editorBtnTitleItalic.isChecked = isTitleItalicActive
                }
            }
        }
    }

    private fun toggleContentInlineFormat(style: Int) {
        val editText = contentBinding.editorEditContent
        val text = editText.text ?: return
        val selStart = editText.selectionStart.coerceAtLeast(0)
        val selEnd   = editText.selectionEnd.coerceAtLeast(0)

        if (selStart != selEnd) {
            val existing = text.getSpans(selStart, selEnd, StyleSpan::class.java)
                .filter { it.style == style
                        && text.getSpanStart(it) >= selStart
                        && text.getSpanEnd(it) <= selEnd }
            if (existing.isNotEmpty()) existing.forEach { text.removeSpan(it) }
            else text.setSpan(StyleSpan(style), selStart, selEnd, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
            when (style) {
                Typeface.BOLD   -> contentBinding.editorBtnContentBold.isChecked   = isBoldActive
                Typeface.ITALIC -> contentBinding.editorBtnContentItalic.isChecked = isItalicActive
            }
        } else {
            when (style) {
                Typeface.BOLD -> {
                    isBoldActive = !isBoldActive
                    contentBinding.editorBtnContentBold.isChecked = isBoldActive
                }
                Typeface.ITALIC -> {
                    isItalicActive = !isItalicActive
                    contentBinding.editorBtnContentItalic.isChecked = isItalicActive
                }
            }
        }
    }

    // ──────────────────────────────────────────────────────────────────────
    // Private – WYSIWYG inline colour picker (content only)
    // ──────────────────────────────────────────────────────────────────────

    private fun showContentColorPicker() {
        val dialogView = android.view.LayoutInflater.from(context).inflate(R.layout.dialog_color_picker, null)
        val grid = dialogView.findViewById<android.widget.GridLayout>(R.id.grid_colors)

        val dialog = AlertDialog.Builder(context)
            .setTitle(R.string.color_picker_title)
            .setView(dialogView)
            .setNegativeButton(R.string.cancel, null)
            .create()

        val size        = context.resources.getDimensionPixelSize(R.dimen.color_swatch_size)
        val margin      = context.resources.getDimensionPixelSize(R.dimen.color_swatch_margin)

        ColorPalette.COLORS.forEach { hexColor ->
            val circle = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(Color.parseColor(hexColor))
            }
            val swatch = FrameLayout(context).apply {
                background = circle
                layoutParams = android.widget.GridLayout.LayoutParams().apply {
                    width = size; height = size
                    setMargins(margin, margin, margin, margin)
                }
                setOnClickListener {
                    applyContentTextColor(hexColor)
                    dialog.dismiss()
                }
            }
            grid.addView(swatch)
        }

        dialog.show()
    }

    private fun applyContentTextColor(hexColor: String) {
        val editText = contentBinding.editorEditContent
        val text     = editText.text ?: return
        val color = try { Color.parseColor(hexColor) } catch (_: IllegalArgumentException) { return }
        val selStart = editText.selectionStart.coerceAtLeast(0)
        val selEnd   = editText.selectionEnd.coerceAtLeast(0)

        if (selStart != selEnd) {
            text.setSpan(ForegroundColorSpan(color), selStart, selEnd, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
            activeTextColor = null
            contentBinding.editorBtnContentColor.isChecked = false
        } else {
            if (activeTextColor == color) {
                activeTextColor = null
                contentBinding.editorBtnContentColor.isChecked = false
            } else {
                activeTextColor = color
                contentBinding.editorBtnContentColor.isChecked = true
            }
        }
    }

    // ──────────────────────────────────────────────────────────────────────
    // Private – colour swatches
    // ──────────────────────────────────────────────────────────────────────

    private fun setupColorSwatches() {
        setupColorSwatchRow(
            container = customBinding.editorColorSwatchesTitle,
            onSelect  = { hex ->
                applyTextColorToView(titleBinding.editorEditTitle, hex)
                updateSwatchSelection(customBinding.editorColorSwatchesTitle, hex)
                onTitleColorChanged(hex)
            }
        )
        setupColorSwatchRow(
            container = customBinding.editorColorSwatchesContent,
            onSelect  = { hex ->
                applyTextColorToView(contentBinding.editorEditContent, hex)
                updateSwatchSelection(customBinding.editorColorSwatchesContent, hex)
                onContentColorChanged(hex)
            }
        )
        updateSwatchSelection(customBinding.editorColorSwatchesTitle, "")
        updateSwatchSelection(customBinding.editorColorSwatchesContent, "")
    }

    private fun setupColorSwatchRow(container: LinearLayout, onSelect: (String) -> Unit) {
        val size        = context.resources.getDimensionPixelSize(R.dimen.color_swatch_size)
        val margin      = context.resources.getDimensionPixelSize(R.dimen.color_swatch_margin)
        val strokeWidth = context.resources.getDimensionPixelSize(R.dimen.color_swatch_stroke_width)

        // "None" swatch
        val noneSwatch = FrameLayout(context).apply {
            tag = ""
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(ContextCompat.getColor(context, android.R.color.transparent))
                setStroke(strokeWidth, Color.LTGRAY)
            }
            layoutParams = LinearLayout.LayoutParams(size, size).apply {
                setMargins(margin, margin, margin, margin)
            }
            setOnClickListener { onSelect("") }
        }
        container.addView(noneSwatch)

        ColorPalette.COLORS.forEach { hexColor ->
            val swatch = FrameLayout(context).apply {
                tag = hexColor
                background = GradientDrawable().apply {
                    shape = GradientDrawable.OVAL
                    setColor(Color.parseColor(hexColor))
                }
                layoutParams = LinearLayout.LayoutParams(size, size).apply {
                    setMargins(margin, margin, margin, margin)
                }
                setOnClickListener { onSelect(hexColor) }
            }
            container.addView(swatch)
        }
    }

    private fun updateSwatchSelection(container: LinearLayout, hexColor: String) {
        val strokeWidth = context.resources.getDimensionPixelSize(R.dimen.color_swatch_stroke_width)
        for (i in 0 until container.childCount) {
            val swatch = container.getChildAt(i) as? FrameLayout ?: continue
            val swatchColor = swatch.tag as? String ?: ""
            val isSelected  = swatchColor == hexColor
            swatch.background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                if (swatchColor.isEmpty()) {
                    setColor(ContextCompat.getColor(context, android.R.color.transparent))
                    setStroke(strokeWidth, if (isSelected) Color.BLACK else Color.LTGRAY)
                } else {
                    setColor(Color.parseColor(swatchColor))
                    if (isSelected) setStroke(strokeWidth, Color.WHITE)
                }
            }
        }
    }

    // ──────────────────────────────────────────────────────────────────────
    // Private – font picker
    // ──────────────────────────────────────────────────────────────────────

    private fun setupFontPicker() {
        FontCatalog.fonts.forEach { fontItem ->
            val btn = MaterialButton(
                context,
                null,
                MaterialR.attr.materialButtonOutlinedStyle
            ).apply {
                tag = fontItem.id
                text = fontItem.displayName
                isCheckable = true
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    val m = context.resources.getDimensionPixelSize(R.dimen.color_swatch_margin)
                    setMargins(m, 0, m, 0)
                }
                setOnClickListener {
                    updateFontSelection(fontItem.id)
                    applyFontToViews(fontItem.id)
                    onFontFamilyChanged(fontItem.id)
                }
            }
            customBinding.editorFontPickerContainer.addView(btn)
            FontCatalog.resolveAsync(context, fontItem.id) { typeface ->
                btn.typeface = typeface
            }
        }
    }

    private fun updateFontSelection(fontId: String) {
        val container = customBinding.editorFontPickerContainer
        for (i in 0 until container.childCount) {
            val btn = container.getChildAt(i) as? MaterialButton ?: continue
            btn.isChecked = btn.tag as? String == fontId
        }
    }

    private fun applyFontToViews(fontId: String) {
        FontCatalog.resolveAsync(context, fontId) { typeface ->
            titleBinding.editorEditTitle.typeface   = typeface
            contentBinding.editorEditContent.typeface = typeface
        }
    }

    // ──────────────────────────────────────────────────────────────────────
    // Private – background picker
    // ──────────────────────────────────────────────────────────────────────

    private fun setupBackgroundSwatches(container: LinearLayout, backgrounds: List<DiaryBackgroundItem>) {
        val size        = context.resources.getDimensionPixelSize(R.dimen.bg_swatch_size)
        val margin      = context.resources.getDimensionPixelSize(R.dimen.color_swatch_margin)
        val strokeWidth = context.resources.getDimensionPixelSize(R.dimen.color_swatch_stroke_width)
        val cornerRadius = context.resources.getDimension(R.dimen.card_corner_radius)

        backgrounds.forEach { item ->
            val swatch = FrameLayout(context).apply {
                tag = item.key
                layoutParams = LinearLayout.LayoutParams(size, size).apply {
                    setMargins(margin, margin, margin, margin)
                }
                clipToOutline = true
                outlineProvider = android.view.ViewOutlineProvider.BACKGROUND
                if (item.drawableRes != 0) {
                    background = try {
                        ContextCompat.getDrawable(context, item.drawableRes)
                    } catch (_: Exception) {
                        GradientDrawable().apply {
                            shape = GradientDrawable.RECTANGLE
                            cornerRadii = FloatArray(8) { cornerRadius }
                            setColor(Color.LTGRAY)
                        }
                    }
                } else {
                    background = GradientDrawable().apply {
                        shape = GradientDrawable.RECTANGLE
                        cornerRadii = FloatArray(8) { cornerRadius }
                        setColor(ContextCompat.getColor(context, android.R.color.transparent))
                        setStroke(strokeWidth, Color.LTGRAY)
                    }
                    addView(TextView(context).apply {
                        text = "✕"
                        textSize = 18f
                        gravity = Gravity.CENTER
                        setTextColor(Color.LTGRAY)
                        layoutParams = FrameLayout.LayoutParams(
                            FrameLayout.LayoutParams.MATCH_PARENT,
                            FrameLayout.LayoutParams.MATCH_PARENT
                        )
                    })
                }
                setOnClickListener {
                    updateBackgroundSelection(item.key)
                    onBackgroundChanged(item.key)
                }
            }
            container.addView(swatch)
        }
    }

    private fun updateBackgroundSelection(selectedKey: String) {
        val strokeWidth  = context.resources.getDimensionPixelSize(R.dimen.color_swatch_stroke_width)
        val cornerRadius = context.resources.getDimension(R.dimen.card_corner_radius)

        allBgContainers.forEach { container ->
            for (i in 0 until container.childCount) {
                val swatch = container.getChildAt(i) as? FrameLayout ?: continue
                val key        = swatch.tag as? String ?: continue
                val isSelected = key == selectedKey
                if (isSelected) {
                    swatch.foreground = GradientDrawable().apply {
                        shape = GradientDrawable.RECTANGLE
                        cornerRadii = FloatArray(8) { cornerRadius }
                        setStroke(strokeWidth, context.resolveThemeColor(MaterialR.attr.colorPrimary))
                        setColor(ContextCompat.getColor(context, android.R.color.transparent))
                    }
                } else {
                    swatch.foreground = null
                }
            }
        }
    }

    // ──────────────────────────────────────────────────────────────────────
    // Private – shared colour helper
    // ──────────────────────────────────────────────────────────────────────

    private fun applyTextColorToView(view: android.widget.TextView, hexColor: String) {
        if (hexColor.isNotEmpty()) {
            try { view.setTextColor(Color.parseColor(hexColor)) }
            catch (_: IllegalArgumentException) { /* ignore invalid colour */ }
        } else {
            view.setTextColor(context.resolveThemeColor(MaterialR.attr.colorOnBackground))
        }
    }

    // ──────────────────────────────────────────────────────────────────────
    // Private – font-size button helper
    // ──────────────────────────────────────────────────────────────────────

    private fun updateFontSizeSelection(buttons: List<Pair<MaterialButton, Float>>, size: Float) {
        buttons.forEach { (btn, s) ->
            btn.isChecked = size > 0f && s == size
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    // Public API
    // ══════════════════════════════════════════════════════════════════════

    // ── Title ──

    fun getTitle(): String =
        titleBinding.editorEditTitle.text
            ?.let { RichTextConverter.spannableToMarkdown(it) } ?: ""

    fun setTitle(markdown: String) {
        val spannable = RichTextConverter.markdownInlineToSpannable(markdown)
        titleBinding.editorEditTitle.setText(spannable, android.widget.TextView.BufferType.EDITABLE)
    }

    fun setTitleColor(hex: String) {
        updateSwatchSelection(customBinding.editorColorSwatchesTitle, hex)
        applyTextColorToView(titleBinding.editorEditTitle, hex)
    }

    fun setTitleFontSize(size: Float) {
        updateFontSizeSelection(titleSizeButtons, size)
        titleBinding.editorEditTitle.textSize = if (size > 0f) size else defaultTitleTextSize
    }

    // ── Content ──

    fun getContent(): String =
        contentBinding.editorEditContent.text
            ?.let { RichTextConverter.spannableToMarkdown(it) } ?: ""

    fun setContent(markdown: String) {
        val spannable = RichTextConverter.markdownInlineToSpannable(markdown)
        contentBinding.editorEditContent.setText(spannable, android.widget.TextView.BufferType.EDITABLE)
    }

    fun setContentColor(hex: String) {
        updateSwatchSelection(customBinding.editorColorSwatchesContent, hex)
        applyTextColorToView(contentBinding.editorEditContent, hex)
    }

    fun setContentFontSize(size: Float) {
        updateFontSizeSelection(contentSizeButtons, size)
        contentBinding.editorEditContent.textSize = if (size > 0f) size else defaultContentTextSize
    }

    /** Clears all active content format toggles (call before entering preview mode). */
    fun resetContentFormatToggles() {
        isBoldActive   = false
        isItalicActive = false
        activeTextColor = null
        contentBinding.editorBtnContentBold.isChecked   = false
        contentBinding.editorBtnContentItalic.isChecked = false
        contentBinding.editorBtnContentColor.isChecked  = false
    }

    // ── Shared style ──

    fun setFontFamily(fontId: String) {
        updateFontSelection(fontId)
        applyFontToViews(fontId)
    }

    fun setBackground(key: String) {
        updateBackgroundSelection(key)
    }

    /**
     * Populates the background picker with the given categories.
     * Each pair is a (labelText, listOfBackgroundItems).
     * Call after [setup] from the host fragment.
     */
    fun setBackgroundCategories(categories: List<Pair<String, List<DiaryBackgroundItem>>>) {
        val container = customBinding.editorBgCategoriesContainer
        container.removeAllViews()
        allBgContainers.clear()

        val margin      = context.resources.getDimensionPixelSize(R.dimen.color_swatch_margin)
        val smallSpacing = context.resources.getDimensionPixelSize(R.dimen.spacing_small)

        categories.forEach { (label, items) ->
            // Category label
            container.addView(TextView(context).apply {
                text = label
                setTextAppearance(context, com.google.android.material.R.style.TextAppearance_Material3_LabelSmall)
                setTextColor(context.resolveThemeColor(MaterialR.attr.colorOnSurfaceVariant))
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { setMargins(0, margin, 0, margin) }
            })

            // Horizontal scroll + swatch row
            val swatchRow = LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
            }
            val scroll = android.widget.HorizontalScrollView(context).apply {
                isHorizontalScrollBarEnabled = false
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { bottomMargin = smallSpacing }
            }
            scroll.addView(swatchRow)
            container.addView(scroll)

            setupBackgroundSwatches(swatchRow, items)
            allBgContainers.add(swatchRow)
        }
    }

    // ── Accessors for host-fragment validation ──

    /** Returns the title TextInputLayout so the host can set/clear error messages. */
    fun getTitleInputLayout() = titleBinding.editorTitleInputLayout
}
