package com.itsabugnotafeature.fitocrazy.workout

import android.app.NotificationManager
import android.app.PendingIntent
import android.app.TaskStackBuilder
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
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
import android.widget.RemoteViews
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentTransaction
import androidx.fragment.app.setFragmentResultListener
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.itsabugnotafeature.fitocrazy.R
import com.itsabugnotafeature.fitocrazy.common.Exercise
import com.itsabugnotafeature.fitocrazy.common.ExerciseDatabase
import com.itsabugnotafeature.fitocrazy.common.ExerciseWithComponentModel
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
 *      - DONE ~~workout list on main page~~
 *      - DONE ~~workout selection on main page~~
 *      - DONE ~~resumable timers~~
 *      - DONE ~~ensure timers don't start if adding sets to historical exercises~~
 *      - DONE ~~verify that historical exercises show their "today" sets appropriately~~
 *      - DONE ~~style main page strings~~
 *      - DONE ~~color and theme main page~~
 *      - DONE ~~chip group for body parts per exercise~~
 *      - DONE ~~total chip group per exercise~~
 *      - DONE ~~refresh DB workout list on result~~
 *      - DONE ~~color and theme workout page~~
 *      - DONE ~~delete workouts on long-press~~
 *      - DONE ~~close delete-workout button on scroll~~
 *      - DONE ~~db.Exercise should link to a workoutID and not a date to group exercises~~
 *      ---- DONE ~~and delete should delete associated exerciseId and Set~~
 *      - DONE ~~BUG: total_time timer resets when adding a new set on today (for saved workouts)~~
 *      - DONE ~~BUG: delete button shows twice sometimes~~
 *      - DONE ~~timer can be paused and restarted~~
 *      - DONE ~~BUG: adding a new workout puts it at the bottom instead of the top~~
 *      - DONE ~~BUG: sets added to previous workouts show up as "today"~~
 *      - DONE ~~style popups for adding exercises and components~~
 *
 * TODO
 *      - bring points to the spinners on exercise type for more flexibility?
 *      - handle background running and all on-resume stuff
 *      - allow editing of exercise components
 *      - "enter" when creating a new exercise component does weird stuff (should trim+enter)
 *      - highlight good points totals
 *      - profile statistics
 *      - exercise PRs in the exercise card in workout?
 *      - BUG: points dont make sense when adding and removing - could be related to bonus? in fact all stats are not loaded correctly
 *      - reset DB
 *      - notification panel
 *      - better logo and splash screen
 *      -
 *      - better font
 *
 * TODO - never
 *      ? tint of chips should be per bodypart - right now its ordered by most frequent
 */


class WorkoutActivity : AppCompatActivity() {

    private val CHANNEL_ID = "FitoCrazyCurrentExerciseChannel"
    private lateinit var db: ExerciseDatabase
    private lateinit var workout: Workout
    private var setTimerIsActive = false

    fun calculatePoints(exerciseModelId: Long, weight: Double, reps: Int): Int {
        // we will never calculate points for an exercise that doesn't exist
        var points: Double
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
        val tags: List<String>,
    )

    interface ExerciseNotification {
        fun setAdded(exerciseModelId: Long, weight: Double, reps: Int): Unit
        fun setRemoved(exerciseModelId: Long, weight: Double, reps: Int)
        fun setDeleted()
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
                if (currentExercise.tags.isNotEmpty()) {
                    val chipGroup = itemView.findViewById<ChipGroup>(R.id.chipGroup_exerciseTags)
                    chipGroup.removeAllViews()
                    chipGroup.visibility = ChipGroup.VISIBLE
                    currentExercise.tags.forEach { chipName ->
                        val newChip = Chip(itemView.context)
                        newChip.text = chipName
                        newChip.setChipBackgroundColorResource(R.color.blue_accent_light)
                        //newChip.setTextColor(context?.let { ContextCompat.getColor(it, R.color.white) } ?: R.color.white)
                        chipGroup.addView(newChip)
                    }
                }

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

                    setListView.findViewById<TextView>(R.id.textlist_weightInSet).text = setWeightString
                    setListView.findViewById<TextView>(R.id.textlist_repsInSet).text = setRepsString
                    exerciseScrollLayout.addView(setListView)
                }

                val setListView = LayoutInflater.from(parent.context).inflate(
                    R.layout.workout_exercise_set_list_horizontal_container_today,
                    parent,
                    false
                )

                setListView.findViewById<TextView>(R.id.label_setDate).text =
                    if (LocalDate.now() == currentExercise.exercise.date) {
                        itemView.context.getString(R.string.today)
                    } else {
                        currentExercise.exercise.date.format(DateTimeFormatter.ofPattern("dd LLL yy"))
                    }

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
                setListView.findViewById<TextView>(R.id.textlist_weightInSet).text = setWeightString
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
                            notifier.setDeleted()
                        } else {
                            runBlocking {
                                db.exerciseDao()
                                    .deleteSetFromExercise(currentExercise.sets.removeLast())
                            }
                            notifier.setRemoved(
                                currentExercise.exercise.exerciseModelId,
                                currentExercise.sets.last().weight,
                                currentExercise.sets.last().reps
                            )
                            notifyItemChanged(adapterPosition)
                        }
                    }
                itemView.findViewById<Button>(R.id.btn_addSetToThisExercise).setOnClickListener {
                    val imm = parent.context.getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
                    imm.hideSoftInputFromWindow(parent.windowToken, 0)

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
                val exerciseModel = db.exerciseDao().getExerciseDetails(currentExercise.first.exerciseModelId)
                val displayName = exerciseModel?.exercise?.displayName ?: "NAME NOT FOUND?"
                val chips = exerciseModel?.exercise?.getChips() ?: emptyList()
                holder.bind(
                    ExerciseView(
                        displayName,
                        currentExercise.first,
                        currentExercise.second,
                        chips
                    ), historicalSets
                )
            }
        }

    }

    val broadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            TODO("Not yet implemented")
        }
    }

    private fun removeNotification() {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(notificationId)
    }
    // TODO DONE ~~update when exercise added, set added, set removed~~
    // TODO DONE ~~only show when workout is today!~~
    // TODO DONE ~~only show on workout activity~~
    // TODO pendingIntent on button press to trigger add set
    // TODO pendingIntent to reopen app
    // TODO only dismiss notification on workout saved, or back button (not home)
    private fun showNotification(
        totalExercises: Int,
        date: LocalDate,
        chronometerBase: Long,
        chronometerRunning: Boolean,
        displayName: String?,
        set: Set?
    ) {
        if (date != LocalDate.now()) return

        val title = "[$totalExercises] ${date.format(DateTimeFormatter.ofPattern("dd LLLL yyyy"))}"
        var exerciseContent = displayName ?: ""
        exerciseContent += if (set != null) ": ${set.weight}kg x ${set.reps}" else ""

        //val pendingIntent = PendingIntent.getActivity(applicationContext, 0, intent, PendingIntent.FLAG_IMMUTABLE)
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val notificationLayout = RemoteViews(packageName, R.layout.notification_small)
        notificationLayout.setTextViewText(R.id.label_notificationTitle, title)
        notificationLayout.setChronometer(R.id.timer_notificationSetTime, chronometerBase, null, chronometerRunning)

        val notificationLayoutExpanded = RemoteViews(packageName, R.layout.notification_large)
        notificationLayoutExpanded.setTextViewText(R.id.label_notificationTitle, title)
        notificationLayoutExpanded.setChronometer(
            R.id.timer_notificationSetTime,
            chronometerBase,
            null,
            chronometerRunning
        )
        notificationLayoutExpanded.setTextViewText(R.id.label_notificationContent, exerciseContent)
        // TODO on click start a new set
        //notificationLayoutExpanded.setOnClickPendingIntent()

        /*val resultIntent = Intent(this, WorkoutActivity::class.java)
        // Create the TaskStackBuilder.
        val resultPendingIntent: PendingIntent? = TaskStackBuilder.create(this).run {
            // Add the intent, which inflates the back stack.
            addNextIntentWithParentStack(resultIntent)
            // Get the PendingIntent containing the entire back stack.
            getPendingIntent(0,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        }*/

        //val pendingIntent: PendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE)
        val customNotification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setContentTitle(title)
            .setSmallIcon(R.drawable.fitocrazy_logo) // TODO get this to work
            .setStyle(NotificationCompat.DecoratedCustomViewStyle())
            .setCustomContentView(notificationLayout)
            .setCustomBigContentView(notificationLayoutExpanded)
            //.setContentIntent(resultPendingIntent)
            .build()
        notificationManager.notify(notificationId, customNotification)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_workout)

        db = ExerciseDatabase.getInstance(applicationContext)

        val today = LocalDate.now()
        var exercises: List<Exercise>
        var exerciseList: MutableList<Pair<Exercise, MutableList<Set>>>
        runBlocking {
            workout = db.exerciseDao().getWorkout(intent.getLongExtra("workoutId", -1)) ?: Workout(0, today)
            if (workout.workoutId == 0L) {
                workout.workoutId = db.exerciseDao().addWorkout(workout)
            }

            exercises = db.exerciseDao().getListOfExerciseInWorkout(workout.workoutId)
            exerciseList =
                exercises.map { Pair(it, db.exerciseDao().getSets(it.exerciseId).toMutableList()) }
                    .toMutableList()

            val exerciseModel =
                db.exerciseDao().getExerciseDetails(exerciseList.lastOrNull()?.first?.exerciseModelId ?: -1)

            // TODO format exercise kg correctly?
            showNotification(
                workout.totalExercises,
                workout.date,
                SystemClock.elapsedRealtime(),
                false,
                exerciseModel?.exercise?.displayName,
                exerciseList.lastOrNull()?.second?.lastOrNull()
            )
        }
        title = title.toString() + " " + workout.date

        val totalWeightLabel = findViewById<TextView>(R.id.totalWeightValue)
        val totalRepsLabel = findViewById<TextView>(R.id.totalRepsValue)
        val totalPointsLabel = findViewById<TextView>(R.id.totalPointsValue)

        totalWeightLabel.text = workout.totalWeight.toString()
        totalRepsLabel.text = workout.totalReps.toString()
        totalPointsLabel.text = workout.totalPoints.toString()

        val totalTimeTimer = findViewById<Chronometer>(R.id.timer_totalTime)
        val setTimeTimer = findViewById<Chronometer>(R.id.timer_timeAfterLastSet)

        class Notifier : ExerciseNotification {
            override fun setAdded(exerciseModelId: Long, weight: Double, reps: Int) {
                workout.totalWeight += weight * reps
                workout.totalReps += reps
                workout.totalSets += 1
                workout.totalPoints += calculatePoints(exerciseModelId, weight, reps)

                totalWeightLabel.text = workout.totalWeight.toString()
                totalRepsLabel.text = workout.totalReps.toString()
                totalPointsLabel.text = workout.totalPoints.toString()

                if (today == workout.date) {
                    // only do set timer if we're currently working out
                    setTimeTimer.base = SystemClock.elapsedRealtime()
                    setTimeTimer.start()
                    setTimerIsActive = true
                }

                val exerciseModel = runBlocking {
                    db.exerciseDao().getExerciseDetails(exerciseModelId)
                }
                showNotification(
                    workout.totalExercises,
                    workout.date,
                    SystemClock.elapsedRealtime(),
                    (today == workout.date),
                    exerciseModel?.exercise?.displayName,
                    exerciseList.lastOrNull()?.second?.lastOrNull()
                )
            }

            override fun setRemoved(exerciseModelId: Long, weight: Double, reps: Int) {
                workout.totalWeight -= weight * reps
                workout.totalReps -= reps
                workout.totalSets -= 1
                workout.totalPoints -= calculatePoints(exerciseModelId, weight, reps)

                totalWeightLabel.text = workout.totalWeight.toString()
                totalRepsLabel.text = workout.totalReps.toString()
                totalPointsLabel.text = workout.totalPoints.toString()

                val exerciseModel = runBlocking {
                    db.exerciseDao().getExerciseDetails(exerciseModelId)
                }
                showNotification(
                    workout.totalExercises,
                    workout.date,
                    SystemClock.elapsedRealtime(),
                    false,
                    exerciseModel?.exercise?.displayName,
                    null
                )
            }

            override fun setDeleted() {
                val exerciseModel = runBlocking {
                    db.exerciseDao().getExerciseDetails(exerciseList.lastOrNull()?.first?.exerciseModelId ?: -1)
                }

                showNotification(
                    workout.totalExercises,
                    workout.date,
                    SystemClock.elapsedRealtime(),
                    false,
                    exerciseModel?.exercise?.displayName,
                    null
                )
            }
        }

        totalTimeTimer.base = SystemClock.elapsedRealtime() - workout.totalTime
        if (workout.date == today) totalTimeTimer.start()

        var pausedTime = 0L
        setTimeTimer.setOnClickListener {
            if (setTimerIsActive) {
                setTimeTimer.stop()
                pausedTime = SystemClock.elapsedRealtime() - setTimeTimer.base
                setTimeTimer.setTextColor(applicationContext.getColor(R.color.orange_main))
            } else {
                setTimeTimer.base = SystemClock.elapsedRealtime() - pausedTime
                setTimeTimer.start()
                setTimeTimer.setTextColor(applicationContext.getColor(R.color.black))

            }
            setTimerIsActive = !setTimerIsActive
        }

        val labelForEmptyExerciseList = findViewById<TextView>(R.id.label_startWorkoutAddExercise)
        val exerciseListView = findViewById<RecyclerView>(R.id.list_exercisesInCurrentWorkout)
        val exerciseListViewAdapter = ExerciseListViewAdapter(exerciseList, db, exerciseListView, Notifier())
        exerciseListView.itemAnimator = null
        exerciseListView.layoutManager = LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)
        exerciseListView.adapter = exerciseListViewAdapter
        // do not scroll on first open
        //exerciseListView.scrollToPosition(exerciseList.size - 1)

        if (exerciseList.isEmpty()) {
            exerciseListView.visibility = RecyclerView.INVISIBLE
            labelForEmptyExerciseList.visibility = TextView.VISIBLE
        }

        val endWorkoutBtn = findViewById<FloatingActionButton>(R.id.btn_endWorkout)
        endWorkoutBtn.setOnClickListener {
            workout.totalExercises = exerciseList.size
            if (today == workout.date) workout.totalTime = SystemClock.elapsedRealtime() - totalTimeTimer.base
            runBlocking {
                workout.topTags = exerciseList.fold(emptyList<String>()) { ongoing, item ->
                    ongoing + db.exerciseDao().getExerciseDetails(item.first.exerciseModelId)!!.exercise.getChips()
                }.groupingBy { it }.eachCount().toSortedMap().asIterable().reversed().take(3)
                    .joinToString(" ") { it.key }.trim()

                db.exerciseDao().updateWorkout(workout)
            }

            setTimeTimer.stop()  // needed?
            setTimerIsActive = false
            totalTimeTimer.stop()  // needed?

            intent.putExtra("dataUpdated", true)
            intent.putExtra("workoutId", workout.workoutId)
            setResult(RESULT_OK, intent)
            finish()
        }

        val addNewExerciseDialog: DialogFragment = AddNewExerciseToWorkoutFragment()
        val addNewExerciseButton = findViewById<FloatingActionButton>(R.id.btn_addNewExerciseToCurrentWorkout)

        addNewExerciseButton.setOnLongClickListener {
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

        addNewExerciseButton.setOnClickListener {
            val ft: FragmentTransaction = supportFragmentManager.beginTransaction()
            val prev: Fragment? = supportFragmentManager.findFragmentByTag(addNewExerciseDialog.tag)
            if (prev != null) {
                ft.remove(prev)
            }
            ft.addToBackStack(null)

            // Create and show the dialog.
            addNewExerciseDialog.show(ft, addNewExerciseDialog.tag)
            addNewExerciseDialog.setFragmentResultListener("exerciseAdded") { _, bundle ->
                val exerciseModelId = bundle.getLong("exerciseID", -1L)
                if (exerciseModelId == -1L) return@setFragmentResultListener

                val exercise = Exercise(0, exerciseModelId, workout.date, exerciseList.size, workout.workoutId)
                var exerciseModel: ExerciseWithComponentModel?
                runBlocking {
                    exercise.exerciseId = db.exerciseDao().addExerciseSet(exercise)
                    exerciseModel = db.exerciseDao().getExerciseDetails(exercise.exerciseModelId)
                }
                exerciseList.add(Pair(exercise, mutableListOf()))
                exerciseListViewAdapter.notifyItemInserted(exerciseList.size - 1)
                exerciseListView.smoothScrollToPosition(exerciseList.size - 1)
                exerciseListView.visibility = RecyclerView.VISIBLE
                labelForEmptyExerciseList.visibility = TextView.GONE

                showNotification(
                    exerciseList.size,
                    workout.date,
                    SystemClock.elapsedRealtime(),
                    false,
                    exerciseModel?.exercise?.displayName,
                    exerciseList.lastOrNull()?.second?.lastOrNull()
                )
            }
        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.layout_currentWorkout)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    override fun onStop() {
        removeNotification()
        super.onStop()
    }

    companion object {
        private val notificationId = 123456

        private val decimalFormatter = DecimalFormat("###.##")

        init {
            decimalFormatter.roundingMode = RoundingMode.FLOOR
        }

    }
}