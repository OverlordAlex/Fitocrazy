package com.itsabugnotafeature.fitocrazy.ui.home.workout.addExercise

import android.content.Context
import android.util.Log
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
import com.itsabugnotafeature.fitocrazy.common.MostCommonExerciseView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDate

class SuggestedExercisesListAdapter() :
    RecyclerView.Adapter<SuggestedExercisesListAdapter.ViewHolder>(),
    DisplayListAdapter<MostCommonExerciseView> {

    override var dataList = emptyList<MostCommonExerciseView>().toMutableList()
    override var displayList = emptyList<MostCommonExerciseView>().toMutableList()

    private val selectedItems: MutableList<Long> = mutableListOf()

    override suspend fun loadData(applicationContext: Context, arguments: Map<String, Any>?) {
        if (dataList.isNotEmpty()) throw InstantiationException("Suggested exercises already populated!")
        //Log.i("TEXT", "loading data for ${(arguments?.get("currentExercisesInWorkout") as LongArray).toList()}")

        val db = ExerciseDatabase.getInstance(applicationContext).exerciseDao()
        var currentExercises = arguments?.get("currentExercisesInWorkout")
        currentExercises = if (currentExercises is Array<*> && currentExercises.isArrayOf<Long>()) {
            currentExercises.toList()
        } else {
            emptyList<Long>()
        }
        withContext(Dispatchers.IO) {
            dataList = db.getMostCommonExercises(
                LocalDate.now(),
                currentExercises as List<Long>
            ).toMutableList()
        }
        displayList.addAll(dataList)
        notifyItemRangeInserted(0, displayList.size)
    }

    override fun filterDataList(filter: String): List<MostCommonExerciseView> {
        val upperQ = filter.uppercase()
        val lowerQ = filter.lowercase()
        return dataList.filter {
            it.bodyPartChips.contains(lowerQ) || upperQ.split(" ")
                .all { filterWord -> it.displayName.contains(filterWord) }
        }
    }

    fun getSelectedItems(): List<Long> = selectedItems.toList()
    fun hasSuggestions(): Boolean = displayList.isNotEmpty()

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        fun bind(suggestion: MostCommonExerciseView) {
            itemView.findViewById<TextView>(R.id.label_exerciseSuggestionName).text = suggestion.displayName

            val lastSeenLabel = itemView.findViewById<TextView>(R.id.label_exerciseSuggestionLastSeen)
            if (suggestion.date == null) {
                lastSeenLabel.visibility = TextView.GONE
            } else {
                lastSeenLabel.text = itemView.context.getString(
                    R.string.add_exercise_days_ago,
                    suggestion.date.format(Converters.dateFormatter),
                    LocalDate.now().toEpochDay() - suggestion.date.toEpochDay()
                )
            }

            val chipGroup = itemView.findViewById<ChipGroup>(R.id.chipgroup_suggestedExerciseBodyParts)
            if (chipGroup.childCount == 0) {
                suggestion.bodyPartChips.split(" ").sorted().forEach { chipName ->
                    val newChip = Chip(itemView.context)
                    newChip.text = chipName
                    newChip.setChipBackgroundColorResource(R.color.blue_accent_light)
                    chipGroup.addView(newChip)
                }
            }

            val btn = itemView.findViewById<Button>(R.id.btn_exerciseSuggestionAddExercise)
            btn.setOnClickListener {
                if (suggestion.exerciseModelId in selectedItems) {
                    selectedItems.remove(suggestion.exerciseModelId)
                    //itemView.isSelected = false // TODO: necessary?

                } else {
                    selectedItems.add(suggestion.exerciseModelId)
                    //itemView.isSelected = true
                }

                notifyItemChanged(adapterPosition)
            }

            if (suggestion.exerciseModelId in selectedItems) {
                btn.text = itemView.context.getString(R.string.btn_minus)
                itemView.setBackgroundColor(itemView.context.getColor(R.color.blue_tertiary))
                itemView.elevation = 0f
            } else {
                btn.text = itemView.context.getString(R.string.btn_add)
                itemView.setBackgroundColor(itemView.context.getColor(R.color.white))
                itemView.elevation = 4f
            }

        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view: View = LayoutInflater.from(parent.context)
            .inflate(R.layout.row_add_new_exercise_to_workout_suggestion, parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount() = displayList.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(displayList[position])
    }

}