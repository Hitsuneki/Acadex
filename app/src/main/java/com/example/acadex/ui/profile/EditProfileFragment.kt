package com.example.acadex.ui.profile

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.example.acadex.R
import com.example.acadex.databinding.FragmentEditProfileBinding
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch

class EditProfileFragment : Fragment() {

    private var _binding: FragmentEditProfileBinding? = null
    private val binding get() = _binding!!
    private val viewModel: EditProfileViewModel by viewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentEditProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.toolbar.setNavigationOnClickListener { findNavController().navigateUp() }
        binding.nameEditText.doAfterTextChanged { viewModel.setDisplayName(it?.toString().orEmpty()) }
        binding.aboutMeEditText.doAfterTextChanged { viewModel.setAboutMe(it?.toString().orEmpty()) }
        binding.genderEditText.doAfterTextChanged { viewModel.setGender(it?.toString().orEmpty()) }

        binding.statusChipGroup.setOnCheckedStateChangeListener { _, checkedIds ->
            val id = checkedIds.firstOrNull() ?: return@setOnCheckedStateChangeListener
            val status = when (id) {
                R.id.chipTeacher -> "teacher"
                R.id.chipOther -> "other"
                else -> "student"
            }
            viewModel.setStatus(status)
        }

        binding.btnSave.setOnClickListener { viewModel.save() }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.displayName.collect {
                        if (binding.nameEditText.text?.toString() != it) binding.nameEditText.setText(it)
                    }
                }
                launch {
                    viewModel.aboutMe.collect {
                        if (binding.aboutMeEditText.text?.toString() != it) binding.aboutMeEditText.setText(it)
                    }
                }
                launch {
                    viewModel.gender.collect {
                        if (binding.genderEditText.text?.toString() != it) binding.genderEditText.setText(it)
                    }
                }
                launch {
                    viewModel.status.collect { status ->
                        binding.chipStudent.isChecked = status == "student"
                        binding.chipTeacher.isChecked = status == "teacher"
                        binding.chipOther.isChecked = status == "other"
                    }
                }
                launch {
                    viewModel.saveResult.collect { result ->
                        when (result) {
                            SaveResult.Success -> {
                                Snackbar.make(binding.root, R.string.profile_updated, Snackbar.LENGTH_SHORT).show()
                                viewModel.onSaveHandled()
                                findNavController().navigateUp()
                            }
                            SaveResult.Failed -> {
                                Snackbar.make(binding.root, R.string.profile_update_failed, Snackbar.LENGTH_SHORT).show()
                                viewModel.onSaveHandled()
                            }
                            SaveResult.ValidationError -> {
                                binding.nameInputLayout.error = getString(R.string.display_name_required)
                                viewModel.onSaveHandled()
                            }
                            null -> binding.nameInputLayout.error = null
                        }
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
