package com.itsabugnotafeature.fitocrazy

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.os.SystemClock
import android.transition.Fade
import android.transition.Slide
import android.transition.TransitionManager
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.View.OnClickListener
import android.view.ViewGroup
import android.widget.AdapterView.OnItemClickListener
import android.widget.Button
import android.widget.Chronometer
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.itsabugnotafeature.fitocrazy.common.Exercise
import com.itsabugnotafeature.fitocrazy.common.ExerciseDatabase
import com.itsabugnotafeature.fitocrazy.common.Set
import com.itsabugnotafeature.fitocrazy.common.Workout
import com.itsabugnotafeature.fitocrazy.workout.WorkoutActivity
import com.itsabugnotafeature.fitocrazy.workout.WorkoutActivity.ExerciseListViewAdapter
import com.itsabugnotafeature.fitocrazy.workout.WorkoutActivity.ExerciseListViewAdapter.ViewHolder
import com.itsabugnotafeature.fitocrazy.workout.WorkoutActivity.ExerciseNotification
import com.itsabugnotafeature.fitocrazy.workout.WorkoutActivity.ExerciseView
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.runBlocking
import org.w3c.dom.Text
import java.time.format.DateTimeFormatter
import java.util.SortedMap
import kotlin.time.Duration
import kotlin.time.DurationUnit
import kotlin.time.toDuration

class MainActivity : AppCompatActivity() {

    private lateinit var workoutListViewAdapter: WorkoutListViewAdapter

    class WorkoutListViewAdapter(
        var workoutList: List<Workout>,
        private val workoutLauncher: ActivityResultLauncher<Intent>
    ) : RecyclerView.Adapter<WorkoutListViewAdapter.ViewHolder>() {
        inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

            fun bind(
                currentWorkout: Workout
            ) {
                itemView.findViewById<TextView>(R.id.label_workoutDate).text =
                    currentWorkout.date.format(DateTimeFormatter.ofPattern("dd LLLL yyyy"))
                itemView.findViewById<TextView>(R.id.label_workoutNumberExercises).text =
                    itemView.context.getString(R.string.number_of_exercises_in_workout, currentWorkout.totalExercises)
                itemView.findViewById<TextView>(R.id.label_workoutPoints).text =
                    itemView.context.getString(R.string.total_points_in_workout, currentWorkout.totalPoints)

                itemView.findViewById<TextView>(R.id.label_workoutTotalWeightValue).text =
                    "%.0f kg".format(currentWorkout.totalWeight)

                itemView.findViewById<TextView>(R.id.label_workoutTotalRepsValue).text =
                    currentWorkout.totalReps.toString()

                itemView.findViewById<TextView>(R.id.label_workoutTotalSetsValue).text =
                    currentWorkout.totalSets.toString()

                val totalTime = currentWorkout.totalTime.toDuration(DurationUnit.MILLISECONDS)
                    .toComponents { hours, minutes, _, _ -> "%02d:%02d".format(hours, minutes) }
                itemView.findViewById<TextView>(R.id.label_workoutTotalTimeValue).text = totalTime

                val chipGroup = itemView.findViewById<ChipGroup>(R.id.chipGroup_workoutTopTags)
                chipGroup.removeAllViews()
                val tags = currentWorkout.topTags
                if (tags.isNotEmpty()) {
                    chipGroup.visibility = ChipGroup.VISIBLE
                    val colourGradient =
                        listOf(R.color.orange_gradient1, R.color.orange_gradient2, R.color.orange_gradient3)

                    tags.trim().split(" ").forEachIndexed { index, chipName ->
                        val newChip = Chip(itemView.context)
                        newChip.text = chipName
                        newChip.setChipBackgroundColorResource(colourGradient[index % 3])

                        newChip.setTextColor(itemView.context.getColor(R.color.black))
                        chipGroup.addView(newChip)
                    }
                }

                val deleteFrame = itemView.findViewById<FrameLayout>(R.id.frame_deleteWorkout)

                itemView.setOnClickListener {
                    if (deleteFrame.visibility == FrameLayout.VISIBLE) {
                        deleteFrame.visibility = FrameLayout.GONE
                        itemView.findViewById<LinearLayout>(R.id.layout_workoutOtherStats).visibility = LinearLayout.VISIBLE
                        itemView.findViewById<TextView>(R.id.label_workoutNumberExercises).visibility = TextView.VISIBLE
                        return@setOnClickListener
                    }

                    workoutLauncher.launch(
                        Intent(
                            itemView.context.applicationContext,
                            WorkoutActivity::class.java
                        ).setAction("oldWorkoutStartedFromHome").putExtra("workoutId", currentWorkout.workoutId)
                    )
                }

                itemView.setOnLongClickListener {
                    // TODO move to onBindViewHolder actually
                    // TODO made delete button better!
                    /*val transition = Slide()
                    transition.duration = 5000
                    transition.addTarget(itemView)
                    TransitionManager.beginDelayedTransition(itemView.parent, transition)*/
                    deleteFrame.visibility = FrameLayout.VISIBLE
                    itemView.findViewById<LinearLayout>(R.id.layout_workoutOtherStats).visibility = LinearLayout.GONE
                    itemView.findViewById<TextView>(R.id.label_workoutNumberExercises).visibility = TextView.GONE
                    /*itemView.postDelayed({
                        deleteFrame.visibility = FrameLayout.GONE
                    }, 10000)*/
                    true
                }
            }
        }

        override fun getItemCount() = workoutList.size

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): WorkoutListViewAdapter.ViewHolder {
            val view: View = LayoutInflater.from(parent.context)
                .inflate(R.layout.workout_row, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: WorkoutListViewAdapter.ViewHolder, position: Int) {
            holder.bind(workoutList[position])
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.layout_currentWorkout)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val db = ExerciseDatabase.getInstance(applicationContext)
        val workoutList = runBlocking {
            db.exerciseDao().listWorkouts()
        }.toMutableList()

        val goToWorkout =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result: ActivityResult ->
                if (result.resultCode == Activity.RESULT_OK) {
                    if (result.data?.getBooleanExtra("dataUpdated", false) == true) {
                        val workoutId = result.data?.getLongExtra("workoutId", -1) ?: -1L
                        val position = workoutList.indexOfFirst { it.workoutId == workoutId }
                        runBlocking {
                            workoutList[position] = db.exerciseDao().getWorkout(workoutId)!!
                        }
                        workoutListViewAdapter.notifyItemChanged(position)
                    }
                }
            }

        workoutListViewAdapter = WorkoutListViewAdapter(workoutList, goToWorkout)
        val workoutListView = findViewById<RecyclerView>(R.id.list_allWorkoutsHomepage)
        workoutListView.layoutManager =
            LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)
        workoutListView.adapter = workoutListViewAdapter

        findViewById<Button>(R.id.btn_AddNewWorkout).setOnClickListener {
            //putExtra workoutID
            startActivity(
                Intent(
                    applicationContext,
                    WorkoutActivity::class.java
                ).setAction("newWorkoutStartedFromHome")
            )
        }
    }
}