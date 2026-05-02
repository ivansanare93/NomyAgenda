package com.nomyagenda.app.ui.agenda

import android.os.Bundle
import android.view.*
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.doOnLayout
import androidx.core.view.updateLayoutParams
import androidx.core.view.updatePadding
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.nomyagenda.app.NomyAgendaApp
import com.nomyagenda.app.R
import com.nomyagenda.app.data.local.entity.AgendaEntry
import com.nomyagenda.app.data.local.entity.EntryType
import com.nomyagenda.app.data.preferences.SettingsRepository
import com.nomyagenda.app.databinding.FragmentAgendaBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AgendaFragment : Fragment() {

    private var _binding: FragmentAgendaBinding? = null
    private val binding get() = _binding!!

    private val viewModel: AgendaViewModel by viewModels {
        val app = requireActivity().application as NomyAgendaApp
        AgendaViewModelFactory(app.agendaRepository, app.reminderService)
    }

    private lateinit var adapter: AgendaAdapter

    private val settingsRepository by lazy { SettingsRepository(requireContext()) }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentAgendaBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        applyIllustrationBackground()

        // Show today's date in the header
        binding.textHeaderDate.text = HEADER_DATE_FORMAT.format(Date())

        adapter = AgendaAdapter(
            onClick = { entry -> openEditor(entry.id) },
            onLongClick = { entry -> confirmDelete(entry) }
        )
        binding.recyclerAgenda.adapter = adapter

        viewModel.entries.observe(viewLifecycleOwner) { entries ->
            adapter.submitList(entries)
            binding.layoutEmptyAgenda.visibility = if (entries.isEmpty()) View.VISIBLE else View.GONE
        }

        binding.editSearch.addTextChangedListener { text ->
            viewModel.setSearchQuery(text?.toString() ?: "")
        }

        setupFilterChips()

        val selectedDateKey = arguments?.getString(ARG_SELECTED_DATE_KEY)
        viewModel.setSelectedDate(selectedDateKey?.takeIf { it.isNotBlank() })

        binding.fabAddEvent.setOnClickListener { openEditor(0) }

        // Ensure FAB stays above the BottomNavigationView and system navigation bar on all devices.
        // bottomNavView absorbs system navigation bar insets into its own height (via Material's
        // built-in fitsSystemWindows behaviour), so maxOf the two avoids double-counting.
        val bottomNavView: View? = requireActivity().findViewById(R.id.bottom_navigation)
        val baseMargin = resources.getDimensionPixelSize(R.dimen.spacing_medium)
        val basePadding = resources.getDimensionPixelSize(R.dimen.spacing_xlarge)
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { _, insets ->
            val navBarHeight = insets.getInsets(WindowInsetsCompat.Type.navigationBars()).bottom
            val totalOffset = maxOf(navBarHeight, bottomNavView?.height ?: navBarHeight)

            binding.fabAddEvent.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                bottomMargin = baseMargin + totalOffset
            }
            binding.recyclerAgenda.updatePadding(bottom = basePadding + totalOffset)
            insets
        }
        // Re-apply insets once BottomNavigationView is measured in case height was 0 on first dispatch.
        bottomNavView?.doOnLayout { _binding?.let { b -> ViewCompat.requestApplyInsets(b.root) } }
    }

    override fun onResume() {
        super.onResume()
        applyIllustrationBackground()
    }

    private fun applyIllustrationBackground() {
        val drawableRes = when (settingsRepository.appBackground) {
            SettingsRepository.APP_BACKGROUND_FLORAL -> R.drawable.bg_illustration_floral
            SettingsRepository.APP_BACKGROUND_STARS -> R.drawable.bg_illustration_stars
            SettingsRepository.APP_BACKGROUND_GEOMETRIC -> R.drawable.bg_illustration_geometric
            SettingsRepository.APP_BACKGROUND_DOTS -> R.drawable.bg_illustration_dots
            SettingsRepository.APP_BACKGROUND_LEAVES -> R.drawable.bg_illustration_leaves
            SettingsRepository.APP_BACKGROUND_BUTTERFLY -> R.drawable.bg_illustration_butterfly
            SettingsRepository.APP_BACKGROUND_MANDALA -> R.drawable.bg_illustration_mandala
            SettingsRepository.APP_BACKGROUND_MOUNTAINS -> R.drawable.bg_illustration_mountains
            SettingsRepository.APP_BACKGROUND_WAVES -> R.drawable.bg_illustration_waves
            else -> null
        }
        if (drawableRes != null) {
            binding.root.background = ContextCompat.getDrawable(requireContext(), drawableRes)
        } else {
            val ta = requireContext().theme.obtainStyledAttributes(intArrayOf(R.attr.agendaBackground))
            val bgColor = ta.getColor(0, 0xFFFFFFFF.toInt())
            ta.recycle()
            binding.root.setBackgroundColor(bgColor)
        }
    }

    private fun setupFilterChips() {
        binding.chipGroupFilter.setOnCheckedStateChangeListener { _, checkedIds ->
            val filterType = when {
                checkedIds.contains(R.id.chip_filter_note) -> EntryType.NOTE
                checkedIds.contains(R.id.chip_filter_task) -> EntryType.TASK
                checkedIds.contains(R.id.chip_filter_reminder) -> EntryType.REMINDER
                else -> null
            }
            viewModel.setFilterType(filterType)
        }
    }

    private fun openEditor(entryId: Int) {
        if (entryId == 0) {
            val action = AgendaFragmentDirections.actionAgendaFragmentToEntryEditorFragment(entryId)
            findNavController().navigate(action)
        } else {
            val action = AgendaFragmentDirections.actionAgendaFragmentToEntryDetailFragment(entryId)
            findNavController().navigate(action)
        }
    }

    private fun confirmDelete(entry: AgendaEntry) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.delete_event)
            .setMessage(R.string.delete_event_confirm)
            .setPositiveButton(R.string.delete) { _, _ ->
                viewModel.deleteEntry(entry)
                Snackbar.make(binding.root, R.string.event_deleted, Snackbar.LENGTH_SHORT).show()
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val ARG_SELECTED_DATE_KEY = "selectedDateKey"
        private val HEADER_DATE_FORMAT = SimpleDateFormat("EEEE, d 'de' MMMM 'de' yyyy", Locale("es"))
    }
}
