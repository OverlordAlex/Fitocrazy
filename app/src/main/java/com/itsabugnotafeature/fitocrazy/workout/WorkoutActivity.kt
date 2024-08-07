package com.itsabugnotafeature.fitocrazy.workout

import android.os.Bundle
import android.os.SystemClock
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateInterpolator
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.Chronometer
import android.widget.EditText
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentTransaction
import androidx.fragment.app.setFragmentResultListener
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.itsabugnotafeature.fitocrazy.R
import com.itsabugnotafeature.fitocrazy.common.Exercise
import com.itsabugnotafeature.fitocrazy.common.ExerciseDatabase
import com.itsabugnotafeature.fitocrazy.common.Set
import com.itsabugnotafeature.fitocrazy.common.Workout
import com.itsabugnotafeature.fitocrazy.workout.addExercise.AddNewExerciseToWorkoutFragment
import kotlinx.coroutines.runBlocking
import java.math.RoundingMode
import java.text.DecimalFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.SortedMap
import kotlin.math.pow

/**
 * TODO
 *      - workout selection on main page
 *      - DONE ~~resumable timers~~
 *      - ensure timers don't start if adding sets to historical exercises
 *      - verify that historical exercises show their "today" sets appropriately
 *      - bring points to the spinners on exercise type for more flexibility?
 *      - chip group for body parts per exercise
 *      - make DB queries live
 *      - handle background running and all on-resume stuff
 */


class WorkoutActivity : AppCompatActivity() {

    private lateinit var db: ExerciseDatabase
    private lateinit var workout: Workout

    fun calculatePoints(exerciseModelId: Long, weight: Double, reps: Int): Int {
        // we will never calculate points for an exercise that doesn't exist
        var points = 0.0
        runBlocking {
            val exercise = db.exerciseDao().getExerciseDetails(exerciseModelId)!!
            val record = db.exerciseDao().getRecord(exercise.exercise.exerciseId)


            val basePoints = exercise.exercise.basePoints
            val maxWeight = record?.maxWeight ?: weight
            val maxReps = record?.maxReps ?: reps
            val maxMoved = record?.mostWeightMoved ?: (weight * reps)

            val repMultiplier: Double = when {
                reps <= 4 -> 0.9
                reps <= 8 -> 1.0
                else -> 1.05
            }

            points = basePoints.toDouble().pow((weight / maxWeight)) * (reps * repMultiplier)
            if (weight > maxWeight) points += 50
            if (reps > maxReps) points += 50
            if ((weight * reps) > maxMoved) points += 50
        }
        return points.toInt()
    }

    data class ExerciseView(
        val displayName: String,
        val exercise: Exercise,
        val sets: MutableList<Set>,
    )

    interface ExerciseNotification {
        fun setAdded(exerciseModelId: Long, weight: Double, reps: Int): Unit
        fun setRemoved(exerciseModelId: Long, weight: Double, reps: Int)
    }

    // the parent list of exercises in a workout
    class ExerciseListViewAdapter(
        private var exerciseList: MutableList<Pair<Exercise, MutableList<Set>>>,
        private var db: ExerciseDatabase,
        private var parent: RecyclerView,
        private var notifier: ExerciseNotification
    ) : RecyclerView.Adapter<ExerciseListViewAdapter.ViewHolder>() {
        inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            fun bind(
                currentExercise: ExerciseView,
                exerciseHistory: SortedMap<Exercise, List<Set>>,
            ) {
                val weightEditText =
                    itemView.findViewById<EditText>(R.id.numberEntry_addKilogramsToThisExercise)
                val repsEditText =
                    itemView.findViewById<EditText>(R.id.numberEntry_addRepsToThisExercise)
                weightEditText.setOnFocusChangeListener { _, hasFocus ->
                    if (hasFocus) weightEditText.text = null
                    else if (weightEditText.text.toString().isNotEmpty()) {
                        val weight = weightEditText.text.toString()
                        if (weight.contains('.')) {
                            weightEditText.setText("%.2f".format((weight.toDouble() * 100).toInt() / 100.0))
                        }
                    }
                }
                repsEditText.setOnFocusChangeListener { _, hasFocus ->
                    if (hasFocus) repsEditText.text = null
                    else if (repsEditText.text.toString().isNotEmpty()) {
                        repsEditText.setText(repsEditText.text.toString().toInt().toString())
                    }
                }

                if (currentExercise.sets.lastOrNull() == null) {
                    weightEditText.text = null
                } else if (currentExercise.sets.lastOrNull()?.weight?.toInt()
                        ?.toDouble() != currentExercise.sets.lastOrNull()?.weight
                ) {
                    // if int() and double() dont match, then there are decimal places
                    weightEditText.setText(currentExercise.sets.lastOrNull()?.weight?.toString())
                } else {
                    // otherwise it's safe to go to int
                    weightEditText.setText(currentExercise.sets.lastOrNull()?.weight?.toInt().toString())
                }

                repsEditText.setText(currentExercise.sets.lastOrNull()?.reps?.toString())

                itemView.findViewById<TextView>(R.id.label_exerciseNameOnCard).text = currentExercise.displayName

                val exerciseScrollLayout = itemView.findViewById<LinearLayout>(R.id.layout_listOfSetsOnExerciseCard)
                exerciseScrollLayout.removeAllViews()

                exerciseHistory.forEach { (workout, sets) ->
                    val setListView = LayoutInflater.from(parent.context).inflate(
                        R.layout.workout_exercise_set_list_horizontal_container,
                        parent,
                        false
                    )
                    setListView.findViewById<TextView>(R.id.label_setDate).text =
                        workout.date.format(DateTimeFormatter.ofPattern("dd LLL yy"))

                    val setWeightString = StringBuilder()
                    val setRepsString = StringBuilder()
                    for (set in sets) {
                        if (set.weight % 1 == 0.0) {
                            setWeightString.appendLine("${decimalFormatter.format(set.weight).padEnd(3)} *")
                        } else {
                            setWeightString.appendLine("${String.format(Locale.US, "%.2f", set.weight)} *")
                        }
                        setRepsString.appendLine(set.reps)
                    }

                    setListView.findViewById<TextView>(R.id.textlist_weightInSet).text =
                        setWeightString
                    setListView.findViewById<TextView>(R.id.textlist_repsInSet).text = setRepsString
                    exerciseScrollLayout.addView(setListView)
                }

                val setListView = LayoutInflater.from(parent.context).inflate(
                    R.layout.workout_exercise_set_list_horizontal_container_today,
                    parent,
                    false
                )
                /*val param = LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.MATCH_PARENT)
                param.weight = 5f
                param.gravity = Gravity.END
                setListView.layoutParams = param*/

                setListView.findViewById<TextView>(R.id.label_setDate).text = "Today"

                val setWeightString = StringBuilder()
                val setRepsString = StringBuilder()
                for (set in currentExercise.sets) {
                    if (set.weight % 1 == 0.0) {
                        setWeightString.appendLine("${decimalFormatter.format(set.weight).padEnd(3)} *")

                    } else {
                        setWeightString.appendLine("${String.format(Locale.US, "%.2f", set.weight)} *")
                    }

                    setRepsString.appendLine(set.reps)
                }
                setWeightString.append("KG *")
                setListView.findViewById<TextView>(R.id.textlist_weightInSet).text =
                    setWeightString
                setListView.findViewById<TextView>(R.id.textlist_repsInSet).text = setRepsString

                setListView.minimumWidth = (parent.width * 0.3).toInt()
                exerciseScrollLayout.addView(setListView)

                val scrollView =
                    itemView.findViewById<HorizontalScrollView>(R.id.scrollview_listOfSetsOnExerciseCard)
                scrollView.post {
                    scrollView.scrollX = setListView.left
                }

                itemView.findViewById<Button>(R.id.btn_removeLastSetFromThisExercise)
                    .setOnClickListener {
                        val imm = parent.context.getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
                        imm.hideSoftInputFromWindow(parent.windowToken, 0)

                        if (currentExercise.sets.isEmpty()) {
                            runBlocking {
                                db.exerciseDao().deleteExercise(currentExercise.exercise)
                            }
                            removeItem(adapterPosition)
                        } else {
                            notifier.setRemoved(
                                currentExercise.exercise.exerciseModelId,
                                currentExercise.sets.last().weight,
                                currentExercise.sets.last().reps
                            )
                            runBlocking {
                                db.exerciseDao()
                                    .deleteSetFromExercise(currentExercise.sets.removeLast())
                            }

                            notifyItemChanged(adapterPosition)
                        }
                    }
                itemView.findViewById<Button>(R.id.btn_addSetToThisExercise).setOnClickListener {
                    val imm = parent.context.getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
                    imm.hideSoftInputFromWindow(parent.windowToken, 0)

                    // TODO work on total reps + weight (incl calculate on load)
                    // TODO refactor some large methods
                    // TODO: ensure weight is formatted correctly to 2 decimal places for storing *100 to int /100.0 ?
                    val weight: Double? = weightEditText.text.toString().toDoubleOrNull()
                    val reps: Int? = repsEditText.text.toString().toIntOrNull()
                    if (weight == null || reps == null) return@setOnClickListener

                    val set = Set(
                        0,
                        currentExercise.exercise.exerciseId,
                        weight,
                        reps,
                        currentExercise.sets.size
                    )
                    runBlocking { set.setID = db.exerciseDao().addSetToExercise(set) }
                    currentExercise.sets.add(set)
                    notifier.setAdded(currentExercise.exercise.exerciseModelId, set.weight, set.reps)

                    // if the current itemview is at the top of the parent, scroll
                    //      if its lower, smooth scroll
                    parent.post {
                        if (itemView.y > parent.y)
                        // dont animate any movement if we're at teh top
                            parent.scrollToPosition(adapterPosition)
                        else
                            parent.smoothScrollToPosition(adapterPosition)
                    }
                    notifyItemChanged(adapterPosition)
                }
            }
        }

        fun removeItem(position: Int) {
            // TODO make the "add exercises" text visible again
            exerciseList.removeAt(position)
            notifyItemRemoved(position)
        }

        override fun getItemCount() = exerciseList.size

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view: View = LayoutInflater.from(parent.context)
                .inflate(R.layout.workout_exercise_row, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            if (exerciseList.isEmpty()) return
            runBlocking {
                val currentExercise = exerciseList[position]
                // TODO: historical sets should be eager-loaded when exercise added to workout
                val historicalSets = db.exerciseDao().getHistoricalSets(
                    currentExercise.first.exerciseModelId,
                    10,
                    currentExercise.first.toTimeStamp()
                ).toSortedMap()
                val displayName = db.exerciseDao()
                    .getExerciseDetails(currentExercise.first.exerciseModelId)?.exercise?.displayName
                    ?: "NAME NOT FOUND?"
                holder.bind(
                    ExerciseView(
                        displayName,
                        currentExercise.first,
                        currentExercise.second
                    ), historicalSets
                )
            }
        }

    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_workout)

        db = ExerciseDatabase.getInstance(applicationContext)

        val today = LocalDate.now()
        var exercises: List<Exercise>
        var exerciseList: MutableList<Pair<Exercise, MutableList<Set>>>
        var order = 1
        runBlocking {
            workout = db.exerciseDao().getWorkout(intent.getLongExtra("workoutId", -1)) ?: Workout(0, today)
            if (workout.workoutId == 0L) {
                workout.workoutId = db.exerciseDao().addWorkout(workout)
            }

            exercises = db.exerciseDao().getListOfExercise(today)
            exerciseList =
                exercises.map { Pair(it, db.exerciseDao().getSets(it.exerciseId).toMutableList()) }
                    .toMutableList()
        }
        exerciseList.forEach {
            it.second.forEach { set ->
                workout.totalWeight += set.weight * set.reps
                workout.totalReps += set.reps
                workout.totalPoints += calculatePoints(it.first.exerciseModelId, set.weight, set.reps)
                workout.totalSets += 1
            }
        }
        findViewById<TextView>(R.id.totalWeight).text = getString(R.string.total_weight, workout.totalWeight)
        findViewById<TextView>(R.id.totalReps).text = getString(R.string.total_reps, workout.totalReps)
        findViewById<TextView>(R.id.totalPoints).text = getString(R.string.total_points, workout.totalPoints)

        val totalTimeTimer = findViewById<Chronometer>(R.id.timer_totalTime)
        val setTimeTimer = findViewById<Chronometer>(R.id.timer_timeAfterLastSet)

        class Notifier : ExerciseNotification {
            override fun setAdded(exerciseModelId: Long, weight: Double, reps: Int) {
                workout.totalWeight += weight * reps
                workout.totalReps += reps
                workout.totalSets += 1
                workout.totalPoints += calculatePoints(exerciseModelId, weight, reps)

                findViewById<TextView>(R.id.totalWeight).text = getString(R.string.total_weight, workout.totalWeight)
                findViewById<TextView>(R.id.totalReps).text = getString(R.string.total_reps, workout.totalReps)
                findViewById<TextView>(R.id.totalPoints).text = getString(R.string.total_points, workout.totalPoints)

                setTimeTimer.base = SystemClock.elapsedRealtime()
                setTimeTimer.start()
                totalTimeTimer.start()
                totalTimeTimer.base = SystemClock.elapsedRealtime() - workout.totalTime
            }

            override fun setRemoved(exerciseModelId: Long, weight: Double, reps: Int) {
                workout.totalWeight -= weight * reps
                workout.totalReps -= reps
                workout.totalSets -= 1
                workout.totalPoints -= calculatePoints(exerciseModelId, weight, reps)

                findViewById<TextView>(R.id.totalWeight).text = getString(R.string.total_weight, workout.totalWeight)
                findViewById<TextView>(R.id.totalReps).text = getString(R.string.total_reps, workout.totalReps)
                findViewById<TextView>(R.id.totalPoints).text = getString(R.string.total_points, workout.totalPoints)
            }
        }

        totalTimeTimer.base = SystemClock.elapsedRealtime() - workout.totalTime
        if (workout.date == today) totalTimeTimer.start()

        val labelForEmptyExerciseList = findViewById<TextView>(R.id.label_startWorkoutAddExercise)
        val exerciseListView = findViewById<RecyclerView>(R.id.list_exercisesInCurrentWorkout)
        val exerciseListViewAdapter = ExerciseListViewAdapter(exerciseList, db, exerciseListView, Notifier())
        exerciseListView.itemAnimator = null
        exerciseListView.layoutManager =
            LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)
        exerciseListView.adapter = exerciseListViewAdapter
        // do not scroll on first open
        //exerciseListView.scrollToPosition(exerciseList.size - 1)

        if (exerciseList.isEmpty()) {
            exerciseListView.visibility = RecyclerView.INVISIBLE
            labelForEmptyExerciseList.visibility = TextView.VISIBLE
        }

        val dialog: DialogFragment = AddNewExerciseToWorkoutFragment()
        val showDialog = findViewById<FloatingActionButton>(R.id.btn_addNewExerciseToCurrentWorkout)

        val endWorkoutBtn = findViewById<FloatingActionButton>(R.id.btn_endWorkout)
        endWorkoutBtn.setOnClickListener {
            workout.totalExercises = exerciseList.size
            workout.totalTime = SystemClock.elapsedRealtime() - totalTimeTimer.base
            runBlocking { db.exerciseDao().updateWorkout(workout) }

            setTimeTimer.stop()
            totalTimeTimer.stop()
        }

        showDialog.setOnLongClickListener {
            endWorkoutBtn.visibility = FloatingActionButton.VISIBLE
            endWorkoutBtn.animate().alpha(1f).setDuration(200).setInterpolator(AccelerateInterpolator())

            endWorkoutBtn.postDelayed({
                endWorkoutBtn.animate().alpha(0f).setDuration(100).setInterpolator(AccelerateInterpolator())
                    .withEndAction {
                        endWorkoutBtn.visibility = FloatingActionButton.GONE
                    }
            }, 2000)
            true
        }
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
                val exerciseModelId = bundle.getLong("exerciseID", -1L)
                if (exerciseModelId == -1L) return@setFragmentResultListener

                val exercise = Exercise(0, exerciseModelId, today, order++)
                runBlocking {
                    exercise.exerciseId = db.exerciseDao().addExerciseSet(exercise)
                }
                exerciseList.add(Pair(exercise, mutableListOf()))
                exerciseListViewAdapter.notifyItemInserted(exerciseList.size - 1)
                exerciseListView.smoothScrollToPosition(exerciseList.size - 1)
                exerciseListView.visibility = RecyclerView.VISIBLE
                labelForEmptyExerciseList.visibility = TextView.GONE
            }
        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.layout_currentWorkout)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    companion object {
        private val decimalFormatter = DecimalFormat("###.##")

        init {
            decimalFormatter.roundingMode = RoundingMode.FLOOR
        }

    }
}