package com.itsabugnotafeature.fitocrazy

import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.view.View
import com.google.android.material.bottomnavigation.BottomNavigationView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.children
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.itsabugnotafeature.fitocrazy.databinding.ActivityHomepageBinding
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

class HomepageActivity : AppCompatActivity() {

    private lateinit var binding: ActivityHomepageBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityHomepageBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val navView: BottomNavigationView = binding.navView
        val navHost = supportFragmentManager.findFragmentById(R.id.nav_host_fragment_activity_homepage) as NavHostFragment

        val navController = navHost.navController
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        val appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.navigation_workoutList, R.id.navigation_dashboard, R.id.navigation_notifications
            )
        )
        setupActionBarWithNavController(navController, appBarConfiguration)
        navView.setupWithNavController(navController)

        val states = arrayOf(
            intArrayOf(android.R.attr.state_checked), // selected
            intArrayOf(-android.R.attr.state_checked), // notselected
        )

        val colors = intArrayOf(
            applicationContext.getColor(R.color.blue_accent_lightest),
            applicationContext.getColor(R.color.blue_main_light),
        )

        val myList = ColorStateList(states, colors)
        navView.itemIconTintList = myList
        navView.backgroundTintList = myList

        /*var lastView:View? =null
        navView.setOnItemSelectedListener { item ->
            Log.i("TEST", "${item.isChecked} ${item.isEnabled}")
            lastView?.setBackgroundColor(applicationContext.getColor(R.color.purple_main))
            lastView = findViewById<View>(item.itemId)
            lastView?.setBackgroundColor(applicationContext.getColor(R.color.blue_main))
            false
        }*/
    }
}