package com.example.acadex

import android.graphics.Color
import android.os.Bundle
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.PopupMenu
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.NavOptions
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.example.acadex.data.repository.ProfileRepository
import com.example.acadex.databinding.ActivityMainBinding
import com.example.acadex.util.AuthSession
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var navController: NavController

    private val authDestinations = setOf(R.id.loginFragment, R.id.registerFragment)
    private val mainTabDestinations = setOf(
        R.id.homeFragment, R.id.browseFragment, R.id.uploadFragment, R.id.quizFragment, R.id.profileFragment
    )

    /**
     * Tracks whether we have received at least one confirmed auth state.
     * Firebase can briefly emit currentUser == null during token refresh,
     * which would incorrectly pop the entire back-stack to the login screen.
     * We only navigate to login if the FIRST fired state (or a subsequent change)
     * explicitly confirms the user is signed out.
     */
    private var initialAuthCheckDone = false

    private val authListener = FirebaseAuth.AuthStateListener { auth ->
        if (auth.currentUser == null) {
            // Only force-navigate to login when we have already confirmed auth
            // once. This prevents a transient null (token refresh) from wiping
            // the back-stack and sending the user to the home screen.
            if (initialAuthCheckDone) {
                navigateToLoginIfNeeded()
            }
            initialAuthCheckDone = true
            binding.bottomNavigation.visibility = View.GONE
            binding.fabUpload.visibility = View.GONE
            binding.mainToolbar.isVisible = false
        } else {
            initialAuthCheckDone = true
            AuthSession.syncProfileFromFirebase()
            lifecycleScope.launch {
                ProfileRepository.ensureProfileExists()
                navigateToHomeIfNeeded()
                updateChrome()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val navHost = supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        navController = navHost.navController
        binding.bottomNavigation.setupWithNavController(navController)

        navController.addOnDestinationChangedListener { _, destination, _ ->
            updateChrome(destination.id)
        }

        binding.btnNotifications.setOnClickListener {
            Snackbar.make(binding.root, R.string.feature_coming_soon, Snackbar.LENGTH_SHORT).show()
        }
        binding.btnMenu.setOnClickListener { showAppMenu() }

        binding.fabUpload.setOnClickListener {
            navController.navigate(R.id.uploadFragment)
        }

        // Register the listener once for the lifetime of the Activity.
        // Using onCreate/onDestroy (instead of onStart/onStop) means the listener
        // does NOT re-fire every time a dialog or bottom sheet is dismissed,
        // preventing spurious back-stack resets.
        FirebaseAuth.getInstance().addAuthStateListener(authListener)
    }

    override fun onDestroy() {
        FirebaseAuth.getInstance().removeAuthStateListener(authListener)
        super.onDestroy()
    }

    private fun showAppMenu() {
        PopupMenu(this, binding.btnMenu).apply {
            menuInflater.inflate(R.menu.menu_app_drawer, menu)
            menu.findItem(R.id.menu_sign_out)?.let { item ->
                val title = SpannableString(item.title)
                title.setSpan(ForegroundColorSpan(Color.parseColor("#D32F2F")), 0, title.length, 0)
                item.title = title
            }
            setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    R.id.menu_profile -> {
                        navController.navigate(R.id.profileFragment)
                        true
                    }
                    R.id.menu_settings -> {
                        navController.navigate(R.id.settingsFragment)
                        true
                    }
                    R.id.menu_submissions -> {
                        navController.navigate(R.id.mySubmissionsFragment)
                        true
                    }
                    R.id.menu_saved -> {
                        navController.navigate(R.id.savedIndexFragment)
                        true
                    }
                    R.id.menu_quiz_history -> {
                        navController.navigate(R.id.quizHistoryFragment)
                        true
                    }
                    R.id.menu_about -> {
                        navController.navigate(R.id.aboutFragment)
                        true
                    }
                    R.id.menu_sign_out -> {
                        FirebaseAuth.getInstance().signOut()
                        true
                    }
                    else -> false
                }
            }
            show()
        }
    }

    private fun navigateToHomeIfNeeded() {
        val currentId = navController.currentDestination?.id ?: return
        if (currentId == R.id.homeFragment) return
        if (currentId !in authDestinations) return

        navController.navigate(
            R.id.homeFragment,
            null,
            NavOptions.Builder()
                .setPopUpTo(R.id.loginFragment, true)
                .build()
        )
    }

    private fun navigateToLoginIfNeeded() {
        val currentId = navController.currentDestination?.id ?: return
        if (currentId in authDestinations) return

        navController.navigate(
            R.id.loginFragment,
            null,
            NavOptions.Builder()
                .setPopUpTo(navController.graph.id, true)
                .build()
        )
    }

    private fun updateChrome(destinationId: Int = navController.currentDestination?.id ?: 0) {
        val signedIn = FirebaseAuth.getInstance().currentUser != null
        val showMainChrome = signedIn && destinationId !in authDestinations
        binding.mainToolbar.isVisible = destinationId in mainTabDestinations
        
        val isTabDest = destinationId in mainTabDestinations
        binding.bottomNavigation.visibility = if (isTabDest) View.VISIBLE else View.GONE
        binding.fabUpload.visibility = if (isTabDest) View.VISIBLE else View.GONE

        if (showMainChrome) {
            binding.mainToolbar.title = when (destinationId) {
                R.id.homeFragment -> getString(R.string.nav_home)
                R.id.browseFragment -> getString(R.string.nav_browse)
                R.id.uploadFragment -> getString(R.string.nav_upload)
                R.id.quizFragment -> getString(R.string.nav_quizzes)
                R.id.profileFragment -> getString(R.string.nav_profile)
                R.id.editProfileFragment -> getString(R.string.edit_profile)
                R.id.settingsFragment -> getString(R.string.row_settings)
                else -> getString(R.string.app_name)
            }
        }
    }
}
