package com.nomyagenda.app.ui.diary

import android.app.DatePickerDialog
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
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
import com.nomyagenda.app.databinding.FragmentDiaryEntryEditorBinding
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

        COLOR_PALETTE.forEach { hexColor ->
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

        private val COLOR_PALETTE = listOf(

            // 🔴 Rojos
            "#FFCDD2", "#EF9A9A", "#E57373", "#F44336", "#D32F2F", "#B71C1C",

            // 🌸 Rosas
            "#F8BBD0", "#F48FB1", "#F06292", "#E91E63", "#C2185B", "#880E4F",

            // 💜 Morados
            "#E1BEE7", "#CE93D8", "#BA68C8", "#9C27B0", "#7B1FA2", "#4A148C",

            // 🟣 Morado profundo / índigo
            "#D1C4E9", "#B39DDB", "#9575CD", "#673AB7", "#512DA8", "#311B92",

            // 🔵 Azules
            "#BBDEFB", "#90CAF9", "#64B5F6", "#2196F3", "#1976D2", "#0D47A1",

            // 🌊 Azul claro / cielo
            "#B3E5FC", "#81D4FA", "#4FC3F7", "#03A9F4", "#0288D1", "#01579B",

            // 🟦 Cian / turquesa
            "#B2EBF2", "#80DEEA", "#4DD0E1", "#00BCD4", "#0097A7", "#006064",

            // 🟢 Verde azulado
            "#B2DFDB", "#80CBC4", "#4DB6AC", "#009688", "#00796B", "#004D40",

            // 🌿 Verdes
            "#C8E6C9", "#A5D6A7", "#81C784", "#4CAF50", "#388E3C", "#1B5E20",

            // 🍏 Verde lima
            "#DCEDC8", "#C5E1A5", "#AED581", "#8BC34A", "#689F38", "#33691E",

            // 🌼 Amarillos
            "#FFF9C4", "#FFF59D", "#FFF176", "#FFEB3B", "#FBC02D", "#F57F17",

            // 🌟 Ámbar / dorado
            "#FFECB3", "#FFE082", "#FFD54F", "#FFC107", "#FFA000", "#FF6F00",

            // 🍊 Naranjas
            "#FFE0B2", "#FFCC80", "#FFB74D", "#FF9800", "#F57C00", "#E65100",

            // 🔥 Naranja rojizo
            "#FFCCBC", "#FFAB91", "#FF8A65", "#FF5722", "#D84315", "#BF360C",

            // 🪵 Marrones
            "#D7CCC8", "#BCAAA4", "#A1887F", "#795548", "#5D4037", "#3E2723",

            // 🌫️ Grises azulados
            "#CFD8DC", "#B0BEC5", "#90A4AE", "#607D8B", "#455A64", "#263238",

            // ⚪ Neutros / grises
            "#FAFAFA", "#F5F5F5", "#EEEEEE", "#E0E0E0", "#BDBDBD", "#9E9E9E", "#616161", "#212121",

            // 🌈 Extras modernos (muy útiles en apps)
            "#FF6F61", // Coral
            "#FF9AA2", // Rosa pastel suave
            "#B5EAD7", // Verde menta
            "#C7CEEA", // Lavanda claro
            "#A0E7E5", // Aqua pastel
            "#FFD3B6", // Melocotón
            "#D5AAFF", // Lila brillante
            "#85E3FF", // Azul hielo
        )

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