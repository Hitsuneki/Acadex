package com.example.classhub

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.example.classhub.databinding.ActivityMainBinding
import com.example.classhub.util.ThemeManager

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        ThemeManager.apply(this)
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController

        binding.bottomNavigation.setupWithNavController(navController)

        navController.addOnDestinationChangedListener { _, destination, _ ->
            val topLevel = setOf(
                R.id.homeFragment,
                R.id.browseFragment,
                R.id.uploadFragment,
                R.id.profileFragment
            )
            binding.bottomNavigation.visibility = if (destination.id in topLevel) {
                android.view.View.VISIBLE
            } else {
                android.view.View.GONE
            }
        }
    }
}
