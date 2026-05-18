package com.example.acadex.ui.detail

import android.graphics.BitmapFactory
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.acadex.R
import com.example.acadex.data.model.DocElement

class DocxViewerAdapter(private val elements: List<DocElement>) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    init {
        // Stable IDs let RecyclerView skip unnecessary rebinds and animations
        // when the list hasn't structurally changed.
        setHasStableIds(true)
    }

    companion object {
        private const val TYPE_HEADING = 0
        private const val TYPE_PARAGRAPH = 1
        private const val TYPE_TABLE = 2
        private const val TYPE_IMAGE = 3
        private const val TYPE_DIVIDER = 4
        private const val TYPE_EMPTY = 5
    }

    override fun getItemViewType(position: Int): Int {
        return when (elements[position]) {
            is DocElement.Heading -> TYPE_HEADING
            is DocElement.Paragraph -> TYPE_PARAGRAPH
            is DocElement.TableBlock -> TYPE_TABLE
            is DocElement.ImageBlock -> TYPE_IMAGE
            is DocElement.Divider -> TYPE_DIVIDER
            is DocElement.EmptyLine -> TYPE_EMPTY
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val context = parent.context
        return when (viewType) {
            TYPE_HEADING -> {
                val tv = TextView(context).apply {
                    layoutParams = ViewGroup.MarginLayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                        setMargins(0, 16.dpToPx(context), 0, 8.dpToPx(context))
                    }
                    setTextColor(context.getColor(R.color.text_primary))
                }
                HeadingViewHolder(tv)
            }
            TYPE_PARAGRAPH -> {
                val tv = TextView(context).apply {
                    layoutParams = ViewGroup.MarginLayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                        setMargins(0, 4.dpToPx(context), 0, 4.dpToPx(context))
                    }
                    setTextColor(context.getColor(R.color.text_primary))
                    setTextIsSelectable(true)
                }
                ParagraphViewHolder(tv)
            }
            TYPE_TABLE -> {
                val ll = LinearLayout(context).apply {
                    orientation = LinearLayout.VERTICAL
                    layoutParams = ViewGroup.MarginLayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                        setMargins(0, 8.dpToPx(context), 0, 8.dpToPx(context))
                    }
                    setBackgroundColor(android.graphics.Color.LTGRAY)
                    setPadding(1.dpToPx(context), 1.dpToPx(context), 1.dpToPx(context), 1.dpToPx(context))
                }
                TableViewHolder(ll)
            }
            TYPE_IMAGE -> {
                val iv = ImageView(context).apply {
                    layoutParams = ViewGroup.MarginLayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                        setMargins(0, 8.dpToPx(context), 0, 8.dpToPx(context))
                    }
                    adjustViewBounds = true
                    scaleType = ImageView.ScaleType.FIT_CENTER
                }
                ImageViewHolder(iv)
            }
            TYPE_DIVIDER -> {
                val view = View(context).apply {
                    layoutParams = ViewGroup.MarginLayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 1.dpToPx(context)).apply {
                        setMargins(0, 16.dpToPx(context), 0, 16.dpToPx(context))
                    }
                    setBackgroundColor(android.graphics.Color.LTGRAY)
                }
                DividerViewHolder(view)
            }
            else -> {
                val view = View(context).apply {
                    layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 16.dpToPx(context))
                }
                EmptyViewHolder(view)
            }
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val element = elements[position]
        when (holder) {
            is HeadingViewHolder -> {
                val e = element as DocElement.Heading
                holder.tv.text = e.text
                holder.tv.textSize = when (e.level) {
                    1 -> 24f
                    2 -> 20f
                    else -> 18f
                }
                holder.tv.setTypeface(null, android.graphics.Typeface.BOLD)
            }
            is ParagraphViewHolder -> {
                val e = element as DocElement.Paragraph
                holder.tv.text = e.text
                holder.tv.textSize = 16f
            }
            is TableViewHolder -> {
                val e = element as DocElement.TableBlock
                holder.container.removeAllViews()
                val context = holder.container.context
                for (row in e.rows) {
                    val rowLayout = LinearLayout(context).apply {
                        orientation = LinearLayout.HORIZONTAL
                        layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
                    }
                    for (cell in row) {
                        val tv = TextView(context).apply {
                            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                                setMargins(1.dpToPx(context), 1.dpToPx(context), 1.dpToPx(context), 1.dpToPx(context))
                            }
                            text = cell
                            setPadding(8.dpToPx(context), 8.dpToPx(context), 8.dpToPx(context), 8.dpToPx(context))
                            setBackgroundColor(context.getColor(R.color.bg_surface))
                            setTextColor(context.getColor(R.color.text_primary))
                        }
                        rowLayout.addView(tv)
                    }
                    holder.container.addView(rowLayout)
                }
            }
            is ImageViewHolder -> {
                val e = element as DocElement.ImageBlock
                // Guard against OOM for large or corrupt embedded images.
                val bitmap = runCatching {
                    BitmapFactory.decodeByteArray(e.bitmapData, 0, e.bitmapData.size)
                }.getOrNull()
                holder.iv.setImageBitmap(bitmap)
            }
        }
    }

    override fun getItemCount(): Int = elements.size

    override fun getItemId(position: Int): Long = position.toLong()

    class HeadingViewHolder(val tv: TextView) : RecyclerView.ViewHolder(tv)
    class ParagraphViewHolder(val tv: TextView) : RecyclerView.ViewHolder(tv)
    class TableViewHolder(val container: LinearLayout) : RecyclerView.ViewHolder(container)
    class ImageViewHolder(val iv: ImageView) : RecyclerView.ViewHolder(iv)
    class DividerViewHolder(view: View) : RecyclerView.ViewHolder(view)
    class EmptyViewHolder(view: View) : RecyclerView.ViewHolder(view)

    private fun Int.dpToPx(context: android.content.Context): Int =
        TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, this.toFloat(), context.resources.displayMetrics).toInt()
}
