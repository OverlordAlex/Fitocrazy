package com.itsabugnotafeature.fitocrazy.ui.workouts

import android.animation.ArgbEvaluator
import android.animation.ValueAnimator
import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.itsabugnotafeature.fitocrazy.R
import com.itsabugnotafeature.fitocrazy.common.Converters
import com.itsabugnotafeature.fitocrazy.common.DisplayListAdapter
import com.itsabugnotafeature.fitocrazy.common.Exercise
import com.itsabugnotafeature.fitocrazy.common.ExerciseDatabase
import com.itsabugnotafeature.fitocrazy.common.Workout
import com.itsabugnotafeature.fitocrazy.common.WorkoutRecordView
import com.itsabugnotafeature.fitocrazy.ui.workouts.workout.WorkoutActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlin.time.DurationUnit
import kotlin.time.toDuration

class WorkoutListViewAdapter : RecyclerView.Adapter<WorkoutListViewAdapter.ViewHolder>(), DisplayListAdapter<Workout> {
    override var dataList = emptyList<Workout>().toMutableList()
    override var displayList = emptyList<Workout>().toMutableList()

    //private var workoutList: MutableList<Workout> = emptyList<Workout>().toMutableList()
    private var totalWorkoutStats: WorkoutRecordView? = null
    private var currentlyShowingActionFrameIndex: Int? = null

    override suspend fun loadData(applicationContext: Context, arguments: Map<String, Any>?) {
        if (dataList.isNotEmpty()) throw InstantiationException("Workout list already populated!")

        withContext(Dispatchers.IO) {
            val db = ExerciseDatabase.getInstance(applicationContext).exerciseDao()
            dataList = db.listWorkouts(
                start = (arguments?.get("start") ?: 0L) as Long,
                end = (arguments?.get("end") ?: Long.MAX_VALUE) as Long
            ).toMutableList()
            displayList.addAll(dataList)
            notifyItemRangeInserted(0, displayList.size)

            totalWorkoutStats = db.getWorkoutStats()
        }
    }

    override fun filterDataList(filter: String): List<Workout> {
        TODO("Not yet implemented")
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        fun bind(currentWorkout: Workout) {
            val layoutWorkoutStats = itemView.findViewById<LinearLayout>(R.id.layout_workoutOtherStats)
            val labelNumberOfExercises = itemView.findViewById<TextView>(R.id.label_workoutNumberExercises)
            val frameActionItems = itemView.findViewById<LinearLayout>(R.id.frame_deleteOrDuplicateWorkout)

            if (adapterPosition == currentlyShowingActionFrameIndex) {
                layoutWorkoutStats.visibility = LinearLayout.GONE
                labelNumberOfExercises.visibility = TextView.GONE
                frameActionItems.visibility = LinearLayout.VISIBLE
            } else {
                layoutWorkoutStats.visibility = LinearLayout.VISIBLE
                labelNumberOfExercises.visibility = TextView.VISIBLE
                frameActionItems.visibility = LinearLayout.GONE
            }

            itemView.findViewById<TextView>(R.id.label_workoutDate).text =
                if (LocalDate.now() == Instant.ofEpochMilli(currentWorkout.date).atZone(ZoneId.systemDefault())
                        .toLocalDate()
                ) {
                    itemView.context.getString(R.string.today)
                } else {
                    Converters.dateFormatter.format(
                        Instant.ofEpochMilli(currentWorkout.date).atZone(ZoneId.systemDefault())
                    )
                }

            labelNumberOfExercises.text =
                itemView.context.getString(R.string.number_of_exercises_in_workout, currentWorkout.totalExercises)

            val labelWorkoutPoints = itemView.findViewById<TextView>(R.id.label_workoutPoints)
            // do a nice color animation on points more than the last 10 average
            if (currentWorkout.totalPoints > (totalWorkoutStats?.avgTotalPoints ?: 0.0)) {
                val colorFrom: Int = itemView.context.getColor(R.color.blue_main)
                val colorTo: Int = itemView.context.getColor(R.color.blue_accent)

                val colorAnimation = ValueAnimator.ofObject(ArgbEvaluator(), colorFrom, colorTo)
                colorAnimation.setDuration(1000) // milliseconds
                colorAnimation.repeatCount = ValueAnimator.INFINITE
                colorAnimation.repeatMode = ValueAnimator.REVERSE
                colorAnimation.addUpdateListener { animator -> labelWorkoutPoints.setTextColor(animator.animatedValue as Int) }
                colorAnimation.start()
            } else {
                labelWorkoutPoints.clearAnimation()
            }

            labelWorkoutPoints.text =
                itemView.context.getString(R.string.total_points_in_workout, currentWorkout.totalPoints)

            itemView.findViewById<TextView>(R.id.label_workoutTotalWeightValue).text =
                itemView.context.getString(R.string.total_weight_in_workout, currentWorkout.totalWeight)

            itemView.findViewById<TextView>(R.id.label_workoutTotalRepsValue).text = currentWorkout.totalReps.toString()

            itemView.findViewById<TextView>(R.id.label_workoutTotalSetsValue).text = currentWorkout.totalSets.toString()

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

            itemView.setOnClickListener {
                if (adapterPosition == currentlyShowingActionFrameIndex) {
                    currentlyShowingActionFrameIndex = null
                    notifyItemChanged(adapterPosition)
                    return@setOnClickListener
                }

                ContextCompat.startActivity(
                    itemView.context, Intent(
                        itemView.context.applicationContext, WorkoutActivity::class.java
                    ).setAction("oldWorkoutStartedFromHome").putExtra("workoutId", currentWorkout.workoutId), null
                )
            }

            itemView.setOnLongClickListener {
                if (currentlyShowingActionFrameIndex == adapterPosition) {
                    // hide if we're the one showing it
                    currentlyShowingActionFrameIndex = null
                } else {
                    val oldIndex = currentlyShowingActionFrameIndex
                    currentlyShowingActionFrameIndex = adapterPosition
                    if (oldIndex != null) notifyItemChanged(oldIndex)
                }
                notifyItemChanged(adapterPosition)
                true
            }

            itemView.findViewById<Button>(R.id.btnDeleteWorkout).setOnClickListener {
                val db = ExerciseDatabase.getInstance(itemView.context)
                runBlocking {
                    db.exerciseDao().deleteWorkout(dataList[adapterPosition])
                    db.exerciseDao().deleteExercisesInWorkout(dataList[adapterPosition].workoutId)
                }
                removeItemAt(adapterPosition)
                currentlyShowingActionFrameIndex = null
            }

            itemView.findViewById<Button>(R.id.btnDuplicateWorkout).setOnClickListener {
                val db = ExerciseDatabase.getInstance(itemView.context).exerciseDao()
                val today = Instant.now()

                runBlocking {
                    val workout = Workout(0, today.toEpochMilli())
                    workout.workoutId = db.addWorkout(workout)
                    var exercisesAdded = 0
                    db.getListOfExerciseInWorkout(currentWorkout.workoutId).map { exercise ->
                        db.addExerciseSet(
                            Exercise(
                                0,
                                exercise.exerciseModelId,
                                today.atZone(ZoneId.systemDefault()).toLocalDate(),
                                exercisesAdded++,
                                workout.workoutId
                            )
                        )
                    }
                    workout.totalExercises = currentWorkout.totalExercises
                    workout.topTags = currentWorkout.topTags
                    db.updateWorkout(workout)

                    Toast.makeText(
                        itemView.context, "Copied $exercisesAdded exercises from ${
                            Converters.dateFormatter.format(
                                Instant.ofEpochMilli(currentWorkout.date).atZone(ZoneId.systemDefault()).toLocalDate()
                            )
                        }", Toast.LENGTH_SHORT
                    ).show()

                    currentlyShowingActionFrameIndex = null
                    notifyItemChanged(adapterPosition)

                    addNewItem(workout)
                    (itemView.parent as RecyclerView).scrollToPosition(0)
                }
            }
        }
    }

    override fun getItemCount() = displayList.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view: View = LayoutInflater.from(parent.context).inflate(R.layout.row_workout, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(displayList[position])
    }
}
