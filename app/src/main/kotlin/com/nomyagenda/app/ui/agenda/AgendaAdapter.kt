package com.nomyagenda.app.ui.agenda

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.nomyagenda.app.data.local.entity.AgendaEntry
import com.nomyagenda.app.data.local.entity.EntryType
import com.nomyagenda.app.databinding.ItemAgendaEntryBinding
import com.nomyagenda.app.ui.editor.ChecklistManager
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AgendaAdapter(
    private val onClick: (AgendaEntry) -> Unit,
    private val onLongClick: (AgendaEntry) -> Unit
) : ListAdapter<AgendaEntry, AgendaAdapter.EntryViewHolder>(DIFF_CALLBACK) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EntryViewHolder {
        val binding = ItemAgendaEntryBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return EntryViewHolder(binding)
    }

    override fun onBindViewHolder(holder: EntryViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class EntryViewHolder(
        private val binding: ItemAgendaEntryBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(entry: AgendaEntry) {
            binding.textEntryTitle.text = entry.title

            binding.chipEntryType.text = when (entry.type) {
                EntryType.NOTE -> "Nota"
                EntryType.TASK -> "Tarea"
                EntryType.REMINDER -> "Recordatorio"
            }

            when (entry.type) {
                EntryType.NOTE -> {
                    binding.textEntryPreview.visibility = if (entry.content.isNotBlank()) View.VISIBLE else View.GONE
                    binding.textEntryPreview.text = entry.content.lines().firstOrNull()?.take(80) ?: ""
                }
                EntryType.TASK -> {
                    val items = ChecklistManager.fromJson(entry.checklistJson)
                    val done = items.count { it.done }
                    binding.textEntryPreview.visibility = if (items.isNotEmpty()) View.VISIBLE else View.GONE
                    binding.textEntryPreview.text = "$done/${items.size} completadas"
                }
                EntryType.REMINDER -> {
                    binding.textEntryPreview.visibility = if (entry.content.isNotBlank()) View.VISIBLE else View.GONE
                    binding.textEntryPreview.text = entry.content.lines().firstOrNull()?.take(80) ?: ""
                }
            }

            if (entry.dueAt != null) {
                binding.textEntryDueDate.visibility = View.VISIBLE
                binding.textEntryDueDate.text = DATE_FORMAT.format(Date(entry.dueAt))
            } else {
                binding.textEntryDueDate.visibility = View.GONE
            }

            if (entry.tags.isNotBlank()) {
                binding.textEntryTags.visibility = View.VISIBLE
                binding.textEntryTags.text = entry.tags.split(",").joinToString(" ") { "#${it.trim()}" }
            } else {
                binding.textEntryTags.visibility = View.GONE
            }

            if (entry.category.isNotBlank()) {
                binding.textEntryCategory.visibility = View.VISIBLE
                binding.textEntryCategory.text = entry.category
            } else {
                binding.textEntryCategory.visibility = View.GONE
            }

            binding.root.setOnClickListener { onClick(entry) }
            binding.root.setOnLongClickListener { onLongClick(entry); true }
        }
    }

    companion object {
        private val DATE_FORMAT = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())

        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<AgendaEntry>() {
            override fun areItemsTheSame(oldItem: AgendaEntry, newItem: AgendaEntry) = oldItem.id == newItem.id
            override fun areContentsTheSame(oldItem: AgendaEntry, newItem: AgendaEntry) = oldItem == newItem
        }
    }
}
