package com.itsabugnotafeature.fitocrazy.ui.home.workout

import android.annotation.SuppressLint
import android.content.Context
import android.os.SystemClock
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.HorizontalScrollView
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity.INPUT_METHOD_SERVICE
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.itsabugnotafeature.fitocrazy.R
import com.itsabugnotafeature.fitocrazy.common.AddSetNotificationManager
import com.itsabugnotafeature.fitocrazy.common.Converters
import com.itsabugnotafeature.fitocrazy.common.DisplayListAdapter
import com.itsabugnotafeature.fitocrazy.common.Exercise
import com.itsabugnotafeature.fitocrazy.common.ExerciseDatabase
import com.itsabugnotafeature.fitocrazy.common.ExerciseWithComponentModel
import com.itsabugnotafeature.fitocrazy.common.Set
import com.itsabugnotafeature.fitocrazy.common.SetRecordView
import com.itsabugnotafeature.fitocrazy.common.Workout
import com.itsabugnotafeature.fitocrazy.ui.home.workout.WorkoutActivity.Companion.RecordType
import com.itsabugnotafeature.fitocrazy.ui.home.workout.WorkoutActivity.ExerciseNotification
import com.itsabugnotafeature.fitocrazy.ui.home.workout.WorkoutActivity.ExerciseView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.format.DateTimeFormatter

// the parent list of exercises in a workout
class ExerciseListViewAdapter(
    private val notifier: ExerciseNotification,
    private val addSetNotificationManager: AddSetNotificationManager,
) : RecyclerView.Adapter<ExerciseListViewAdapter.ViewHolder>(), DisplayListAdapter<ExerciseView> {
    override var dataList = emptyList<ExerciseView>().toMutableList()
    override var displayList = emptyList<ExerciseView>().toMutableList()
    private var inReorderMode: Boolean = false
    private var lastAdded: ExerciseView? = null

    lateinit var workout: Workout

    @SuppressLint("NotifyDataSetChanged")
    fun refresh(hard: Boolean = false) {
        if (hard) notifyDataSetChanged() else notifyItemRangeInserted(0, displayList.size)
    }

    override suspend fun loadData(applicationContext: Context, arguments: Map<String, Any>?) {
        if (dataList.isNotEmpty()) throw InstantiationException()

        withContext(Dispatchers.IO) {
            val db = ExerciseDatabase.getInstance(applicationContext).exerciseDao()
            workout = db.getWorkout(arguments?.get("workoutId") as Long) ?: Workout(0, LocalDate.now())
            if (workout.workoutId == 0L) {
                workout.workoutId = db.addWorkout(workout)
            }

            dataList = db.getListOfExerciseInWorkout(workout.workoutId).map { exercise ->
                val thisExerciseModel = db.getExerciseDetails(exercise.exerciseModelId)!!
                ExerciseView(
                    displayName = thisExerciseModel.exercise.displayName,
                    tags = thisExerciseModel.exercise.getChips(),
                    exercise = exercise,
                    sets = db.getSets(exercise.exerciseId).toMutableList(),
                    record = db.getRecord(exercise.exerciseModelId),
                    historicalSets = db
                        .getHistoricalSets(thisExerciseModel.exercise.exerciseId, 3, exercise.toTimeStamp())
                        .map { Pair(it.key.date, it.value) }.sortedBy { it.first },
                    basePoints = thisExerciseModel.exercise.basePoints,
                )
            }.sorted().toMutableList()

            displayList.addAll(dataList)
            //Log.i("Fitocrazy", displayList.joinToString { it.exercise.toString() })
            notifyItemRangeInserted(0, displayList.size)

            lastAdded = displayList.lastOrNull()
        }
    }

    override fun filterDataList(filter: String): List<ExerciseView> {
        TODO("Not yet implemented")
    }

    override fun addNewItem(item: ExerciseView) {
        super.addNewItem(item)
        lastAdded = item
    }

    override fun removeItemAt(index: Int): ExerciseView {
        val removedItem = super.removeItemAt(index)
        if (removedItem == lastAdded) lastAdded = null
        return removedItem
    }

    fun getMostRecentlyAdded(): ExerciseView? = lastAdded

    suspend fun updateExerciseDates(applicationContext: Context, newDate: LocalDate) {
        val db = ExerciseDatabase.getInstance(applicationContext).exerciseDao()
        withContext(Dispatchers.IO) {
            dataList.forEach {
                it.exercise.date = newDate
                db.updateExercise(it.exercise)
            }
        }
    }

    suspend fun addExercises(applicationContext: Context, listOfExerciseIds: List<Long>) {
        for (exerciseModelId in listOfExerciseIds) {
            val exercise = Exercise(
                0,
                exerciseModelId,
                workout.date,
                itemCount + 1,
                workout.workoutId
            )
            var exerciseModel: ExerciseWithComponentModel?
            val record: SetRecordView?
            val historicalSets: List<Pair<LocalDate, List<Set>>>


            withContext(Dispatchers.IO) {
                val db = ExerciseDatabase.getInstance(applicationContext).exerciseDao()
                withContext(Dispatchers.IO) {
                    exercise.exerciseId = db.addExerciseSet(exercise)
                    exerciseModel = db.getExerciseDetails(exercise.exerciseModelId)
                    record = db.getRecord(exercise.exerciseModelId)
                    historicalSets = db
                        .getHistoricalSets(exercise.exerciseModelId, 3, exercise.toTimeStamp())
                        .toSortedMap().toList().map { Pair(it.first.date, it.second) }
                }
            }
            Log.i("TEXT", "Historical ${exerciseModel?.exercise?.exerciseId} sets: ${historicalSets.size}")
            addNewItem(
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
            workout.topTags = getTopTags()
            workout.totalExercises = itemCount
            saveWorkout(applicationContext)
        }
    }

    fun addSetToExercise(exerciseId: Long, set: Set) {
        val idx = dataList.indexOfFirst { it.exercise.exerciseId == exerciseId }
        dataList[idx].sets.add(set) // dont need to add twice as the underlying object is the same!
        notifyItemChanged(idx)
    }

    fun addSetToExercise(exercise: ExerciseView, set: Set) {
        addSetToExercise(exercise.exercise.exerciseId, set)
    }

    fun updateWorkoutPoints() {
        workout.recalculateWorkoutTotals(dataList)
    }

    fun saveTimers(totalTimeTime: Long, currentSetTimeBase: Long) {
        if (LocalDate.now() == workout.date) {
            workout.totalTime = SystemClock.elapsedRealtime() - totalTimeTime
            workout.currentSetTime = SystemClock.elapsedRealtime() - currentSetTimeBase
        }
    }

    suspend fun saveWorkout(applicationContext: Context) {
        withContext(Dispatchers.IO) {
            val db = ExerciseDatabase.getInstance(applicationContext).exerciseDao()
            db.updateWorkout(workout)
        }
    }

    fun showNotificationAgain(chronometerBase: Long, chronometerRunning: Boolean = true) {
        addSetNotificationManager.showNotificationAgain(chronometerBase, chronometerRunning)
    }

    fun showNotification() {
        val mostRecent = getMostRecentlyAdded()

        addSetNotificationManager.showNotification(
            totalExercises = workout.totalExercises,
            date = workout.date,
            chronometerBase = SystemClock.elapsedRealtime(),
            chronometerRunning = false,
            exerciseId = mostRecent?.exercise?.exerciseId,
            displayName = mostRecent?.displayName,
            set = mostRecent?.sets?.lastOrNull(),
            numPrevSets = mostRecent?.sets?.takeLastWhile {
                it.weight == (mostRecent.sets.last().weight)
            }?.count(),
            totalSets = mostRecent?.sets?.size
        )
    }

    fun currentExerciseIds(): List<Long> {
        return dataList.map { it.exercise.exerciseModelId }
    }

    fun getTopTags(): String {
        return dataList
            .fold(emptyList<String>()) { ongoing, item -> ongoing + item.tags }
            .groupingBy { it }
            .eachCount().asIterable()
            .sortedBy { it.value }.reversed()
            .take(3)
            .joinToString(" ") { it.key }
            .trim()
    }

    fun toggleReorderMode(cancel: Boolean = false): Boolean {
        if (!inReorderMode && cancel) {
            return false
        }

        inReorderMode = !inReorderMode
        // TODO move reorder mode to individual view holder / data items, and pass in payload of range changed?
        notifyItemRangeChanged(0, displayList.size)
        return inReorderMode
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        fun bind(currentExercise: ExerciseView) {
            val exerciseNameOnCard = itemView.findViewById<TextView>(R.id.label_exerciseNameOnCard)
            exerciseNameOnCard.text = currentExercise.displayName

            val chipGroup = itemView.findViewById<ChipGroup>(R.id.chipGroup_exerciseTags)
            if (chipGroup.childCount == 0) {
                currentExercise.tags.forEach { chipName ->
                    val newChip = Chip(itemView.context)
                    newChip.text = chipName
                    newChip.setChipBackgroundColorResource(R.color.blue_accent_light)
                    chipGroup.addView(newChip)
                }
            }

            val points = Workout.calculatePoints(currentExercise)
            val pointsChip = itemView.findViewById<Chip>(R.id.chip_exercisePoints)
            pointsChip.text = points.points.toString()

            if (inReorderMode) {
                val btnMoveUp = itemView.findViewById<Button>(R.id.btn_moveSetUpInExercise)
                val btnMoveDown = itemView.findViewById<Button>(R.id.btn_moveSetDownInExercise)

                when (adapterPosition) {
                    // it is laid out in reverse order, hence the end of the list is at the "top"
                    displayList.size - 1 -> {
                        btnMoveUp.visibility = Button.INVISIBLE
                        btnMoveDown.visibility = Button.VISIBLE

                    }

                    0 -> {
                        btnMoveUp.visibility = Button.VISIBLE
                        btnMoveDown.visibility = Button.INVISIBLE
                    }

                    else -> {
                        btnMoveUp.visibility = Button.VISIBLE
                        btnMoveDown.visibility = Button.VISIBLE
                    }
                }
                if (itemCount == 1) {
                    // cannot reorder 1 item!
                    btnMoveUp.visibility = Button.INVISIBLE
                    btnMoveDown.visibility = Button.INVISIBLE
                }

                // TODO: change ORDER and call re-order
                btnMoveUp.setOnClickListener {
                    runBlocking {
                        withContext(Dispatchers.IO) {
                            val db = ExerciseDatabase.getInstance(itemView.context).exerciseDao()
                            dataList[adapterPosition].exercise.order += 1
                            dataList[adapterPosition + 1].exercise.order -= 1
                            db.updateExercise(dataList[adapterPosition].exercise)
                            db.updateExercise(dataList[adapterPosition + 1].exercise)
                        }
                    }
                    swap(adapterPosition, adapterPosition + 1)
                }
                btnMoveDown.setOnClickListener {
                    runBlocking {
                        withContext(Dispatchers.IO) {
                            val db = ExerciseDatabase.getInstance(itemView.context).exerciseDao()
                            dataList[adapterPosition - 1].exercise.order += 1
                            dataList[adapterPosition].exercise.order -= 1
                            db.updateExercise(dataList[adapterPosition - 1].exercise)
                            db.updateExercise(dataList[adapterPosition].exercise)
                        }
                    }
                    swap(adapterPosition, adapterPosition - 1)
                }

                return // dont have to waste time rendering further
            }

            val weightEditText = itemView.findViewById<EditText>(R.id.numberEntry_addKilogramsToThisExercise)
            val lastSet = currentExercise.sets.lastOrNull()
            if (lastSet == null) {
                weightEditText.text = null
            } else {
                weightEditText.setText(Converters.formatDoubleWeight(lastSet.weight))
            }
            weightEditText.setOnFocusChangeListener { _, hasFocus ->
                // clear text on focus
                if (hasFocus) {
                    weightEditText.text = null
                } else if (weightEditText.text.toString().isNotEmpty()) {
                    // when losing focus, format text
                    weightEditText.setText(Converters.formatDoubleWeight(weightEditText.text.toString().toDouble()))
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

            val exerciseSetsScrollLayout = itemView.findViewById<LinearLayout>(R.id.layout_listOfSetsOnExerciseCard)
            exerciseSetsScrollLayout.removeAllViews()

            currentExercise.historicalSets.forEach { (workoutDate, setList) ->
                val setListView = LayoutInflater.from(itemView.context).inflate(
                    R.layout.container_workout_exercise_set_list_horizontal, itemView.rootView as ViewGroup, false
                )
                setListView.findViewById<TextView>(R.id.label_setDate).text =
                    workoutDate.format(Converters.dateFormatter)

                val setWeightString = StringBuilder()
                val setRepsString = StringBuilder()
                for (set in setList) {
                    setWeightString.appendLine("${Converters.formatDoubleWeight(set.weight)} x")
                    setRepsString.appendLine(set.reps)
                }

                setListView.findViewById<TextView>(R.id.textlist_weightInSet).text = setWeightString
                setListView.findViewById<TextView>(R.id.textlist_repsInSet).text = setRepsString
                exerciseSetsScrollLayout.addView(setListView)
            }

            val setListView = LayoutInflater.from(itemView.context).inflate(
                R.layout.container_workout_exercise_set_list_horizontal_today,
                itemView.rootView as ViewGroup,
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
            Log.i("TEST", "Rendering ${currentExercise.sets.size} sets")
            for (set in currentExercise.sets) {
                setWeightString.appendLine("${Converters.formatDoubleWeight(set.weight)} x")
                setRepsString.appendLine(set.reps)
            }
            setWeightString.append("KG *")
            setListView.findViewById<TextView>(R.id.textlist_weightInSet).gravity = Gravity.END
            setListView.findViewById<TextView>(R.id.textlist_weightInSet).text = setWeightString
            setListView.findViewById<TextView>(R.id.textlist_repsInSet).text = setRepsString

            setListView.minimumWidth = (itemView.rootView.width * 0.3).toInt()
            exerciseSetsScrollLayout.addView(setListView)

            val scrollView = itemView.findViewById<HorizontalScrollView>(R.id.scrollview_listOfSetsOnExerciseCard)
            scrollView.post { scrollView.scrollX = setListView.left }

            if (currentExercise.historicalSets.isEmpty()) {
                pointsChip.setChipBackgroundColorResource(R.color.blue_accent)
                pointsChip.setTextColor(itemView.context.getColor(R.color.white))
            } else {
                pointsChip.setChipBackgroundColorResource(R.color.blue_accent_lightest)
                pointsChip.setTextColor(itemView.context.getColor(R.color.slate_dark))
            }

            itemView.findViewById<Button>(R.id.btn_removeLastSetFromThisExercise).setOnClickListener {
                val imm = itemView.context.getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
                imm.hideSoftInputFromWindow(itemView.windowToken, 0)

                if (currentExercise.sets.isEmpty()) {
                    runBlocking {
                        val db = ExerciseDatabase.getInstance(itemView.context).exerciseDao()
                        db.deleteExercise(currentExercise.exercise)

                        removeItemAt(adapterPosition)

                        workout.totalExercises = itemCount
                        saveWorkout(itemView.context)
                    }
                    showNotification()
                } else {
                    val removedSet = currentExercise.sets.removeLast()
                    runBlocking {
                        val db = ExerciseDatabase.getInstance(itemView.context).exerciseDao()
                        db.deleteSetFromExercise(removedSet)
                        updateWorkoutPoints()
                        saveWorkout(itemView.context)
                    }
                    // TODO: show notification
                    /*showNotification(
                    totalExercises = workout.totalExercises,
                    date = workout.date,
                    chronometerBase = SystemClock.elapsedRealtime(),
                    chronometerRunning = false,
                    exerciseId = null,
                    displayName = exercise.displayName,
                    set = null,
                    numPrevSets = null,
                    totalSets = null
                    )*/

                    // TODO: move to normal bind instead of in button, and then notify change
                    val mostWeight = itemView.findViewById<ImageView>(R.id.img_achievementMostWeight)
                    val mostReps = itemView.findViewById<ImageView>(R.id.img_achievementMostReps)
                    val mostMoved = itemView.findViewById<ImageView>(R.id.img_achievementMostMoved)
                    mostWeight.visibility = ImageView.GONE
                    mostReps.visibility = ImageView.GONE
                    mostMoved.visibility = ImageView.GONE

                    val newPoints = Workout.calculatePoints(currentExercise)
                    pointsChip.text = newPoints.points.toString()
                    newPoints.records.forEach {
                        when (it.recordType) {
                            RecordType.MAX_WEIGHT -> mostWeight.visibility = ImageView.VISIBLE

                            RecordType.MAX_REPS -> mostReps.visibility = ImageView.VISIBLE

                            RecordType.MAX_WEIGHT_MOVED -> mostMoved.visibility = ImageView.VISIBLE
                        }
                    }

                    notifier.setRemoved(currentExercise, removedSet)
                }
            }

            itemView.findViewById<Button>(R.id.btn_addSetToThisExercise).setOnClickListener {
                val imm = itemView.context.getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
                imm.hideSoftInputFromWindow(itemView.windowToken, 0)
                itemView.clearFocus()

                val weight: Double? = weightEditText.text.toString().toDoubleOrNull()
                val reps: Int? = repsEditText.text.toString().toIntOrNull()
                if (weight == null || reps == null) return@setOnClickListener
                if (weight == 0.0 || reps == 0) return@setOnClickListener

                val set = Set(
                    0,
                    currentExercise.exercise.exerciseId,
                    weight,
                    reps,
                    currentExercise.sets.size
                )
                runBlocking {
                    val db = ExerciseDatabase.getInstance(itemView.context).exerciseDao()
                    set.setID = db.addSetToExercise(set)


                    addSetToExercise(currentExercise, set)
                    updateWorkoutPoints()
                    saveWorkout(itemView.context)
                }
                notifier.setAdded(currentExercise, set)
                // TODO update notification
                /*showNotification(
                    totalExercises = workout.totalExercises,
                    date = workout.date,
                    chronometerBase = SystemClock.elapsedRealtime(),
                    chronometerRunning = (today == workout.date),
                    exerciseId = exercise.exercise.exerciseId,
                    displayName = exercise.displayName,
                    set = exercise.sets.lastOrNull(),
                    numPrevSets = exercise.sets.takeLastWhile { it.weight == exercise.sets.last().weight }.count(),
                    totalSets = exercise.sets.size
                )*/

                val mostWeight = itemView.findViewById<ImageView>(R.id.img_achievementMostWeight)
                val mostReps = itemView.findViewById<ImageView>(R.id.img_achievementMostReps)
                val mostMoved = itemView.findViewById<ImageView>(R.id.img_achievementMostMoved)
                mostWeight.visibility = ImageView.GONE
                mostReps.visibility = ImageView.GONE
                mostMoved.visibility = ImageView.GONE

                val newPoints = Workout.calculatePoints(currentExercise)
                pointsChip.text = newPoints.points.toString()
                newPoints.records.forEach {
                    when (it.recordType) {
                        RecordType.MAX_WEIGHT -> mostWeight.visibility = ImageView.VISIBLE

                        RecordType.MAX_REPS -> mostReps.visibility = ImageView.VISIBLE

                        RecordType.MAX_WEIGHT_MOVED -> mostMoved.visibility = ImageView.VISIBLE
                    }
                }
            }
        }
    }

    override fun getItemCount() = displayList.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view: View = if (inReorderMode)
            LayoutInflater.from(parent.context).inflate(R.layout.row_workout_exercise_moving, parent, false)
        else
            LayoutInflater.from(parent.context).inflate(R.layout.row_workout_exercise, parent, false)

        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(displayList[position])
    }
}