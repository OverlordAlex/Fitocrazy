package com.itsabugnotafeature.fitocrazy.workout

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
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
import com.itsabugnotafeature.fitocrazy.workout.addExercise.AddNewExerciseToWorkoutFragment
import kotlinx.coroutines.runBlocking
import java.math.RoundingMode
import java.text.DecimalFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.SortedMap


class WorkoutActivity : AppCompatActivity() {

    private lateinit var db: ExerciseDatabase
    private var totalWeight = 0.0
    private var totalReps = 0

    data class ExerciseView(
        val displayName: String,
        val exercise: Exercise,
        val sets: MutableList<Set>,
    )

    interface ExerciseNotification {
        fun setAdded(weight: Double, reps: Int): Unit
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
                        val idx = weight.lastIndexOf('.')
                        if (idx > 0) {
                            if (idx == weight.length - 2) {
                                weightEditText.setText(weight + "0")
                            }
                            else if (idx < weight.length - 3) {
                                weightEditText.setText(weight.dropLast(weight.length - idx - 3))
                            }

                        }
                        /*if (weight % 1 == 0.0) {
                            //weightEditText.setText(decimalFormatter.format(weight).padEnd(3))
                            weightEditText.setText("0")
                        } else {
                            //weightEditText.setText(String.format(Locale.US, "%.2f", weight))
                            weightEditText.setText("%+.${2}f".format(weight))
                        }*/
                    }
                }
                repsEditText.setOnFocusChangeListener { _, hasFocus ->
                    if (hasFocus) repsEditText.text = null
                    else if (repsEditText.text.toString().isNotEmpty()) {
                        repsEditText.setText(repsEditText.text.toString().toInt().toString())
                    }
                }

                weightEditText.setText(
                    currentExercise.sets.lastOrNull()?.weight?.toString() ?: "10.0"
                )
                repsEditText.setText(currentExercise.sets.lastOrNull()?.reps?.toString() ?: "5")

                itemView.findViewById<TextView>(R.id.label_exerciseNameOnCard).text =
                    currentExercise.displayName

                val exerciseScrollLayout =
                    itemView.findViewById<LinearLayout>(R.id.layout_listOfSetsOnExerciseCard)
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
                    //setWeightString.appendLine("${set.weight} *")
                    //setWeightString.appendLine("${DecimalFormat("###.##").format(set.weight)} *")
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
                        weightEditText.onEditorAction(EditorInfo.IME_ACTION_DONE)
                        repsEditText.onEditorAction(EditorInfo.IME_ACTION_DONE)

                        if (currentExercise.sets.isEmpty()) {
                            runBlocking {
                                db.exerciseDao().deleteExercise(currentExercise.exercise)
                            }
                            removeItem(adapterPosition)
                        } else {
                            runBlocking {
                                db.exerciseDao()
                                    .deleteSetFromExercise(currentExercise.sets.removeLast())
                            }
                            notifyItemChanged(adapterPosition)
                        }
                    }
                itemView.findViewById<Button>(R.id.btn_addSetToThisExercise).setOnClickListener {
                    weightEditText.onEditorAction(EditorInfo.IME_ACTION_DONE)
                    repsEditText.onEditorAction(EditorInfo.IME_ACTION_DONE)

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
                    notifier.setAdded(set.weight, set.reps)

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
        db = ExerciseDatabase.getInstance(applicationContext)

        val today = LocalDate.now()
        var exercises: List<Exercise>
        var exerciseList: MutableList<Pair<Exercise, MutableList<Set>>>
        var order = 1

        runBlocking {
            exercises = db.exerciseDao().getWorkout(today)
            exerciseList =
                exercises.map { Pair(it, db.exerciseDao().getSets(it.exerciseId).toMutableList()) }
                    .toMutableList()
        }

        enableEdgeToEdge()
        setContentView(R.layout.activity_workout)

        class Notifier: ExerciseNotification {
            override fun setAdded(weight: Double, reps: Int) {
                totalWeight += weight * reps
                findViewById<TextView>(R.id.totalWeight).text = totalWeight.toString()
            }
        }

        val labelForEmptyExerciseList = findViewById<TextView>(R.id.label_startWorkoutAddExercise)
        val exerciseListView = findViewById<RecyclerView>(R.id.list_exercisesInCurrentWorkout)
        val exerciseListViewAdapter = ExerciseListViewAdapter(exerciseList, db, exerciseListView, Notifier())
        exerciseListView.itemAnimator = null
        exerciseListView.layoutManager =
            LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)
        exerciseListView.adapter = exerciseListViewAdapter
        exerciseListView.scrollToPosition(exerciseList.size - 1)

        if (exerciseList.isEmpty()) {
            exerciseListView.visibility = RecyclerView.INVISIBLE
            labelForEmptyExerciseList.visibility = TextView.VISIBLE
        }

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
                val exerciseModelId = bundle.getLong("exerciseID", -1L)
                if (exerciseModelId == -1L) return@setFragmentResultListener

                val exercise = Exercise(0, exerciseModelId, today, order++)
                runBlocking {
                    exercise.exerciseId = db.exerciseDao().addExerciseSet(exercise)
                }
                exerciseList.add(Pair(exercise, mutableListOf()))
                exerciseListViewAdapter.notifyItemInserted(exerciseList.size - 1)
                exerciseListView.scrollToPosition(exerciseList.size - 1)
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
            decimalFormatter.roundingMode = RoundingMode.UNNECESSARY
        }

    }
}