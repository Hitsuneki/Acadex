package com.example.acadex.adapter

import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.acadex.R
import com.example.acadex.data.model.ResourceFile
import com.example.acadex.databinding.ItemFileCardBinding
import com.example.acadex.databinding.ItemFileCardTileBinding
import com.example.acadex.databinding.ItemFileCardCompactBinding
import com.example.acadex.util.FileTypeUtils

enum class FileCardAction { NONE, DELETE, BOOKMARK_FILLED, BOOKMARK_OUTLINE }
enum class ViewMode { ROW, TILE, COMPACT }

class FileCardAdapter(
    private val onItemClick: (ResourceFile) -> Unit,
    private val action: FileCardAction = FileCardAction.NONE,
    private val onActionClick: ((ResourceFile) -> Unit)? = null
) : ListAdapter<ResourceFile, RecyclerView.ViewHolder>(Diff()) {

    var viewMode: ViewMode = ViewMode.ROW
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    override fun getItemViewType(position: Int): Int {
        return viewMode.ordinal
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            ViewMode.ROW.ordinal -> RowVH(
                ItemFileCardBinding.inflate(inflater, parent, false)
            )
            ViewMode.TILE.ordinal -> TileVH(
                ItemFileCardTileBinding.inflate(inflater, parent, false)
            )
            ViewMode.COMPACT.ordinal -> CompactVH(
                ItemFileCardCompactBinding.inflate(inflater, parent, false)
            )
            else -> throw IllegalArgumentException("Unknown viewType: $viewType")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val file = getItem(position)
        when (holder) {
            is RowVH -> holder.bind(file)
            is TileVH -> holder.bind(file)
            is CompactVH -> holder.bind(file)
        }
    }

    inner class RowVH(private val b: ItemFileCardBinding) : RecyclerView.ViewHolder(b.root) {
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
                    b.btnCardAction.setImageResource(R.drawable.ic_bookmark_filled)
                    b.btnCardAction.imageTintList = ColorStateList.valueOf(
                        ContextCompat.getColor(ctx, R.color.star_amber)
                    )
                    b.btnCardAction.scaleType = android.widget.ImageView.ScaleType.CENTER_INSIDE
                    b.btnCardAction.setPadding(8, 8, 8, 8)
                }
                FileCardAction.BOOKMARK_OUTLINE -> {
                    b.btnCardAction.isVisible = true
                    b.btnCardAction.setImageResource(R.drawable.ic_bookmark_outline)
                    b.btnCardAction.imageTintList = ColorStateList.valueOf(
                        ContextCompat.getColor(ctx, R.color.text_muted)
                    )
                    b.btnCardAction.scaleType = android.widget.ImageView.ScaleType.CENTER_INSIDE
                    b.btnCardAction.setPadding(8, 8, 8, 8)
                }
            }
            b.btnCardAction.setOnClickListener { onActionClick?.invoke(file) }
            b.root.setOnClickListener { onItemClick(file) }
        }
    }

    inner class TileVH(private val b: ItemFileCardTileBinding) : RecyclerView.ViewHolder(b.root) {
        fun bind(file: ResourceFile) {
            val ctx = b.root.context
            b.fileTitle.text = file.title
            b.subjectBadge.text = file.subject
            b.ratingText.text = "★ %.1f".format(file.rating)

            val bg = ContextCompat.getColor(ctx, FileTypeUtils.bgRes(file.fileType))
            val fg = ContextCompat.getColor(ctx, FileTypeUtils.fgRes(file.fileType))
            ViewCompat.setBackgroundTintList(b.fileTypeIconContainer, ColorStateList.valueOf(bg))
            b.fileTypeIcon.setImageResource(FileTypeUtils.iconRes(file.fileType))
            b.fileTypeIcon.imageTintList = ColorStateList.valueOf(fg)

            b.root.setOnClickListener { onItemClick(file) }
        }
    }

    inner class CompactVH(private val b: ItemFileCardCompactBinding) : RecyclerView.ViewHolder(b.root) {
        fun bind(file: ResourceFile) {
            val ctx = b.root.context
            b.fileTitle.text = file.title
            b.subjectBadge.text = file.subject

            val bg = ContextCompat.getColor(ctx, FileTypeUtils.bgRes(file.fileType))
            ViewCompat.setBackgroundTintList(b.fileTypeDot, ColorStateList.valueOf(bg))

            b.root.setOnClickListener { onItemClick(file) }
        }
    }

    private class Diff : DiffUtil.ItemCallback<ResourceFile>() {
        override fun areItemsTheSame(a: ResourceFile, b: ResourceFile) = a.id == b.id
        override fun areContentsTheSame(a: ResourceFile, b: ResourceFile) = a == b
    }
}
