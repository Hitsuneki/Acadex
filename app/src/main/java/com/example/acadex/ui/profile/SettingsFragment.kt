package com.example.acadex.ui.profile

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatDelegate
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.acadex.R
import com.example.acadex.data.repository.ProfileRepository
import com.example.acadex.data.repository.QuizRepository
import com.example.acadex.data.repository.SavedRepository
import com.example.acadex.data.result.RepoResult
import com.example.acadex.databinding.FragmentSettingsBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch

class SettingsFragment : Fragment() {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!
    private val prefs by lazy { requireContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE) }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.toolbar.setNavigationOnClickListener { findNavController().navigateUp() }

        binding.switchDarkMode.isChecked = prefs.getBoolean(KEY_DARK, false)
        binding.switchNotifUploads.isChecked = prefs.getBoolean(KEY_NOTIF_UPLOADS, true)
        binding.switchNotifQuizzes.isChecked = prefs.getBoolean(KEY_NOTIF_QUIZZES, true)

        applyDarkMode(binding.switchDarkMode.isChecked)

        binding.switchDarkMode.setOnCheckedChangeListener { _, checked ->
            prefs.edit().putBoolean(KEY_DARK, checked).apply()
            applyDarkMode(checked)
        }
        binding.switchNotifUploads.setOnCheckedChangeListener { _, checked ->
            prefs.edit().putBoolean(KEY_NOTIF_UPLOADS, checked).apply()
        }
        binding.switchNotifQuizzes.setOnCheckedChangeListener { _, checked ->
            prefs.edit().putBoolean(KEY_NOTIF_QUIZZES, checked).apply()
        }

        val profile = ProfileRepository.cachedProfile.value
        binding.rowAccountName.text = getString(R.string.display_name)
        binding.rowAccountSection.text = getString(R.string.section_year)
        updateAccountSubtitles(profile?.displayName, profile?.section)

        val toEdit = View.OnClickListener {
            findNavController().navigate(SettingsFragmentDirections.actionSettingsToEditProfile())
        }
        binding.rowAccountName.setOnClickListener(toEdit)
        binding.rowAccountSection.setOnClickListener(toEdit)

        binding.rowClearSaved.setOnClickListener {
            MaterialAlertDialogBuilder(requireContext())
                .setMessage(R.string.confirm_clear_saved)
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(R.string.clear) { _, _ -> clearSaved() }
                .show()
        }
        binding.rowClearQuizHistory.setOnClickListener {
            MaterialAlertDialogBuilder(requireContext())
                .setMessage(R.string.confirm_clear_quiz_history)
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(R.string.clear) { _, _ -> clearQuizHistory() }
                .show()
        }
    }

    private fun applyDarkMode(enabled: Boolean) {
        AppCompatDelegate.setDefaultNightMode(
            if (enabled) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO
        )
    }

    private fun clearSaved() {
        viewLifecycleOwner.lifecycleScope.launch {
            when (SavedRepository.clearAll()) {
                is RepoResult.Success ->
                    Snackbar.make(binding.root, R.string.saved_index_cleared, Snackbar.LENGTH_SHORT).show()
                is RepoResult.Error ->
                    Snackbar.make(binding.root, R.string.clear_failed, Snackbar.LENGTH_SHORT).show()
            }
        }
    }

    private fun clearQuizHistory() {
        viewLifecycleOwner.lifecycleScope.launch {
            when (QuizRepository.clearHistory()) {
                is RepoResult.Success ->
                    Snackbar.make(binding.root, R.string.quiz_history_cleared, Snackbar.LENGTH_SHORT).show()
                is RepoResult.Error ->
                    Snackbar.make(binding.root, R.string.clear_failed, Snackbar.LENGTH_SHORT).show()
            }
        }
    }

    private fun updateAccountSubtitles(name: String?, section: String?) {
        binding.rowAccountName.text = buildString {
            append(getString(R.string.display_name))
            if (!name.isNullOrBlank()) append("\n").append(name)
        }
        binding.rowAccountSection.text = buildString {
            append(getString(R.string.section_year))
            append("\n").append(section?.ifBlank { "—" } ?: "—")
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val PREFS = "acadex_settings"
        private const val KEY_DARK = "acadex_pref_dark_mode"
        private const val KEY_NOTIF_UPLOADS = "acadex_pref_notif_uploads"
        private const val KEY_NOTIF_QUIZZES = "acadex_pref_notif_quizzes"
    }
}
