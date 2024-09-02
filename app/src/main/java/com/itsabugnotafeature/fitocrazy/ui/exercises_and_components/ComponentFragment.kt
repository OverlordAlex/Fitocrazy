package com.itsabugnotafeature.fitocrazy.ui.exercises_and_components

import android.graphics.Paint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.SearchView
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentTransaction
import androidx.fragment.app.setFragmentResultListener
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.itsabugnotafeature.fitocrazy.R
import com.itsabugnotafeature.fitocrazy.common.ExerciseComponentModel
import com.itsabugnotafeature.fitocrazy.common.ExerciseComponentType
import com.itsabugnotafeature.fitocrazy.common.ExerciseDatabase
import com.itsabugnotafeature.fitocrazy.common.ExerciseModel
import com.itsabugnotafeature.fitocrazy.ui.home.workout.addExercise.EnterTextForNewExerciseFragment
import kotlinx.coroutines.runBlocking

class ComponentFragment(private val type: ExerciseComponentType?) : Fragment() {
    data class ComponentView(
        val id: Long,
        var name: String,
        var exercises: List<ExerciseModel>,
    ) : Comparable<ComponentView> {
        constructor(component: ExerciseComponentModel, exercises: List<ExerciseModel>) : this(
            component.componentId,
            component.name,
            exercises
        )

        override fun compareTo(other: ComponentView) = this.name.compareTo(other.name)

        override fun equals(other: Any?): Boolean {
            if (other is ComponentView) return this.id == other.id
            return super.equals(other)
        }

        override fun hashCode(): Int {
            return id.hashCode()
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_exercise_component, container, false)

        val componentTypeLabel = view.findViewById<TextView>(R.id.label_componentType)
        componentTypeLabel.paintFlags = componentTypeLabel.paintFlags.or(Paint.UNDERLINE_TEXT_FLAG)
        componentTypeLabel.text = type?.name.toString().lowercase().replaceFirstChar { it.titlecase() }
        if (type == null) return view

        val listOfComponents = view.findViewById<RecyclerView>(R.id.list_exerciseComponents)
        val componentListAdapter = ComponentDisplayListAdapter(type)
        listOfComponents.adapter = componentListAdapter
        listOfComponents.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false)
        val autoComplete = view.findViewById<SearchView>(R.id.search_componentList)

        val newComponentButton = view.findViewById<Button>(R.id.btn_addNewComponent)
        newComponentButton.setOnClickListener {
            val textFrag: DialogFragment =
                EnterTextForNewExerciseFragment(type.toString())
            val ft: FragmentTransaction = childFragmentManager.beginTransaction()
            val prev: Fragment? = childFragmentManager.findFragmentByTag(textFrag.tag)
            if (prev != null) {
                ft.remove(prev)
            }
            ft.addToBackStack(null)

            textFrag.show(ft, textFrag.tag)
            textFrag.setFragmentResultListener("exerciseTextEntered") { _, bundle ->
                val enteredText: String? = bundle.getString("userInputtedString")
                if (!enteredText.isNullOrBlank()) {
                    var component: ExerciseComponentModel?
                    runBlocking {
                        val db = ExerciseDatabase.getInstance(requireContext())
                        component = db.exerciseDao().getExerciseComponent(enteredText, type)
                        if (component == null) {
                            component = ExerciseComponentModel(
                                0,
                                enteredText,
                                type
                            )
                            val compID = db.exerciseDao().addExerciseComponent(component!!)

                            autoComplete.setQuery("", false)
                            autoComplete.clearFocus()
                            componentListAdapter.addNewItem(ComponentView(compID, enteredText, emptyList()))
                        } else {
                            Toast.makeText(requireContext(), "$component already exists", Toast.LENGTH_SHORT).show()
                        }
                    }

                }
            }
        }

        class QueryTextChangedListener : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                return false
            }

            override fun onQueryTextChange(query: String?): Boolean {
                if (query != null) {
                    componentListAdapter.updateDisplayedItems(query.uppercase())
                    return true
                }

                return false
            }
        }
        autoComplete.setOnQueryTextListener(QueryTextChangedListener())

        runBlocking {
            componentListAdapter.loadData(requireContext().applicationContext)
        }

        return view
    }
}
