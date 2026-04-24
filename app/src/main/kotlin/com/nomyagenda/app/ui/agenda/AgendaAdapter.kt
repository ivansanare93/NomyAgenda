package com.nomyagenda.app.ui.agenda

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.nomyagenda.app.R
import com.nomyagenda.app.data.local.entity.AgendaEntry
import com.nomyagenda.app.data.local.entity.EntryType
import com.nomyagenda.app.databinding.ItemAgendaEntryBinding
import com.nomyagenda.app.ui.editor.ChecklistManager
import com.nomyagenda.app.ui.resolveThemeColor
import io.noties.markwon.Markwon
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AgendaAdapter(
    private val onClick: (AgendaEntry) -> Unit,
    private val onLongClick: (AgendaEntry) -> Unit
) : ListAdapter<AgendaEntry, AgendaAdapter.EntryViewHolder>(DIFF_CALLBACK) {

    private lateinit var markwon: Markwon

    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        super.onAttachedToRecyclerView(recyclerView)
        markwon = Markwon.create(recyclerView.context)
    }

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
            val context = binding.root.context
            binding.textEntryTitle.text = entry.title

            binding.chipEntryType.text = when (entry.type) {
                EntryType.NOTE -> context.getString(R.string.type_note)
                EntryType.TASK -> context.getString(R.string.type_task)
                EntryType.REMINDER -> context.getString(R.string.type_reminder)
            }

            val (cardColorAttr, accentColorAttr) = when (entry.type) {
                EntryType.NOTE -> R.attr.noteCardBackground to R.attr.noteAccentColor
                EntryType.TASK -> R.attr.taskCardBackground to R.attr.taskAccentColor
                EntryType.REMINDER -> R.attr.reminderCardBackground to R.attr.reminderAccentColor
            }
            binding.root.setCardBackgroundColor(context.resolveThemeColor(cardColorAttr))
            if (entry.color.isNotEmpty()) {
                binding.viewAccentStripe.setBackgroundColor(android.graphics.Color.parseColor(entry.color))
            } else {
                binding.viewAccentStripe.setBackgroundColor(context.resolveThemeColor(accentColorAttr))
            }

            when (entry.type) {
                EntryType.NOTE -> {
                    if (entry.content.isNotBlank()) {
                        binding.textEntryPreview.visibility = View.VISIBLE
                        markwon.setMarkdown(binding.textEntryPreview, entry.content.take(PREVIEW_MAX_CHARS))
                    } else {
                        binding.textEntryPreview.visibility = View.GONE
                    }
                }
                EntryType.TASK -> {
                    val items = ChecklistManager.fromJson(entry.checklistJson)
                    val done = items.count { it.done }
                    binding.textEntryPreview.visibility = if (items.isNotEmpty()) View.VISIBLE else View.GONE
                    binding.textEntryPreview.text = "$done/${items.size} completadas"
                }
                EntryType.REMINDER -> {
                    if (entry.content.isNotBlank()) {
                        binding.textEntryPreview.visibility = View.VISIBLE
                        markwon.setMarkdown(binding.textEntryPreview, entry.content.take(PREVIEW_MAX_CHARS))
                    } else {
                        binding.textEntryPreview.visibility = View.GONE
                    }
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

            binding.textEntryCategory.visibility = View.GONE

            binding.root.setOnClickListener { onClick(entry) }
            binding.root.setOnLongClickListener { onLongClick(entry); true }
        }
    }

    companion object {
        private val DATE_FORMAT = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
        private const val PREVIEW_MAX_CHARS = 200

        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<AgendaEntry>() {
            override fun areItemsTheSame(oldItem: AgendaEntry, newItem: AgendaEntry) = oldItem.id == newItem.id
            override fun areContentsTheSame(oldItem: AgendaEntry, newItem: AgendaEntry) = oldItem == newItem
        }
    }
}
