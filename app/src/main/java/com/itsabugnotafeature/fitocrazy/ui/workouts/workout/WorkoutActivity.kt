package com.itsabugnotafeature.fitocrazy.ui.workouts.workout

import android.annotation.SuppressLint
import android.app.Activity
import android.app.DatePickerDialog
import android.app.Dialog
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.os.SystemClock
import android.view.animation.AccelerateInterpolator
import android.widget.Chronometer
import android.widget.DatePicker
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.content.res.AppCompatResources
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.Lifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.itsabugnotafeature.fitocrazy.R
import com.itsabugnotafeature.fitocrazy.common.AddSetNotificationManager
import com.itsabugnotafeature.fitocrazy.common.Converters
import com.itsabugnotafeature.fitocrazy.common.Set
import com.itsabugnotafeature.fitocrazy.ui.workouts.workout.addExercise.AddNewExerciseToWorkoutActivity
import kotlinx.coroutines.runBlocking
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime

class WorkoutActivity : AppCompatActivity() {

    private var lastToast: Toast? = null
    private var setTimerIsActive = false

    private lateinit var exerciseListViewAdapter: ExerciseListViewAdapter
    private var broadcastReceiver: BroadcastReceiver? = null

    interface ExerciseNotification {
        fun setAdded(exercise: ExerciseListViewAdapter.ExerciseView, set: Set)
        fun setRemoved(exercise: ExerciseListViewAdapter.ExerciseView, set: Set)
        fun exerciseDeleted()
    }

    /*override fun onPause() {
        super.onPause()

        val totalTimeTimer = findViewById<Chronometer>(R.id.timer_totalTime)
        val setTimeTimer = findViewById<Chronometer>(R.id.timer_timeAfterLastSet)

        // TODO: why do we save the timers on pause? timers should still run until workout is officially saved!
        exerciseListViewAdapter.saveTimers(totalTimeTimer.base, setTimeTimer.base)
        runBlocking {
            exerciseListViewAdapter.saveWorkout(applicationContext)
        }
    }*/

    override fun onDestroy() {
        super.onDestroy()

        if (broadcastReceiver != null) {
            applicationContext.unregisterReceiver(broadcastReceiver)
        }

        runBlocking {
            exerciseListViewAdapter.saveWorkout(applicationContext)
        }
    }

    override fun onResume() {
        super.onResume()

        // TODO: refresh the exercise that was shown in the notification in case the user added more sets
        //exerciseListViewAdapter.showNotification()

        val totalWeightLabel = findViewById<TextView>(R.id.totalWeightValue)
        val totalRepsLabel = findViewById<TextView>(R.id.totalRepsValue)
        val totalPointsLabel = findViewById<TextView>(R.id.totalPointsValue)

        totalWeightLabel.text = exerciseListViewAdapter.workout.totalWeight.toString()
        totalRepsLabel.text = exerciseListViewAdapter.workout.totalReps.toString()
        totalPointsLabel.text = exerciseListViewAdapter.workout.totalPoints.toString()

        val labelForEmptyExerciseList = findViewById<TextView>(R.id.label_startWorkoutAddExercise)
        val totalTimeTimer = findViewById<Chronometer>(R.id.timer_totalTime)

        // TODO fix timer
        val workoutDate = Instant.ofEpochMilli(exerciseListViewAdapter.workout.date)
        // if its today, and we haven't clicked "save"
        if (workoutDate.atZone(ZoneId.systemDefault())
                .toLocalDate() == LocalDate.now() && exerciseListViewAdapter.workout.totalTime == 0L
        ) {
            totalTimeTimer.base =
                workoutDate.toEpochMilli() - System.currentTimeMillis() + SystemClock.elapsedRealtime()
            totalTimeTimer.start()
        } else {
            totalTimeTimer.base = SystemClock.elapsedRealtime() - exerciseListViewAdapter.workout.totalTime
        }
        val exerciseListView = findViewById<RecyclerView>(R.id.list_exercisesInCurrentWorkout)
        exerciseListView.adapter = exerciseListViewAdapter

        if (exerciseListViewAdapter.itemCount == 0) {
            exerciseListView.visibility = RecyclerView.INVISIBLE
            labelForEmptyExerciseList.visibility = TextView.VISIBLE
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()
        setContentView(R.layout.activity_workout)

        val today = LocalDate.now()

        class DatePickerFragment : DialogFragment(), DatePickerDialog.OnDateSetListener {
            override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
                // Use the current workout Date as the default date in the picker.
                // Create a new instance of DatePickerDialog and return it.
                val date = Instant.ofEpochMilli(exerciseListViewAdapter.workout.date).atZone(ZoneId.systemDefault())
                    .toLocalDate()
                return DatePickerDialog(
                    requireContext(),
                    this,
                    date.year,
                    date.monthValue - 1,
                    date.dayOfMonth
                )
            }

            @SuppressLint("SetTextI18n")
            override fun onDateSet(view: DatePicker, year: Int, month: Int, day: Int) {
                // Date picker 0-indexes the month REEE
                val userDate = LocalDate.of(year, month + 1, day)
                if (userDate ==
                    Instant.ofEpochMilli(exerciseListViewAdapter.workout.date)
                        .atZone(ZoneId.systemDefault())
                        .toLocalDate()
                ) {
                    return
                }

                if (userDate > today) {
                    Toast.makeText(applicationContext, "We are not presently in the future", Toast.LENGTH_SHORT).show()
                    return
                }

                exerciseListViewAdapter.workout.date = ZonedDateTime.of(
                    userDate,
                    Instant.ofEpochMilli(exerciseListViewAdapter.workout.date).atZone(ZoneId.systemDefault())
                        .toLocalTime(),
                    ZoneId.systemDefault()
                ).toEpochSecond() * 1000
                runBlocking {
                    exerciseListViewAdapter.saveWorkout(applicationContext)
                    exerciseListViewAdapter.updateExerciseDates(applicationContext, userDate)
                }

                findViewById<TextView>(R.id.toolbar_title).text =
                    "Fitocrazy - " + (if (today == Instant.ofEpochMilli(exerciseListViewAdapter.workout.date)
                            .atZone(ZoneId.systemDefault()).toLocalDate()
                    ) getString(R.string.today) else {
                        Converters.dateFormatter.format(
                            Instant.ofEpochMilli(exerciseListViewAdapter.workout.date).atZone(ZoneId.systemDefault())
                        )
                    })
            }
        }

        val labelForEmptyExerciseList = findViewById<TextView>(R.id.label_startWorkoutAddExercise)
        val totalWeightLabel = findViewById<TextView>(R.id.totalWeightValue)
        val totalRepsLabel = findViewById<TextView>(R.id.totalRepsValue)
        val totalPointsLabel = findViewById<TextView>(R.id.totalPointsValue)
        val totalTimeTimer = findViewById<Chronometer>(R.id.timer_totalTime)
        val setTimeTimer = findViewById<Chronometer>(R.id.timer_timeAfterLastSet)

        class SetAddedBroadcastReceiver : BroadcastReceiver() {
            override fun onReceive(context: Context?, incomingIntent: Intent?) {
                val exerciseId = incomingIntent?.getLongExtra("exerciseId", -1L) ?: -1L
                if (exerciseId == -1L) return

                runBlocking {
                    exerciseListViewAdapter.addSetSameAsLast(applicationContext, exerciseId)
                    /*if (lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
                        // TODO is this needed? was used to avoid notifying item changed
                    } else {
                        //showNotificationAgainWithSetInc(SystemClock.elapsedRealtime())
                    }*/
                }
            }
        }

        val exerciseListView = findViewById<RecyclerView>(R.id.list_exercisesInCurrentWorkout)

        class Notifier : ExerciseNotification {
            override fun setAdded(exercise: ExerciseListViewAdapter.ExerciseView, set: Set) {
                totalWeightLabel.text = exerciseListViewAdapter.workout.totalWeight.toString()
                totalRepsLabel.text = exerciseListViewAdapter.workout.totalReps.toString()
                totalPointsLabel.text = exerciseListViewAdapter.workout.totalPoints.toString()

                if (today == Instant.ofEpochMilli(exerciseListViewAdapter.workout.date).atZone(ZoneId.systemDefault())
                        .toLocalDate()
                ) {
                    // only do set timer if we're currently working out
                    setTimeTimer.base = SystemClock.elapsedRealtime()
                    setTimeTimer.setTextColor(applicationContext.getColor(R.color.black))
                    setTimeTimer.start()
                    //TODO setTimerIsActive = true
                }
            }

            override fun setRemoved(exercise: ExerciseListViewAdapter.ExerciseView, set: Set) {
                totalWeightLabel.text = exerciseListViewAdapter.workout.totalWeight.toString()
                totalRepsLabel.text = exerciseListViewAdapter.workout.totalReps.toString()
                totalPointsLabel.text = exerciseListViewAdapter.workout.totalPoints.toString()

                if (lifecycle.currentState == Lifecycle.State.RESUMED) {
                    lastToast?.cancel()
                    lastToast = Toast.makeText(
                        applicationContext, "Removed: ${set.weight} kg x ${set.reps}", Toast.LENGTH_SHORT
                    )
                    lastToast?.show()
                }
            }

            override fun exerciseDeleted() {
                if (exerciseListViewAdapter.itemCount == 0) {
                    exerciseListView.visibility = RecyclerView.INVISIBLE
                    labelForEmptyExerciseList.visibility = TextView.VISIBLE
                }
            }
        }

        exerciseListViewAdapter = ExerciseListViewAdapter(
            Notifier(),
            AddSetNotificationManager(applicationContext, intent)
        )
        runBlocking {
            exerciseListViewAdapter.loadData(
                applicationContext,
                mapOf("workoutId" to intent.getLongExtra("workoutId", -1))
            )
        }

        // exerciseListView.itemAnimator = null
        val exerciseListViewLayoutManager = LinearLayoutManager(this, LinearLayoutManager.VERTICAL, true)
        exerciseListViewLayoutManager.stackFromEnd = true
        exerciseListView.layoutManager = exerciseListViewLayoutManager
        exerciseListView.adapter = exerciseListViewAdapter
        exerciseListView.scrollToPosition(exerciseListViewAdapter.getNextReadyExerciseIdx())

        broadcastReceiver = SetAddedBroadcastReceiver()
        val filter = IntentFilter(AddSetNotificationManager.NOTIFICATION_ACTION_COMPLETE_SET)
        ContextCompat.registerReceiver(applicationContext, broadcastReceiver, filter, ContextCompat.RECEIVER_EXPORTED)

        var pausedTime = 0L
        setTimeTimer.setOnClickListener {
            if (setTimerIsActive) {
                setTimeTimer.stop()
                pausedTime = SystemClock.elapsedRealtime() - setTimeTimer.base
                setTimeTimer.setTextColor(this.getColor(R.color.blue_secondary))
                setTimeTimer.setCompoundDrawablesWithIntrinsicBounds(
                    AppCompatResources.getDrawable(
                        applicationContext,
                        android.R.drawable.ic_media_pause
                    ), null, null, null
                )
            } else {
                setTimeTimer.base = SystemClock.elapsedRealtime() - pausedTime
                setTimeTimer.start()
                setTimeTimer.setTextColor(this.getColor(R.color.black))
                setTimeTimer.setCompoundDrawablesWithIntrinsicBounds(
                    AppCompatResources.getDrawable(
                        applicationContext,
                        android.R.drawable.ic_media_play
                    ), null, null, null
                )

                // start the total timer again
                totalTimeTimer.start()
                exerciseListViewAdapter.workout.totalTime = 0L
                runBlocking {
                    exerciseListViewAdapter.saveWorkout(applicationContext)
                }
            }
            setTimerIsActive = !setTimerIsActive
            exerciseListViewAdapter.showNotificationAgain(setTimeTimer.base, setTimerIsActive)
        }

        val endWorkoutBtn = findViewById<FloatingActionButton>(R.id.btn_endWorkout)
        endWorkoutBtn.setOnClickListener {
            exerciseListViewAdapter.updateTimers(totalTimeTimer.base)
            runBlocking {
                exerciseListViewAdapter.saveWorkout(applicationContext)
            }

            intent.putExtra("dataUpdated", true)
            intent.putExtra("workoutId", exerciseListViewAdapter.workout.workoutId)
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

                    runBlocking {
                        exerciseListViewAdapter.addExercises(applicationContext, exerciseModelIds.reversed())
                    }
                    //exerciseListViewAdapter.showNotification()
                    exerciseListView.visibility = RecyclerView.VISIBLE
                    labelForEmptyExerciseList.visibility = TextView.GONE

                    // start the timer fresh
                    setTimeTimer.base = SystemClock.elapsedRealtime()
                    setTimeTimer.setTextColor(applicationContext.getColor(R.color.black))
                    setTimeTimer.stop() // stop it if it's running? might revisit later based on experience
                }
            }

        addNewExerciseButton.setOnClickListener {
            exerciseListViewAdapter.toggleReorderMode(cancel = true)
            addExerciseResult.launch(
                Intent(this, AddNewExerciseToWorkoutActivity::class.java)
                    .setAction("addNewExerciseFromWorkout")
                    .putExtra("workoutId", exerciseListViewAdapter.workout.workoutId)
                    .putExtra(
                        "currentExercisesInWorkout", exerciseListViewAdapter.currentExerciseIds().toLongArray()
                    )
            )
        }
        labelForEmptyExerciseList.setOnClickListener {
            addExerciseResult.launch(
                Intent(this, AddNewExerciseToWorkoutActivity::class.java)
                    .setAction("addNewExerciseFromWorkout")
                    .putExtra("workoutId", exerciseListViewAdapter.workout.workoutId)
            )
        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.layout_currentWorkout))
        { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val toolbar = findViewById<Toolbar>(R.id.toolbar_workout)
        val toolbarTitle = toolbar.findViewById<TextView>(R.id.toolbar_title)

        toolbarTitle.text =
            "Fitocrazy - " + (if (today == Instant.ofEpochMilli(exerciseListViewAdapter.workout.date)
                    .atZone(ZoneId.systemDefault())
                    .toLocalDate()
            ) getString(R.string.today) else Converters.dateFormatter.format(
                Instant.ofEpochMilli(exerciseListViewAdapter.workout.date).atZone(ZoneId.systemDefault())
            )) // TODO use short-form
        toolbarTitle.setOnClickListener { finish() }
        toolbarTitle.setOnLongClickListener {
            val datePicker = DatePickerFragment()
            datePicker.show(supportFragmentManager, "datePicker")
            true
        }

        val editWorkoutOrderBtn = toolbar.findViewById<MaterialButton>(R.id.btn_editWorkoutOrder)
        editWorkoutOrderBtn.setOnClickListener {
            if (exerciseListViewAdapter.toggleReorderMode()) {
                editWorkoutOrderBtn.setIconResource(android.R.drawable.ic_menu_save)
            } else {
                editWorkoutOrderBtn.setIconResource(android.R.drawable.ic_menu_sort_by_size)
            }

            // reset adapter to change view
            exerciseListView.adapter = exerciseListViewAdapter
        }

        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }
}