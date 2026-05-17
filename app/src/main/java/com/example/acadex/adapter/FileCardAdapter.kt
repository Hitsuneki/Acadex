package com.example.acadex.adapter

import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.acadex.data.model.ResourceFile
import com.example.acadex.databinding.ItemFileCardBinding
import com.example.acadex.util.FileTypeUtils

class FileCardAdapter(
    private val onItemClick: (ResourceFile) -> Unit
) : ListAdapter<ResourceFile, FileCardAdapter.VH>(Diff()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        VH(ItemFileCardBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: VH, position: Int) = holder.bind(getItem(position))

    inner class VH(private val b: ItemFileCardBinding) : RecyclerView.ViewHolder(b.root) {
        fun bind(file: ResourceFile) {
            val ctx = b.root.context
            b.fileTitle.text = file.title
            b.uploaderInfo.text = "${file.uploaderName} · ${file.uploadDate}"
            b.subjectBadge.text = file.subject
            b.ratingText.text = "★ %.1f".format(file.rating)
            b.downloadCount.text = "${file.downloadCount} downloads"
            b.typePill.text = file.fileType.displayName()

            val bg = ContextCompat.getColor(ctx, FileTypeUtils.bgRes(file.fileType))
            val fg = ContextCompat.getColor(ctx, FileTypeUtils.fgRes(file.fileType))
            ViewCompat.setBackgroundTintList(b.fileTypeIconContainer, ColorStateList.valueOf(bg))
            b.fileTypeIcon.setImageResource(FileTypeUtils.iconRes(file.fileType))
            b.fileTypeIcon.imageTintList = ColorStateList.valueOf(fg)
            b.typePill.setTextColor(fg)
            b.typePill.setBackgroundColor(bg)

            b.root.setOnClickListener { onItemClick(file) }
        }
    }

    private class Diff : DiffUtil.ItemCallback<ResourceFile>() {
        override fun areItemsTheSame(a: ResourceFile, b: ResourceFile) = a.id == b.id
        override fun areContentsTheSame(a: ResourceFile, b: ResourceFile) = a == b
    }
}
