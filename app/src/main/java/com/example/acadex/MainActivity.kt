package com.example.acadex

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.example.acadex.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val nav = (supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment).navController
        binding.bottomNavigation.setupWithNavController(nav)

        val topLevel = setOf(
            R.id.homeFragment, R.id.browseFragment, R.id.uploadFragment,
            R.id.quizFragment, R.id.profileFragment
        )
        nav.addOnDestinationChangedListener { _, dest, _ ->
            binding.bottomNavigation.visibility =
                if (dest.id in topLevel) View.VISIBLE else View.GONE
        }
    }
}
