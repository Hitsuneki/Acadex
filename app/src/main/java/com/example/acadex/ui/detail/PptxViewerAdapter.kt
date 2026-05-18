package com.example.acadex.ui.detail

import android.graphics.BitmapFactory
import android.util.TypedValue
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.constraintlayout.widget.Guideline
import androidx.recyclerview.widget.RecyclerView
import com.example.acadex.R
import com.example.acadex.data.model.SlideData
import com.example.acadex.data.model.SlideElement
import com.google.android.material.card.MaterialCardView

class PptxViewerAdapter(private val slides: List<SlideData>) :
    RecyclerView.Adapter<PptxViewerAdapter.SlideViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SlideViewHolder {
        val context = parent.context

        // Compute 16:9 height from the RecyclerView's current width minus horizontal margins (16dp * 2).
        // This avoids the ClassCastException that occurred when casting ViewGroup.LayoutParams
        // to ConstraintLayout.LayoutParams (the card is a child of a FrameLayout, not a ConstraintLayout).
        val horizontalMarginPx = 32.dpToPx(context)
        val cardWidthPx = parent.width.takeIf { it > 0 }
            ?: (context.resources.displayMetrics.widthPixels - horizontalMarginPx)
        val cardHeightPx = cardWidthPx * 9 / 16

        val card = MaterialCardView(context).apply {
            layoutParams = ViewGroup.MarginLayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                cardHeightPx
            ).apply {
                setMargins(16.dpToPx(context), 8.dpToPx(context), 16.dpToPx(context), 8.dpToPx(context))
            }
            radius = 8f.dpToPxF(context)
            cardElevation = 2f.dpToPxF(context)
            setCardBackgroundColor(context.getColor(R.color.bg_surface))
        }

        val container = ConstraintLayout(context).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            id = View.generateViewId()
        }

        card.addView(container)
        return SlideViewHolder(card, container)
    }

    override fun onBindViewHolder(holder: SlideViewHolder, position: Int) {
        val slide = slides[position]
        holder.container.removeAllViews()
        holder.container.setBackgroundColor(slide.backgroundColor)

        val context = holder.container.context

        // Build a single ConstraintSet for the entire slide — cloning once and applying once
        // is far cheaper than cloning on every element iteration.
        val cs = ConstraintSet()
        cs.clone(holder.container)

        for (element in slide.elements) {
            val viewId = View.generateViewId()

            val view: View = when (element) {
                is SlideElement.TextElement -> {
                    TextView(context).apply {
                        id = viewId
                        text = element.text
                        setTextColor(context.getColor(R.color.text_primary))
                        textSize = 14f
                    }
                }
                is SlideElement.ImageElement -> {
                    ImageView(context).apply {
                        id = viewId
                        adjustViewBounds = true
                        scaleType = ImageView.ScaleType.FIT_XY
                        val bitmap = BitmapFactory.decodeByteArray(element.bytes, 0, element.bytes.size)
                        setImageBitmap(bitmap)
                    }
                }
            }

            view.layoutParams = ConstraintLayout.LayoutParams(0, 0)
            holder.container.addView(view)

            val xRatio = element.xRatio
            val yRatio = element.yRatio
            val wRatio = element.widthRatio
            val hRatio = element.heightRatio

            // Create percent-based guidelines so each element is positioned
            // relative to the slide canvas size.
            val glStartId = View.generateViewId()
            val glTopId    = View.generateViewId()
            val glEndId    = View.generateViewId()
            val glBottomId = View.generateViewId()

            fun makeGuideline(orientation: Int, guideId: Int): Guideline =
                Guideline(context).apply {
                    id = guideId
                    layoutParams = ConstraintLayout.LayoutParams(
                        ConstraintLayout.LayoutParams.WRAP_CONTENT,
                        ConstraintLayout.LayoutParams.WRAP_CONTENT
                    ).also { it.orientation = orientation }
                }

            holder.container.addView(makeGuideline(ConstraintLayout.LayoutParams.VERTICAL, glStartId))
            holder.container.addView(makeGuideline(ConstraintLayout.LayoutParams.HORIZONTAL, glTopId))
            holder.container.addView(makeGuideline(ConstraintLayout.LayoutParams.VERTICAL, glEndId))
            holder.container.addView(makeGuideline(ConstraintLayout.LayoutParams.HORIZONTAL, glBottomId))

            // Refresh the ConstraintSet snapshot now that new views exist in the container.
            cs.clone(holder.container)

            cs.setGuidelinePercent(glStartId,  xRatio.coerceIn(0f, 1f))
            cs.setGuidelinePercent(glTopId,    yRatio.coerceIn(0f, 1f))
            cs.setGuidelinePercent(glEndId,   (xRatio + wRatio).coerceIn(0f, 1f))
            cs.setGuidelinePercent(glBottomId, (yRatio + hRatio).coerceIn(0f, 1f))

            cs.connect(viewId, ConstraintSet.START,  glStartId,  ConstraintSet.START)
            cs.connect(viewId, ConstraintSet.TOP,    glTopId,    ConstraintSet.TOP)
            cs.connect(viewId, ConstraintSet.END,    glEndId,    ConstraintSet.START)
            cs.connect(viewId, ConstraintSet.BOTTOM, glBottomId, ConstraintSet.TOP)

            cs.constrainWidth(viewId,  ConstraintSet.MATCH_CONSTRAINT)
            cs.constrainHeight(viewId, ConstraintSet.MATCH_CONSTRAINT)
        }

        cs.applyTo(holder.container)
    }

    override fun getItemCount(): Int = slides.size

    class SlideViewHolder(card: MaterialCardView, val container: ConstraintLayout) :
        RecyclerView.ViewHolder(card)

    // Extension helpers ---------------------------------------------------------

    private val SlideElement.xRatio: Float
        get() = when (this) {
            is SlideElement.TextElement  -> this.xRatio
            is SlideElement.ImageElement -> this.xRatio
        }
    private val SlideElement.yRatio: Float
        get() = when (this) {
            is SlideElement.TextElement  -> this.yRatio
            is SlideElement.ImageElement -> this.yRatio
        }
    private val SlideElement.widthRatio: Float
        get() = when (this) {
            is SlideElement.TextElement  -> this.widthRatio
            is SlideElement.ImageElement -> this.widthRatio
        }
    private val SlideElement.heightRatio: Float
        get() = when (this) {
            is SlideElement.TextElement  -> this.heightRatio
            is SlideElement.ImageElement -> this.heightRatio
        }

    private fun Int.dpToPx(context: android.content.Context): Int =
        TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP, this.toFloat(), context.resources.displayMetrics
        ).toInt()

    private fun Float.dpToPxF(context: android.content.Context): Float =
        TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP, this, context.resources.displayMetrics
        )
}
