package com.itsabugnotafeature.fitocrazy.workout

import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.LinearLayout
import android.widget.LinearLayout.VISIBLE
import android.widget.LinearLayout.inflate
import android.widget.Spinner
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentTransaction
import androidx.fragment.app.setFragmentResultListener
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.itsabugnotafeature.fitocrazy.R
import com.itsabugnotafeature.fitocrazy.common.Equipment
import com.itsabugnotafeature.fitocrazy.common.Exercise
import com.itsabugnotafeature.fitocrazy.common.ExerciseDatabase
import com.itsabugnotafeature.fitocrazy.common.ExerciseSets
import com.itsabugnotafeature.fitocrazy.common.ExerciseWithComponents
import com.itsabugnotafeature.fitocrazy.common.Movement
import com.itsabugnotafeature.fitocrazy.common.Position
import com.itsabugnotafeature.fitocrazy.common.Set
import com.itsabugnotafeature.fitocrazy.common.Workout
import com.itsabugnotafeature.fitocrazy.workout.addExercise.AddNewExerciseToWorkoutFragment
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.time.Instant
import java.util.Date


class WorkoutActivity : AppCompatActivity() {

    private lateinit var db: ExerciseDatabase

    val exerciseTestData = listOf(
        ExerciseSets(ExerciseWithComponents(Exercise(1,1,1, 1), Equipment(1, "BARBELL"), Position(1, "BENCH"), Movement(1, "PRESS")), listOf(Set(20.0, 15), Set(60.0, 3), Set(120.0, 3))),
        ExerciseSets(ExerciseWithComponents(Exercise(1,1,1, 1), Equipment(1, "DUMBBELL"), Position(1, "BENCH"), Movement(1, "PRESS")), listOf(Set(10.0, 20))),
        ExerciseSets(ExerciseWithComponents(Exercise(1,1,1, 1), Equipment(1, "SEATED"), Position(1, "LEG"), Movement(1, "CURL")), listOf(Set(40.0, 5), Set(60.0, 3))),
    )

    override fun onResume() {
        super.onResume()

        lifecycleScope.launch {
            db = ExerciseDatabase.getInstance(applicationContext)
            findViewById<Spinner>(R.id.spinner_equipment)?.adapter = ArrayAdapter(applicationContext, android.R.layout.simple_spinner_item, db.exerciseComponentsDao().getAllMovement())
        }
       }

    // the parent list of exercises in a workout
    class ExerciseListViewAdapter(private var workout: Workout): RecyclerView.Adapter<ExerciseListViewAdapter.ViewHolder>() {
        private lateinit var parent: ViewGroup

        inner class ViewHolder(itemView: View): RecyclerView.ViewHolder(itemView) {
            fun bind(exerciseHistory: List<ExerciseSets>, date:Date) {
                val exerciseScrollLayout = itemView.findViewById<LinearLayout>(R.id.layout_listOfSetsOnExerciseCard)
                for (set in exerciseHistory) {
                    val setList = TextView(itemView.context)
                    setList.gravity = Gravity.CENTER_HORIZONTAL
                    setList.layoutParams = ViewGroup.LayoutParams(RecyclerView.LayoutParams.WRAP_CONTENT, RecyclerView.LayoutParams.MATCH_PARENT)
                    //setList.setEms(12)
                    setList.visibility = VISIBLE

                    val setListString = StringBuilder()
                    setListString.append(SimpleDateFormat("dd MMM yyyy").format(date))
                    setListString.append("\n------------\n")
                    set.sets.map { setListString.append("${it.weight.toString().padStart(5)}kg * ${it.reps.toString().padEnd(7)}\n")}
                    setList.text = setListString

                    exerciseScrollLayout.addView(setList)
                }
            }
        }

        override fun getItemCount() = workout.exerciseSets.size

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view: View = LayoutInflater.from(parent.context).inflate(R.layout.workout_exercise_row, parent, false)
            this.parent = parent
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            // TODO: should fetch history based on which exercise is in the position
            holder.bind(workout.exerciseSets, workout.date)
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()
        setContentView(R.layout.activity_workout)

        val exerciseListView = findViewById<RecyclerView>(R.id.list_exercisesInCurrentWorkout)
        val exerciseListViewAdapter = ExerciseListViewAdapter(Workout(0, Date.from(Instant.now()),exerciseTestData))
        exerciseListView.layoutManager = LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)
        exerciseListView.adapter = exerciseListViewAdapter

        val dialog: DialogFragment = AddNewExerciseToWorkoutFragment()
        val showDialog = findViewById<FloatingActionButton>(R.id.btn_addNewExerciseToCurrentWorkout)
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

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.layout_currentWorkout)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }
}