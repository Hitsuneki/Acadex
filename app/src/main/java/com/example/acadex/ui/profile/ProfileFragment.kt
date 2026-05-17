package com.example.acadex.ui.profile

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.example.acadex.R
import com.example.acadex.databinding.FragmentProfileBinding
import androidx.navigation.NavOptions
import com.example.acadex.databinding.IncludeErrorStateBinding
import com.example.acadex.ui.common.UiState
import kotlinx.coroutines.launch

class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!
    private var errorBinding: IncludeErrorStateBinding? = null
    private val viewModel: ProfileViewModel by viewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        errorBinding = IncludeErrorStateBinding.bind(binding.errorInclude.root)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        errorBinding?.btnRetry?.setOnClickListener { viewModel.load() }
        binding.toolbar.setNavigationOnClickListener {
            findNavController().navigate(
                R.id.homeFragment,
                null,
                NavOptions.Builder()
                    .setPopUpTo(R.id.homeFragment, true)
                    .setLaunchSingleTop(true)
                    .build()
            )
        }
        binding.btnEditProfile.setOnClickListener {
            findNavController().navigate(R.id.editProfileFragment)
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { render(it) }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.load()
    }

    private fun render(state: UiState<ProfileUiData>) {
        binding.loadingBar.isVisible = state is UiState.Loading
        binding.profileContent.isVisible = state is UiState.Success
        errorBinding?.root?.isVisible = state is UiState.Error
        if (state is UiState.Error) {
            errorBinding?.errorMessage?.text = state.message
        }
        if (state is UiState.Success) {
            val data = state.data
            binding.profileName.text = data.displayName
            binding.profileStatus.text = viewModel.formatStatus(data.status)
            binding.avatarInitials.text = viewModel.initials(data.displayName)
            binding.profileAbout.text = data.aboutMe.ifBlank { getString(R.string.profile_about_empty) }
            binding.profileGender.text = data.gender.ifBlank { getString(R.string.profile_field_empty) }
            binding.statUploads.text = data.uploads.toString()
            binding.statDownloads.text = data.downloads.toString()
            binding.statAvgRating.text = if (data.uploads == 0) "—" else "%.1f".format(data.avgRating)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        errorBinding = null
        _binding = null
    }
}
