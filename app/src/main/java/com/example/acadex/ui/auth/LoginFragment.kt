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
import com.example.acadex.databinding.FragmentLoginBinding
class LoginFragment : Fragment() {

    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!
    private val viewModel: LoginViewModel by viewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentLoginBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.emailEditText.doAfterTextChanged { viewModel.clearError() }
        binding.passwordEditText.doAfterTextChanged { viewModel.clearError() }

        binding.btnSignIn.setOnClickListener {
            viewModel.signIn(
                binding.emailEditText.text?.toString().orEmpty(),
                binding.passwordEditText.text?.toString().orEmpty()
            )
        }

        binding.btnGoToRegister.setOnClickListener {
            findNavController().navigate(R.id.action_login_to_register)
        }

        viewModel.loading.observe(viewLifecycleOwner) { loading ->
            binding.progressBar.isVisible = loading
            binding.btnSignIn.isEnabled = !loading
        }

        viewModel.errorMessage.observe(viewLifecycleOwner) { message ->
            binding.errorText.isVisible = !message.isNullOrBlank()
            binding.errorText.text = message
        }

        // Navigation to Home is handled by MainActivity's FirebaseAuth listener
        // to avoid duplicate navigations that crash after auth succeeds.
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
