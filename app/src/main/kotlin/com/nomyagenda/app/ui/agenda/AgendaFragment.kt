package com.nomyagenda.app.ui.agenda

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.nomyagenda.app.NomyAgendaApp
import com.nomyagenda.app.R
import com.nomyagenda.app.data.local.entity.AgendaEvent
import com.nomyagenda.app.data.repository.AgendaRepository
import com.nomyagenda.app.databinding.DialogAddEventBinding
import com.nomyagenda.app.databinding.FragmentAgendaBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AgendaFragment : Fragment() {

    private var _binding: FragmentAgendaBinding? = null
    private val binding get() = _binding!!

    private val viewModel: AgendaViewModel by viewModels {
        val app = requireActivity().application as NomyAgendaApp
        AgendaViewModelFactory(AgendaRepository(app.database.agendaEventDao()))
    }

    private lateinit var adapter: AgendaAdapter
    private var selectedDateMillis: Long = System.currentTimeMillis()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAgendaBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = AgendaAdapter { event -> confirmDeleteEvent(event) }
        binding.recyclerAgenda.adapter = adapter

        viewModel.events.observe(viewLifecycleOwner) { events ->
            adapter.submitList(events)
            binding.textEmptyAgenda.visibility =
                if (events.isEmpty()) View.VISIBLE else View.GONE
        }

        binding.fabAddEvent.setOnClickListener { showAddEventDialog() }
    }

    private fun showAddEventDialog() {
        selectedDateMillis = System.currentTimeMillis()
        val dialogBinding = DialogAddEventBinding.inflate(layoutInflater)
        dialogBinding.editEventDate.setText(formatDate(selectedDateMillis))
        dialogBinding.editEventDate.setOnClickListener {
            showDatePicker { millis ->
                selectedDateMillis = millis
                dialogBinding.editEventDate.setText(formatDate(millis))
            }
        }
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.add_event)
            .setView(dialogBinding.root)
            .setPositiveButton(R.string.save) { _, _ ->
                val title = dialogBinding.editEventTitle.text?.toString()?.trim()
                if (!title.isNullOrEmpty()) {
                    viewModel.addEvent(AgendaEvent(title = title, dateTimeMillis = selectedDateMillis))
                }
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun showDatePicker(onDateSelected: (Long) -> Unit) {
        val picker = MaterialDatePicker.Builder.datePicker()
            .setTitleText(R.string.select_date)
            .setSelection(selectedDateMillis)
            .build()
        picker.addOnPositiveButtonClickListener { millis -> onDateSelected(millis) }
        picker.show(parentFragmentManager, "date_picker")
    }

    private fun confirmDeleteEvent(event: AgendaEvent) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.delete_event)
            .setMessage(R.string.delete_event_confirm)
            .setPositiveButton(R.string.delete) { _, _ ->
                viewModel.deleteEvent(event)
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
        // Reused across calls; safe because all usages are on the main thread
        private val DATE_FORMAT = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

        fun formatDate(millis: Long): String = DATE_FORMAT.format(Date(millis))
    }
}
