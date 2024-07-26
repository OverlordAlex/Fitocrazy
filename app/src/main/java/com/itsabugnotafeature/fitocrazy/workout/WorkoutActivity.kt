package com.itsabugnotafeature.fitocrazy.workout

import android.os.Bundle
import android.util.Log
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentTransaction
import androidx.fragment.app.setFragmentResultListener
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.itsabugnotafeature.fitocrazy.R
import com.itsabugnotafeature.fitocrazy.workout.addexercise.AddNewExerciseToWorkoutFragment

class WorkoutActivity : AppCompatActivity() {


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //val db = Room.databaseBuilder(applicationContext, ExerciseDatabase::class.java, "exercise").build()
        enableEdgeToEdge()
        setContentView(R.layout.activity_workout)

        val dialog: DialogFragment = AddNewExerciseToWorkoutFragment()
        val showDialog = findViewById<FloatingActionButton>(R.id.addNewExercise)
        showDialog.setOnClickListener {
            val ft: FragmentTransaction = supportFragmentManager.beginTransaction()
            val prev: Fragment? = supportFragmentManager.findFragmentByTag(dialog.tag)
            if (prev != null) {
                ft.remove(prev)
            }
            ft.addToBackStack(null)

            // Create and show the dialog.
            dialog.show(ft, dialog.tag)
            dialog.setFragmentResultListener("exerciseAdded") { _, bundle ->
                Log.i("test", "${bundle.getString("equipment")} ${bundle.getString("position")} ${bundle.getString("movement")}")
            }
        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    fun addWorkout(equipment: String,) {
        Log.i("", "$equipment")
    }
}