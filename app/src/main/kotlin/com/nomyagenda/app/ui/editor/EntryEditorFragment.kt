package com.nomyagenda.app.ui.editor

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.graphics.Typeface
import android.os.Bundle
import android.text.Editable
import android.text.Spannable
import android.text.TextWatcher
import android.text.style.StyleSpan
import android.view.*
import android.widget.ArrayAdapter
import android.widget.Toast
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
        EntryEditorViewModelFactory(app.agendaRepository, app.reminderService)
    }

    private lateinit var editorHelper: RichTextEditorHelper
    private lateinit var checklistAdapter: ChecklistAdapter
    private lateinit var markwon: Markwon
    private lateinit var advanceNoticeAdapter: ArrayAdapter<String>
    private val checklistItems = mutableListOf<ChecklistItem>()

    private var selectedDueAt: Long? = null
    private var currentType: EntryType = EntryType.NOTE
    private var selectedAdvanceNoticeMinutes: Int = SettingsRepository.ADVANCE_NOTICE_NONE

    // Style properties tracked via editorHelper callbacks
    private var selectedColor: String = ""
    private var selectedContentColor: String = ""
    private var selectedFontFamily: String = ""
    private var selectedBackground: String = ""
    private var selectedTitleFontSize: Float = 0f
    private var selectedContentFontSize: Float = 0f

    // ---- WYSIWYG format toggle state (checklist item) ----
    private var isChecklistBoldActive = false
    private var isChecklistItalicActive = false
    private var checklistLastInsertStart = 0
    private var checklistLastInsertCount = 0
    private var isChecklistApplyingSpans = false

    /** Applies bold/italic spans to checklist item text inserted while a toggle is active. */
    private val checklistFormatWatcher = object : TextWatcher {
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            if (!isChecklistApplyingSpans) {
                checklistLastInsertStart = start
                checklistLastInsertCount = count
            }
        }

        override fun afterTextChanged(s: Editable?) {
            if (s == null || checklistLastInsertCount == 0 || isChecklistApplyingSpans) return
            isChecklistApplyingSpans = true
            try {
                val end = checklistLastInsertStart + checklistLastInsertCount
                if (isChecklistBoldActive)
                    s.setSpan(StyleSpan(Typeface.BOLD), checklistLastInsertStart, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                if (isChecklistItalicActive)
                    s.setSpan(StyleSpan(Typeface.ITALIC), checklistLastInsertStart, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
            } finally {
                isChecklistApplyingSpans = false
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

        // ── Shared rich-text editor ──────────────────────────────────────
        editorHelper = RichTextEditorHelper(
            context        = requireContext(),
            titleBinding   = binding.editorTitle,
            contentBinding = binding.editorContent,
            customBinding  = binding.editorCustomization
        )
        editorHelper.onTitleColorChanged      = { hex -> selectedColor          = hex }
        editorHelper.onContentColorChanged    = { hex -> selectedContentColor   = hex }
        editorHelper.onFontFamilyChanged      = { id  -> selectedFontFamily     = id  }
        editorHelper.onBackgroundChanged      = { key -> selectedBackground     = key }
        editorHelper.onTitleFontSizeChanged   = { sp  -> selectedTitleFontSize  = sp  }
        editorHelper.onContentFontSizeChanged = { sp  -> selectedContentFontSize = sp }
        editorHelper.setup()

        // Configure background categories for entries (no "basic" palette)
        editorHelper.setBackgroundCategories(listOf(
            getString(R.string.diary_background_thematic) to EntryBackgroundCatalog.thematicBackgrounds,
            getString(R.string.diary_background_festive)  to EntryBackgroundCatalog.festiveBackgrounds,
            getString(R.string.diary_background_premium)  to EntryBackgroundCatalog.premiumBackgrounds,
            getString(R.string.diary_background_photos)   to EntryBackgroundCatalog.photoBackgrounds
        ))

        // ── Entry-specific setup ─────────────────────────────────────────
        markwon = Markwon.builder(requireContext())
            .usePlugin(HtmlPlugin.create())
            .build()

        binding.toolbar.setNavigationOnClickListener { findNavController().navigateUp() }

        checklistAdapter = ChecklistAdapter(checklistItems) { /* updated */ }
        binding.recyclerChecklist.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerChecklist.adapter = checklistAdapter

        binding.chipNote.setOnClickListener     { setType(EntryType.NOTE) }
        binding.chipReminder.setOnClickListener { setType(EntryType.REMINDER) }

        binding.btnNoteEdit.isChecked = true
        binding.toggleNoteMode.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isChecked) {
                when (checkedId) {
                    R.id.btn_note_edit    -> showNoteEditMode()
                    R.id.btn_note_preview -> showNotePreviewMode()
                }
            }
        }

        // Checklist format toolbar – bold / italic WYSIWYG toggles for checklist item input
        binding.btnChecklistFormatBold.setOnClickListener   { toggleChecklistInlineFormat(Typeface.BOLD) }
        binding.btnChecklistFormatItalic.setOnClickListener { toggleChecklistInlineFormat(Typeface.ITALIC) }
        binding.editNewChecklistItem.addTextChangedListener(checklistFormatWatcher)

        binding.buttonAddChecklistItem.setOnClickListener {
            val editable = binding.editNewChecklistItem.text
            val plainText = editable?.toString()?.trim() ?: ""
            if (plainText.isNotBlank()) {
                val markdownText = RichTextConverter.spannableToMarkdown(editable!!)
                checklistAdapter.addItem(markdownText)
                binding.editNewChecklistItem.setText("")
                binding.editNewChecklistItem.clearFocus()
                resetChecklistFormatToggles()
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
        selectedDueAt = System.currentTimeMillis()
        binding.editDueDate.setText(DATE_FORMAT.format(Date(selectedDueAt!!)))

        binding.fabSaveEntry.setOnClickListener { saveEntry() }
        setType(currentType)

        if (args.entryId > 0) {
            viewModel.load(args.entryId)
        }

        viewModel.entry.observe(viewLifecycleOwner) { entry ->
            entry ?: return@observe
            editorHelper.setTitle(entry.title)
            setType(entry.type)
            when (entry.type) {
                EntryType.NOTE     -> editorHelper.setContent(entry.content)
                EntryType.TASK     -> {
                    checklistItems.clear()
                    checklistItems.addAll(ChecklistManager.fromJson(entry.checklistJson))
                    checklistAdapter.notifyDataSetChanged()
                }
                EntryType.REMINDER -> {
                    editorHelper.setContent(entry.content)
                    selectedAdvanceNoticeMinutes = entry.advanceNoticeMinutes
                    val label = advanceOptions.firstOrNull { it.first == entry.advanceNoticeMinutes }?.second
                        ?: advanceOptions[0].second
                    binding.spinnerAdvanceNotice.setText(label, false)
                    binding.spinnerAdvanceNotice.setAdapter(advanceNoticeAdapter)
                }
            }
            selectedDueAt = entry.dueAt ?: selectedDueAt ?: System.currentTimeMillis()
            binding.editDueDate.setText(DATE_FORMAT.format(Date(selectedDueAt!!)))
            binding.editTags.setText(entry.tags)

            // Restore style – also sync local vars since helpers don't fire callbacks here
            selectedColor = entry.color.also {
                if (it.isNotEmpty()) editorHelper.setTitleColor(it)
            }
            selectedContentColor = entry.contentColor.also {
                if (it.isNotEmpty()) editorHelper.setContentColor(it)
            }
            selectedFontFamily = entry.fontFamily.also {
                if (it.isNotEmpty()) editorHelper.setFontFamily(it)
            }
            selectedBackground = entry.background.also {
                if (it.isNotEmpty()) editorHelper.setBackground(it)
            }
            selectedTitleFontSize = entry.titleFontSize.also {
                if (it > 0f) editorHelper.setTitleFontSize(it)
            }
            selectedContentFontSize = entry.contentFontSize.also {
                if (it > 0f) editorHelper.setContentFontSize(it)
            }
        }

        viewModel.saveSuccessEvent.observe(viewLifecycleOwner) { saved ->
            if (saved) {
                viewModel.consumeSaveSuccessEvent()
                Toast.makeText(requireContext(), R.string.entry_saved, Toast.LENGTH_SHORT).show()
                findNavController().navigateUp()
            }
        }

        viewModel.saveErrorEvent.observe(viewLifecycleOwner) { error ->
            if (!error.isNullOrBlank()) {
                viewModel.consumeSaveErrorEvent()
                Toast.makeText(requireContext(), error, Toast.LENGTH_SHORT).show()
            }
        }
    }

    // ---------- type / mode switching ----------

    private fun setType(type: EntryType) {
        currentType = type
        binding.chipNote.isChecked     = type == EntryType.NOTE
        binding.chipReminder.isChecked = type == EntryType.REMINDER
        binding.layoutNoteContent.visibility = if (type == EntryType.NOTE || type == EntryType.REMINDER) View.VISIBLE else View.GONE
        binding.layoutReminderContent.visibility = View.VISIBLE
        binding.layoutAdvanceNotice.visibility = if (type == EntryType.REMINDER) View.VISIBLE else View.GONE
        if (type == EntryType.NOTE || type == EntryType.REMINDER) {
            binding.btnNoteEdit.isChecked = true
            showNoteEditMode()
        }
    }

    private fun showNoteEditMode() {
        binding.editorContent.root.visibility = View.VISIBLE
        binding.cardNotePreview.visibility    = View.GONE
    }

    private fun showNotePreviewMode() {
        markwon.setMarkdown(binding.textNotePreview, editorHelper.getContent())
        binding.editorContent.root.visibility = View.GONE
        binding.cardNotePreview.visibility    = View.VISIBLE
        editorHelper.resetContentFormatToggles()
    }

    // ---------- line-level prefix formatting ----------

    private fun applyLinePrefix(prefix: String) {
        val editText = binding.editorContent.editorEditContent
        val text = editText.text ?: return
        val cursor = editText.selectionStart.coerceAtLeast(0)
        val lineStart = (text.lastIndexOf('\n', cursor - 1) + 1).coerceAtLeast(0)
        text.insert(lineStart, prefix)
        editText.setSelection(cursor + prefix.length)
    }

    // ---------- checklist item inline-format toggle ----------

    private fun toggleChecklistInlineFormat(style: Int) {
        val editText = binding.editNewChecklistItem
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
                Typeface.BOLD   -> binding.btnChecklistFormatBold.isChecked   = isChecklistBoldActive
                Typeface.ITALIC -> binding.btnChecklistFormatItalic.isChecked = isChecklistItalicActive
            }
        } else {
            when (style) {
                Typeface.BOLD -> {
                    isChecklistBoldActive = !isChecklistBoldActive
                    binding.btnChecklistFormatBold.isChecked = isChecklistBoldActive
                }
                Typeface.ITALIC -> {
                    isChecklistItalicActive = !isChecklistItalicActive
                    binding.btnChecklistFormatItalic.isChecked = isChecklistItalicActive
                }
            }
        }
    }

    private fun resetChecklistFormatToggles() {
        isChecklistBoldActive = false
        isChecklistItalicActive = false
        binding.btnChecklistFormatBold.isChecked   = false
        binding.btnChecklistFormatItalic.isChecked = false
    }

    // ---------- date/time picker ----------

    private fun showDateTimePicker() {
        val cal = Calendar.getInstance().apply {
            selectedDueAt?.let { timeInMillis = it }
        }
        DatePickerDialog(requireContext(), { _, year, month, day ->
            cal.set(Calendar.YEAR, year)
            cal.set(Calendar.MONTH, month)
            cal.set(Calendar.DAY_OF_MONTH, day)
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)
            TimePickerDialog(requireContext(), { _, hour, minute ->
                cal.set(Calendar.HOUR_OF_DAY, hour)
                cal.set(Calendar.MINUTE, minute)
                selectedDueAt = cal.timeInMillis
                binding.editDueDate.setText(DATE_FORMAT.format(Date(selectedDueAt!!)))
            }, cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), true).show()
        }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show()
    }

    // ---------- save ----------

    private fun saveEntry() {
        val titleInputLayout = editorHelper.getTitleInputLayout()
        val titlePlain = binding.editorTitle.editorEditTitle.text?.toString()?.trim() ?: ""
        if (titlePlain.isEmpty()) {
            titleInputLayout.error = getString(R.string.error_title_required)
            return
        }
        titleInputLayout.error = null

        val entry = AgendaEntry(
            id                   = args.entryId.takeIf { it > 0 } ?: 0,
            title                = editorHelper.getTitle(),
            type                 = currentType,
            content              = when (currentType) {
                EntryType.NOTE, EntryType.REMINDER -> editorHelper.getContent()
                EntryType.TASK                     -> ""
            },
            checklistJson        = if (currentType == EntryType.TASK) ChecklistManager.toJson(checklistAdapter.getItems()) else "[]",
            dueAt                = selectedDueAt ?: System.currentTimeMillis(),
            advanceNoticeMinutes = if (currentType == EntryType.REMINDER) selectedAdvanceNoticeMinutes else 0,
            tags                 = binding.editTags.text?.toString()?.trim() ?: "",
            color                = selectedColor,
            contentColor         = selectedContentColor,
            fontFamily           = selectedFontFamily,
            background           = selectedBackground,
            titleFontSize        = selectedTitleFontSize,
            contentFontSize      = selectedContentFontSize
        )

        viewModel.save(entry)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private val DATE_FORMAT = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
    }
}
