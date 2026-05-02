package com.nomyagenda.app.ui.diary

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.nomyagenda.app.R
import com.nomyagenda.app.core.datetime.formatDiaryDateKey
import com.nomyagenda.app.data.local.entity.DiaryEntry
import com.nomyagenda.app.databinding.ItemDiaryEntryBinding
import com.nomyagenda.app.ui.common.font.FontCatalog
import org.json.JSONArray

class DiaryAdapter(
    private val onClick: (DiaryEntry) -> Unit,
    private val onLongClick: (DiaryEntry) -> Unit
) : ListAdapter<DiaryEntry, DiaryAdapter.DiaryViewHolder>(DIFF_CALLBACK) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DiaryViewHolder {
        val binding = ItemDiaryEntryBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return DiaryViewHolder(binding)
    }

    override fun onBindViewHolder(holder: DiaryViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class DiaryViewHolder(
        private val binding: ItemDiaryEntryBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(entry: DiaryEntry) {
            val context = binding.root.context
            binding.textDiaryEntryDate.text = formatDiaryDateKey(entry.dateKey)

            val titleColor = if (entry.color.isNotEmpty()) {
                Color.parseColor(entry.color)
            } else {
                ContextCompat.getColor(context, R.color.md_theme_light_onSurface)
            }
            binding.textDiaryEntryDate.setTextColor(titleColor)
            binding.textDiaryEntryTitle.setTextColor(titleColor)

            val contentColor = if (entry.contentColor.isNotEmpty()) {
                Color.parseColor(entry.contentColor)
            } else {
                ContextCompat.getColor(context, R.color.md_theme_light_onSurface)
            }
            binding.textDiaryEntryPreview.setTextColor(contentColor)

            if (entry.mood.isNotEmpty()) {
                binding.textDiaryEntryMood.visibility = View.VISIBLE
                binding.textDiaryEntryMood.text = entry.mood
            } else {
                binding.textDiaryEntryMood.visibility = View.GONE
            }

            if (entry.title.isNotBlank()) {
                binding.textDiaryEntryTitle.visibility = View.VISIBLE
                binding.textDiaryEntryTitle.text = entry.title
            } else {
                binding.textDiaryEntryTitle.visibility = View.GONE
            }

            if (entry.content.isNotBlank()) {
                binding.textDiaryEntryPreview.visibility = View.VISIBLE
                binding.textDiaryEntryPreview.text = entry.content.take(PREVIEW_MAX_CHARS)
            } else {
                binding.textDiaryEntryPreview.visibility = View.GONE
            }

            val photoCount = countPhotos(entry.photoPaths)
            if (photoCount > 0) {
                binding.textDiaryPhotoCount.visibility = View.VISIBLE
                val ctx = binding.root.context
                binding.textDiaryPhotoCount.text = ctx.resources.getQuantityString(
                    com.nomyagenda.app.R.plurals.diary_photo_count, photoCount, photoCount
                )
            } else {
                binding.textDiaryPhotoCount.visibility = View.GONE
            }

            val typeface = FontCatalog.resolve(context, entry.fontFamily)
            binding.textDiaryEntryDate.typeface = typeface
            binding.textDiaryEntryTitle.typeface = typeface
            binding.textDiaryEntryPreview.typeface = typeface
            binding.textDiaryPhotoCount.typeface = typeface

            binding.root.setOnClickListener { onClick(entry) }
            binding.root.setOnLongClickListener { onLongClick(entry); true }

            val bgDrawableRes = DiaryBackgroundCatalog.resolveDrawable(entry.background)
            if (bgDrawableRes != 0) {
                binding.imageDiaryBackground.visibility = View.VISIBLE
                binding.imageDiaryBackground.setImageResource(bgDrawableRes)
            } else {
                binding.imageDiaryBackground.visibility = View.GONE
                binding.imageDiaryBackground.setImageDrawable(null)
            }
        }
    }

    companion object {
        private const val PREVIEW_MAX_CHARS = 150
        private fun countPhotos(photoPaths: String): Int {
            return try {
                JSONArray(photoPaths).length()
            } catch (_: Exception) {
                0
            }
        }

        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<DiaryEntry>() {
            override fun areItemsTheSame(oldItem: DiaryEntry, newItem: DiaryEntry) =
                oldItem.id == newItem.id
            override fun areContentsTheSame(oldItem: DiaryEntry, newItem: DiaryEntry) =
                oldItem == newItem
        }
    }
}
