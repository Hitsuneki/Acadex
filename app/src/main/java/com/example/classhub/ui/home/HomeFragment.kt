package com.example.classhub.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.classhub.R
import com.example.classhub.adapters.FileCardAdapter
import com.example.classhub.data.MockDataSource
import com.example.classhub.databinding.FragmentHomeBinding
import com.example.classhub.util.SubjectChipHelper
import com.example.classhub.util.ThemeManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private val viewModel: HomeViewModel by viewModels()
    private lateinit var fileAdapter: FileCardAdapter
    private lateinit var chipHelper: SubjectChipHelper

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        chipHelper = SubjectChipHelper(binding.subjectChipsRecycler) { subject ->
            viewModel.setSubject(subject)
        }
        chipHelper.setup()

        fileAdapter = FileCardAdapter { file ->
            findNavController().navigate(
                HomeFragmentDirections.actionHomeToFileDetail(fileId = file.id)
            )
        }
        binding.filesRecycler.layoutManager = LinearLayoutManager(requireContext())
        binding.filesRecycler.adapter = fileAdapter

        viewModel.files.observe(viewLifecycleOwner) { files ->
            fileAdapter.submitList(files)
        }

        binding.btnSearch.setOnClickListener {
            findNavController().navigate(
                HomeFragmentDirections.actionHomeToBrowse(focusSearch = true)
            )
        }

        binding.btnTheme.setOnClickListener { showThemePicker() }

        binding.btnSeeAll.setOnClickListener {
            findNavController().navigate(R.id.browseFragment)
        }

        binding.btnSeeAllQuizzes.setOnClickListener { goToQuizzes() }
        binding.quizPromoCard.setOnClickListener { goToQuizzes() }

        refreshDashboard()
    }

    override fun onResume() {
        super.onResume()
        viewModel.refresh()
        refreshDashboard()
    }

    private fun refreshDashboard() {
        val name = MockDataSource.profileName.ifBlank { "Student" }
        binding.welcomeTitle.text = getString(R.string.welcome_back, name)

        binding.statMaterials.text = MockDataSource.files.size.toString()
        val subjectCount = MockDataSource.subjects.count { it != "All" }
        binding.statSubjects.text = subjectCount.toString()
        binding.statQuizzes.text = MockDataSource.quizSets.size.toString()

        binding.quizSetsSubtitle.text = getString(
            R.string.quiz_sets_available,
            MockDataSource.quizSets.size
        )
    }

    private fun goToQuizzes() {
        findNavController().navigate(R.id.quizFragment)
    }

    private fun showThemePicker() {
        val options = arrayOf(
            getString(R.string.theme_light),
            getString(R.string.theme_dark),
            getString(R.string.theme_system)
        )
        val current = ThemeManager.getMode(requireContext())
        val checked = when (current) {
            ThemeManager.MODE_LIGHT -> 0
            ThemeManager.MODE_DARK -> 1
            else -> 2
        }

        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.choose_theme)
            .setSingleChoiceItems(options, checked) { dialog, which ->
                val mode = when (which) {
                    0 -> ThemeManager.MODE_LIGHT
                    1 -> ThemeManager.MODE_DARK
                    else -> ThemeManager.MODE_SYSTEM
                }
                ThemeManager.saveMode(requireContext(), mode)
                dialog.dismiss()
                Snackbar.make(binding.root, R.string.theme_applied, Snackbar.LENGTH_SHORT).show()
                requireActivity().recreate()
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
