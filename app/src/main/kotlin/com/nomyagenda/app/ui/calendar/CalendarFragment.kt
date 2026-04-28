package com.nomyagenda.app.ui.calendar

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.GridLayout
import android.widget.TextView
import androidx.core.os.bundleOf
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.google.android.material.color.MaterialColors
import com.nomyagenda.app.NomyAgendaApp
import com.nomyagenda.app.R
import com.nomyagenda.app.core.datetime.toDateKey
import com.nomyagenda.app.databinding.FragmentCalendarBinding
import com.nomyagenda.app.databinding.ItemCalendarMonthDayBinding
import com.nomyagenda.app.ui.agenda.AgendaFragment
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class CalendarFragment : Fragment() {

    private var _binding: FragmentCalendarBinding? = null
    private val binding get() = _binding!!

    private val viewModel: CalendarViewModel by viewModels {
        val app = requireActivity().application as NomyAgendaApp
        CalendarViewModelFactory(app.agendaRepository)
    }

    private val currentMonth = Calendar.getInstance().apply {
        set(Calendar.DAY_OF_MONTH, 1)
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }

    private var markedDates: Set<String> = emptySet()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentCalendarBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupWeekdayHeader()
        setupMonthNavigation()
        renderMonth()

        viewModel.entryDates.observe(viewLifecycleOwner) { dates ->
            markedDates = dates
            renderMonth()
        }
    }

    private fun setupMonthNavigation() {
        binding.btnPrevMonth.setOnClickListener {
            currentMonth.add(Calendar.MONTH, -1)
            renderMonth()
        }
        binding.btnNextMonth.setOnClickListener {
            currentMonth.add(Calendar.MONTH, 1)
            renderMonth()
        }
    }

    private fun setupWeekdayHeader() {
        val weekdays = listOf(
            getString(R.string.calendar_weekday_monday),
            getString(R.string.calendar_weekday_tuesday),
            getString(R.string.calendar_weekday_wednesday),
            getString(R.string.calendar_weekday_thursday),
            getString(R.string.calendar_weekday_friday),
            getString(R.string.calendar_weekday_saturday),
            getString(R.string.calendar_weekday_sunday)
        )
        weekdays.forEach { dayLabel ->
            val tv = TextView(requireContext()).apply {
                text = dayLabel
                textAlignment = View.TEXT_ALIGNMENT_CENTER
                setTextAppearance(com.google.android.material.R.style.TextAppearance_Material3_LabelSmall)
                setTextColor(MaterialColors.getColor(this, com.google.android.material.R.attr.colorOnSurfaceVariant))
                layoutParams = GridLayout.LayoutParams().apply {
                    width = 0
                    height = ViewGroup.LayoutParams.WRAP_CONTENT
                    columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
                }
            }
            binding.gridWeekdays.addView(tv)
        }
    }

    private fun renderMonth() {
        binding.textMonthTitle.text = MONTH_FORMAT.format(currentMonth.time)
        binding.gridDays.removeAllViews()

        val firstDayOfWeek = ((currentMonth.get(Calendar.DAY_OF_WEEK) - Calendar.MONDAY + 7) % 7)
        val daysInMonth = currentMonth.getActualMaximum(Calendar.DAY_OF_MONTH)
        val todayKey = Calendar.getInstance().timeInMillis.toDateKey()

        repeat(firstDayOfWeek) {
            binding.gridDays.addView(createEmptyCell())
        }

        for (day in 1..daysInMonth) {
            val dayCal = currentMonth.clone() as Calendar
            dayCal.set(Calendar.DAY_OF_MONTH, day)
            val dateKey = dayCal.timeInMillis.toDateKey()
            val hasEntries = markedDates.contains(dateKey)
            val isToday = dateKey == todayKey
            binding.gridDays.addView(createDayCell(day, hasEntries, isToday))
        }
    }

    private fun createEmptyCell(): View {
        return View(requireContext()).apply {
            layoutParams = GridLayout.LayoutParams().apply {
                width = 0
                height = resources.getDimensionPixelSize(R.dimen.calendar_day_cell_height)
                columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
            }
        }
    }

    private fun createDayCell(day: Int, hasEntries: Boolean, isToday: Boolean): View {
        val dayBinding = ItemCalendarMonthDayBinding.inflate(layoutInflater, binding.gridDays, false)
        dayBinding.textDayNumber.text = day.toString()
        dayBinding.dotEntries.visibility = if (hasEntries) View.VISIBLE else View.INVISIBLE

        if (isToday) {
            dayBinding.textDayNumber.background =
                ContextCompat.getDrawable(requireContext(), R.drawable.bg_calendar_day_today)
            dayBinding.textDayNumber.setTextColor(
                MaterialColors.getColor(dayBinding.root, com.google.android.material.R.attr.colorPrimary)
            )
        }

        dayBinding.root.layoutParams = GridLayout.LayoutParams().apply {
            width = 0
            height = resources.getDimensionPixelSize(R.dimen.calendar_day_cell_height)
            columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
        }
        dayBinding.root.setOnClickListener {
            val dateKey = (currentMonth.clone() as Calendar).apply {
                set(Calendar.DAY_OF_MONTH, day)
            }.timeInMillis.toDateKey()
            findNavController().navigate(
                R.id.agendaFragment,
                bundleOf(AgendaFragment.ARG_SELECTED_DATE_KEY to dateKey)
            )
        }
        return dayBinding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private val MONTH_FORMAT = SimpleDateFormat("MMMM yyyy", Locale("es"))
    }
}
