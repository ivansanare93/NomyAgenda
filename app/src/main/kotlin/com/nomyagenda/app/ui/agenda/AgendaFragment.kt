package com.nomyagenda.app.ui.agenda

import android.os.Bundle
import android.view.*
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.nomyagenda.app.NomyAgendaApp
import com.nomyagenda.app.R
import com.nomyagenda.app.data.local.entity.AgendaEntry
import com.nomyagenda.app.data.repository.AgendaRepository
import com.nomyagenda.app.databinding.FragmentAgendaBinding

class AgendaFragment : Fragment() {

    private var _binding: FragmentAgendaBinding? = null
    private val binding get() = _binding!!

    private val viewModel: AgendaViewModel by viewModels {
        val app = requireActivity().application as NomyAgendaApp
        AgendaViewModelFactory(AgendaRepository(app.database.agendaEntryDao()))
    }

    private lateinit var adapter: AgendaAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentAgendaBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = AgendaAdapter(
            onClick = { entry -> openEditor(entry.id) },
            onLongClick = { entry -> confirmDelete(entry) }
        )
        binding.recyclerAgenda.adapter = adapter

        viewModel.entries.observe(viewLifecycleOwner) { entries ->
            adapter.submitList(entries)
            binding.textEmptyAgenda.visibility = if (entries.isEmpty()) View.VISIBLE else View.GONE
        }

        binding.editSearch.addTextChangedListener { text ->
            viewModel.setSearchQuery(text?.toString() ?: "")
        }

        binding.fabAddEvent.setOnClickListener { openEditor(0) }
    }

    private fun openEditor(entryId: Int) {
        val action = AgendaFragmentDirections.actionAgendaFragmentToEntryEditorFragment(entryId)
        findNavController().navigate(action)
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
}
