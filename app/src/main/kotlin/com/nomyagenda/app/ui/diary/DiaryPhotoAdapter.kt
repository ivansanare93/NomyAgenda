package com.nomyagenda.app.ui.diary

import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.nomyagenda.app.databinding.ItemDiaryPhotoBinding
import java.io.File

class DiaryPhotoAdapter(
    private val onRemove: (String) -> Unit
) : RecyclerView.Adapter<DiaryPhotoAdapter.PhotoViewHolder>() {

    private val paths = mutableListOf<String>()

    fun submitList(newPaths: List<String>) {
        paths.clear()
        paths.addAll(newPaths)
        notifyDataSetChanged()
    }

    override fun getItemCount() = paths.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PhotoViewHolder {
        val binding = ItemDiaryPhotoBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return PhotoViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PhotoViewHolder, position: Int) {
        holder.bind(paths[position])
    }

    inner class PhotoViewHolder(
        private val binding: ItemDiaryPhotoBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(path: String) {
            val file = File(path)
            if (file.exists()) {
                binding.imageDiaryPhoto.setImageURI(Uri.fromFile(file))
                binding.imageDiaryPhoto.visibility = View.VISIBLE
            } else {
                binding.imageDiaryPhoto.setImageDrawable(null)
            }
            binding.btnRemovePhoto.setOnClickListener { onRemove(path) }
        }
    }
}
