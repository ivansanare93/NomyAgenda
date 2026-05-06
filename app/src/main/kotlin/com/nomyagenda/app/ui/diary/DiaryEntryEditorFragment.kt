package com.nomyagenda.app.ui.diary

import android.app.DatePickerDialog
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.text.Editable
import android.text.Spannable
import android.text.TextWatcher
import android.text.style.StyleSpan
import android.view.*
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.R as MaterialR
import com.nomyagenda.app.NomyAgendaApp
import com.nomyagenda.app.R
import com.nomyagenda.app.core.datetime.formatDiaryDateKey
import com.nomyagenda.app.databinding.FragmentDiaryEntryEditorBinding
import com.nomyagenda.app.ui.common.color.ColorPalette
import com.nomyagenda.app.ui.common.font.FontCatalog
import com.nomyagenda.app.ui.editor.RichTextConverter
import com.nomyagenda.app.ui.resolveThemeColor
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class DiaryEntryEditorFragment : Fragment() {

    private var _binding: FragmentDiaryEntryEditorBinding? = null
    private val binding get() = _binding!!

    private val args: DiaryEntryEditorFragmentArgs by navArgs()

    private val viewModel: DiaryEntryEditorViewModel by viewModels {
        val app = requireActivity().application as NomyAgendaApp
        DiaryEntryEditorViewModelFactory(app.diaryRepository)
    }

    private lateinit var photoAdapter: DiaryPhotoAdapter

    // ---- WYSIWYG format toggle state (content) ----
    private var isBoldActive = false
    private var isItalicActive = false

    private var lastInsertStart = 0
    private var lastInsertCount = 0
    private var isApplyingSpans = false

    // ---- WYSIWYG format toggle state (title) ----
    private var isTitleBoldActive = false
    private var isTitleItalicActive = false

    private var titleLastInsertStart = 0
    private var titleLastInsertCount = 0
    private var isTitleApplyingSpans = false

    /** Applies bold/italic spans to title text inserted while a toggle is active. */
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
                if (isTitleBoldActive) {
                    s.setSpan(
                        StyleSpan(Typeface.BOLD),
                        titleLastInsertStart, end,
                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                    )
                }
                if (isTitleItalicActive) {
                    s.setSpan(
                        StyleSpan(Typeface.ITALIC),
                        titleLastInsertStart, end,
                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                    )
                }
            } finally {
                isTitleApplyingSpans = false
            }
        }
    }

    /** Applies formatting spans to text inserted while a toggle is active. */
    private val formatWatcher = object : TextWatcher {
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
                if (isBoldActive) {
                    s.setSpan(
                        StyleSpan(Typeface.BOLD),
                        lastInsertStart, end,
                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                    )
                }
                if (isItalicActive) {
                    s.setSpan(
                        StyleSpan(Typeface.ITALIC),
                        lastInsertStart, end,
                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                    )
                }
            } finally {
                isApplyingSpans = false
            }
        }
    }

    private val pickImage = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            viewModel.addPhoto(requireContext(), uri)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDiaryEntryEditorBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupToolbar()
        setupMoodChips()
        setupColorPicker()
        setupBackgroundPicker()
        setupFontPicker()
        setupPhotos()
        setupFormatToolbar()

        val defaultDateKey = if (args.dateKey.isNotEmpty()) args.dateKey else DiaryFragment.todayDateKey()
        viewModel.loadEntry(args.entryId, defaultDateKey)

        observeViewModel()
    }

    private fun setupToolbar() {
        binding.toolbarDiaryEditor.setNavigationOnClickListener {
            findNavController().navigateUp()
        }
        binding.toolbarDiaryEditor.inflateMenu(R.menu.menu_diary_editor)
        binding.toolbarDiaryEditor.setOnMenuItemClickListener { item ->
            if (item.itemId == R.id.action_save_diary) {
                saveDiaryEntry()
                true
            } else false
        }
    }

    private fun setupFormatToolbar() {
        binding.editDiaryTitle.addTextChangedListener(titleFormatWatcher)
        binding.btnDiaryTitleFormatBold.setOnClickListener { toggleTitleInlineFormat(Typeface.BOLD) }
        binding.btnDiaryTitleFormatItalic.setOnClickListener { toggleTitleInlineFormat(Typeface.ITALIC) }

        binding.editDiaryContent.addTextChangedListener(formatWatcher)
        binding.btnDiaryFormatBold.setOnClickListener { toggleInlineFormat(Typeface.BOLD) }
        binding.btnDiaryFormatItalic.setOnClickListener { toggleInlineFormat(Typeface.ITALIC) }
    }

    private fun toggleTitleInlineFormat(style: Int) {
        val editText = binding.editDiaryTitle
        val text = editText.text ?: return
        val selStart = editText.selectionStart.coerceAtLeast(0)
        val selEnd   = editText.selectionEnd.coerceAtLeast(0)

        if (selStart != selEnd) {
            val existing = text.getSpans(selStart, selEnd, StyleSpan::class.java)
                .filter { it.style == style
                        && text.getSpanStart(it) >= selStart
                        && text.getSpanEnd(it) <= selEnd }
            if (existing.isNotEmpty()) {
                existing.forEach { text.removeSpan(it) }
            } else {
                text.setSpan(
                    StyleSpan(style), selStart, selEnd,
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                )
            }
            when (style) {
                Typeface.BOLD   -> binding.btnDiaryTitleFormatBold.isChecked   = isTitleBoldActive
                Typeface.ITALIC -> binding.btnDiaryTitleFormatItalic.isChecked = isTitleItalicActive
            }
        } else {
            when (style) {
                Typeface.BOLD -> {
                    isTitleBoldActive = !isTitleBoldActive
                    binding.btnDiaryTitleFormatBold.isChecked = isTitleBoldActive
                }
                Typeface.ITALIC -> {
                    isTitleItalicActive = !isTitleItalicActive
                    binding.btnDiaryTitleFormatItalic.isChecked = isTitleItalicActive
                }
            }
        }
    }

    private fun toggleInlineFormat(style: Int) {
        val editText = binding.editDiaryContent
        val text = editText.text ?: return
        val selStart = editText.selectionStart.coerceAtLeast(0)
        val selEnd   = editText.selectionEnd.coerceAtLeast(0)

        if (selStart != selEnd) {
            val existing = text.getSpans(selStart, selEnd, StyleSpan::class.java)
                .filter { it.style == style
                        && text.getSpanStart(it) >= selStart
                        && text.getSpanEnd(it) <= selEnd }
            if (existing.isNotEmpty()) {
                existing.forEach { text.removeSpan(it) }
            } else {
                text.setSpan(
                    StyleSpan(style), selStart, selEnd,
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                )
            }
            when (style) {
                Typeface.BOLD   -> binding.btnDiaryFormatBold.isChecked   = isBoldActive
                Typeface.ITALIC -> binding.btnDiaryFormatItalic.isChecked = isItalicActive
            }
        } else {
            when (style) {
                Typeface.BOLD -> {
                    isBoldActive = !isBoldActive
                    binding.btnDiaryFormatBold.isChecked = isBoldActive
                }
                Typeface.ITALIC -> {
                    isItalicActive = !isItalicActive
                    binding.btnDiaryFormatItalic.isChecked = isItalicActive
                }
            }
        }
    }

    private fun setupMoodChips() {
        val moodOptions = listOf("😊", "🙂", "😐", "😔", "😡")
        moodOptions.forEach { emoji ->
            val chip = com.google.android.material.chip.Chip(requireContext()).apply {
                text = emoji
                textSize = 20f
                isCheckable = true
                chipBackgroundColor = null
                chipStrokeWidth = 0f
            }
            chip.setOnCheckedChangeListener { _, isChecked ->
                if (isChecked) {
                    viewModel.mood.value = emoji
                } else if (viewModel.mood.value == emoji) {
                    viewModel.mood.value = ""
                }
            }
            binding.chipGroupMood.addView(chip)
        }
    }

    private fun setupColorPicker() {
        setupColorSwatches(
            container = binding.colorSwatchesContainerDiary,
            onSelect = { hex ->
                viewModel.color.value = hex
                updateSwatchSelection(binding.colorSwatchesContainerDiary, hex)
            }
        )
        setupColorSwatches(
            container = binding.colorSwatchesContainerDiaryContent,
            onSelect = { hex ->
                viewModel.contentColor.value = hex
                updateSwatchSelection(binding.colorSwatchesContainerDiaryContent, hex)
            }
        )
        updateSwatchSelection(binding.colorSwatchesContainerDiary, "")
        updateSwatchSelection(binding.colorSwatchesContainerDiaryContent, "")
    }

    private fun setupColorSwatches(container: LinearLayout, onSelect: (String) -> Unit) {
        val size = resources.getDimensionPixelSize(R.dimen.color_swatch_size)
        val margin = resources.getDimensionPixelSize(R.dimen.color_swatch_margin)
        val strokeWidth = resources.getDimensionPixelSize(R.dimen.color_swatch_stroke_width)

        val noneDrawable = GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(ContextCompat.getColor(requireContext(), android.R.color.transparent))
            setStroke(strokeWidth, Color.LTGRAY)
        }
        val noneSwatch = FrameLayout(requireContext()).apply {
            tag = ""
            background = noneDrawable
            layoutParams = LinearLayout.LayoutParams(size, size).apply {
                setMargins(margin, margin, margin, margin)
            }
            setOnClickListener { onSelect("") }
        }
        container.addView(noneSwatch)

        ColorPalette.COLORS.forEach { hexColor ->
            val swatch = FrameLayout(requireContext()).apply {
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
        val strokeWidth = resources.getDimensionPixelSize(R.dimen.color_swatch_stroke_width)
        for (i in 0 until container.childCount) {
            val swatch = container.getChildAt(i) as? FrameLayout ?: continue
            val swatchColor = swatch.tag as? String ?: ""
            val isSelected = swatchColor == hexColor
            swatch.background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                if (swatchColor.isEmpty()) {
                    setColor(ContextCompat.getColor(requireContext(), android.R.color.transparent))
                    setStroke(strokeWidth, if (isSelected) Color.BLACK else Color.LTGRAY)
                } else {
                    setColor(Color.parseColor(swatchColor))
                    if (isSelected) setStroke(strokeWidth, Color.WHITE)
                }
            }
        }
    }

    private fun setupFontPicker() {
        FontCatalog.fonts.forEach { fontItem ->
            val typeface = FontCatalog.resolve(requireContext(), fontItem.id)
            val btn = com.google.android.material.button.MaterialButton(
                requireContext(),
                null,
                com.google.android.material.R.attr.materialButtonOutlinedStyle
            ).apply {
                tag = fontItem.id
                text = fontItem.displayName
                this.typeface = typeface
                isCheckable = true
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    setMargins(
                        resources.getDimensionPixelSize(R.dimen.color_swatch_margin),
                        0,
                        resources.getDimensionPixelSize(R.dimen.color_swatch_margin),
                        0
                    )
                }
                setOnClickListener {
                    viewModel.fontFamily.value = fontItem.id
                }
            }
            binding.fontPickerContainer.addView(btn)
        }
    }

    private fun updateFontSelection(selectedId: String) {
        for (i in 0 until binding.fontPickerContainer.childCount) {
            val btn = binding.fontPickerContainer.getChildAt(i)
                as? com.google.android.material.button.MaterialButton ?: continue
            btn.isChecked = btn.tag as? String == selectedId
        }
    }

    private fun applyFontToViews(fontId: String) {
        val typeface = FontCatalog.resolve(requireContext(), fontId)
        binding.editDiaryTitle.typeface = typeface
        binding.editDiaryContent.typeface = typeface
    }

    private fun setupBackgroundPicker() {
        setupBackgroundSwatches(binding.bgSwatchesBasic, DiaryBackgroundCatalog.basicBackgrounds)
        setupBackgroundSwatches(binding.bgSwatchesThematic, DiaryBackgroundCatalog.thematicBackgrounds)
        setupBackgroundSwatches(binding.bgSwatchesFestive, DiaryBackgroundCatalog.festiveBackgrounds)
    }

    private fun setupBackgroundSwatches(container: LinearLayout, backgrounds: List<DiaryBackgroundItem>) {
        val size = resources.getDimensionPixelSize(R.dimen.bg_swatch_size)
        val margin = resources.getDimensionPixelSize(R.dimen.color_swatch_margin)
        val strokeWidth = resources.getDimensionPixelSize(R.dimen.color_swatch_stroke_width)
        val cornerRadius = resources.getDimension(R.dimen.card_corner_radius)

        backgrounds.forEach { item ->
            val swatch = FrameLayout(requireContext()).apply {
                tag = item.key
                layoutParams = LinearLayout.LayoutParams(size, size).apply {
                    setMargins(margin, margin, margin, margin)
                }
                clipToOutline = true
                outlineProvider = android.view.ViewOutlineProvider.BACKGROUND
                if (item.drawableRes != 0) {
                    background = ContextCompat.getDrawable(requireContext(), item.drawableRes)
                } else {
                    background = GradientDrawable().apply {
                        shape = GradientDrawable.RECTANGLE
                        cornerRadii = FloatArray(8) { cornerRadius }
                        setColor(ContextCompat.getColor(requireContext(), android.R.color.transparent))
                        setStroke(strokeWidth, Color.LTGRAY)
                    }
                    val noneLabel = android.widget.TextView(requireContext()).apply {
                        text = "✕"
                        textSize = 18f
                        gravity = android.view.Gravity.CENTER
                        setTextColor(Color.LTGRAY)
                        layoutParams = FrameLayout.LayoutParams(
                            FrameLayout.LayoutParams.MATCH_PARENT,
                            FrameLayout.LayoutParams.MATCH_PARENT
                        )
                    }
                    addView(noneLabel)
                }
                setOnClickListener {
                    viewModel.background.value = item.key
                }
            }
            container.addView(swatch)
        }
    }

    private fun updateBackgroundSelection(selectedKey: String) {
        val containers = listOf(binding.bgSwatchesBasic, binding.bgSwatchesThematic, binding.bgSwatchesFestive)
        val strokeWidth = resources.getDimensionPixelSize(R.dimen.color_swatch_stroke_width)
        val cornerRadius = resources.getDimension(R.dimen.card_corner_radius)

        containers.forEach { container ->
            for (i in 0 until container.childCount) {
                val swatch = container.getChildAt(i) as? FrameLayout ?: continue
                val key = swatch.tag as? String ?: continue
                val isSelected = key == selectedKey

                if (isSelected) {
                    val selectionOverlay = GradientDrawable().apply {
                        shape = GradientDrawable.RECTANGLE
                        cornerRadii = FloatArray(8) { cornerRadius }
                        setStroke(strokeWidth, requireContext().resolveThemeColor(MaterialR.attr.colorPrimary))
                        setColor(ContextCompat.getColor(requireContext(), android.R.color.transparent))
                    }
                    swatch.foreground = selectionOverlay
                } else {
                    swatch.foreground = null
                }
            }
        }
    }

    private fun setupPhotos() {
        photoAdapter = DiaryPhotoAdapter(onRemove = { path -> viewModel.removePhoto(path) })
        binding.recyclerPhotos.adapter = photoAdapter
        binding.recyclerPhotos.layoutManager =
            LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)

        binding.btnAddPhoto.setOnClickListener {
            pickImage.launch("image/*")
        }
    }

    private fun observeViewModel() {

        viewModel.dateKey.observe(viewLifecycleOwner) { dk ->
            binding.btnSelectDate.text = formatDiaryDateKey(dk ?: "")
        }

        viewModel.title.observe(viewLifecycleOwner) { t ->
            if (t == null) return@observe
            val spannable = RichTextConverter.markdownInlineToSpannable(t)
            if (binding.editDiaryTitle.text?.toString() != spannable.toString()) {
                binding.editDiaryTitle.setText(spannable, android.widget.TextView.BufferType.EDITABLE)
            }
        }

        viewModel.content.observe(viewLifecycleOwner) { c ->
            if (c == null) return@observe
            val spannable = RichTextConverter.markdownInlineToSpannable(c)
            binding.editDiaryContent.setText(spannable, android.widget.TextView.BufferType.EDITABLE)
        }

        viewModel.mood.observe(viewLifecycleOwner) { currentMood ->
            for (i in 0 until binding.chipGroupMood.childCount) {
                val chip = binding.chipGroupMood.getChildAt(i) as? com.google.android.material.chip.Chip ?: continue
                val shouldBeChecked = chip.text.toString() == currentMood
                if (chip.isChecked != shouldBeChecked) chip.isChecked = shouldBeChecked
            }
        }

        viewModel.photoPaths.observe(viewLifecycleOwner) { paths ->
            photoAdapter.submitList(paths)
        }

        // 🔥 CAMBIO IMPORTANTE: aplicar color en tiempo real
        viewModel.color.observe(viewLifecycleOwner) { selectedColor ->
            updateSwatchSelection(binding.colorSwatchesContainerDiary, selectedColor ?: "")

            val color = if (selectedColor.isNullOrEmpty()) {
                Color.BLACK
            } else {
                Color.parseColor(selectedColor)
            }

            binding.editDiaryTitle.setTextColor(color)
            binding.editDiaryTitle.setHintTextColor(color)
        }

        viewModel.contentColor.observe(viewLifecycleOwner) { selectedColor ->
            updateSwatchSelection(binding.colorSwatchesContainerDiaryContent, selectedColor ?: "")

            val color = if (selectedColor.isNullOrEmpty()) {
                Color.BLACK
            } else {
                Color.parseColor(selectedColor)
            }

            binding.editDiaryContent.setTextColor(color)
            binding.editDiaryContent.setHintTextColor(color)
        }

        viewModel.background.observe(viewLifecycleOwner) { bgKey ->
            updateBackgroundSelection(bgKey ?: "")
        }

        viewModel.fontFamily.observe(viewLifecycleOwner) { fontId ->
            updateFontSelection(fontId ?: "")
            applyFontToViews(fontId ?: "")
        }

        viewModel.saveSuccessEvent.observe(viewLifecycleOwner) { saved ->
            if (saved) {
                viewModel.consumeSaveSuccessEvent()
                Toast.makeText(requireContext(), R.string.diary_saved, Toast.LENGTH_SHORT).show()
                findNavController().navigateUp()
            }
        }

        viewModel.saveErrorEvent.observe(viewLifecycleOwner) { error ->
            if (!error.isNullOrBlank()) {
                viewModel.consumeSaveErrorEvent()
                Toast.makeText(requireContext(), error, Toast.LENGTH_SHORT).show()
            }
        }

        binding.btnSelectDate.setOnClickListener { showDatePicker() }
    }

    private fun showDatePicker() {
        val cal = Calendar.getInstance()
        val dk = viewModel.dateKey.value
        if (!dk.isNullOrEmpty()) {
            try {
                val parsed = DATE_PARSE_FORMAT.parse(dk)
                if (parsed != null) cal.time = parsed
            } catch (_: Exception) {}
        }

        DatePickerDialog(
            requireContext(),
            { _, year, month, day ->
                val newKey = "%04d-%02d-%02d".format(year, month + 1, day)
                viewModel.dateKey.value = newKey
            },
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH),
            cal.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    private fun saveDiaryEntry() {
        viewModel.title.value = binding.editDiaryTitle.text
            ?.let { RichTextConverter.spannableToMarkdown(it) }
            ?: ""
        viewModel.content.value = binding.editDiaryContent.text
            ?.let { RichTextConverter.spannableToMarkdown(it) }
            ?: ""
        viewModel.save()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private val DATE_PARSE_FORMAT = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    }
}