package com.nomyagenda.app.ui.agenda

import android.os.Bundle
import android.view.*
import android.widget.LinearLayout
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
import com.google.android.material.color.MaterialColors
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.nomyagenda.app.NomyAgendaApp
import com.nomyagenda.app.R
import com.nomyagenda.app.data.local.entity.AgendaEntry
import com.nomyagenda.app.data.local.entity.EntryType
import com.nomyagenda.app.data.local.entity.SortOrder
import com.nomyagenda.app.data.preferences.SettingsRepository
import com.nomyagenda.app.databinding.FragmentAgendaBinding
import com.nomyagenda.app.databinding.ItemCalendarDayBinding
import com.nomyagenda.app.ui.agenda.AgendaViewModel.Companion.toDateKey
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class AgendaFragment : Fragment() {

    private var _binding: FragmentAgendaBinding? = null
    private val binding get() = _binding!!

    private val viewModel: AgendaViewModel by viewModels {
        val app = requireActivity().application as NomyAgendaApp
        AgendaViewModelFactory(app.agendaRepository, app)
    }

    private lateinit var adapter: AgendaAdapter

    private val settingsRepository by lazy { SettingsRepository(requireContext()) }

    // Calendar week strip state
    private var currentWeekStart: Calendar = mondayOf(Calendar.getInstance())
    private var selectedDateKey: String? = null
    private val dayBindings = arrayOfNulls<ItemCalendarDayBinding>(7)

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
        setupSortButton()
        setupCalendarStrip()

        viewModel.entryDates.observe(viewLifecycleOwner) { dates ->
            updateCalendarDays(dates)
        }

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

    private fun setupCalendarStrip() {
        // Inflate 7 day cells into the week LinearLayout
        for (i in 0..6) {
            val dayBinding = ItemCalendarDayBinding.inflate(layoutInflater, binding.layoutCalendarDays, false)
            dayBinding.root.layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            binding.layoutCalendarDays.addView(dayBinding.root)
            dayBindings[i] = dayBinding
            val index = i
            dayBinding.root.setOnClickListener {
                val day = currentWeekStart.clone() as Calendar
                day.add(Calendar.DAY_OF_MONTH, index)
                val dateKey = day.timeInMillis.toDateKey()
                selectedDateKey = if (selectedDateKey == dateKey) null else dateKey
                viewModel.setSelectedDate(selectedDateKey)
                updateCalendarDays(viewModel.entryDates.value ?: emptySet())
            }
        }

        binding.btnPrevWeek.setOnClickListener {
            currentWeekStart.add(Calendar.WEEK_OF_YEAR, -1)
            updateCalendarHeader()
            updateCalendarDays(viewModel.entryDates.value ?: emptySet())
        }

        binding.btnNextWeek.setOnClickListener {
            currentWeekStart.add(Calendar.WEEK_OF_YEAR, 1)
            updateCalendarHeader()
            updateCalendarDays(viewModel.entryDates.value ?: emptySet())
        }

        updateCalendarHeader()
        updateCalendarDays(emptySet())
    }

    private fun updateCalendarHeader() {
        val endOfWeek = currentWeekStart.clone() as Calendar
        endOfWeek.add(Calendar.DAY_OF_MONTH, 6)
        val startDay = currentWeekStart.get(Calendar.DAY_OF_MONTH)
        val endDay = endOfWeek.get(Calendar.DAY_OF_MONTH)
        val startMonth = currentWeekStart.get(Calendar.MONTH)
        val endMonth = endOfWeek.get(Calendar.MONTH)
        val weekHeaderText = if (startMonth == endMonth) {
            "$startDay – $endDay ${MONTH_YEAR_FORMAT.format(endOfWeek.time)}"
        } else {
            "${DAY_MONTH_FORMAT.format(currentWeekStart.time)} – $endDay ${MONTH_YEAR_FORMAT.format(endOfWeek.time)}"
        }
        binding.textWeekRange.text = weekHeaderText
    }

    private fun updateCalendarDays(datesWithEntries: Set<String>) {
        val todayKey = Calendar.getInstance().timeInMillis.toDateKey()
        for (i in 0..6) {
            val dayBinding = dayBindings[i] ?: continue
            val day = currentWeekStart.clone() as Calendar
            day.add(Calendar.DAY_OF_MONTH, i)
            val dateKey = day.timeInMillis.toDateKey()

            dayBinding.textDayName.text = DAY_NAME_FORMAT.format(day.time).uppercase()
            dayBinding.textDayNumber.text = day.get(Calendar.DAY_OF_MONTH).toString()
            dayBinding.dotEntries.visibility =
                if (datesWithEntries.contains(dateKey)) View.VISIBLE else View.INVISIBLE

            val isSelected = dateKey == selectedDateKey
            val isToday = dateKey == todayKey
            val ctx = requireContext()
            when {
                isSelected -> {
                    dayBinding.textDayNumber.background =
                        ContextCompat.getDrawable(ctx, R.drawable.bg_calendar_day_selected)
                    dayBinding.textDayNumber.setTextColor(
                        MaterialColors.getColor(dayBinding.root, com.google.android.material.R.attr.colorOnPrimary)
                    )
                }
                isToday -> {
                    dayBinding.textDayNumber.background =
                        ContextCompat.getDrawable(ctx, R.drawable.bg_calendar_day_today)
                    dayBinding.textDayNumber.setTextColor(
                        MaterialColors.getColor(dayBinding.root, com.google.android.material.R.attr.colorPrimary)
                    )
                }
                else -> {
                    dayBinding.textDayNumber.background = null
                    dayBinding.textDayNumber.setTextColor(
                        MaterialColors.getColor(dayBinding.root, com.google.android.material.R.attr.colorOnBackground)
                    )
                }
            }
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

    private fun setupSortButton() {
        val sortLabels = arrayOf(
            getString(R.string.sort_due_date),
            getString(R.string.sort_created_at),
            getString(R.string.sort_category)
        )
        val sortOrders = arrayOf(SortOrder.DUE_DATE, SortOrder.CREATED_AT, SortOrder.CATEGORY)

        binding.btnSort.setOnClickListener {
            val currentIndex = sortOrders.indexOf(viewModel.currentSortOrder).coerceAtLeast(0)
            MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.sort_by)
                .setSingleChoiceItems(sortLabels, currentIndex) { dialog, which ->
                    viewModel.setSortOrder(sortOrders[which])
                    dialog.dismiss()
                }
                .show()
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
        private val HEADER_DATE_FORMAT = SimpleDateFormat("EEEE, d 'de' MMMM 'de' yyyy", Locale("es"))
        private val DAY_NAME_FORMAT = SimpleDateFormat("EEE", Locale.getDefault())
        private val MONTH_YEAR_FORMAT = SimpleDateFormat("MMM yyyy", Locale.getDefault())
        private val DAY_MONTH_FORMAT = SimpleDateFormat("d MMM", Locale.getDefault())

        /** Returns a new Calendar set to the Monday of the week containing [from]. */
        private fun mondayOf(from: Calendar): Calendar {
            val cal = from.clone() as Calendar
            val dow = cal.get(Calendar.DAY_OF_WEEK)
            val daysFromMonday = (dow - Calendar.MONDAY + 7) % 7
            cal.add(Calendar.DAY_OF_MONTH, -daysFromMonday)
            cal.set(Calendar.HOUR_OF_DAY, 0)
            cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)
            return cal
        }
    }
}
