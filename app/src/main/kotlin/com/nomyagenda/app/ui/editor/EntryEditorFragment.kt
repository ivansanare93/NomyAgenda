package com.nomyagenda.app.ui.editor

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.view.*
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
import com.nomyagenda.app.data.repository.AgendaRepository
import com.nomyagenda.app.databinding.FragmentEntryEditorBinding
import io.noties.markwon.Markwon
import java.text.SimpleDateFormat
import java.util.*

class EntryEditorFragment : Fragment() {

    private var _binding: FragmentEntryEditorBinding? = null
    private val binding get() = _binding!!

    private val args: EntryEditorFragmentArgs by navArgs()

    private val viewModel: EntryEditorViewModel by viewModels {
        val app = requireActivity().application as NomyAgendaApp
        EntryEditorViewModelFactory(AgendaRepository(app.database.agendaEntryDao()), app)
    }

    private lateinit var checklistAdapter: ChecklistAdapter
    private lateinit var markwon: Markwon
    private val checklistItems = mutableListOf<ChecklistItem>()
    private var selectedDueAt: Long? = null
    private var currentType: EntryType = EntryType.NOTE

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentEntryEditorBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        markwon = Markwon.create(requireContext())

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

        binding.btnFormatBold.setOnClickListener { applyInlineFormat("**") }
        binding.btnFormatItalic.setOnClickListener { applyInlineFormat("*") }
        binding.btnFormatStrikethrough.setOnClickListener { applyInlineFormat("~~") }
        binding.btnFormatHeading.setOnClickListener { applyLinePrefix("# ") }
        binding.btnFormatBullet.setOnClickListener { applyLinePrefix("- ") }
        binding.btnFormatNumbered.setOnClickListener { applyLinePrefix("1. ") }
        binding.btnFormatQuote.setOnClickListener { applyLinePrefix("> ") }

        binding.buttonAddChecklistItem.setOnClickListener {
            val text = binding.editNewChecklistItem.text?.toString()?.trim() ?: ""
            if (text.isNotBlank()) {
                checklistAdapter.addItem(text)
                binding.editNewChecklistItem.setText("")
                binding.editNewChecklistItem.clearFocus()
            }
        }

        binding.editDueDate.setOnClickListener { showDateTimePicker() }

        binding.fabSaveEntry.setOnClickListener { saveEntry() }

        if (args.entryId > 0) {
            viewModel.load(args.entryId)
        }

        viewModel.entry.observe(viewLifecycleOwner) { entry ->
            entry ?: return@observe
            binding.editEntryTitle.setText(entry.title)
            setType(entry.type)
            when (entry.type) {
                EntryType.NOTE -> binding.editNoteContent.setText(entry.content)
                EntryType.TASK -> {
                    checklistItems.clear()
                    checklistItems.addAll(ChecklistManager.fromJson(entry.checklistJson))
                    checklistAdapter.notifyDataSetChanged()
                }
                EntryType.REMINDER -> {
                    binding.editReminderContent.setText(entry.content)
                    entry.dueAt?.let { dueAt ->
                        selectedDueAt = dueAt
                        binding.editDueDate.setText(DATE_FORMAT.format(Date(dueAt)))
                    }
                }
            }
            binding.editTags.setText(entry.tags)
            binding.editCategory.setText(entry.category)
        }
    }

    private fun setType(type: EntryType) {
        currentType = type
        binding.chipNote.isChecked = type == EntryType.NOTE
        binding.chipTask.isChecked = type == EntryType.TASK
        binding.chipReminder.isChecked = type == EntryType.REMINDER
        binding.layoutNoteContent.visibility = if (type == EntryType.NOTE) View.VISIBLE else View.GONE
        binding.layoutTaskContent.visibility = if (type == EntryType.TASK) View.VISIBLE else View.GONE
        binding.layoutReminderContent.visibility = if (type == EntryType.REMINDER) View.VISIBLE else View.GONE
        if (type == EntryType.NOTE) {
            // Reset to edit mode when switching to NOTE type
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
        val markdown = binding.editNoteContent.text?.toString() ?: ""
        markwon.setMarkdown(binding.textNotePreview, markdown)
        binding.scrollFormatToolbar.visibility = View.GONE
        binding.inputLayoutNoteContent.visibility = View.GONE
        binding.cardNotePreview.visibility = View.VISIBLE
    }

    private fun applyInlineFormat(marker: String) {
        val editText = binding.editNoteContent
        val text = editText.text ?: return
        val start = editText.selectionStart.coerceAtLeast(0)
        val end = editText.selectionEnd.coerceAtLeast(0)
        if (start == end) {
            // No selection: insert markers and place cursor between them
            text.insert(start, "$marker$marker")
            editText.setSelection(start + marker.length)
        } else {
            // Wrap the selected text with the markers
            val selected = text.subSequence(start, end).toString()
            text.replace(start, end, "$marker$selected$marker")
            editText.setSelection(start + marker.length, start + marker.length + selected.length)
        }
    }

    private fun applyLinePrefix(prefix: String) {
        val editText = binding.editNoteContent
        val text = editText.text ?: return
        val cursor = editText.selectionStart.coerceAtLeast(0)
        val lineStart = (text.lastIndexOf('\n', cursor - 1) + 1).coerceAtLeast(0)
        text.insert(lineStart, prefix)
        editText.setSelection(cursor + prefix.length)
    }

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

    private fun saveEntry() {
        val title = binding.editEntryTitle.text?.toString()?.trim()
        if (title.isNullOrEmpty()) {
            binding.inputLayoutTitle.error = getString(R.string.error_title_required)
            return
        }
        binding.inputLayoutTitle.error = null

        val tags = binding.editTags.text?.toString()?.trim() ?: ""
        val category = binding.editCategory.text?.toString()?.trim() ?: ""

        val entry = AgendaEntry(
            id = args.entryId.takeIf { it > 0 } ?: 0,
            title = title,
            type = currentType,
            content = when (currentType) {
                EntryType.NOTE -> binding.editNoteContent.text?.toString()?.trim() ?: ""
                EntryType.REMINDER -> binding.editReminderContent.text?.toString()?.trim() ?: ""
                EntryType.TASK -> ""
            },
            checklistJson = if (currentType == EntryType.TASK) ChecklistManager.toJson(checklistAdapter.getItems()) else "[]",
            dueAt = if (currentType == EntryType.REMINDER) selectedDueAt else null,
            tags = tags,
            category = category
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
    }
}
