package com.nomyagenda.app.ui.agenda

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.nomyagenda.app.data.local.entity.AgendaEvent
import com.nomyagenda.app.databinding.ItemAgendaPlaceholderBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AgendaAdapter(
    private val onLongClick: (AgendaEvent) -> Unit
) : ListAdapter<AgendaEvent, AgendaAdapter.EventViewHolder>(DIFF_CALLBACK) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EventViewHolder {
        val binding = ItemAgendaPlaceholderBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return EventViewHolder(binding)
    }

    override fun onBindViewHolder(holder: EventViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class EventViewHolder(
        private val binding: ItemAgendaPlaceholderBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(event: AgendaEvent) {
            binding.textEventTitle.text = event.title
            binding.textEventDate.text = DATE_FORMAT.format(Date(event.dateTimeMillis))
            binding.root.setOnLongClickListener {
                onLongClick(event)
                true
            }
        }
    }

    companion object {
        // SimpleDateFormat is not inherently thread-safe, but onBindViewHolder always
        // runs on the main thread, so reusing a single instance here is safe.
        private val DATE_FORMAT = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<AgendaEvent>() {
            override fun areItemsTheSame(oldItem: AgendaEvent, newItem: AgendaEvent) =
                oldItem.id == newItem.id

            override fun areContentsTheSame(oldItem: AgendaEvent, newItem: AgendaEvent) =
                oldItem == newItem
        }
    }
}
