package com.example.acadex.ui.profile

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.acadex.R
import com.example.acadex.data.MockDataSource
import com.example.acadex.databinding.FragmentProfileBinding
import com.google.android.material.snackbar.Snackbar

class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        refresh()
        binding.btnSaveName.setOnClickListener {
            val name = binding.nameEditText.text?.toString()?.trim().orEmpty()
            if (name.isNotEmpty()) {
                MockDataSource.profileName = name
                refresh()
                Snackbar.make(binding.root, R.string.name_saved, Snackbar.LENGTH_SHORT).show()
            }
        }
        val stub = View.OnClickListener {
            Snackbar.make(binding.root, R.string.feature_coming_soon, Snackbar.LENGTH_SHORT).show()
        }
        binding.rowSubmissions.setOnClickListener(stub)
        binding.rowSaved.setOnClickListener {
            val saved = MockDataSource.getSavedFiles().firstOrNull()
            if (saved != null) {
                findNavController().navigate(ProfileFragmentDirections.actionProfileToFileDetail(saved.id))
            } else stub.onClick(binding.rowSaved)
        }
        binding.rowQuizHistory.setOnClickListener(stub)
        binding.rowRatings.setOnClickListener(stub)
        binding.rowSettings.setOnClickListener(stub)
        binding.rowAbout.setOnClickListener(stub)
    }

    private fun refresh() {
        val name = MockDataSource.profileName
        binding.nameEditText.setText(name)
        binding.profileName.text = name
        val initials = name.split(" ").mapNotNull { it.firstOrNull()?.uppercaseChar() }.take(2).joinToString("")
        binding.avatarInitials.text = initials.ifEmpty { "S" }
        binding.statUploads.text = MockDataSource.files.count { it.uploaderName == name }.toString()
        binding.statDownloads.text = MockDataSource.files.sumOf { it.downloadCount }.toString()
        val avg = MockDataSource.files.map { it.rating }.average()
        binding.statAvgRating.text = if (avg.isNaN()) "—" else "%.1f".format(avg)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
