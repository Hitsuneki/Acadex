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
import com.example.acadex.databinding.FragmentProfileBinding
import com.example.acadex.databinding.IncludeErrorStateBinding
import com.example.acadex.ui.common.UiState
import com.google.firebase.auth.FirebaseAuth
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
        binding.btnEditProfile.setOnClickListener {
            findNavController().navigate(ProfileFragmentDirections.actionProfileToEditProfile())
        }
        binding.rowSubmissions.setOnClickListener {
            findNavController().navigate(ProfileFragmentDirections.actionProfileToMySubmissions())
        }
        binding.rowSaved.setOnClickListener {
            findNavController().navigate(ProfileFragmentDirections.actionProfileToSavedIndex())
        }
        binding.rowQuizHistory.setOnClickListener {
            findNavController().navigate(ProfileFragmentDirections.actionProfileToQuizHistory())
        }
        binding.rowSettings.setOnClickListener {
            findNavController().navigate(ProfileFragmentDirections.actionProfileToSettings())
        }
        binding.rowAbout.setOnClickListener {
            findNavController().navigate(ProfileFragmentDirections.actionProfileToAbout())
        }
        binding.btnSignOut.setOnClickListener { FirebaseAuth.getInstance().signOut() }

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
            binding.profileSection.text = data.section
            binding.avatarInitials.text = viewModel.initials(data.displayName)
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
