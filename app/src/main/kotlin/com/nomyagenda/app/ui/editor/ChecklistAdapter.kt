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
        holder.bind(items[position])
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

        fun bind(item: ChecklistItem) {
            binding.checkboxItem.setOnCheckedChangeListener(null)
            binding.checkboxItem.isChecked = item.done
            binding.checkboxItem.text = item.text
            binding.checkboxItem.setOnCheckedChangeListener { _, isChecked ->
                @Suppress("DEPRECATION")
                val pos = adapterPosition
                if (pos != RecyclerView.NO_POSITION) {
                    items[pos] = items[pos].copy(done = isChecked)
                    onChanged()
                }
            }
            binding.buttonDeleteItem.setOnClickListener {
                @Suppress("DEPRECATION")
                val pos = adapterPosition
                if (pos != RecyclerView.NO_POSITION) {
                    items.removeAt(pos)
                    notifyItemRemoved(pos)
                    notifyItemRangeChanged(pos, items.size)
                    onChanged()
                }
            }
        }
    }
}
