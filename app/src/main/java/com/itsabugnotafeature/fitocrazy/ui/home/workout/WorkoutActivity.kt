package com.itsabugnotafeature.fitocrazy.ui.home.workout

import android.app.Activity
import android.app.DatePickerDialog
import android.app.Dialog
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.ProgressDialog
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
import android.widget.DatePicker
import android.widget.EditText
import android.widget.HorizontalScrollView
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RemoteViews
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.Lifecycle
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.itsabugnotafeature.fitocrazy.R
import com.itsabugnotafeature.fitocrazy.common.Converters
import com.itsabugnotafeature.fitocrazy.common.Exercise
import com.itsabugnotafeature.fitocrazy.common.ExerciseDatabase
import com.itsabugnotafeature.fitocrazy.common.ExerciseWithComponentModel
import com.itsabugnotafeature.fitocrazy.common.Set
import com.itsabugnotafeature.fitocrazy.common.SetRecordView
import com.itsabugnotafeature.fitocrazy.common.Workout
import com.itsabugnotafeature.fitocrazy.ui.home.workout.addExercise.AddNewExerciseToWorkoutActivity
import kotlinx.coroutines.runBlocking
import org.w3c.dom.Text
import java.math.RoundingMode
import java.text.DecimalFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Calendar
import java.util.Locale


class WorkoutActivity : AppCompatActivity() {

    private lateinit var workout: Workout
    private var lastToast: Toast? = null

    private var setTimerIsActive = false

    private lateinit var exerciseList: MutableList<ExerciseView>
    private lateinit var exerciseListViewAdapter: ExerciseListViewAdapter

    private var broadcastReceiver: BroadcastReceiver? = null

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
        var exerciseList: MutableList<ExerciseView>,
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
                    setListView.findViewById<TextView>(R.id.label_setDate).text =
                        workoutDate.format(Converters.dateFormatter)

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
                                RecordType.MAX_WEIGHT -> itemView.findViewById<ImageView>(R.id.img_achievementMostWeight).visibility =
                                    ImageView.VISIBLE

                                RecordType.MAX_REPS -> itemView.findViewById<ImageView>(R.id.img_achievementMostReps).visibility =
                                    ImageView.VISIBLE

                                RecordType.MAX_WEIGHT_MOVED -> itemView.findViewById<ImageView>(R.id.img_achievementMostMoved).visibility =
                                    ImageView.VISIBLE
                            }
                        }
                        if (records.isNotEmpty()) exerciseNameOnCard.setPadding(12, 0, 0, 0)

                        notifyItemChanged(adapterPosition)
                    }
                }

                itemView.findViewById<Button>(R.id.btn_addSetToThisExercise).setOnClickListener {
                    val imm = parent.context.getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
                    imm.hideSoftInputFromWindow(parent.windowToken, 0)
                    itemView.clearFocus()

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
                            RecordType.MAX_WEIGHT -> itemView.findViewById<ImageView>(R.id.img_achievementMostWeight).visibility =
                                ImageView.VISIBLE

                            RecordType.MAX_REPS -> itemView.findViewById<ImageView>(R.id.img_achievementMostReps).visibility =
                                ImageView.VISIBLE

                            RecordType.MAX_WEIGHT_MOVED -> itemView.findViewById<ImageView>(R.id.img_achievementMostMoved).visibility =
                                ImageView.VISIBLE
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
        }

    }


    override fun onPause() {
        super.onPause()
        val totalTimeTimer = findViewById<Chronometer>(R.id.timer_totalTime)
        val setTimeTimer = findViewById<Chronometer>(R.id.timer_timeAfterLastSet)

        if (LocalDate.now() == workout.date) {
            workout.totalTime = SystemClock.elapsedRealtime() - totalTimeTimer.base
            workout.currentSetTime = SystemClock.elapsedRealtime() - setTimeTimer.base
        }

        val db = ExerciseDatabase.getInstance(this)
        runBlocking {
            db.exerciseDao().updateWorkout(workout)
        }

        if (broadcastReceiver != null) {
            LocalBroadcastManager.getInstance(this).unregisterReceiver(broadcastReceiver!!)
            //ContextCompat.registerReceiver(this, broadcastReceiver, filter, ContextCompat.RECEIVER_EXPORTED)
        }

    }

    fun showNotification(
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
            this,
            0,
            Intent(addAnotherSetIntent),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val contentIntent: PendingIntent =
            PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE)

        val customNotification = NotificationCompat.Builder(this, CHANNEL_ID).setContentTitle(title)
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
            this.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, customNotification.build())
    }

    override fun onResume() {
        super.onResume()

        LocalBroadcastManager.getInstance(this)
            .registerReceiver(broadcastReceiver!!, IntentFilter(NOTIFICATION_ACTION_COMPLETE_SET))

        val db = ExerciseDatabase.getInstance(this).exerciseDao()
        val latestExerciseModel = runBlocking {
            exerciseList = db.getListOfExerciseInWorkout(workout.workoutId).map { exercise ->
                val thisExerciseModel = db.getExerciseDetails(exercise.exerciseModelId)!!
                ExerciseView(
                    displayName = thisExerciseModel.exercise.displayName,
                    tags = thisExerciseModel.exercise.getChips(),
                    exercise = exercise,
                    sets = db.getSets(exercise.exerciseId).toMutableList(),
                    record = db.getRecord(exercise.exerciseModelId),
                    historicalSets = db
                        .getHistoricalSets(exercise.exerciseModelId, 3, exercise.toTimeStamp()).toSortedMap()
                        .map { Pair(it.key.date, it.value) },
                    basePoints = thisExerciseModel.exercise.basePoints,
                )
            }.toMutableList()

            db.getExerciseDetails(exerciseList.lastOrNull()?.exercise?.exerciseModelId ?: -1)
        }
        exerciseListViewAdapter.exerciseList = exerciseList

        showNotification(
            totalExercises = workout.totalExercises,
            date = workout.date,
            chronometerBase = SystemClock.elapsedRealtime(),
            chronometerRunning = false,
            exerciseId = exerciseList.lastOrNull()?.exercise?.exerciseId,
            displayName = latestExerciseModel?.exercise?.displayName,
            set = exerciseList.lastOrNull()?.sets?.lastOrNull(),
            numPrevSets = exerciseList.lastOrNull()?.sets?.takeLastWhile { it.weight == (exerciseList.lastOrNull()?.sets?.last()?.weight) }
                ?.count(),
            totalSets = exerciseList.lastOrNull()?.sets?.size)

        val today = LocalDate.now()

        class DatePickerFragment : DialogFragment(), DatePickerDialog.OnDateSetListener {
            override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
                // Use the current date as the default date in the picker.
                val c = Calendar.getInstance()
                val year = c.get(Calendar.YEAR)
                val month = c.get(Calendar.MONTH)
                val day = c.get(Calendar.DAY_OF_MONTH)

                // Create a new instance of DatePickerDialog and return it.
                return DatePickerDialog(requireContext(), this, year, month, day)
            }

            override fun onDateSet(view: DatePicker, year: Int, month: Int, day: Int) {
                // Date picker 0-indexes the month REEE
                val userDate = LocalDate.of(year, month + 1, day)
                if (userDate == workout.date) return

                if (userDate > today) {
                    Toast.makeText(applicationContext, "We are not presently in the future", Toast.LENGTH_SHORT).show()
                    return
                }

                // update Exercise
                // update workout
                runBlocking {
                    // TODO run this on a non-UI thread
                    //val db = ExerciseDatabase.getInstance(applicationContext).exerciseDao()
                    workout.date = userDate
                    db.updateWorkout(workout)

                    for (exercise in exerciseList) {
                        exercise.exercise.date = userDate
                        db.updateExercise(exercise.exercise)
                    }
                }

                findViewById<TextView>(R.id.toolbar_title).text =
                    "Fitocrazy - " + (if (today == workout.date) getString(R.string.today) else workout.date)
            }
        }

        val toolbar = findViewById<Toolbar>(R.id.toolbar_workout)
        val editDateBtn = toolbar.findViewById<Button>(R.id.btn_editWorkoutDate)
        editDateBtn.setOnClickListener {
            val datePicker = DatePickerFragment()
            datePicker.show(supportFragmentManager, "datePicker")
        }
        editDateBtn.setBackgroundColor(getColor(R.color.purple_accent))

        toolbar.findViewById<TextView>(R.id.toolbar_title).text =
            "Fitocrazy - " + (if (today == workout.date) getString(R.string.today) else workout.date)

        setSupportActionBar(toolbar)


        val totalWeightLabel = findViewById<TextView>(R.id.totalWeightValue)
        val totalRepsLabel = findViewById<TextView>(R.id.totalRepsValue)
        val totalPointsLabel = findViewById<TextView>(R.id.totalPointsValue)

        totalWeightLabel.text = workout.totalWeight.toString()
        totalRepsLabel.text = workout.totalReps.toString()
        totalPointsLabel.text = workout.totalPoints.toString()

        val labelForEmptyExerciseList = findViewById<TextView>(R.id.label_startWorkoutAddExercise)
        val totalTimeTimer = findViewById<Chronometer>(R.id.timer_totalTime)

        totalTimeTimer.base = SystemClock.elapsedRealtime() - workout.totalTime
        if (workout.date == today) totalTimeTimer.start()

        val exerciseListView = findViewById<RecyclerView>(R.id.list_exercisesInCurrentWorkout)
        if (exerciseList.isEmpty()) {
            exerciseListView.visibility = RecyclerView.INVISIBLE
            labelForEmptyExerciseList.visibility = TextView.VISIBLE
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_workout)

        val today = LocalDate.now()

        val db = ExerciseDatabase.getInstance(this)
        runBlocking {
            workout = db.exerciseDao().getWorkout(intent.getLongExtra("workoutId", -1)) ?: Workout(0, today)
            if (workout.workoutId == 0L) {
                workout.workoutId = db.exerciseDao().addWorkout(workout)
            }

            exerciseList = db.exerciseDao().getListOfExerciseInWorkout(workout.workoutId).map { exercise ->
                val thisExerciseModel = db.exerciseDao().getExerciseDetails(exercise.exerciseModelId)!!
                ExerciseView(
                    displayName = thisExerciseModel.exercise.displayName,
                    tags = thisExerciseModel.exercise.getChips(),
                    exercise = exercise,
                    sets = db.exerciseDao().getSets(exercise.exerciseId).toMutableList(),
                    record = db.exerciseDao().getRecord(exercise.exerciseModelId),
                    historicalSets = db.exerciseDao()
                        .getHistoricalSets(exercise.exerciseModelId, 3, exercise.toTimeStamp()).toSortedMap()
                        .map { Pair(it.key.date, it.value) },
                    basePoints = thisExerciseModel.exercise.basePoints,
                )
            }.toMutableList()
        }

        val labelForEmptyExerciseList = findViewById<TextView>(R.id.label_startWorkoutAddExercise)
        val totalWeightLabel = findViewById<TextView>(R.id.totalWeightValue)
        val totalRepsLabel = findViewById<TextView>(R.id.totalRepsValue)
        val totalPointsLabel = findViewById<TextView>(R.id.totalPointsValue)
        val totalTimeTimer = findViewById<Chronometer>(R.id.timer_totalTime)
        val setTimeTimer = findViewById<Chronometer>(R.id.timer_timeAfterLastSet)

        class Notifier : ExerciseNotification {
            override fun setAdded(exercise: ExerciseView, set: Set): List<ExerciseRecord> {
                val oldTotal = workout.totalPoints
                workout.recalculateWorkoutTotals(exerciseList)
                if (lifecycle.currentState == Lifecycle.State.RESUMED) {
                    lastToast?.cancel()
                    lastToast = Toast.makeText(
                        applicationContext, "${workout.totalPoints - oldTotal} ★ pts", Toast.LENGTH_SHORT
                    )
                    lastToast?.show()
                }

                runBlocking {
                    db.exerciseDao().updateWorkout(workout)
                }

                totalWeightLabel.text = workout.totalWeight.toString()
                totalRepsLabel.text = workout.totalReps.toString()
                totalPointsLabel.text = workout.totalPoints.toString()

                if (today == workout.date) {
                    // only do set timer if we're currently working out
                    setTimeTimer.base = SystemClock.elapsedRealtime()
                    setTimeTimer.setTextColor(applicationContext.getColor(R.color.black))
                    setTimeTimer.start()
                    setTimerIsActive = true
                }

                showNotification(
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

            override fun setRemoved(exercise: ExerciseView, set: Set): List<ExerciseRecord> {
                workout.recalculateWorkoutTotals(exerciseList)
                runBlocking {
                    db.exerciseDao().updateWorkout(workout)
                }

                totalWeightLabel.text = workout.totalWeight.toString()
                totalRepsLabel.text = workout.totalReps.toString()
                totalPointsLabel.text = workout.totalPoints.toString()

                showNotification(
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
                workout.totalExercises = exerciseList.size

                val exerciseModel = runBlocking {
                    db.exerciseDao().updateWorkout(workout)
                    db.exerciseDao().getExerciseDetails(exerciseList.lastOrNull()?.exercise?.exerciseModelId ?: -1)
                }
                if (exerciseList.isEmpty()) labelForEmptyExerciseList.visibility = TextView.VISIBLE

                showNotification(
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
        val exerciseListView = findViewById<RecyclerView>(R.id.list_exercisesInCurrentWorkout)
        exerciseListViewAdapter = ExerciseListViewAdapter(exerciseList, db, exerciseListView, notifier)
        exerciseListView.itemAnimator = null
        val exerciseListViewLayoutManager = LinearLayoutManager(this, LinearLayoutManager.VERTICAL, true)
        exerciseListViewLayoutManager.stackFromEnd = true
        exerciseListView.layoutManager = exerciseListViewLayoutManager
        exerciseListView.adapter = exerciseListViewAdapter

        class SetAddedBroadcastReceiver : BroadcastReceiver() {
            override fun onReceive(context: Context?, incomingIntent: Intent?) {
                val exerciseId = incomingIntent?.getLongExtra("exerciseId", -1L) ?: -1L
                if (exerciseId == -1L) return

                runBlocking {
                    //val db = ExerciseDatabase.getInstance(this)
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

        broadcastReceiver = SetAddedBroadcastReceiver()
        val filter = IntentFilter(NOTIFICATION_ACTION_COMPLETE_SET)
        ContextCompat.registerReceiver(this, broadcastReceiver, filter, ContextCompat.RECEIVER_EXPORTED)

        var pausedTime = 0L
        setTimeTimer.setOnClickListener {
            if (setTimerIsActive) {
                setTimeTimer.stop()
                pausedTime = SystemClock.elapsedRealtime() - setTimeTimer.base
                setTimeTimer.setTextColor(this.getColor(R.color.orange_main))
            } else {
                setTimeTimer.base = SystemClock.elapsedRealtime() - pausedTime
                setTimeTimer.start()
                setTimeTimer.setTextColor(this.getColor(R.color.black))

            }
            setTimerIsActive = !setTimerIsActive
        }

        val endWorkoutBtn = findViewById<FloatingActionButton>(R.id.btn_endWorkout)
        endWorkoutBtn.setOnClickListener {
            workout.totalExercises = exerciseList.size
            if (today == workout.date) workout.totalTime = SystemClock.elapsedRealtime() - totalTimeTimer.base
            runBlocking {
                workout.topTags = exerciseList.fold(emptyList<String>()) { ongoing, item ->
                    ongoing + db.exerciseDao().getExerciseDetails(item.exercise.exerciseModelId)!!.exercise.getChips()
                }.groupingBy { it }.eachCount().asIterable().sortedBy { it.value }.reversed().take(3)
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

        val addExerciseResult =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result: ActivityResult ->
                if (result.resultCode == Activity.RESULT_OK) {
                    val exerciseModelIds = result.data?.extras?.getLongArray("exerciseIDs") ?: longArrayOf()
                    if (exerciseModelIds.isEmpty()) return@registerForActivityResult

                    for (exerciseModelId in exerciseModelIds.reversed()) {
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

                            workout.totalExercises = exerciseList.size
                            workout.topTags = exerciseList.fold(emptyList<String>()) { ongoing, item ->
                                ongoing + db.exerciseDao()
                                    .getExerciseDetails(item.exercise.exerciseModelId)!!.exercise.getChips()
                            }.groupingBy { it }.eachCount().asIterable().sortedBy { it.value }.reversed().take(3)
                                .joinToString(" ") { it.key }.trim()
                            db.exerciseDao().updateWorkout(workout)
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

                        showNotification(
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

                    exerciseListView.smoothScrollToPosition(exerciseList.size - 1)
                    exerciseListView.visibility = RecyclerView.VISIBLE
                    labelForEmptyExerciseList.visibility = TextView.GONE
                }
            }

        addNewExerciseButton.setOnClickListener {
            addExerciseResult.launch(
                Intent(
                    this, AddNewExerciseToWorkoutActivity::class.java
                ).setAction("addNewExerciseFromWorkout")
                    .putExtra("workoutId", workout.workoutId)
                    .putExtra(
                        "currentExercisesInWorkout",
                        exerciseList.map { it.exercise.exerciseModelId }.toLongArray()
                    )
            )
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
        private const val CHANNEL_ID = "FitocrazyCurrentExerciseChannel"

        private val decimalFormatter = DecimalFormat("###.##")

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