package com.example.acadex.adapter

import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.acadex.data.model.ResourceFile
import com.example.acadex.databinding.ItemFileCardBinding
import com.example.acadex.util.FileTypeUtils

enum class FileCardAction { NONE, DELETE, BOOKMARK_FILLED, BOOKMARK_OUTLINE }

class FileCardAdapter(
    private val onItemClick: (ResourceFile) -> Unit,
    private val action: FileCardAction = FileCardAction.NONE,
    private val onActionClick: ((ResourceFile) -> Unit)? = null
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

            when (action) {
                FileCardAction.NONE -> b.btnCardAction.isVisible = false
                FileCardAction.DELETE -> {
                    b.btnCardAction.isVisible = true
                    b.btnCardAction.setImageResource(android.R.drawable.ic_menu_delete)
                }
                FileCardAction.BOOKMARK_FILLED -> {
                    b.btnCardAction.isVisible = true
                    b.btnCardAction.setImageResource(android.R.drawable.btn_star_big_on)
                }
                FileCardAction.BOOKMARK_OUTLINE -> {
                    b.btnCardAction.isVisible = true
                    b.btnCardAction.setImageResource(android.R.drawable.btn_star_big_off)
                }
            }
            b.btnCardAction.setOnClickListener { onActionClick?.invoke(file) }
            b.root.setOnClickListener { onItemClick(file) }
        }
    }

    private class Diff : DiffUtil.ItemCallback<ResourceFile>() {
        override fun areItemsTheSame(a: ResourceFile, b: ResourceFile) = a.id == b.id
        override fun areContentsTheSame(a: ResourceFile, b: ResourceFile) = a == b
    }
}
