package com.itsabugnotafeature.fitocrazy.ui.exercises_and_components.exercises

import android.content.Intent
import android.graphics.Paint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.SearchView
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.itsabugnotafeature.fitocrazy.R
import com.itsabugnotafeature.fitocrazy.common.ExerciseModel
import com.itsabugnotafeature.fitocrazy.common.SetRecordView
import com.itsabugnotafeature.fitocrazy.ui.workouts.workout.addExercise.AddNewExerciseToWorkoutActivity
import kotlinx.coroutines.runBlocking
import java.time.LocalDate

class ExerciseFragment: Fragment() {
    data class ExerciseView(
        val exercise: ExerciseModel,
        var lastWorkout: LocalDate?,
        val record: SetRecordView?,
        val total: Int?,
    ) : Comparable<ExerciseView> {
        override fun compareTo(other: ExerciseView) = this.exercise.displayName.compareTo(other.exercise.displayName)
        override fun equals(other: Any?): Boolean {
            if (other is ExerciseView) return this.exercise.exerciseId == other.exercise.exerciseId
            return super.equals(other)
        }

        override fun hashCode(): Int {
            return this.exercise.exerciseId.hashCode()
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_exercise_component, container, false)

        val componentTypeLabel = view.findViewById<TextView>(R.id.label_componentType)
        componentTypeLabel.paintFlags = componentTypeLabel.paintFlags.or(Paint.UNDERLINE_TEXT_FLAG)
        componentTypeLabel.text = "Exercises"

        val exerciseListView = view.findViewById<RecyclerView>(R.id.list_exerciseComponents)

        val exerciseListAdapter = ExerciseDisplayListAdapter(childFragmentManager)
        exerciseListView.adapter = exerciseListAdapter
        exerciseListView.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false)

        val addExerciseResult = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {}

        val newExerciseBtn = view.findViewById<Button>(R.id.btn_addNewComponent)
        newExerciseBtn.setOnClickListener {
            addExerciseResult.launch(
                Intent(
                    requireContext(), AddNewExerciseToWorkoutActivity::class.java
                ).setAction("addNewExerciseFromWorkout")
            )
        }

        class QueryTextChangedListener : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                return false
            }

            override fun onQueryTextChange(query: String?): Boolean {
                if (query != null) {
                    exerciseListAdapter.updateDisplayedItems(query.trim().uppercase())
                    return true
                }

                return false
            }
        }

        val autoComplete = view.findViewById<SearchView>(R.id.search_componentList)
        autoComplete.setOnQueryTextListener(QueryTextChangedListener())

        runBlocking {
            exerciseListAdapter.loadData(requireContext())
        }

        return view
    }
}