package com.itsabugnotafeature.fitocrazy.workout

import android.graphics.Typeface
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
import com.itsabugnotafeature.fitocrazy.common.ExerciseDatabase
import com.itsabugnotafeature.fitocrazy.workout.addExercise.AddNewExerciseToWorkoutFragment
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.time.Instant
import java.util.Date


class WorkoutActivity : AppCompatActivity() {

    private lateinit var db: ExerciseDatabase

    // the parent list of exercises in a workout
    /*class ExerciseListViewAdapter(private var workout: Workout): RecyclerView.Adapter<ExerciseListViewAdapter.ViewHolder>() {
        private lateinit var parent: ViewGroup

        inner class ViewHolder(itemView: View): RecyclerView.ViewHolder(itemView) {
            fun bind(exerciseHistory: WorkoutSets, date:Date) {
                val exerciseScrollLayout = itemView.findViewById<LinearLayout>(R.id.layout_listOfSetsOnExerciseCard)
                for (set in exerciseHistory.exerciseSets) {
                    val setList = TextView(itemView.context)
                    setList.gravity = Gravity.CENTER_HORIZONTAL
                    setList.layoutParams = ViewGroup.LayoutParams(RecyclerView.LayoutParams.WRAP_CONTENT, RecyclerView.LayoutParams.MATCH_PARENT)
                    setList.visibility = VISIBLE

                    val setListString = StringBuilder()
                    setListString.append(SimpleDateFormat("dd MMM yyyy").format(date))
                    setListString.append("\n------------\n")
                    //TODO set.sets.map { setListString.append("${it.weight.toString().padStart(5)}kg * ${it.reps.toString().padEnd(7)}\n")}
                    setList.text = setListString
                    setList.typeface = Typeface.MONOSPACE

                    exerciseScrollLayout.addView(setList)
                }
            }
        }

        override fun getItemCount() = 1 //TODO workout.exerciseSets.size

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view: View = LayoutInflater.from(parent.context).inflate(R.layout.workout_exercise_row, parent, false)
            this.parent = parent
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            // TODO: should fetch history based on which exercise is in the position
            //holder.bind(workout, workout.date)
        }
    }*/


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // TODO - resume ongoing workout?

        enableEdgeToEdge()
        setContentView(R.layout.activity_workout)

        /*val workout = Workout(0, Date.from(Instant.now()))

        val exerciseListView = findViewById<RecyclerView>(R.id.list_exercisesInCurrentWorkout)
        val exerciseListViewAdapter = ExerciseListViewAdapter(workout)
        exerciseListView.layoutManager = LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)
        exerciseListView.adapter = exerciseListViewAdapter*/

        val dialog: DialogFragment = AddNewExerciseToWorkoutFragment()
        val showDialog = findViewById<FloatingActionButton>(R.id.btn_addNewExerciseToCurrentWorkout)
        showDialog.setOnClickListener {
            // TODO workout.exerciseSets.add(ExerciseSets(ExerciseWithComponents(Exercise(1,1,1,1), Equipment(1, "barbell"), Position(1, "bench"), Movement(1, "press")), emptyList()))
            //exerciseListViewAdapter.notifyItemInserted(workout.exerciseSets.size - 1)
           // Log.i("TEST", workout.exerciseSets.size.toString())

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