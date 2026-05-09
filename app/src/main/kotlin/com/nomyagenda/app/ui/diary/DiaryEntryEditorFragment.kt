package com.nomyagenda.app.ui.diary

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.*
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import com.nomyagenda.app.NomyAgendaApp
import com.nomyagenda.app.R
import com.nomyagenda.app.core.datetime.formatDiaryDateKey
import com.nomyagenda.app.databinding.FragmentDiaryEntryEditorBinding
import com.nomyagenda.app.ui.editor.RichTextEditorHelper
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

    private lateinit var editorHelper: RichTextEditorHelper
    private lateinit var photoAdapter: DiaryPhotoAdapter

    private val pickImage = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) viewModel.addPhoto(requireContext(), uri)
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

        // ── Shared rich-text editor ──────────────────────────────────────
        editorHelper = RichTextEditorHelper(
            context        = requireContext(),
            titleBinding   = binding.editorTitle,
            contentBinding = binding.editorContent,
            customBinding  = binding.editorCustomization
        )
        editorHelper.onTitleColorChanged      = { hex -> viewModel.color.value          = hex }
        editorHelper.onContentColorChanged    = { hex -> viewModel.contentColor.value   = hex }
        editorHelper.onFontFamilyChanged      = { id  -> viewModel.fontFamily.value     = id  }
        editorHelper.onBackgroundChanged      = { key -> viewModel.background.value     = key }
        editorHelper.onTitleFontSizeChanged   = { sp  -> viewModel.titleFontSize.value  = sp  }
        editorHelper.onContentFontSizeChanged = { sp  -> viewModel.contentFontSize.value = sp }
        editorHelper.setup()

        // Configure background categories for diary (includes "basic" palette)
        editorHelper.setBackgroundCategories(listOf(
            getString(R.string.diary_background_basic)    to DiaryBackgroundCatalog.basicBackgrounds,
            getString(R.string.diary_background_thematic) to DiaryBackgroundCatalog.thematicBackgrounds,
            getString(R.string.diary_background_festive)  to DiaryBackgroundCatalog.festiveBackgrounds,
            getString(R.string.diary_background_premium)  to DiaryBackgroundCatalog.premiumBackgrounds,
            getString(R.string.diary_background_photos)   to DiaryBackgroundCatalog.photoBackgrounds
        ))

        // Set diary-specific hints
        binding.editorTitle.editorTitleInputLayout.hint   = getString(R.string.diary_title_hint)
        binding.editorContent.editorContentInputLayout.hint = getString(R.string.diary_content_hint)

        // ── Diary-specific setup ─────────────────────────────────────────
        setupToolbar()
        setupMoodChips()
        setupPhotos()

        val defaultDateKey = if (args.dateKey.isNotEmpty()) args.dateKey else DiaryFragment.todayDateKey()
        viewModel.loadEntry(args.entryId, defaultDateKey)

        observeViewModel()
    }

    private fun setupToolbar() {
        binding.toolbarDiaryEditor.setNavigationOnClickListener { findNavController().navigateUp() }
        binding.toolbarDiaryEditor.inflateMenu(R.menu.menu_diary_editor)
        binding.toolbarDiaryEditor.setOnMenuItemClickListener { item ->
            if (item.itemId == R.id.action_save_diary) { saveDiaryEntry(); true } else false
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
                if (isChecked) viewModel.mood.value = emoji
                else if (viewModel.mood.value == emoji) viewModel.mood.value = ""
            }
            binding.chipGroupMood.addView(chip)
        }
    }

    private fun setupPhotos() {
        photoAdapter = DiaryPhotoAdapter(onRemove = { path -> viewModel.removePhoto(path) })
        binding.recyclerPhotos.adapter = photoAdapter
        binding.recyclerPhotos.layoutManager =
            LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        binding.btnAddPhoto.setOnClickListener { pickImage.launch("image/*") }
    }

    private fun observeViewModel() {
        binding.btnSelectDate.setOnClickListener { showDatePicker() }

        viewModel.dateKey.observe(viewLifecycleOwner) { dk ->
            binding.btnSelectDate.text = formatDiaryDateKey(dk ?: "")
        }

        viewModel.title.observe(viewLifecycleOwner) { t ->
            if (t == null) return@observe
            // Only update if different to avoid cursor reset while typing
            if (binding.editorTitle.editorEditTitle.text?.toString() != t) {
                editorHelper.setTitle(t)
            }
        }

        viewModel.content.observe(viewLifecycleOwner) { c ->
            if (c == null) return@observe
            editorHelper.setContent(c)
        }

        viewModel.mood.observe(viewLifecycleOwner) { currentMood ->
            for (i in 0 until binding.chipGroupMood.childCount) {
                val chip = binding.chipGroupMood.getChildAt(i)
                    as? com.google.android.material.chip.Chip ?: continue
                val shouldBeChecked = chip.text.toString() == currentMood
                if (chip.isChecked != shouldBeChecked) chip.isChecked = shouldBeChecked
            }
        }

        viewModel.photoPaths.observe(viewLifecycleOwner) { paths ->
            photoAdapter.submitList(paths)
        }

        viewModel.color.observe(viewLifecycleOwner) { hex ->
            editorHelper.setTitleColor(hex ?: "")
        }

        viewModel.contentColor.observe(viewLifecycleOwner) { hex ->
            editorHelper.setContentColor(hex ?: "")
        }

        viewModel.background.observe(viewLifecycleOwner) { bgKey ->
            editorHelper.setBackground(bgKey ?: "")
        }

        viewModel.fontFamily.observe(viewLifecycleOwner) { fontId ->
            editorHelper.setFontFamily(fontId ?: "")
        }

        viewModel.titleFontSize.observe(viewLifecycleOwner) { size ->
            editorHelper.setTitleFontSize(size ?: 0f)
        }

        viewModel.contentFontSize.observe(viewLifecycleOwner) { size ->
            editorHelper.setContentFontSize(size ?: 0f)
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
                viewModel.dateKey.value = "%04d-%02d-%02d".format(year, month + 1, day)
            },
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH),
            cal.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    private fun saveDiaryEntry() {
        viewModel.title.value   = editorHelper.getTitle()
        viewModel.content.value = editorHelper.getContent()
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
