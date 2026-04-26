package com.nomyagenda.app.ui.diary

import android.os.Bundle
import android.view.*
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.doOnLayout
import androidx.core.view.updateLayoutParams
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.nomyagenda.app.NomyAgendaApp
import com.nomyagenda.app.R
import com.nomyagenda.app.data.local.entity.DiaryEntry
import com.nomyagenda.app.databinding.FragmentDiaryBinding
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class DiaryFragment : Fragment() {

    private var _binding: FragmentDiaryBinding? = null
    private val binding get() = _binding!!

    private val viewModel: DiaryViewModel by viewModels {
        val app = requireActivity().application as NomyAgendaApp
        DiaryViewModelFactory(app.diaryRepository)
    }

    private lateinit var adapter: DiaryAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDiaryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.textDiaryDate.text = HEADER_DATE_FORMAT.format(Date())

        adapter = DiaryAdapter(
            onClick = { entry -> openDetail(entry.id) },
            onLongClick = { entry -> confirmDelete(entry) }
        )
        binding.recyclerDiary.adapter = adapter

        viewModel.entries.observe(viewLifecycleOwner) { entries ->
            adapter.submitList(entries)
            binding.layoutEmptyDiary.visibility =
                if (entries.isEmpty()) View.VISIBLE else View.GONE
        }

        binding.fabAddDiaryEntry.setOnClickListener { openEditor(0) }

        val bottomNavView: View? = requireActivity().findViewById(R.id.bottom_navigation)
        val baseMargin = resources.getDimensionPixelSize(R.dimen.spacing_medium)
        val basePadding = resources.getDimensionPixelSize(R.dimen.spacing_xlarge)
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { _, insets ->
            val navBarHeight = insets.getInsets(WindowInsetsCompat.Type.navigationBars()).bottom
            val totalOffset = maxOf(navBarHeight, bottomNavView?.height ?: navBarHeight)
            binding.fabAddDiaryEntry.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                bottomMargin = baseMargin + totalOffset
            }
            binding.recyclerDiary.updatePadding(bottom = basePadding + totalOffset)
            insets
        }
        bottomNavView?.doOnLayout { _binding?.let { b -> ViewCompat.requestApplyInsets(b.root) } }
    }

    private fun openDetail(entryId: Int) {
        val action = DiaryFragmentDirections.actionDiaryFragmentToDiaryEntryDetailFragment(entryId)
        findNavController().navigate(action)
    }

    private fun openEditor(entryId: Int) {
        val todayKey = todayDateKey()
        val action = DiaryFragmentDirections.actionDiaryFragmentToDiaryEntryEditorFragment(
            entryId = entryId,
            dateKey = if (entryId == 0) todayKey else ""
        )
        findNavController().navigate(action)
    }

    private fun confirmDelete(entry: DiaryEntry) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.diary_delete_title)
            .setMessage(R.string.diary_delete_confirm)
            .setPositiveButton(R.string.delete) { _, _ ->
                viewModel.deleteEntry(entry)
                Snackbar.make(binding.root, R.string.diary_deleted, Snackbar.LENGTH_SHORT).show()
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private val HEADER_DATE_FORMAT = SimpleDateFormat("EEEE, d 'de' MMMM 'de' yyyy", Locale("es"))
        private val DATE_KEY_FORMAT = SimpleDateFormat("yyyy-MM-dd", Locale.US)

        fun todayDateKey(): String = DATE_KEY_FORMAT.format(Calendar.getInstance().time)
    }
}
