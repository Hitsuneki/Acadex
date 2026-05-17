package com.example.classhub.util

import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.classhub.R
import com.example.classhub.data.MockDataSource
import com.google.android.material.chip.Chip

class SubjectChipHelper(
    private val recyclerView: RecyclerView,
    private val onSubjectSelected: (String) -> Unit
) {
    private var selectedSubject = "All"

    fun setup(initialSubject: String = "All") {
        selectedSubject = initialSubject
        recyclerView.layoutManager = LinearLayoutManager(
            recyclerView.context,
            LinearLayoutManager.HORIZONTAL,
            false
        )
        recyclerView.adapter = ChipAdapter()
    }

    fun setSelected(subject: String) {
        selectedSubject = subject
        recyclerView.adapter?.notifyDataSetChanged()
    }

    private inner class ChipAdapter : RecyclerView.Adapter<ChipAdapter.ChipViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChipViewHolder {
            val chip = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_subject_chip, parent, false) as Chip
            return ChipViewHolder(chip)
        }

        override fun onBindViewHolder(holder: ChipViewHolder, position: Int) {
            holder.bind(MockDataSource.subjects[position])
        }

        override fun getItemCount() = MockDataSource.subjects.size

        inner class ChipViewHolder(private val chip: Chip) :
            RecyclerView.ViewHolder(chip) {

            fun bind(subject: String) {
                chip.text = subject
                val isSelected = subject == selectedSubject
                chip.isChecked = isSelected
                val context = chip.context
                if (isSelected) {
                    chip.chipBackgroundColor = ColorStateList.valueOf(
                        ContextCompat.getColor(context, R.color.accent_blue)
                    )
                    chip.setTextColor(ContextCompat.getColor(context, R.color.white))
                    chip.chipStrokeWidth = 0f
                } else {
                    chip.chipBackgroundColor = ColorStateList.valueOf(
                        ContextCompat.getColor(context, R.color.chip_unselected_bg)
                    )
                    chip.setTextColor(ContextCompat.getColor(context, R.color.text_primary))
                    chip.chipStrokeWidth = context.resources.displayMetrics.density
                    chip.chipStrokeColor = ColorStateList.valueOf(
                        ContextCompat.getColor(context, R.color.chip_stroke)
                    )
                }
                chip.setOnClickListener {
                    selectedSubject = subject
                    notifyDataSetChanged()
                    onSubjectSelected(subject)
                }
            }
        }
    }
}
