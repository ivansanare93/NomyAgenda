package com.nomyagenda.app.ui.diary

import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.R as MaterialR
import com.nomyagenda.app.NomyAgendaApp
import com.nomyagenda.app.core.datetime.formatDiaryDateKey
import com.nomyagenda.app.databinding.FragmentDiaryEntryDetailBinding
import com.nomyagenda.app.ui.common.font.FontCatalog
import com.nomyagenda.app.ui.resolveThemeColor
import org.json.JSONArray

class DiaryEntryDetailFragment : Fragment() {

    private var _binding: FragmentDiaryEntryDetailBinding? = null
    private val binding get() = _binding!!

    private val args: DiaryEntryDetailFragmentArgs by navArgs()

    private val viewModel: DiaryEntryDetailViewModel by viewModels {
        val app = requireActivity().application as NomyAgendaApp
        DiaryEntryDetailViewModelFactory(app.diaryRepository)
    }

    private lateinit var photoAdapter: DiaryPhotoAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDiaryEntryDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.toolbarDiaryDetail.setNavigationOnClickListener {
            findNavController().navigateUp()
        }

        photoAdapter = DiaryPhotoAdapter(onRemove = {}, readOnly = true)
        binding.recyclerDetailPhotos.adapter = photoAdapter
        binding.recyclerDetailPhotos.layoutManager =
            LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)

        viewModel.load(args.entryId)

        viewModel.entry.observe(viewLifecycleOwner) { entry ->
            if (entry == null) return@observe

            binding.textDetailDiaryDate.text = formatDiaryDateKey(entry.dateKey)

            if (entry.mood.isNotEmpty()) {
                binding.textDetailDiaryMood.visibility = View.VISIBLE
                binding.textDetailDiaryMood.text = entry.mood
            } else {
                binding.textDetailDiaryMood.visibility = View.GONE
            }

            val titleColor = if (entry.color.isNotEmpty()) {
                Color.parseColor(entry.color)
            } else {
                requireContext().resolveThemeColor(MaterialR.attr.colorOnBackground)
            }

            if (entry.title.isNotBlank()) {
                binding.textDetailDiaryTitle.visibility = View.VISIBLE
                binding.textDetailDiaryTitle.text = entry.title
                binding.textDetailDiaryTitle.setTextColor(titleColor)
            } else {
                binding.textDetailDiaryTitle.visibility = View.GONE
            }

            binding.textDetailDiaryDate.setTextColor(titleColor)

            val contentColor = if (entry.contentColor.isNotEmpty()) {
                Color.parseColor(entry.contentColor)
            } else {
                requireContext().resolveThemeColor(MaterialR.attr.colorOnBackground)
            }

            if (entry.content.isNotBlank()) {
                binding.textDetailDiaryContent.visibility = View.VISIBLE
                binding.textDetailDiaryContent.text = entry.content
                binding.textDetailDiaryContent.setTextColor(contentColor)
            } else {
                binding.textDetailDiaryContent.visibility = View.GONE
            }

            val photoPaths = parsePhotoPaths(entry.photoPaths)
            if (photoPaths.isNotEmpty()) {
                binding.textDetailPhotosLabel.visibility = View.VISIBLE
                binding.recyclerDetailPhotos.visibility = View.VISIBLE
                photoAdapter.submitList(photoPaths)
            } else {
                binding.textDetailPhotosLabel.visibility = View.GONE
                binding.recyclerDetailPhotos.visibility = View.GONE
            }

            val bgDrawableRes = DiaryBackgroundCatalog.resolveDrawable(entry.background)
            if (bgDrawableRes != 0) {
                binding.imageDiaryBg.setImageResource(bgDrawableRes)
                binding.imageDiaryBg.visibility = View.VISIBLE
            } else {
                binding.imageDiaryBg.visibility = View.GONE
            }

            val typeface = FontCatalog.resolve(requireContext(), entry.fontFamily)
            binding.textDetailDiaryTitle.typeface = typeface
            binding.textDetailDiaryContent.typeface = typeface
        }

        binding.fabEditDiaryEntry.setOnClickListener {
            // entryId > 0: the editor loads the date from the existing entry; dateKey is unused
            val action = DiaryEntryDetailFragmentDirections
                .actionDiaryEntryDetailFragmentToDiaryEntryEditorFragment(
                    entryId = args.entryId,
                    dateKey = ""
                )
            findNavController().navigate(action)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private fun parsePhotoPaths(json: String): List<String> {
            return try {
                val arr = JSONArray(json)
                (0 until arr.length()).map { arr.getString(it) }
            } catch (_: Exception) {
                emptyList()
            }
        }
    }
}
