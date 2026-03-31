package com.practicum.playlistmaker.core.ui.root

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.practicum.playlistmaker.R
import com.practicum.playlistmaker.databinding.ActivityRootBinding

class RootActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRootBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRootBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.rootFragmentContainerView) as NavHostFragment
        val navController = navHostFragment.navController

        binding.bottomNavigationView.setupWithNavController(navController)

        navController.addOnDestinationChangedListener { _, destination, _ ->
            when (destination.id) {
                R.id.audioFragment -> {
                    animateBottomNavigationView(false)
                    animateNavMenuLine(false)
                }

                R.id.playlistMakerFragment -> {
                    animateBottomNavigationView(false)
                    animateNavMenuLine(false)
                }

                R.id.playlistInfoFragment -> {
                    animateBottomNavigationView(false)
                    animateNavMenuLine(false)
                }

                else -> {
                    animateBottomNavigationView(true)
                    animateNavMenuLine(true)
                }
            }
        }
    }

    fun animateBottomNavigationView(isVisible: Boolean) {
        binding.bottomNavigationView.isVisible = isVisible
    }

    private fun animateNavMenuLine(isVisible: Boolean) {
        binding.navMenuLine.isVisible = isVisible
    }
}
