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
import com.nomyagenda.app.databinding.FragmentDiaryEntryEditorBinding
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
        setupPhotos()

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
            binding.btnSelectDate.text = formatDateKey(dk ?: "")
        }

        viewModel.title.observe(viewLifecycleOwner) { t ->
            if (binding.editDiaryTitle.text.toString() != t) {
                binding.editDiaryTitle.setText(t)
            }
        }

        viewModel.content.observe(viewLifecycleOwner) { c ->
            if (binding.editDiaryContent.text.toString() != c) {
                binding.editDiaryContent.setText(c)
            }
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

        viewModel.isSaved.observe(viewLifecycleOwner) { saved ->
            if (saved) {
                Toast.makeText(requireContext(), R.string.diary_saved, Toast.LENGTH_SHORT).show()
                findNavController().navigateUp()
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
            } catch (_: Exception) { /* use today */ }
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
        viewModel.title.value = binding.editDiaryTitle.text.toString()
        viewModel.content.value = binding.editDiaryContent.text.toString()
        viewModel.save()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private val DATE_PARSE_FORMAT = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        private val DATE_DISPLAY_FORMAT = SimpleDateFormat("EEEE, d 'de' MMMM 'de' yyyy", Locale("es"))

        fun formatDateKey(dateKey: String): String {
            return try {
                val date = DATE_PARSE_FORMAT.parse(dateKey) ?: return dateKey
                DATE_DISPLAY_FORMAT.format(date).replaceFirstChar { it.uppercase() }
            } catch (_: Exception) {
                dateKey
            }
        }
    }
}
