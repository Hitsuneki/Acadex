package com.example.classhub.ui.profile

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.classhub.R
import com.example.classhub.adapters.FileCardAdapter
import com.example.classhub.data.MockDataSource
import com.example.classhub.databinding.FragmentProfileBinding
import com.example.classhub.util.ThemeManager
import com.google.android.material.snackbar.Snackbar

class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!

    private lateinit var savedAdapter: FileCardAdapter
    private var suppressThemeCallback = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.nameEditText.setText(MockDataSource.profileName)

        binding.btnSaveName.setOnClickListener {
            val name = binding.nameEditText.text?.toString()?.trim().orEmpty()
            if (name.isNotEmpty()) {
                MockDataSource.profileName = name
                Snackbar.make(binding.root, R.string.name_saved, Snackbar.LENGTH_SHORT).show()
            }
        }

        setupThemeToggle()

        savedAdapter = FileCardAdapter { file ->
            findNavController().navigate(
                ProfileFragmentDirections.actionProfileToFileDetail(fileId = file.id)
            )
        }
        binding.savedRecycler.layoutManager = LinearLayoutManager(requireContext())
        binding.savedRecycler.adapter = savedAdapter

        refreshSaved()
    }

    private fun setupThemeToggle() {
        suppressThemeCallback = true
        when (ThemeManager.getMode(requireContext())) {
            ThemeManager.MODE_LIGHT -> binding.themeToggleGroup.check(R.id.btnThemeLight)
            ThemeManager.MODE_DARK -> binding.themeToggleGroup.check(R.id.btnThemeDark)
            else -> binding.themeToggleGroup.check(R.id.btnThemeSystem)
        }
        suppressThemeCallback = false

        binding.themeToggleGroup.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (suppressThemeCallback || !isChecked) return@addOnButtonCheckedListener
            val mode = when (checkedId) {
                R.id.btnThemeLight -> ThemeManager.MODE_LIGHT
                R.id.btnThemeDark -> ThemeManager.MODE_DARK
                else -> ThemeManager.MODE_SYSTEM
            }
            if (ThemeManager.getMode(requireContext()) != mode) {
                ThemeManager.saveMode(requireContext(), mode)
                Snackbar.make(binding.root, R.string.theme_applied, Snackbar.LENGTH_SHORT).show()
                requireActivity().recreate()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        refreshSaved()
    }

    private fun refreshSaved() {
        val saved = MockDataSource.getSavedFiles()
        savedAdapter.submitList(saved)
        binding.emptySaved.isVisible = saved.isEmpty()
        binding.savedRecycler.isVisible = saved.isNotEmpty()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
