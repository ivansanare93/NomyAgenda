package com.nomyagenda.app.ui.detail

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.google.android.material.R as MaterialR
import com.nomyagenda.app.NomyAgendaApp
import com.nomyagenda.app.R
import com.nomyagenda.app.data.local.entity.EntryType
import com.nomyagenda.app.databinding.FragmentEntryDetailBinding
import com.nomyagenda.app.ui.common.font.FontCatalog
import com.nomyagenda.app.ui.editor.ChecklistManager
import com.nomyagenda.app.ui.resolveThemeColor
import io.noties.markwon.Markwon
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class EntryDetailFragment : Fragment() {

    private var _binding: FragmentEntryDetailBinding? = null
    private val binding get() = _binding!!

    private val args: EntryDetailFragmentArgs by navArgs()

    private val viewModel: EntryDetailViewModel by viewModels {
        val app = requireActivity().application as NomyAgendaApp
        EntryDetailViewModelFactory(app.agendaRepository)
    }

    private lateinit var markwon: Markwon

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentEntryDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        markwon = Markwon.create(requireContext())

        binding.toolbar.setNavigationOnClickListener { findNavController().navigateUp() }

        viewModel.load(args.entryId)

        viewModel.entry.observe(viewLifecycleOwner) { entry ->
            if (entry == null) return@observe

            // Type chip
            binding.chipDetailType.text = when (entry.type) {
                EntryType.NOTE -> getString(R.string.type_note)
                EntryType.TASK -> getString(R.string.type_task)
                EntryType.REMINDER -> getString(R.string.type_reminder)
            }

            // Title
            binding.textDetailTitle.text = entry.title
            if (entry.color.isNotEmpty()) {
                binding.textDetailTitle.setTextColor(Color.parseColor(entry.color))
            }

            // Due date
            if (entry.dueAt != null) {
                binding.textDetailDueDate.visibility = View.VISIBLE
                binding.textDetailDueDate.text = DATE_FORMAT.format(Date(entry.dueAt))
            } else {
                binding.textDetailDueDate.visibility = View.GONE
            }

            // Tags
            if (entry.tags.isNotBlank()) {
                binding.textDetailTags.visibility = View.VISIBLE
                binding.textDetailTags.text = entry.tags.split(",").joinToString(" ") { "#${it.trim()}" }
            } else {
                binding.textDetailTags.visibility = View.GONE
            }

            // Content
            val contentColor = if (entry.contentColor.isNotEmpty()) {
                Color.parseColor(entry.contentColor)
            } else {
                requireContext().resolveThemeColor(MaterialR.attr.colorOnBackground)
            }

            when (entry.type) {
                EntryType.NOTE, EntryType.REMINDER -> {
                    if (entry.content.isNotBlank()) {
                        binding.textDetailContent.visibility = View.VISIBLE
                        markwon.setMarkdown(binding.textDetailContent, entry.content)
                        binding.textDetailContent.setTextColor(contentColor)
                    } else {
                        binding.textDetailContent.visibility = View.GONE
                    }
                }
                EntryType.TASK -> {
                    val items = ChecklistManager.fromJson(entry.checklistJson)
                    if (items.isNotEmpty()) {
                        binding.textDetailContent.visibility = View.VISIBLE
                        val sb = StringBuilder()
                        items.forEachIndexed { index, item ->
                            val check = if (item.done) "☑" else "☐"
                            sb.append("$check  ${item.text}")
                            if (index < items.lastIndex) sb.append("\n")
                        }
                        binding.textDetailContent.text = sb.toString()
                        binding.textDetailContent.setTextColor(contentColor)
                    } else {
                        binding.textDetailContent.visibility = View.GONE
                    }
                }
            }
            // Font
            val typeface = FontCatalog.resolve(requireContext(), entry.fontFamily)
            binding.textDetailTitle.typeface = typeface
            binding.textDetailContent.typeface = typeface
        }

        binding.fabEditEntry.setOnClickListener {
            val action = EntryDetailFragmentDirections.actionEntryDetailFragmentToEntryEditorFragment(args.entryId)
            findNavController().navigate(action)
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
