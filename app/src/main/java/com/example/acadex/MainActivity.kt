package com.example.acadex

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.NavController
import androidx.navigation.NavOptions
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.example.acadex.databinding.ActivityMainBinding
import androidx.lifecycle.lifecycleScope
import com.example.acadex.data.repository.ProfileRepository
import com.example.acadex.util.AuthSession
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var navController: NavController

    private val authDestinations = setOf(R.id.loginFragment, R.id.registerFragment)
    private val topLevelDestinations = setOf(
        R.id.homeFragment, R.id.browseFragment, R.id.uploadFragment,
        R.id.quizFragment, R.id.profileFragment
    )

    private val authListener = FirebaseAuth.AuthStateListener { auth ->
        if (auth.currentUser == null) {
            navigateToLoginIfNeeded()
            binding.bottomNavigation.visibility = View.GONE
        } else {
            AuthSession.syncProfileFromFirebase()
            lifecycleScope.launch {
                ProfileRepository.ensureProfileExists()
                navigateToHomeIfNeeded()
                updateBottomNavVisibility()
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
            updateBottomNavVisibility(destination.id)
        }

        if (FirebaseAuth.getInstance().currentUser != null) {
            AuthSession.syncProfileFromFirebase()
            lifecycleScope.launch {
                ProfileRepository.ensureProfileExists()
                navigateToHomeIfNeeded()
            }
        }
    }

    override fun onStart() {
        super.onStart()
        FirebaseAuth.getInstance().addAuthStateListener(authListener)
    }

    override fun onStop() {
        super.onStop()
        FirebaseAuth.getInstance().removeAuthStateListener(authListener)
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

    private fun updateBottomNavVisibility(destinationId: Int = navController.currentDestination?.id ?: 0) {
        binding.bottomNavigation.visibility =
            if (destinationId in topLevelDestinations) View.VISIBLE else View.GONE
    }
}
