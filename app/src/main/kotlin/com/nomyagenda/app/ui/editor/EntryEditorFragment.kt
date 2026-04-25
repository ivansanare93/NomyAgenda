package com.nomyagenda.app.ui.editor

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.text.Editable
import android.text.Spannable
import android.text.TextWatcher
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import android.view.*
import android.widget.ArrayAdapter
import android.widget.FrameLayout
import android.widget.GridLayout
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import com.nomyagenda.app.NomyAgendaApp
import com.nomyagenda.app.R
import com.nomyagenda.app.data.local.entity.AgendaEntry
import com.nomyagenda.app.data.local.entity.ChecklistItem
import com.nomyagenda.app.data.local.entity.EntryType
import com.nomyagenda.app.data.preferences.SettingsRepository
import com.nomyagenda.app.databinding.FragmentEntryEditorBinding
import io.noties.markwon.Markwon
import io.noties.markwon.html.HtmlPlugin
import java.text.SimpleDateFormat
import java.util.*

class EntryEditorFragment : Fragment() {

    private var _binding: FragmentEntryEditorBinding? = null
    private val binding get() = _binding!!

    private val args: EntryEditorFragmentArgs by navArgs()

    private val viewModel: EntryEditorViewModel by viewModels {
        val app = requireActivity().application as NomyAgendaApp
        EntryEditorViewModelFactory(app.agendaRepository, app)
    }

    private lateinit var checklistAdapter: ChecklistAdapter
    private lateinit var markwon: Markwon
    private lateinit var advanceNoticeAdapter: ArrayAdapter<String>
    private val checklistItems = mutableListOf<ChecklistItem>()
    private var selectedDueAt: Long? = null
    private var currentType: EntryType = EntryType.NOTE
    private var selectedAdvanceNoticeMinutes: Int = SettingsRepository.ADVANCE_NOTICE_NONE
    private var selectedColor: String = ""

    // ---- WYSIWYG format toggle state ----
    private var isBoldActive = false
    private var isItalicActive = false
    private var activeTextColor: Int? = null   // null = no colour mode active

    // Tracked inside TextWatcher to know the range of the latest insertion
    private var lastInsertStart = 0
    private var lastInsertCount = 0
    // Guard flag: prevents re-entrant span application in the TextWatcher
    private var isApplyingSpans = false

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
                activeTextColor?.let { color ->
                    s.setSpan(
                        ForegroundColorSpan(color),
                        lastInsertStart, end,
                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                    )
                }
            } finally {
                isApplyingSpans = false
            }
        }
    }

    private val advanceOptions by lazy {
        listOf(
            SettingsRepository.ADVANCE_NOTICE_NONE to getString(R.string.settings_advance_notice_none),
            SettingsRepository.ADVANCE_NOTICE_1H  to getString(R.string.settings_advance_notice_1h),
            SettingsRepository.ADVANCE_NOTICE_1D  to getString(R.string.settings_advance_notice_1d),
            SettingsRepository.ADVANCE_NOTICE_1W  to getString(R.string.settings_advance_notice_1w)
        )
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentEntryEditorBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        markwon = Markwon.builder(requireContext())
            .usePlugin(HtmlPlugin.create())
            .build()

        binding.toolbar.setNavigationOnClickListener { findNavController().navigateUp() }

        checklistAdapter = ChecklistAdapter(checklistItems) { /* updated */ }
        binding.recyclerChecklist.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerChecklist.adapter = checklistAdapter

        binding.chipNote.setOnClickListener { setType(EntryType.NOTE) }
        binding.chipTask.setOnClickListener { setType(EntryType.TASK) }
        binding.chipReminder.setOnClickListener { setType(EntryType.REMINDER) }

        binding.btnNoteEdit.isChecked = true
        binding.toggleNoteMode.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isChecked) {
                when (checkedId) {
                    R.id.btn_note_edit -> showNoteEditMode()
                    R.id.btn_note_preview -> showNotePreviewMode()
                }
            }
        }

        // Format toolbar – bold / italic are WYSIWYG toggles; others keep their
        // prefix-insertion behaviour (they operate at line level).
        binding.btnFormatBold.setOnClickListener    { toggleInlineFormat(Typeface.BOLD) }
        binding.btnFormatItalic.setOnClickListener  { toggleInlineFormat(Typeface.ITALIC) }
        binding.btnFormatHeading.setOnClickListener  { applyLinePrefix("# ") }
        binding.btnFormatBullet.setOnClickListener   { applyLinePrefix("- ") }
        binding.btnFormatNumbered.setOnClickListener { applyLinePrefix("1. ") }
        binding.btnFormatQuote.setOnClickListener    { applyLinePrefix("> ") }
        binding.btnFormatColor.setOnClickListener    { showColorPicker() }

        // Attach the TextWatcher that applies active format spans while typing
        binding.editNoteContent.addTextChangedListener(formatWatcher)

        binding.buttonAddChecklistItem.setOnClickListener {
            val text = binding.editNewChecklistItem.text?.toString()?.trim() ?: ""
            if (text.isNotBlank()) {
                checklistAdapter.addItem(text)
                binding.editNewChecklistItem.setText("")
                binding.editNewChecklistItem.clearFocus()
            }
        }

        binding.editDueDate.setOnClickListener { showDateTimePicker() }

        val advanceAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, advanceOptions.map { it.second })
        advanceNoticeAdapter = advanceAdapter
        binding.spinnerAdvanceNotice.setText(advanceOptions[0].second, false)
        binding.spinnerAdvanceNotice.setAdapter(advanceAdapter)
        binding.spinnerAdvanceNotice.setOnItemClickListener { _, _, position, _ ->
            selectedAdvanceNoticeMinutes = advanceOptions[position].first
        }

        binding.fabSaveEntry.setOnClickListener { saveEntry() }

        setupEntryColorPicker()

        if (args.entryId > 0) {
            viewModel.load(args.entryId)
        }

        viewModel.entry.observe(viewLifecycleOwner) { entry ->
            entry ?: return@observe
            binding.editEntryTitle.setText(entry.title)
            setType(entry.type)
            when (entry.type) {
                EntryType.NOTE -> setNoteContent(entry.content)
                EntryType.TASK -> {
                    checklistItems.clear()
                    checklistItems.addAll(ChecklistManager.fromJson(entry.checklistJson))
                    checklistAdapter.notifyDataSetChanged()
                }
                EntryType.REMINDER -> {
                    setNoteContent(entry.content)
                    entry.dueAt?.let { dueAt ->
                        selectedDueAt = dueAt
                        binding.editDueDate.setText(DATE_FORMAT.format(Date(dueAt)))
                    }
                    selectedAdvanceNoticeMinutes = entry.advanceNoticeMinutes
                    val label = advanceOptions.firstOrNull { it.first == entry.advanceNoticeMinutes }?.second
                        ?: advanceOptions[0].second
                    binding.spinnerAdvanceNotice.setText(label, false)
                    binding.spinnerAdvanceNotice.setAdapter(advanceNoticeAdapter)
                }
            }
            binding.editTags.setText(entry.tags)
            if (entry.color.isNotEmpty()) {
                selectEntryColor(entry.color)
            }
        }
    }

    // ---------- content helpers ----------

    /**
     * Converts inline Markdown in [markdown] to spans and sets the result as the
     * EditText content so the user sees visual formatting instead of raw markers.
     */
    private fun setNoteContent(markdown: String) {
        val spannable = RichTextConverter.markdownInlineToSpannable(markdown)
        binding.editNoteContent.setText(spannable, android.widget.TextView.BufferType.EDITABLE)
    }

    /** Returns the current note content serialised as a Markdown string. */
    private fun getNoteContentAsMarkdown(): String =
        binding.editNoteContent.text
            ?.let { RichTextConverter.spannableToMarkdown(it) }
            ?: ""

    // ---------- type / mode switching ----------

    private fun setType(type: EntryType) {
        currentType = type
        binding.chipNote.isChecked = type == EntryType.NOTE
        binding.chipTask.isChecked = type == EntryType.TASK
        binding.chipReminder.isChecked = type == EntryType.REMINDER
        binding.layoutNoteContent.visibility = if (type == EntryType.NOTE || type == EntryType.REMINDER) View.VISIBLE else View.GONE
        binding.layoutTaskContent.visibility = if (type == EntryType.TASK) View.VISIBLE else View.GONE
        binding.layoutReminderContent.visibility = if (type == EntryType.REMINDER) View.VISIBLE else View.GONE
        if (type == EntryType.NOTE || type == EntryType.REMINDER) {
            binding.btnNoteEdit.isChecked = true
            showNoteEditMode()
        }
    }

    private fun showNoteEditMode() {
        binding.scrollFormatToolbar.visibility = View.VISIBLE
        binding.inputLayoutNoteContent.visibility = View.VISIBLE
        binding.cardNotePreview.visibility = View.GONE
    }

    private fun showNotePreviewMode() {
        markwon.setMarkdown(binding.textNotePreview, getNoteContentAsMarkdown())
        binding.scrollFormatToolbar.visibility = View.GONE
        binding.inputLayoutNoteContent.visibility = View.GONE
        binding.cardNotePreview.visibility = View.VISIBLE
        // Reset toggles when entering preview (they make no sense outside edit mode)
        resetFormatToggles()
    }

    // ---------- WYSIWYG inline-format toggle ----------

    /**
     * Handles a click on a checkable format button (bold / italic).
     *
     * • If there is a text selection, the span is toggled on/off for that range
     *   without changing the typing-mode toggle.
     * • If there is no selection, the typing-mode toggle is flipped.
     */
    private fun toggleInlineFormat(style: Int) {
        val editText = binding.editNoteContent
        val text = editText.text ?: return
        val selStart = editText.selectionStart.coerceAtLeast(0)
        val selEnd   = editText.selectionEnd.coerceAtLeast(0)

        if (selStart != selEnd) {
            // Toggle the span on the selection; only remove spans fully within the selection
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
            // Keep the current typing-mode state unchanged
        } else {
            // No selection – flip the typing-mode toggle
            when (style) {
                Typeface.BOLD -> {
                    isBoldActive = !isBoldActive
                    binding.btnFormatBold.isChecked = isBoldActive
                }
                Typeface.ITALIC -> {
                    isItalicActive = !isItalicActive
                    binding.btnFormatItalic.isChecked = isItalicActive
                }
            }
        }
    }

    /** Clears all active format toggles and syncs button states. */
    private fun resetFormatToggles() {
        isBoldActive = false
        isItalicActive = false
        activeTextColor = null
        binding.btnFormatBold.isChecked = false
        binding.btnFormatItalic.isChecked = false
        binding.btnFormatColor.isChecked = false
    }

    // ---------- line-level prefix formatting (unchanged) ----------

    private fun applyLinePrefix(prefix: String) {
        val editText = binding.editNoteContent
        val text = editText.text ?: return
        val cursor = editText.selectionStart.coerceAtLeast(0)
        val lineStart = (text.lastIndexOf('\n', cursor - 1) + 1).coerceAtLeast(0)
        text.insert(lineStart, prefix)
        editText.setSelection(cursor + prefix.length)
    }

    // ---------- entry colour picker (unchanged) ----------

    private fun setupEntryColorPicker() {
        val container = binding.colorSwatchesContainer
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
            layoutParams = android.widget.LinearLayout.LayoutParams(size, size).apply {
                setMargins(margin, margin, margin, margin)
            }
            setOnClickListener { selectEntryColor("") }
        }
        container.addView(noneSwatch)

        COLOR_PALETTE.forEach { hexColor ->
            val swatch = FrameLayout(requireContext()).apply {
                tag = hexColor
                background = GradientDrawable().apply {
                    shape = GradientDrawable.OVAL
                    setColor(Color.parseColor(hexColor))
                }
                layoutParams = android.widget.LinearLayout.LayoutParams(size, size).apply {
                    setMargins(margin, margin, margin, margin)
                }
                setOnClickListener { selectEntryColor(hexColor) }
            }
            container.addView(swatch)
        }

        updateSwatchSelection(container, "")
    }

    private fun selectEntryColor(hexColor: String) {
        selectedColor = hexColor
        updateSwatchSelection(binding.colorSwatchesContainer, hexColor)
    }

    private fun updateSwatchSelection(container: android.widget.LinearLayout, hexColor: String) {
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

    // ---------- text-colour picker (WYSIWYG) ----------

    private fun showColorPicker() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_color_picker, null)
        val grid = dialogView.findViewById<GridLayout>(R.id.grid_colors)

        val dialog = AlertDialog.Builder(requireContext())
            .setTitle(R.string.color_picker_title)
            .setView(dialogView)
            .setNegativeButton(R.string.cancel, null)
            .create()

        COLOR_PALETTE.forEach { hexColor ->
            val size = resources.getDimensionPixelSize(R.dimen.color_swatch_size)
            val margin = resources.getDimensionPixelSize(R.dimen.color_swatch_margin)

            val circle = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(Color.parseColor(hexColor))
            }

            val swatch = FrameLayout(requireContext()).apply {
                background = circle
                layoutParams = GridLayout.LayoutParams().apply {
                    width = size
                    height = size
                    setMargins(margin, margin, margin, margin)
                }
                setOnClickListener {
                    applyTextColor(hexColor)
                    dialog.dismiss()
                }
            }
            grid.addView(swatch)
        }

        dialog.show()
    }

    /**
     * If there is a text selection the colour span is applied immediately.
     * Otherwise, colour-typing mode is toggled: subsequent characters will
     * receive a [ForegroundColorSpan] until the user taps the colour button again
     * (or taps the same colour to deactivate it).
     */
    private fun applyTextColor(hexColor: String) {
        val editText = binding.editNoteContent
        val text = editText.text ?: return
        val selStart = editText.selectionStart.coerceAtLeast(0)
        val selEnd   = editText.selectionEnd.coerceAtLeast(0)
        val color = try {
            Color.parseColor(hexColor)
        } catch (_: IllegalArgumentException) {
            return
        }

        if (selStart != selEnd) {
            // Apply immediately to the selection
            text.setSpan(
                ForegroundColorSpan(color), selStart, selEnd,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )
            // Deactivate colour-typing mode when we operate on a selection
            activeTextColor = null
            binding.btnFormatColor.isChecked = false
        } else {
            // Toggle colour-typing mode
            if (activeTextColor == color) {
                activeTextColor = null
                binding.btnFormatColor.isChecked = false
            } else {
                activeTextColor = color
                binding.btnFormatColor.isChecked = true
            }
        }
    }

    // ---------- date/time picker ----------

    private fun showDateTimePicker() {
        val cal = Calendar.getInstance().apply {
            selectedDueAt?.let { timeInMillis = it }
        }
        DatePickerDialog(requireContext(), { _, year, month, day ->
            TimePickerDialog(requireContext(), { _, hour, minute ->
                cal.set(year, month, day, hour, minute, 0)
                cal.set(Calendar.MILLISECOND, 0)
                selectedDueAt = cal.timeInMillis
                binding.editDueDate.setText(DATE_FORMAT.format(Date(selectedDueAt!!)))
            }, cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), true).show()
        }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show()
    }

    // ---------- save ----------

    private fun saveEntry() {
        val title = binding.editEntryTitle.text?.toString()?.trim()
        if (title.isNullOrEmpty()) {
            binding.inputLayoutTitle.error = getString(R.string.error_title_required)
            return
        }
        binding.inputLayoutTitle.error = null

        val tags = binding.editTags.text?.toString()?.trim() ?: ""

        val noteContent: String = getNoteContentAsMarkdown()

        val entry = AgendaEntry(
            id = args.entryId.takeIf { it > 0 } ?: 0,
            title = title,
            type = currentType,
            content = when (currentType) {
                EntryType.NOTE, EntryType.REMINDER -> noteContent
                EntryType.TASK -> ""
            },
            checklistJson = if (currentType == EntryType.TASK) ChecklistManager.toJson(checklistAdapter.getItems()) else "[]",
            dueAt = if (currentType == EntryType.REMINDER) selectedDueAt else null,
            advanceNoticeMinutes = if (currentType == EntryType.REMINDER) selectedAdvanceNoticeMinutes else 0,
            tags = tags,
            color = selectedColor
        )

        viewModel.save(entry) {
            requireActivity().runOnUiThread {
                Toast.makeText(requireContext(), R.string.entry_saved, Toast.LENGTH_SHORT).show()
                findNavController().navigateUp()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private val DATE_FORMAT = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())

        private val COLOR_PALETTE = listOf(
            "#E53935",
            "#E91E63",
            "#9C27B0",
            "#3F51B5",
            "#2196F3",
            "#009688",
            "#4CAF50",
            "#FF9800",
            "#FF5722",
            "#795548"
        )
    }
}

