package com.example.acadex.ui.auth

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.example.acadex.R
import com.example.acadex.databinding.FragmentRegisterBinding
class RegisterFragment : Fragment() {

    private var _binding: FragmentRegisterBinding? = null
    private val binding get() = _binding!!
    private val viewModel: RegisterViewModel by viewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentRegisterBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.toolbar.setNavigationOnClickListener { findNavController().navigateUp() }

        binding.emailEditText.doAfterTextChanged { viewModel.clearError() }
        binding.passwordEditText.doAfterTextChanged { viewModel.clearError() }
        binding.confirmPasswordEditText.doAfterTextChanged { viewModel.clearError() }

        binding.btnRegister.setOnClickListener {
            viewModel.signUp(
                binding.emailEditText.text?.toString().orEmpty(),
                binding.passwordEditText.text?.toString().orEmpty(),
                binding.confirmPasswordEditText.text?.toString().orEmpty()
            )
        }

        binding.btnGoToLogin.setOnClickListener { findNavController().navigateUp() }

        viewModel.loading.observe(viewLifecycleOwner) { loading ->
            binding.progressBar.isVisible = loading
            binding.btnRegister.isEnabled = !loading
        }

        viewModel.errorMessage.observe(viewLifecycleOwner) { message ->
            binding.errorText.isVisible = !message.isNullOrBlank()
            binding.errorText.text = message
        }

        // Navigation to Home is handled by MainActivity's FirebaseAuth listener.
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
