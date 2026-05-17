package com.example.acadex.util

import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.acadex.R
import com.google.android.material.chip.Chip

class SubjectChipHelper(
    private val recyclerView: RecyclerView,
    private val onSelect: (String) -> Unit
) {
    private var selected = "All"
    private var subjects: List<String> = listOf("All")

    fun setup(initialSubjects: List<String> = listOf("All")) {
        subjects = initialSubjects
        recyclerView.layoutManager = LinearLayoutManager(recyclerView.context, LinearLayoutManager.HORIZONTAL, false)
        recyclerView.adapter = Adapter()
    }

    fun updateSubjects(newSubjects: List<String>) {
        subjects = newSubjects
        if (selected !in subjects) selected = "All"
        recyclerView.adapter?.notifyDataSetChanged()
    }

    private inner class Adapter : RecyclerView.Adapter<Adapter.H>() {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): H {
            val chip = LayoutInflater.from(parent.context).inflate(R.layout.item_subject_chip, parent, false) as Chip
            return H(chip)
        }
        override fun onBindViewHolder(holder: H, position: Int) = holder.bind(subjects[position])
        override fun getItemCount() = subjects.size
        inner class H(private val chip: Chip) : RecyclerView.ViewHolder(chip) {
            fun bind(label: String) {
                chip.text = label
                val ctx = chip.context
                val on = label == selected
                if (on) {
                    chip.chipBackgroundColor = ColorStateList.valueOf(ContextCompat.getColor(ctx, R.color.accent_blue))
                    chip.setTextColor(ContextCompat.getColor(ctx, R.color.white))
                    chip.chipStrokeWidth = 0f
                } else {
                    chip.chipBackgroundColor = ColorStateList.valueOf(ContextCompat.getColor(ctx, R.color.bg_white))
                    chip.setTextColor(ContextCompat.getColor(ctx, R.color.text_secondary))
                    chip.chipStrokeWidth = ctx.resources.displayMetrics.density
                    chip.chipStrokeColor = ColorStateList.valueOf(ContextCompat.getColor(ctx, R.color.border_light))
                }
                chip.setOnClickListener {
                    selected = label
                    notifyDataSetChanged()
                    onSelect(label)
                }
            }
        }
    }
}
