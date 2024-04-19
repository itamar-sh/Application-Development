package com.example.contentify

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.navigation.fragment.NavHostFragment


class MainActivity : AppCompatActivity(R.layout.activity_main) {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setNavigationGraph()
    }

    private fun setNavigationGraph() {
        /**
         * This is the initialization of the navigation graph and navigation component
         * In your fragment you can use the navController to navigate between fragments.
         */
        val navController =
            (supportFragmentManager.findFragmentById(R.id.main_nav_host) as NavHostFragment).
            navController
        val graph = navController.navInflater.inflate(R.navigation.navigation)
        navController.graph = graph
    }
}
