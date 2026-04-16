package com.nomyagenda.app.ui.editor

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.nomyagenda.app.data.local.entity.ChecklistItem
import com.nomyagenda.app.databinding.ItemChecklistBinding

class ChecklistAdapter(
    private val items: MutableList<ChecklistItem>,
    private val onChanged: () -> Unit
) : RecyclerView.Adapter<ChecklistAdapter.ChecklistViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChecklistViewHolder {
        val binding = ItemChecklistBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ChecklistViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ChecklistViewHolder, position: Int) {
        holder.bind(items[position], position)
    }

    override fun getItemCount(): Int = items.size

    fun addItem(text: String) {
        if (text.isBlank()) return
        items.add(ChecklistItem(text = text.trim(), done = false))
        notifyItemInserted(items.size - 1)
        onChanged()
    }

    fun getItems(): List<ChecklistItem> = items.toList()

    inner class ChecklistViewHolder(
        private val binding: ItemChecklistBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: ChecklistItem, position: Int) {
            binding.checkboxItem.isChecked = item.done
            binding.checkboxItem.text = item.text
            binding.checkboxItem.setOnCheckedChangeListener { _, isChecked ->
                items[position] = items[position].copy(done = isChecked)
                onChanged()
            }
            binding.buttonDeleteItem.setOnClickListener {
                items.removeAt(position)
                notifyItemRemoved(position)
                notifyItemRangeChanged(position, items.size)
                onChanged()
            }
        }
    }
}
