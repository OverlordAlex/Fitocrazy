package com.itsabugnotafeature.fitocrazy.ui.exercises_and_components

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.itsabugnotafeature.fitocrazy.R
import com.itsabugnotafeature.fitocrazy.common.Converters
import com.itsabugnotafeature.fitocrazy.common.DisplayListAdapter
import com.itsabugnotafeature.fitocrazy.common.ExerciseDatabase
import com.itsabugnotafeature.fitocrazy.ui.exercises_and_components.ExerciseFragment.ExerciseView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import java.time.LocalDate

class ExerciseDisplayListAdapter : RecyclerView.Adapter<ExerciseDisplayListAdapter.ViewHolder>(),
    DisplayListAdapter<ExerciseView> {
    override var dataList = emptyList<ExerciseView>().toMutableList()
    override var displayList = emptyList<ExerciseView>().toMutableList()

    override suspend fun loadData(applicationContext: Context) {
        if (dataList.isNotEmpty()) throw InstantiationException("Component list already populated!")

        withContext(Dispatchers.IO) {
            val db = ExerciseDatabase.getInstance(applicationContext).exerciseDao()
            dataList = db.getExercises().map {
                ExerciseView(
                    it.exercise,
                    db.getLastExerciseOccurrence(it.exercise.exerciseId)?.date,
                    db.getRecord(it.exercise.exerciseId),
                    db.getExerciseCount(it.exercise.exerciseId)
                )
            }.sorted().toMutableList()
            displayList.addAll(dataList)
            notifyItemRangeInserted(0, displayList.size)
        }
    }

    override fun filterDataList(filter: String): List<ExerciseView> {
        return dataList.filter { exerciseView ->
            exerciseView.exercise.displayName.contains(filter) || (exerciseView.exercise.bodyPartChips?.uppercase()
                ?.contains(filter) == true)
        }.toMutableList()
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        fun bind(exerciseView: ExerciseView) {
            itemView.findViewById<Chip>(R.id.chip_exerciseCount).text = exerciseView.total?.toString() ?: "0"

            itemView.findViewById<TextView>(R.id.label_exerciseName).text =
                exerciseView.exercise.displayName

            val maxWeight = itemView.findViewById<TextView>(R.id.label_exerciseMaxWeight)
            if (exerciseView.record?.maxWeight == null) {
                maxWeight.visibility = TextView.GONE
            } else {
                maxWeight.text = itemView.context.getString(R.string.exercise_max_weight, exerciseView.record.maxWeight)
            }

            val maxReps = itemView.findViewById<TextView>(R.id.label_exerciseMaxReps)
            if (exerciseView.record?.maxReps == null) {
                maxReps.visibility = TextView.GONE
            } else {
                maxReps.text = itemView.context.getString(R.string.exercise_max_reps, exerciseView.record.maxReps)
            }

            val maxMoved = itemView.findViewById<TextView>(R.id.label_exerciseWeightMoved)
            if (exerciseView.record?.mostWeightMoved == null) {
                maxMoved.visibility = TextView.GONE
            } else {
                maxMoved.text =
                    itemView.context.getString(R.string.exercise_max_moved, exerciseView.record.mostWeightMoved)
            }

            val actionButton = itemView.findViewById<Button>(R.id.btn_deleteExercise)
            val lastSeen = itemView.findViewById<TextView>(R.id.label_exerciseLastOccurrence)
            if (exerciseView.lastWorkout == null) {
                lastSeen.text = itemView.context.getString(R.string.exercise_last_seen_never)
            } else {
                //actionButton.text = getString(R.string.btn_edit)
                actionButton.visibility = Button.GONE
                val today = LocalDate.now()
                lastSeen.text = if (today == exerciseView.lastWorkout) {
                    itemView.context.getString(
                        R.string.exercise_last_seen_today,
                        exerciseView.lastWorkout!!.format(Converters.dateFormatter)
                    )
                } else {
                    itemView.context.getString(
                        R.string.exercise_last_seen,
                        exerciseView.lastWorkout!!.format(Converters.dateFormatter),
                        today.toEpochDay() - exerciseView.lastWorkout!!.toEpochDay()
                    )
                }
            }

            val chipGroup = itemView.findViewById<ChipGroup>(R.id.chipgroup_Bodyparts)
            chipGroup.removeAllViews()
            chipGroup.visibility = ChipGroup.VISIBLE
            exerciseView.exercise.bodyPartChips?.split(" ")?.forEach { chipName ->
                val newChip = Chip(itemView.context)
                newChip.text = chipName
                newChip.setChipBackgroundColorResource(R.color.blue_accent_light)
                chipGroup.addView(newChip)
            }


            actionButton.setOnClickListener {
                if (exerciseView.lastWorkout == null) {
                    runBlocking {
                        val db = ExerciseDatabase.getInstance(itemView.context).exerciseDao()
                        withContext(Dispatchers.IO) {
                            db.deleteExerciseModel(exerciseView.exercise.exerciseId)
                            db.deleteExerciseComponentCrossRefByExerciseId(exerciseView.exercise.exerciseId)
                        }
                        removeItemAt(adapterPosition)
                    }
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val row: View =
            LayoutInflater.from(parent.context)
                .inflate(R.layout.row_exercise_component, parent, false)
        return ViewHolder(row)
    }

    override fun getItemCount() = displayList.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(displayList[position])
    }

}