package com.example.classhub.adapters

import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.classhub.data.models.ResourceFile
import com.example.classhub.databinding.ItemFileCardBinding
import com.example.classhub.util.FileTypeUtils

class FileCardAdapter(
    private val onItemClick: (ResourceFile) -> Unit
) : ListAdapter<ResourceFile, FileCardAdapter.ViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemFileCardBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(
        private val binding: ItemFileCardBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(file: ResourceFile) {
            val context = binding.root.context
            binding.fileTitle.text = file.title
            binding.uploaderInfo.text = "${file.uploaderName} · ${file.uploadDate}"
            binding.subjectBadge.text = file.subject
            binding.ratingText.text = "★ %.1f".format(file.rating)
            binding.downloadCount.text = file.downloadCount.toString()

            val bgColor = ContextCompat.getColor(context, FileTypeUtils.bgColorRes(file.fileType))
            binding.fileTypeIconContainer.backgroundTintList = ColorStateList.valueOf(bgColor)
            binding.fileTypeIcon.setImageResource(FileTypeUtils.iconRes(file.fileType))

            binding.root.setOnClickListener { onItemClick(file) }
        }
    }

    private class DiffCallback : DiffUtil.ItemCallback<ResourceFile>() {
        override fun areItemsTheSame(oldItem: ResourceFile, newItem: ResourceFile) =
            oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: ResourceFile, newItem: ResourceFile) =
            oldItem == newItem
    }
}
