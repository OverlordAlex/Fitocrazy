package com.itsabugnotafeature.fitocrazy.ui.workouts.workout

import android.annotation.SuppressLint
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
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
import androidx.cardview.widget.CardView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.widget.doOnTextChanged
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.itsabugnotafeature.fitocrazy.R
import com.itsabugnotafeature.fitocrazy.common.AddSetNotificationManager
import com.itsabugnotafeature.fitocrazy.common.Converters
import com.itsabugnotafeature.fitocrazy.common.DisplayListAdapter
import com.itsabugnotafeature.fitocrazy.common.Exercise
import com.itsabugnotafeature.fitocrazy.common.ExerciseDatabase
import com.itsabugnotafeature.fitocrazy.common.ExerciseModel
import com.itsabugnotafeature.fitocrazy.common.ExerciseWithComponentModel
import com.itsabugnotafeature.fitocrazy.common.RecordType
import com.itsabugnotafeature.fitocrazy.common.Set
import com.itsabugnotafeature.fitocrazy.common.SetRecordView
import com.itsabugnotafeature.fitocrazy.common.Workout
import com.itsabugnotafeature.fitocrazy.ui.workouts.workout.ExerciseListViewAdapter.ExerciseView
import com.itsabugnotafeature.fitocrazy.ui.workouts.workout.WorkoutActivity.ExerciseNotification
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Runnable
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.EnumSet
import kotlin.math.min

// the parent list of exercises in a workout
class ExerciseListViewAdapter(
    private val notifier: ExerciseNotification,
    private val addSetNotificationManager: AddSetNotificationManager,
) : RecyclerView.Adapter<ExerciseListViewAdapter.ViewHolder>(), DisplayListAdapter<ExerciseView> {
    override var dataList = emptyList<ExerciseView>().toMutableList()
    override var displayList = emptyList<ExerciseView>().toMutableList()
    private var suggestedExercisePerOrderPosition = emptyList<MutableList<ExerciseModel>>()
    private var inReorderMode: Boolean = false
    private var lastAdded: ExerciseView? = null
    private val taskHandler: Handler = Handler(Looper.getMainLooper())

    lateinit var workout: Workout

    enum class DrawState {
        READY,
        RESTING,
        IN_PROGRESS,
        OLD
    }

    data class ExerciseInputCache(
        var weight: String? = null,
        var reps: String? = null,
        var order: Int? = null,
    )

    data class ExerciseView(
        val displayName: String?,
        val tags: List<String>,
        val exercise: Exercise,
        val sets: MutableList<Set>,
        var record: SetRecordView?,
        val historicalSets: List<Pair<LocalDate, List<Set>>>,
        val basePoints: Int,

        var drawState: DrawState = DrawState.OLD,
        var timerTasks: List<Runnable> = emptyList(),
        var editTextValues: ExerciseInputCache = ExerciseInputCache()

    ) : Comparable<ExerciseView> {
        override fun compareTo(other: ExerciseView): Int {
            return this.exercise.order.compareTo(other.exercise.order)
        }

        fun lastSetIsFail(): Boolean {
            if (sets.size < 2) return false

            val lastTwo = sets.takeLast(2)
            return (lastTwo.last().weight == lastTwo.first().weight) && (lastTwo.last().reps < lastTwo.first().reps)
        }
    }

    /*@SuppressLint("NotifyDataSetChanged")
    fun refresh(hard: Boolean = false) {
        if (hard) notifyDataSetChanged() else notifyItemRangeInserted(0, displayList.size)
    }*/

    override suspend fun loadData(applicationContext: Context, arguments: Map<String, Any>?) {
        if (dataList.isNotEmpty()) throw InstantiationException()

        withContext(Dispatchers.IO) {
            val db = ExerciseDatabase.getInstance(applicationContext).exerciseDao()
            workout = db.getWorkout(arguments?.get("workoutId") as Long) ?: Workout(
                0,
                Instant.now().atZone(ZoneId.systemDefault()).toEpochSecond() * 1000
            )
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
                        .toList().groupBy { it.first.date }
                        .map { Pair(it.key, it.value.map { sec -> sec.second }.flatten()) },
                    basePoints = thisExerciseModel.exercise.basePoints,
                )
            }.sorted().toMutableList()

            suggestedExercisePerOrderPosition = db.getExercisesWithOrders().map {
                // count is used in SQL for ordering, returned as convenience
                it.getCountPerExercise().map { (exerciseId, count) ->
                    db.getExerciseDetails(exerciseId)!!.exercise
                }.toMutableList()
            }
        }
        displayList.addAll(dataList)
        notifyItemRangeInserted(0, displayList.size)
        lastAdded = displayList.lastOrNull()

        showNotification()
        notifier.dataLoaded()
    }

    override fun filterDataList(filter: String): List<ExerciseView> {
        TODO("Not yet implemented")
    }

    override fun addNewItem(item: ExerciseView) {
        super.addNewItem(item)
        lastAdded = item
        showNotification()
    }

    override fun removeItemAt(index: Int): ExerciseView {
        val removedItem = super.removeItemAt(index)
        if (removedItem == lastAdded) lastAdded = null
        return removedItem
    }

    private fun getMostRecentlyAdded(): ExerciseView? = lastAdded ?: dataList.lastOrNull()

    fun getNextReadyExerciseIdx(): Int {
        return dataList.indexOfFirst { it.sets.isEmpty() }
    }

    suspend fun updateExerciseDates(context: Context, newDate: LocalDate) {
        dataList.forEach { it.exercise.date = newDate }
        saveExercises(context)
    }

    suspend fun addExercises(context: Context, listOfExerciseIds: List<Long>) {
        for (exerciseModelId in listOfExerciseIds) {
            val exercise = Exercise(
                0,
                exerciseModelId,
                Instant.ofEpochMilli(workout.date).atZone(ZoneId.systemDefault()).toLocalDate(),
                itemCount + 1,
                workout.workoutId
            )
            var exerciseModel: ExerciseWithComponentModel?
            val record: SetRecordView?
            val historicalSets: List<Pair<LocalDate, List<Set>>>

            val db = ExerciseDatabase.getInstance(context).exerciseDao()
            withContext(Dispatchers.IO) {
                exercise.exerciseId = db.addExerciseSet(exercise)
                exerciseModel = db.getExerciseDetails(exercise.exerciseModelId)
                record = db.getRecord(exercise.exerciseModelId)
                historicalSets = db
                    .getHistoricalSets(exercise.exerciseModelId, 3, exercise.toTimeStamp())
                    .toList().groupBy { it.first.date }
                    .map { Pair(it.key, it.value.map { sec -> sec.second }.flatten()) }
            }
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
            workout.totalExercises = itemCount
            saveWorkout(context)
        }
        notifier.exerciseAdded()
    }

    private suspend fun deleteExercise(context: Context, position: Int) {
        withContext(Dispatchers.IO) {
            val db = ExerciseDatabase.getInstance(context).exerciseDao()
            db.deleteExercise(dataList[position].exercise)
        }
        removeItemAt(position)
        workout.totalExercises = itemCount
        saveWorkout(context)
        notifier.exerciseDeleted()
        showNotification()
    }

    suspend fun addSetSameAsLast(context: Context, exerciseId: Long) {
        val sets = withContext(Dispatchers.IO) {
            val db = ExerciseDatabase.getInstance(context).exerciseDao()
            db.getSets(exerciseId)
        }
        val set = Set(0, exerciseId, sets.last().weight, sets.last().reps, sets.size)
        addSet(context, set)
    }

    suspend fun addSet(context: Context, set: Set) {
        val idx = dataList.indexOfFirst { it.exercise.exerciseId == set.exerciseId }
        val exercise = dataList[idx]

        val db = ExerciseDatabase.getInstance(context).exerciseDao()
        withContext(Dispatchers.IO) {
            set.setID = db.addSetToExercise(set)
        }

        exercise.sets.add(set) // dont need to add to displaylist as the underlying object is the same!
        val newPoints = Workout.calculatePoints(exercise)
        if (exercise.historicalSets.isNotEmpty()) {
            // you can only set records on exercises that have been done before
            exercise.exercise.addRecords(newPoints.records)
        }

        withContext(Dispatchers.IO) {
            exercise.record = db.getRecord(exercise.exercise.exerciseModelId)
        }

        exercise.drawState = DrawState.RESTING
        val readyToWorkTask = Runnable {
            exercise.drawState = DrawState.READY
            notifyItemChanged(idx)
        }
        val inProgressTask = Runnable {
            exercise.drawState = DrawState.IN_PROGRESS
            notifyItemChanged(idx)
        }
        val oldTask = Runnable {
            exercise.drawState = DrawState.OLD
            notifyItemChanged(idx)
        }

        exercise.editTextValues =
            ExerciseInputCache(Converters.formatDoubleWeight(set.weight.toString().toDouble()), set.reps.toString(), -1)

        // ready after 1 minute, assumed working after 2, old after 3.5
        exercise.timerTasks.map { taskHandler.removeCallbacks(it) }
//        taskHandler.postDelayed(readyToWorkTask, 60_000)
//        taskHandler.postDelayed(inProgressTask, 120_000)
//        taskHandler.postDelayed(oldTask, 210_000)
        taskHandler.postDelayed(readyToWorkTask, 5_000)
        taskHandler.postDelayed(inProgressTask, 10_000)
        taskHandler.postDelayed(oldTask, 15_000)
        exercise.timerTasks = listOf(readyToWorkTask, inProgressTask, oldTask)
        displayList[idx] = dataList[idx]

        updateWorkoutPoints()
        saveWorkout(context)
        saveExercises(context)

        notifyItemChanged(idx)
        showNotification(true)
        notifier.setAdded(exercise, set)
    }

    suspend fun deleteLastSet(context: Context, position: Int) {
        val exercise = dataList[position]

        if (exercise.sets.isEmpty()) {
            deleteExercise(context, position)
        } else {
            val removedSet = exercise.sets.removeLast()
            withContext(Dispatchers.IO) {
                val db = ExerciseDatabase.getInstance(context).exerciseDao()
                db.deleteSetFromExercise(removedSet)
                exercise.record = db.getRecord(exercise.exercise.exerciseModelId)
            }
            val newPoints = Workout.calculatePoints(exercise)
            exercise.exercise.recordsAchieved = if (newPoints.records.isEmpty()) {
                EnumSet.noneOf(RecordType::class.java)
            } else {
                EnumSet.copyOf(newPoints.records.map { it.recordType })
            }

            updateWorkoutPoints()
            saveWorkout(context)
            saveExercises(context)
            showNotification(true)

            notifyItemChanged(position)
            notifier.setRemoved(exercise, removedSet)
        }
    }

    fun getSuggestedNextExercises(): List<ExerciseModel> {
        // TODO: update list when item added or removed to datalist

        val existingExercises = currentExerciseIds()
        val possibleSuggestions =
            suggestedExercisePerOrderPosition
                .map { it.filter { exerciseModel -> exerciseModel.exerciseId !in existingExercises } }
                .toMutableList()
        val position = min(itemCount, suggestedExercisePerOrderPosition.size) // for which position we are suggesting

        val suggestions = possibleSuggestions
            .subList(position, possibleSuggestions.size)
            .flatten()
            .toSet()
            .take(3).toMutableList()
        if (suggestions.size < 3) {
            suggestions.addAll(
                possibleSuggestions.subList(0, position)
                    .reversed()
                    .flatten()
                    .toSet()
                    .filter { it !in suggestions }
                    .take(3 - suggestions.size)
            )
        }

        return suggestions
    }

    private suspend fun saveExercises(context: Context) {
        val db = ExerciseDatabase.getInstance(context).exerciseDao()
        withContext(Dispatchers.IO) {
            dataList.filter { it.exercise.isDirty() }.forEach {
                db.updateExercise(it.exercise)
                it.exercise.clearDirty()
            }
        }
    }

    private fun updateWorkoutPoints() {
        workout.recalculateWorkoutTotals(dataList)
    }

    fun updateTimers(totalTimeTime: Long) {
        if (LocalDate.now() == Instant.ofEpochMilli(workout.date).atZone(ZoneId.systemDefault()).toLocalDate()) {
            workout.totalTime = SystemClock.elapsedRealtime() - totalTimeTime
            //workout.currentSetTime = SystemClock.elapsedRealtime() - currentSetTimeBase // , currentSetTimeBase: Long
        }
    }

    suspend fun saveWorkout(context: Context) {
        // whenever we save the workout is when the top tags are actually calculated
        workout.topTags = getTopTags()
        withContext(Dispatchers.IO) {
            val db = ExerciseDatabase.getInstance(context).exerciseDao()
            db.updateWorkout(workout)
        }
    }

    fun showNotificationAgain(chronometerBase: Long, chronometerRunning: Boolean = true) {
        addSetNotificationManager.showNotificationAgain(chronometerBase, chronometerRunning)
    }

    @Suppress("SameParameterValue")
    private fun showNotification(chronometerRunning: Boolean = false) {
        val mostRecent = getMostRecentlyAdded()

        addSetNotificationManager.showNotification(
            totalExercises = workout.totalExercises,
            date = Instant.ofEpochMilli(workout.date).atZone(ZoneId.systemDefault()).toLocalDate(),
            chronometerBase = SystemClock.elapsedRealtime(),
            chronometerRunning = chronometerRunning,
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

    private fun getTopTags(): String {
        return dataList
            .fold(mutableListOf<String>()) { ongoing, item -> repeat(item.sets.size) { ongoing.addAll(item.tags) }; ongoing }
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
        notifyItemRangeChanged(0, displayList.size)
        return inReorderMode
    }

    fun swapNext(position: Int) {
        dataList[position].exercise.order += 1
        dataList[position + 1].exercise.order -= 1

        swap(position, position + 1)
    }

    fun swapPrev(position: Int) {
        dataList[position - 1].exercise.order += 1
        dataList[position].exercise.order -= 1

        swap(position, position - 1)
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val card = itemView.findViewById<CardView>(R.id.card_workoutRow)
        private val cardContents = itemView.findViewById<ConstraintLayout>(R.id.layout_suggestedNextExercises)

        private val btnMoveUp = itemView.findViewById<Button>(R.id.btn_moveSetUpInExercise)
        private val btnMoveDown = itemView.findViewById<Button>(R.id.btn_moveSetDownInExercise)

        private val pointsChip = itemView.findViewById<Chip>(R.id.chip_exercisePoints)
        private var exerciseNameOnCard = itemView.findViewById<TextView>(R.id.label_exerciseNameOnCard)
        private val bodyPartsChipGroup = itemView.findViewById<ChipGroup>(R.id.chipGroup_exerciseTags)

        private val exerciseSetsScrollLayout = itemView.findViewById<LinearLayout>(R.id.layout_listOfSetsOnExerciseCard)
        private val listOfSetsScrollView =
            itemView.findViewById<HorizontalScrollView>(R.id.scrollview_listOfSetsOnExerciseCard)

        private val mostWeightAchievementBadge = itemView.findViewById<ImageView>(R.id.img_achievementMostWeight)
        private val mostRepsAchivementBadge = itemView.findViewById<ImageView>(R.id.img_achievementMostReps)
        private val mostMovedAchivementBadge = itemView.findViewById<ImageView>(R.id.img_achievementMostMoved)

        private val btnAddSet = itemView.findViewById<Button>(R.id.btn_addSetToThisExercise)
        private val btnRemoveLastSet = itemView.findViewById<Button>(R.id.btn_removeLastSetFromThisExercise)
        private val repsEditText = itemView.findViewById<EditText>(R.id.numberEntry_addRepsToThisExercise)
        private val weightEditText = itemView.findViewById<EditText>(R.id.numberEntry_addKilogramsToThisExercise)

        @SuppressLint("ClickableViewAccessibility")
        fun bind(currentExercise: ExerciseView) {
            exerciseNameOnCard.text =
                currentExercise.displayName + if (currentExercise.sets.isEmpty()) "" else " [" + currentExercise.sets.size + "]"
            exerciseNameOnCard.isSelected = true  // required for marquee

            bodyPartsChipGroup.removeAllViews()
            currentExercise.tags.sorted().forEach { chipName ->
                val newChip = Chip(itemView.context)
                newChip.text = chipName
                newChip.setChipBackgroundColorResource(R.color.blue_accent_light)
                bodyPartsChipGroup.addView(newChip)
            }

            val points = Workout.calculatePoints(currentExercise)
            pointsChip.text = points.points.toString()

            if (inReorderMode) {
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

                btnMoveUp.setOnClickListener {
                    swapNext(adapterPosition)
                    runBlocking {
                        saveExercises(itemView.context)
                    }
                }
                btnMoveDown.setOnClickListener {
                    swapPrev(adapterPosition)
                    runBlocking {
                        saveExercises(itemView.context)
                    }
                }

                return  // don't have to waste time rendering further
            }

            mostWeightAchievementBadge.visibility = ImageView.INVISIBLE
            mostRepsAchivementBadge.visibility = ImageView.INVISIBLE
            mostMovedAchivementBadge.visibility = ImageView.INVISIBLE

            currentExercise.exercise.recordsAchieved?.forEach {
                when (it) {
                    RecordType.MAX_WEIGHT -> mostWeightAchievementBadge.visibility = ImageView.VISIBLE

                    RecordType.MAX_REPS -> mostRepsAchivementBadge.visibility = ImageView.VISIBLE

                    RecordType.MAX_WEIGHT_MOVED -> mostMovedAchivementBadge.visibility = ImageView.VISIBLE

                    null -> TODO("Not implemented")
                }
            }

            weightEditText.setText(currentExercise.editTextValues.weight)
            weightEditText.setOnTouchListener { _, _ ->
                // clear text on touch
                weightEditText.text = null
                false
            }
            weightEditText.doOnTextChanged { text, _, _, _ ->
                if (!text.isNullOrEmpty()) {
                    // TODO: why datalist? and why doesnt it work in a method?
                    dataList[adapterPosition].editTextValues.weight =
                        Converters.formatDoubleWeight(weightEditText.text.toString().toDouble())
                    dataList[adapterPosition].editTextValues.order = adapterPosition
                }
            }
            weightEditText.setOnFocusChangeListener { _, hasFocus ->
                if (hasFocus) {
                    weightEditText.post {
                        // wild that this required
                        weightEditText.setSelection(weightEditText.text.length)
                    }
                } else if (weightEditText.text.toString().isNotEmpty()) {
                    // when losing focus, format text
                    weightEditText.setText(Converters.formatDoubleWeight(weightEditText.text.toString().toDouble()))
                }
            }

            repsEditText.setText(currentExercise.editTextValues.reps)
            repsEditText.setOnTouchListener { _, _ ->
                // clear text on touch
                repsEditText.text = null
                false
            }
            repsEditText.doOnTextChanged { text, _, _, _ ->
                if (!text.isNullOrEmpty()) {
                    dataList[adapterPosition].editTextValues.reps = repsEditText.text.toString()
                    dataList[adapterPosition].editTextValues.order = adapterPosition
                }
            }

            repsEditText.setOnFocusChangeListener { _, hasFocus ->
                if (hasFocus) {
                    repsEditText.post {
                        // wild that this required
                        repsEditText.setSelection(repsEditText.text.length)
                    }
                } else if (repsEditText.text.toString().isNotEmpty()) {
                    // when losing focus, format text
                    repsEditText.setText(repsEditText.text.toString().toInt().toString())
                }
            }

            exerciseSetsScrollLayout.removeAllViews()

            val recordListView = LayoutInflater.from(itemView.context).inflate(
                R.layout.container_workout_exercise_set_list_horizontal, itemView.rootView as ViewGroup, false
            )
            // TODO: internationalisation of these
            val recordTypes = listOf("Weight", "Reps", "Moved").joinToString("\n")
            val recordValues = listOf(
                currentExercise.record?.maxWeight ?: "-",
                currentExercise.record?.maxReps ?: "-",
                currentExercise.record?.mostWeightMoved ?: "-"
            ).joinToString("\n")
            recordListView.findViewById<TextView>(R.id.label_setDate).text = "Records"
            recordListView.findViewById<TextView>(R.id.textlist_weightInSet).text = recordTypes
            recordListView.findViewById<TextView>(R.id.textlist_repsInSet).text = recordValues
            exerciseSetsScrollLayout.addView(recordListView)

            currentExercise.historicalSets.reversed().forEach { (workoutDate, setList) ->
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
            for (set in currentExercise.sets) {
                setWeightString.appendLine("${Converters.formatDoubleWeight(set.weight)} x")
                setRepsString.appendLine(set.reps)
            }
            // set the color of the "today" if the last set failed (not permanent, not super useful TODO)
            if (currentExercise.lastSetIsFail()) {
                setListView.findViewById<TextView>(R.id.label_setDate)
                    .setTextColor(itemView.context.getColor(R.color.purple_accent))
            }

            setWeightString.append("KG *")
            setListView.findViewById<TextView>(R.id.textlist_weightInSet).gravity = Gravity.END
            setListView.findViewById<TextView>(R.id.textlist_weightInSet).text = setWeightString
            setListView.findViewById<TextView>(R.id.textlist_repsInSet).text = setRepsString
            setListView.minimumWidth = 250  // TODO: 1/4 of screen width?

            exerciseSetsScrollLayout.addView(setListView)
            listOfSetsScrollView.post { listOfSetsScrollView.scrollX = setListView.left }

            if (currentExercise.historicalSets.isEmpty()) {
                pointsChip.setChipBackgroundColorResource(R.color.blue_accent)
                pointsChip.setTextColor(itemView.context.getColor(R.color.white))
            } else {
                pointsChip.setChipBackgroundColorResource(R.color.blue_accent_lightest)
                pointsChip.setTextColor(itemView.context.getColor(R.color.slate_dark))
            }

            if (currentExercise.sets.isEmpty()) {
                currentExercise.drawState = DrawState.READY
            }

            when (currentExercise.drawState) {
                DrawState.RESTING -> {
                    cardContents.setBackgroundColor(itemView.context.getColor(R.color.white))
                    card.setCardBackgroundColor(itemView.context.getColor(R.color.white))
                }

                DrawState.READY -> {
                    cardContents.setBackgroundColor(itemView.context.getColor(R.color.blue_accent_lightest))
                    card.setCardBackgroundColor(itemView.context.getColor(R.color.blue_accent_lightest))
                }

                DrawState.IN_PROGRESS -> {
                    cardContents.setBackgroundColor(itemView.context.getColor(R.color.orange_accent_light))
                    card.setCardBackgroundColor(itemView.context.getColor(R.color.orange_accent_light))
                }

                DrawState.OLD -> {
                    cardContents.setBackgroundColor(itemView.context.getColor(R.color.grey_light))
                    card.setCardBackgroundColor(itemView.context.getColor(R.color.grey_light))
                }
            }

            btnRemoveLastSet.setOnClickListener {
                val imm = itemView.context.getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
                imm.hideSoftInputFromWindow(itemView.windowToken, 0)

                runBlocking {
                    deleteLastSet(itemView.context, adapterPosition)
                }
            }

            btnAddSet.setOnClickListener {
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
                    addSet(itemView.context, set)
                }
            }
        }
    }

    // We always have a suggestion box, hence + 1
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