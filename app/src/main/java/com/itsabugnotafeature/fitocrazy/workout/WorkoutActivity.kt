package com.itsabugnotafeature.fitocrazy.workout

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.os.SystemClock
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateInterpolator
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.Chronometer
import android.widget.EditText
import android.widget.HorizontalScrollView
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RemoteViews
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.marginStart
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
import com.itsabugnotafeature.fitocrazy.common.SetRecordView
import com.itsabugnotafeature.fitocrazy.common.Workout
import com.itsabugnotafeature.fitocrazy.workout.addExercise.AddNewExerciseToWorkoutFragment
import kotlinx.coroutines.runBlocking
import java.math.RoundingMode
import java.text.DecimalFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale


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
 *      - DONE ~~refactor ExerciseList to use ExerciseView instead of Pair~~
 *      - DONE ~~notification panel~~
 *      - DONE ~~make the "add exercises" text visible again~~
 *      - DONE ~~don't round corners on list views~~
 *      - DONE ~~points should be wholistic across all sets in an exercise~~
 *      - DONE ~~historical sets should be eager-loaded when exercise added to workout (and not in bind viewholder)~~
 *      - DONE ~~number of sets in notifcation should be the number at the current weight~~
 *      - DONE ~~move calculation of points from companion object to on workout itself as general update~~
 *      - DOME ~~go back to adding from the top - the new exercise button becomes confusing otherwise~~
 *      - DONE ~~BUG: when too many body-part chips are assigned to an exercise it causes the remove-set button to hide
 *                 ~~- max 3 chips selectable?~~
 *      - DONE ~~exercise PRs in the exercise card in workout?~~
 *      - DONE ~~highlight good points totals~~
 *      - DONE ~~BUG: points dont make sense when adding and removing - could be related to bonus? in fact all stats are not loaded correctly~~
 *      - DONE ~~BUG records are not updated when set is removed?~~ <- its a view? shouldnt be possible
 *      - DONE ~~the workout overview should format date as "today" when appropriate~~
 *      - DONE ~~toast/floating up emoji on adding points~~
 *
 * TODO
 *      - handle background running and all on-resume stuff
 *      - allow editing of exercise components
 *      - full navigation over haul
 *      - add graphs for exercise history
 *      - "enter" when creating a new exercise component does weird stuff (should trim+enter)
 *      - profile statistics
 *      - reset DB
 *      - better logo and splash screen
 *      - fix icon (lighter background, missing shine on second stickout on F)
 *          add a dumbbell in bottom right of logo
 *      - better font
 *      - fix points on loading old workouts (points are read from DB instead of calculating
 *      - the weight + reps enters should have more strict validation provided by android itself?
 *      - better icons for achievements
 *      - total points per exercise in chip next to exercise name (floating popups enough?)
 *      - icon to indicate time can be paused
 *      - number of sets at current weight in notification should be displayed better (closer to the weight?)
 *      - dont show achievements on first instance of that exercise ever (no history)
 *              should achievements be per-row? or at least on the set card itself?
*       - records should live with their exercises forever
 *      - BUG adding a new set when teh timer is paused does not set the color correctly
 *      - dont show toast if activity is in the background
 *
 * TODO notification
 *     - DONE ~~update when exercise added, set added, set removed~~
 *     - DONE ~~only show when workout is today!~~
 *     - DONE ~~only show on workout activity~~
 *     - DONE ~~pendingIntent on button press to trigger add set~~
 *     - DONE ~~pendingIntent to reopen app~~
 *     - DONE ~~only dismiss notification on workout saved, or back button (not home)~~
 *     - DONE ~~ enhance set data with the number of sets at this weight~~
 *     - BUG: null pointer on notification trying to set exercise?
 *     - better text for the action
 *
 * TODO from actual workout
 *      - notification action doesnt work if app backgrounded for a long time (either no workout added, or duplicate empty workouts created)
 *      - need to be able to delete components (wants to put "seated" first on "seated dumbbell arnold press"
 *      - add average weight per set ajd rep in the workout overview page
 *      - basic weight tracking
 *      - changes for suspend/resume already identified
 *
 * TODO - never
 *      ? tint of chips should be per bodypart - right now its ordered by most frequent
 *      ? bring points to the spinners on exercise type for more flexibility?
 */


class WorkoutActivity : AppCompatActivity() {

    private lateinit var db: ExerciseDatabase
    private lateinit var workout: Workout
    private var lastToast: Toast? = null

    private var setTimerIsActive = false
    lateinit var exerciseListViewAdapter: ExerciseListViewAdapter
    lateinit var exerciseList: MutableList<ExerciseView>

    interface ExerciseNotification {
        fun setAdded(exercise: ExerciseView, set: Set): List<ExerciseRecord>
        fun setRemoved(exercise: ExerciseView, set: Set): List<ExerciseRecord>
        fun exerciseDeleted()
    }

    data class ExerciseView(
        val displayName: String?,
        val tags: List<String>,
        val exercise: Exercise,
        val sets: MutableList<Set>,
        val record: SetRecordView?,
        val historicalSets: List<Pair<LocalDate, List<Set>>>,
        val basePoints: Int,
    )


    // the parent list of exercises in a workout
    class ExerciseListViewAdapter(
        private var exerciseList: MutableList<ExerciseView>,
        private var db: ExerciseDatabase,
        private var parent: RecyclerView,
        private var notifier: ExerciseNotification
    ) : RecyclerView.Adapter<ExerciseListViewAdapter.ViewHolder>() {
        inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            fun bind(
                currentExercise: ExerciseView,
            ) {
                val weightEditText = itemView.findViewById<EditText>(R.id.numberEntry_addKilogramsToThisExercise)
                val lastSet = currentExercise.sets.lastOrNull()
                if (lastSet == null) {
                    weightEditText.text = null
                } else {
                    weightEditText.setText(formatDoubleWeight(lastSet.weight))
                }
                weightEditText.setOnFocusChangeListener { _, hasFocus ->
                    // clear text on focus
                    if (hasFocus) weightEditText.text = null
                    else if (weightEditText.text.toString().isNotEmpty()) {
                        // when losing focus, format text
                        weightEditText.setText(formatDoubleWeight(weightEditText.text.toString().toDouble()))
                    }
                }

                val repsEditText = itemView.findViewById<EditText>(R.id.numberEntry_addRepsToThisExercise)
                repsEditText.setText(currentExercise.sets.lastOrNull()?.reps?.toString())
                repsEditText.setOnFocusChangeListener { _, hasFocus ->
                    if (hasFocus) repsEditText.text = null
                    else if (repsEditText.text.toString().isNotEmpty()) {
                        repsEditText.setText(repsEditText.text.toString().toInt().toString())
                    }
                }

                val exerciseNameOnCard = itemView.findViewById<TextView>(R.id.label_exerciseNameOnCard)
                exerciseNameOnCard.text = currentExercise.displayName
                if (currentExercise.tags.isNotEmpty()) {
                    val chipGroup = itemView.findViewById<ChipGroup>(R.id.chipGroup_exerciseTags)
                    chipGroup.removeAllViews()
                    chipGroup.visibility = ChipGroup.VISIBLE
                    currentExercise.tags.forEach { chipName ->
                        val newChip = Chip(itemView.context)
                        newChip.text = chipName
                        newChip.setChipBackgroundColorResource(R.color.blue_accent_light)
                        chipGroup.addView(newChip)
                    }
                }

                val exerciseSetsScrollLayout = itemView.findViewById<LinearLayout>(R.id.layout_listOfSetsOnExerciseCard)
                exerciseSetsScrollLayout.removeAllViews()

                // TODO

                currentExercise.historicalSets.forEach { (workoutDate, setList) ->
                    val setListView = LayoutInflater.from(parent.context).inflate(
                        R.layout.workout_exercise_set_list_horizontal_container, parent, false
                    )
                    setListView.findViewById<TextView>(R.id.label_setDate).text = workoutDate.format(dateFormatter)

                    val setWeightString = StringBuilder()
                    val setRepsString = StringBuilder()
                    for (set in setList) {
                        setWeightString.appendLine("${formatDoubleWeight(set.weight)} x")
                        setRepsString.appendLine(set.reps)
                    }

                    setListView.findViewById<TextView>(R.id.textlist_weightInSet).text = setWeightString
                    setListView.findViewById<TextView>(R.id.textlist_repsInSet).text = setRepsString
                    exerciseSetsScrollLayout.addView(setListView)
                }

                val setListView = LayoutInflater.from(parent.context).inflate(
                    R.layout.workout_exercise_set_list_horizontal_container_today, parent, false
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
                    setWeightString.appendLine("${formatDoubleWeight(set.weight)} x")
                    setRepsString.appendLine(set.reps)
                }
                setWeightString.append("KG *")
                setListView.findViewById<TextView>(R.id.textlist_weightInSet).gravity = Gravity.END
                setListView.findViewById<TextView>(R.id.textlist_weightInSet).text = setWeightString
                setListView.findViewById<TextView>(R.id.textlist_repsInSet).text = setRepsString

                setListView.minimumWidth = (parent.width * 0.3).toInt()
                exerciseSetsScrollLayout.addView(setListView)

                val scrollView = itemView.findViewById<HorizontalScrollView>(R.id.scrollview_listOfSetsOnExerciseCard)
                scrollView.post { scrollView.scrollX = setListView.left }

                itemView.findViewById<Button>(R.id.btn_removeLastSetFromThisExercise).setOnClickListener {
                    val imm = parent.context.getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
                    imm.hideSoftInputFromWindow(parent.windowToken, 0)

                    if (currentExercise.sets.isEmpty()) {
                        runBlocking {
                            db.exerciseDao().deleteExercise(currentExercise.exercise)
                        }
                        exerciseList.removeAt(adapterPosition)
                        notifier.exerciseDeleted()
                        notifyItemRemoved(adapterPosition)
                    } else {
                        val removedSet = currentExercise.sets.removeLast()
                        runBlocking {
                            db.exerciseDao().deleteSetFromExercise(removedSet)
                        }

                        itemView.findViewById<ImageView>(R.id.img_achievementMostWeight).visibility = ImageView.GONE
                        itemView.findViewById<ImageView>(R.id.img_achievementMostReps).visibility = ImageView.GONE
                        itemView.findViewById<ImageView>(R.id.img_achievementMostMoved).visibility = ImageView.GONE
                        exerciseNameOnCard.setPadding(0, 0, 0, 0)

                        val records = notifier.setRemoved(currentExercise, removedSet)
                        records.forEach {
                            when (it.recordType) {
                                RecordType.MAX_WEIGHT -> itemView.findViewById<ImageView>(R.id.img_achievementMostWeight).visibility = ImageView.VISIBLE
                                RecordType.MAX_REPS -> itemView.findViewById<ImageView>(R.id.img_achievementMostReps).visibility = ImageView.VISIBLE
                                RecordType.MAX_WEIGHT_MOVED -> itemView.findViewById<ImageView>(R.id.img_achievementMostMoved).visibility = ImageView.VISIBLE
                            }
                        }
                        if (records.isNotEmpty()) exerciseNameOnCard.setPadding(12, 0, 0, 0)

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
                        0, currentExercise.exercise.exerciseId, weight, reps, currentExercise.sets.size
                    )
                    runBlocking { set.setID = db.exerciseDao().addSetToExercise(set) }
                    currentExercise.sets.add(set)

                    val records = notifier.setAdded(currentExercise, set)
                    records.forEach {
                        when (it.recordType) {
                            RecordType.MAX_WEIGHT -> itemView.findViewById<ImageView>(R.id.img_achievementMostWeight).visibility = ImageView.VISIBLE
                            RecordType.MAX_REPS -> itemView.findViewById<ImageView>(R.id.img_achievementMostReps).visibility = ImageView.VISIBLE
                            RecordType.MAX_WEIGHT_MOVED -> itemView.findViewById<ImageView>(R.id.img_achievementMostMoved).visibility = ImageView.VISIBLE
                        }
                    }
                    if (records.isNotEmpty()) exerciseNameOnCard.setPadding(12, 0, 0, 0)

                    notifyItemChanged(adapterPosition)
                }
            }
        }

        override fun getItemCount() = exerciseList.size

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view: View = LayoutInflater.from(parent.context).inflate(R.layout.workout_exercise_row, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            if (exerciseList.isEmpty()) return
            val currentExercise = exerciseList[position]
            holder.bind(currentExercise)

            /*runBlocking {
                val exerciseModel = db.exerciseDao().getExerciseDetails(currentExercise.exercise.exerciseModelId)!!
                val displayName = exerciseModel.exercise.displayName
                val chips = exerciseModel.exercise.getChips()
                holder.bind(
                    ExerciseView(
                        displayName = displayName,
                        tags = chips,
                        exercise = currentExercise.exercise,
                        sets = currentExercise.sets,
                        record = currentExercise.record,
                        historicalSets = currentExercise.historicalSets
                    ),
                )
            }*/
        }

    }

    override fun onResume() {
        super.onResume()/*Log.i("TEST", "onREsume")
        exerciseListViewAdapter.notifyItemChanged(0)*/
        // update the last exercise in teh list in case it got updated by a notification
        runBlocking {
            // TODO: update everything
            /*if (exerciseList.isNotEmpty()) {
                exerciseList[exerciseList.size - 1] = ExerciseView(
                    exerciseList.last().displayName,
                    exerciseList.last().exercise,
                    db.exerciseDao().getSets(exerciseList.last().exercise.exerciseId).toMutableList(),
                    exerciseList.last().tags,
                )
            }
            exerciseListViewAdapter.notifyItemChanged(exerciseList.size - 1)*/
        }
    }

    override fun onPause() {
        super.onPause()
        val totalTimeTimer = findViewById<Chronometer>(R.id.timer_totalTime)
        val setTimeTimer = findViewById<Chronometer>(R.id.timer_timeAfterLastSet)

        workout.totalExercises = exerciseList.size
        if (LocalDate.now() == workout.date) workout.totalTime = SystemClock.elapsedRealtime() - totalTimeTimer.base
        runBlocking {
            workout.topTags = exerciseList.fold(emptyList<String>()) { ongoing, item ->
                ongoing + db.exerciseDao().getExerciseDetails(item.exercise.exerciseModelId)!!.exercise.getChips()
            }.groupingBy { it }.eachCount().toSortedMap().asIterable().reversed().take(3).joinToString(" ") { it.key }
                .trim()

            db.exerciseDao().updateWorkout(workout)
        }
    }

    fun showNotification(
        parentActivityIntent: Intent?,
        totalExercises: Int,
        date: LocalDate,
        chronometerBase: Long,
        chronometerRunning: Boolean,
        exerciseId: Long?,
        displayName: String?,
        set: Set?,
        numPrevSets: Int?,
        totalSets: Int?,
    ) {
        if (date != LocalDate.now()) return

        val title = "${date.format(DateTimeFormatter.ofPattern("dd LLLL yyyy"))} [$totalExercises]"

        val notificationLayout = RemoteViews("com.itsabugnotafeature.fitocrazy", R.layout.notification_small)
        notificationLayout.setTextViewText(R.id.label_notificationTitle, title)
        notificationLayout.setChronometer(R.id.timer_notificationSetTime, chronometerBase, null, chronometerRunning)

        val notificationLayoutExpanded = RemoteViews("com.itsabugnotafeature.fitocrazy", R.layout.notification_large)
        notificationLayoutExpanded.setTextViewText(R.id.label_notificationTitle, title)
        notificationLayoutExpanded.setChronometer(/* viewId = */ R.id.timer_notificationSetTime,/* base = */
            chronometerBase,/* format = */
            null,/* started = */
            chronometerRunning
        )
        notificationLayoutExpanded.setTextViewText(
            R.id.label_notificationContent,
            if (numPrevSets != null) "[$numPrevSets/$totalSets] $displayName" else (displayName ?: "")
        )
        notificationLayoutExpanded.setTextViewText(
            R.id.label_notificationLastSet, if (set != null) "${formatDoubleWeight(set.weight)}kg x ${set.reps}" else ""
        )

        val addAnotherSetIntent = Intent().apply {
            action = NOTIFICATION_ACTION_COMPLETE_SET
            putExtra("exerciseId", exerciseId)
        }

        val addAnotherSetPendingIntent = PendingIntent.getBroadcast(
            applicationContext,
            0,
            Intent(addAnotherSetIntent),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val contentIntent: PendingIntent =
            PendingIntent.getActivity(applicationContext, 0, parentActivityIntent, PendingIntent.FLAG_IMMUTABLE)

        val customNotification = NotificationCompat.Builder(applicationContext, CHANNEL_ID).setContentTitle(title)
            .setSmallIcon(R.drawable.fitocrazy_logo) // TODO get this to work
            .setStyle(NotificationCompat.DecoratedCustomViewStyle()).setCustomContentView(notificationLayout)
            .setCustomBigContentView(notificationLayoutExpanded).setContentIntent(contentIntent)
            .setCategory(NotificationCompat.CATEGORY_STATUS).setVisibility(NotificationCompat.VISIBILITY_PUBLIC)

        if (set != null) {
            customNotification.addAction(
                android.R.drawable.ic_menu_add, NOTIFICATION_ACTION_COMPLETE_SET, addAnotherSetPendingIntent
            )
        }

        val notificationManager =
            applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, customNotification.build())
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_workout)

        db = ExerciseDatabase.getInstance(applicationContext)

        val today = LocalDate.now()
        var exercises: List<Exercise>
        runBlocking {
            workout = db.exerciseDao().getWorkout(intent.getLongExtra("workoutId", -1)) ?: Workout(0, today)
            if (workout.workoutId == 0L) {
                workout.workoutId = db.exerciseDao().addWorkout(workout)
            }

            exercises = db.exerciseDao().getListOfExerciseInWorkout(workout.workoutId)
            exerciseList = exercises.map { exercise ->
                val exerciseModel = db.exerciseDao().getExerciseDetails(exercise.exerciseModelId)!!
                ExerciseView(
                    displayName = exerciseModel.exercise.displayName,
                    tags = exerciseModel.exercise.getChips(),
                    exercise = exercise,
                    sets = db.exerciseDao().getSets(exercise.exerciseId).toMutableList(),
                    record = db.exerciseDao().getRecord(exercise.exerciseModelId),
                    historicalSets = db.exerciseDao()
                        .getHistoricalSets(exercise.exerciseModelId, 3, exercise.toTimeStamp()).toSortedMap()
                        .map { Pair(it.key.date, it.value) },
                    basePoints = exerciseModel.exercise.basePoints,
                )
            }.toMutableList()

            val latestExercise = exerciseList.lastOrNull()
            val exerciseModel = db.exerciseDao().getExerciseDetails(latestExercise?.exercise?.exerciseModelId ?: -1)

            showNotification(parentActivityIntent = parentActivityIntent,
                totalExercises = workout.totalExercises,
                date = workout.date,
                chronometerBase = SystemClock.elapsedRealtime(),
                chronometerRunning = false,
                exerciseId = latestExercise?.exercise?.exerciseId,
                displayName = exerciseModel?.exercise?.displayName,
                set = latestExercise?.sets?.lastOrNull(),
                numPrevSets = latestExercise?.sets?.takeLastWhile { it.weight == latestExercise.sets.last().weight }
                    ?.count(),
                totalSets = latestExercise?.sets?.size)
        }
        title = title.toString() + " " + workout.date

        val totalWeightLabel = findViewById<TextView>(R.id.totalWeightValue)
        val totalRepsLabel = findViewById<TextView>(R.id.totalRepsValue)
        val totalPointsLabel = findViewById<TextView>(R.id.totalPointsValue)

        totalWeightLabel.text = workout.totalWeight.toString()
        totalRepsLabel.text = workout.totalReps.toString()
        totalPointsLabel.text = workout.totalPoints.toString()

        val labelForEmptyExerciseList = findViewById<TextView>(R.id.label_startWorkoutAddExercise)
        val totalTimeTimer = findViewById<Chronometer>(R.id.timer_totalTime)
        val setTimeTimer = findViewById<Chronometer>(R.id.timer_timeAfterLastSet)

        class Notifier : ExerciseNotification {
            override fun setAdded(exercise: ExerciseView, set: Set): List<ExerciseRecord> {
                val oldTotal = workout.totalPoints
                workout.recalculateWorkoutTotals(exerciseList)
                lastToast?.cancel()
                lastToast = Toast.makeText(applicationContext, "${workout.totalPoints - oldTotal} ★ pts", Toast.LENGTH_SHORT)
                lastToast?.show()

                runBlocking {
                    db.exerciseDao().updateWorkout(workout)
                }

                totalWeightLabel.text = workout.totalWeight.toString()
                totalRepsLabel.text = workout.totalReps.toString()
                totalPointsLabel.text = workout.totalPoints.toString()

                if (today == workout.date) {
                    // only do set timer if we're currently working out
                    setTimeTimer.base = SystemClock.elapsedRealtime()
                    setTimeTimer.start()
                    setTimerIsActive = true
                }

                showNotification(
                    parentActivityIntent = parentActivityIntent,
                    totalExercises = workout.totalExercises,
                    date = workout.date,
                    chronometerBase = SystemClock.elapsedRealtime(),
                    chronometerRunning = (today == workout.date),
                    exerciseId = exercise.exercise.exerciseId,
                    displayName = exercise.displayName,
                    set = exercise.sets.lastOrNull(),
                    numPrevSets = exercise.sets.takeLastWhile { it.weight == exercise.sets.last().weight }.count(),
                    totalSets = exercise.sets.size
                )

                return workout.calculatePoints(exercise).records
            }

            override fun setRemoved(exercise: ExerciseView, set: Set): List<ExerciseRecord>  {
                workout.recalculateWorkoutTotals(exerciseList)
                runBlocking {
                    db.exerciseDao().updateWorkout(workout)
                }

                totalWeightLabel.text = workout.totalWeight.toString()
                totalRepsLabel.text = workout.totalReps.toString()
                totalPointsLabel.text = workout.totalPoints.toString()

                showNotification(
                    parentActivityIntent = parentActivityIntent,
                    totalExercises = workout.totalExercises,
                    date = workout.date,
                    chronometerBase = SystemClock.elapsedRealtime(),
                    chronometerRunning = false,
                    exerciseId = null,
                    displayName = exercise.displayName,
                    set = null,
                    numPrevSets = null,
                    totalSets = null
                )

                return workout.calculatePoints(exercise).records
            }

            override fun exerciseDeleted() {
                val exerciseModel = runBlocking {
                    db.exerciseDao().getExerciseDetails(exerciseList.lastOrNull()?.exercise?.exerciseModelId ?: -1)
                }
                if (exerciseList.isEmpty()) labelForEmptyExerciseList.visibility = TextView.VISIBLE

                showNotification(
                    parentActivityIntent = parentActivityIntent,
                    totalExercises = workout.totalExercises,
                    date = workout.date,
                    chronometerBase = SystemClock.elapsedRealtime(),
                    chronometerRunning = false,
                    exerciseId = null,
                    displayName = exerciseModel?.exercise?.displayName,
                    set = null,
                    numPrevSets = null,
                    totalSets = null,
                )
            }
        }

        val notifier = Notifier()

        class SetAddedBroadcastReceiver : BroadcastReceiver() {
            override fun onReceive(context: Context?, incomingIntent: Intent?) {
                val exerciseId = incomingIntent?.getLongExtra("exerciseId", -1L) ?: -1L
                if (exerciseId == -1L) return

                //goAsync() - might be needed?
                runBlocking {
                    val db = ExerciseDatabase.getInstance(applicationContext)
                    val sets = db.exerciseDao().getSets(exerciseId)

                    val set = Set(
                        0, exerciseId, sets.last().weight, sets.last().reps, sets.size
                    )

                    set.setID = db.exerciseDao().addSetToExercise(set)
                    //exerciseList.last().second.add(set)
                    val exercise = exerciseList.find { it.exercise.exerciseId == exerciseId }!!
                    exercise.sets.add(set)

                    notifier.setAdded(exercise, set)
                    exerciseListViewAdapter.notifyItemChanged(exerciseList.indexOf(exercise))
                }
            }
        }

        val broadcastReceiver = SetAddedBroadcastReceiver()
        val filter = IntentFilter(NOTIFICATION_ACTION_COMPLETE_SET)
        ContextCompat.registerReceiver(this, broadcastReceiver, filter, ContextCompat.RECEIVER_EXPORTED)

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

        val exerciseListView = findViewById<RecyclerView>(R.id.list_exercisesInCurrentWorkout)
        exerciseListViewAdapter = ExerciseListViewAdapter(exerciseList, db, exerciseListView, notifier)
        exerciseListView.itemAnimator = null
        val exerciseListViewLayoutManager = LinearLayoutManager(this, LinearLayoutManager.VERTICAL, true)
        exerciseListViewLayoutManager.stackFromEnd = true
        exerciseListView.layoutManager = exerciseListViewLayoutManager
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
                    ongoing + db.exerciseDao().getExerciseDetails(item.exercise.exerciseModelId)!!.exercise.getChips()
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
                val record: SetRecordView?
                val historicalSets: List<Pair<LocalDate, List<Set>>>
                runBlocking {
                    exercise.exerciseId = db.exerciseDao().addExerciseSet(exercise)
                    exerciseModel = db.exerciseDao().getExerciseDetails(exercise.exerciseModelId)
                    record = db.exerciseDao().getRecord(exercise.exerciseModelId)
                    historicalSets =
                        db.exerciseDao().getHistoricalSets(exercise.exerciseModelId, 3, exercise.toTimeStamp())
                            .toSortedMap().toList().map { Pair(it.first.date, it.second) }
                }
                exerciseList.add(
                    ExerciseView(
                        displayName = exerciseModel?.exercise?.displayName,
                        tags = exerciseModel?.exercise?.getChips() ?: emptyList(),
                        exercise = exercise,
                        sets = mutableListOf(),
                        record = record,
                        historicalSets = historicalSets,
                        basePoints = exerciseModel?.exercise?.basePoints ?: 10,
                    )
                )
                exerciseListViewAdapter.notifyItemInserted(exerciseList.size - 1)
                exerciseListView.smoothScrollToPosition(exerciseList.size - 1)
                exerciseListView.visibility = RecyclerView.VISIBLE
                labelForEmptyExerciseList.visibility = TextView.GONE

                showNotification(
                    parentActivityIntent = parentActivityIntent,
                    totalExercises = exerciseList.size,
                    date = workout.date,
                    chronometerBase = SystemClock.elapsedRealtime(),
                    chronometerRunning = false,
                    exerciseId = exercise.exerciseId,
                    displayName = exerciseModel?.exercise?.displayName,
                    set = null,
                    numPrevSets = null,
                    totalSets = null,
                )
            }
        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.layout_currentWorkout)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    companion object {
        const val NOTIFICATION_ID = 123456
        private const val NOTIFICATION_ACTION_COMPLETE_SET = "same again"
        private const val CHANNEL_ID = "FitoCrazyCurrentExerciseChannel"

        private val decimalFormatter = DecimalFormat("###.##")
        private val dateFormatter = DateTimeFormatter.ofPattern("dd LLL yy")

        private fun formatDoubleWeight(weight: Double): String {
            return if (weight % 1 == 0.0) {
                // there is nothing behind the .
                weight.toInt().toString()
            } else {
                // prevent rounding and just drop the extra behind 2 decimal places
                String.format(Locale.US, "%.2f", (weight * 100).toInt() / 100.0)
            }
        }

        enum class RecordType { MAX_WEIGHT, MAX_REPS, MAX_WEIGHT_MOVED }
        data class ExerciseRecord(val oldBest: Number, val newBest: Number, val recordType: RecordType)
        data class PointsResult(val points: Int, val records: List<ExerciseRecord>)

        init {
            decimalFormatter.roundingMode = RoundingMode.FLOOR
        }

    }
}