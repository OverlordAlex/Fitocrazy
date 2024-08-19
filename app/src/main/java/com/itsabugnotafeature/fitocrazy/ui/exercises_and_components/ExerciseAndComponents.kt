package com.itsabugnotafeature.fitocrazy.ui.exercises_and_components

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.SearchView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentTransaction
import androidx.fragment.app.setFragmentResultListener
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.tabs.TabLayout
import com.itsabugnotafeature.fitocrazy.R
import com.itsabugnotafeature.fitocrazy.common.Exercise
import com.itsabugnotafeature.fitocrazy.common.ExerciseComponentModel
import com.itsabugnotafeature.fitocrazy.common.ExerciseComponentType
import com.itsabugnotafeature.fitocrazy.common.ExerciseDatabase
import com.itsabugnotafeature.fitocrazy.common.ExerciseModel
import com.itsabugnotafeature.fitocrazy.common.SetRecordView
import com.itsabugnotafeature.fitocrazy.common.Workout
import com.itsabugnotafeature.fitocrazy.ui.home.workout.addExercise.EnterTextForNewExerciseFragment
import kotlinx.coroutines.runBlocking
import org.w3c.dom.Text
import kotlin.math.abs
import kotlin.math.min

class ExerciseAndComponents : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_exercise_and_components, container, false)
    }

    data class ExerciseView(
        val exercise: ExerciseModel,
        var workouts: List<Exercise>,
        val record: SetRecordView?,
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

    class ExerciseFragment() : Fragment() {
        lateinit var listOfExercises: MutableList<ExerciseView>

        override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
            val view = inflater.inflate(R.layout.fragment_exercise_component, container, false)

            view.findViewById<TextView>(R.id.label_componentType).text = "EXERCISES"

            val exerciseListView = view.findViewById<RecyclerView>(R.id.list_exerciseComponents)

            runBlocking {
                val db = ExerciseDatabase.getInstance(requireContext())
                listOfExercises = db.exerciseDao().getExercises().map {
                    ExerciseView(
                        it.exercise,
                        db.exerciseDao().getWorkoutsWithExercise(it.exercise.exerciseId),
                        db.exerciseDao().getRecord(it.exercise.exerciseId)
                    )
                }.sorted().toMutableList()
            }

            class ExerciseListAdapter(
                var exerciseList: MutableList<ExerciseView>
            ): RecyclerView.Adapter<ExerciseListAdapter.ViewHolder>() {
                inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
                    fun bind(exerciseView: ExerciseView) {
                        itemView.findViewById<TextView>(R.id.label_exerciseName).text = exerciseView.exercise.displayName
                        itemView.findViewById<TextView>(R.id.label_exerciseMaxWeight).text = exerciseView.record?.maxWeight.toString() // TODO FORMAT
                        itemView.findViewById<TextView>(R.id.label_exerciseMaxReps).text = exerciseView.record?.maxReps.toString()
                        itemView.findViewById<TextView>(R.id.label_exerciseWeightMoved).text = exerciseView.record?.mostWeightMoved.toString()
                    }
                }

                override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
                    val row: View =
                        LayoutInflater.from(parent.context).inflate(R.layout.fragment_exercise_component_exerciserow, parent, false)
                    return ViewHolder(row)
                }

                override fun getItemCount() = exerciseList.size

                override fun onBindViewHolder(holder: ViewHolder, position: Int) {
                    holder.bind(exerciseList[position])
                }
            }

            val exerciseListAdapter = ExerciseListAdapter(listOfExercises)
            exerciseListView.adapter = exerciseListAdapter
            exerciseListView.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false)

            return view
        }
    }



    data class ComponentView(
        val id: Long,
        var name: String,
        var exercises: List<ExerciseModel>,
    ) : Comparable<ComponentView> {
        override fun compareTo(other: ComponentView) = this.name.compareTo(other.name)
        override fun equals(other: Any?): Boolean {
            if (other is ComponentView) return this.id == other.id
            return super.equals(other)
        }

        override fun hashCode(): Int {
            return id.hashCode()
        }
    }

    class ComponentFragment(private val type: ExerciseComponentType?) : Fragment() {
        lateinit var listOfComponentsOfThisType: MutableList<ComponentView>

        constructor() : this(null)

        interface ComponentChangeNotifier {
            fun componentAdded(component: ComponentView)
            fun componentRemoved(component: ComponentView)
            fun componentRenamed(component: ComponentView)
        }

        class ComponentListAdapter(
            var componentList: MutableList<ComponentView>,
            val type: ExerciseComponentType,
            val changeNotifier: ComponentChangeNotifier
        ) : RecyclerView.Adapter<ComponentListAdapter.ViewHolder>() {
            inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
                fun bind(component: ComponentView) {
                    itemView.findViewById<TextView>(R.id.label_componentName).text = component.name

                    val exerciseList = itemView.findViewById<LinearLayout>(R.id.layout_listExercisesUsingComponent)
                    exerciseList.removeAllViews()
                    component.exercises.forEach {
                        val exercise = TextView(itemView.context)
                        exercise.text = it.displayName
                        exerciseList.addView(exercise)
                    }

                    val actionButton = itemView.findViewById<Button>(R.id.btn_deleteComponent)
                    if (component.exercises.isEmpty()) {
                        actionButton.text = itemView.context.getString(R.string.btn_minus)
                        actionButton.setOnClickListener {
                            runBlocking {
                                ExerciseDatabase.getInstance(itemView.context).exerciseDao()
                                    .deleteExerciseComponent(component.id)
                            }
                            componentList.remove(component)
                            notifyItemRemoved(adapterPosition)
                            changeNotifier.componentRemoved(component)
                        }
                    } else {
                        actionButton.text = itemView.context.getString(R.string.btn_rename)
                        actionButton.setOnClickListener {
                            val application = itemView.context as AppCompatActivity
                            val textFrag: DialogFragment =
                                EnterTextForNewExerciseFragment(type.toString(), component.name)
                            val ft: FragmentTransaction = application.supportFragmentManager.beginTransaction()
                            val prev: Fragment? = application.supportFragmentManager.findFragmentByTag(textFrag.tag)
                            if (prev != null) {
                                ft.remove(prev)
                            }
                            ft.addToBackStack(null)

                            textFrag.show(ft, textFrag.tag)
                            textFrag.setFragmentResultListener("exerciseTextEntered") { _, bundle ->
                                val enteredText: String? = bundle.getString("userInputtedString")
                                if (!enteredText.isNullOrBlank() && enteredText != component.name) {
                                    runBlocking {
                                        val newComponent = ExerciseComponentModel(component.id, enteredText, type)
                                        val db = ExerciseDatabase.getInstance(itemView.context).exerciseDao()
                                        db.updateExerciseComponent(newComponent)
                                        val exercises = db.getExercisesUsingComponent(component.id)
                                        for (exerciseId in exercises) {
                                            val newDisplayName =
                                                db.getExerciseDetails(exerciseId)!!.components.joinToString(" ") {
                                                    if (it.type == type) enteredText else it.name
                                                }
                                            db.updateExerciseDisplayName(exerciseId, newDisplayName)
                                        }

                                        component.name = enteredText
                                        component.exercises = db.getExerciseDetailsWithComponent(component.id)

                                        val idx = componentList.sorted().indexOf(component)
                                        if (idx != adapterPosition) {
                                            // have to reload view if things are resorted
                                            componentList.sort()
                                            //notifyItemRangeChanged(min(idx, adapterPosition), abs(adapterPosition-idx))
                                            notifyDataSetChanged()
                                        } else {
                                            notifyItemChanged(adapterPosition)
                                        }

                                        changeNotifier.componentRenamed(component)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
                val view: View =
                    LayoutInflater.from(parent.context).inflate(R.layout.fragment_exercise_component_row, parent, false)
                return ViewHolder(view)
            }

            override fun getItemCount() = componentList.size

            override fun onBindViewHolder(holder: ViewHolder, position: Int) {
                holder.bind(componentList[position])
            }

        }

        override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
            val view = inflater.inflate(R.layout.fragment_exercise_component, container, false)

            view.findViewById<TextView>(R.id.label_componentType).text = type?.name ?: "Unknown Type"
            if (type == null) return view

            val listOfComponents = view.findViewById<RecyclerView>(R.id.list_exerciseComponents)

            runBlocking {
                val db = ExerciseDatabase.getInstance(requireContext())
                listOfComponentsOfThisType = db.exerciseDao().getExerciseComponent(type).map {
                    ComponentView(
                        it.componentId,
                        it.name,
                        db.exerciseDao().getExerciseDetailsWithComponent(it.componentId)
                    )
                }.sorted().toMutableList()
            }

            class ChangeNotifier: ComponentChangeNotifier {
                override fun componentAdded(component: ComponentView) {
                    listOfComponentsOfThisType.add(component)
                }

                override fun componentRemoved(component: ComponentView) {
                    listOfComponentsOfThisType.remove(component)
                }

                override fun componentRenamed(component: ComponentView) {
                    listOfComponentsOfThisType.replaceAll { if (it.id == component.id) component else it  }
                }
            }

            val changeNotifier = ChangeNotifier()
            val componentListAdapter = ComponentListAdapter(listOfComponentsOfThisType, type, changeNotifier)
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
                                val componentView = ComponentView(compID, enteredText, emptyList())
                                componentListAdapter.componentList.add(componentView)
                                componentListAdapter.componentList.sort()
                                componentListAdapter.notifyItemInserted(
                                    componentListAdapter.componentList.indexOf(
                                        componentView
                                    )
                                )
                                changeNotifier.componentAdded(componentView)
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

                // could change any number of items on filter, could be more efficient with SortedListAdapter
                //          https://stackoverflow.com/questions/30398247/how-to-filter-a-recyclerview-with-a-searchview
                @SuppressLint("NotifyDataSetChanged")
                override fun onQueryTextChange(query: String?): Boolean {
                    if (query != null) {
                        val upperQ = query.uppercase()
                        componentListAdapter.componentList = listOfComponentsOfThisType.filter { componentView ->
                            componentView.name.contains(upperQ) || (componentView.exercises.find {
                                it.displayName.contains(upperQ)
                            } != null)
                        }.toMutableList()
                        componentListAdapter.notifyDataSetChanged()
                        listOfComponents.scrollToPosition(0)
                        return true
                    }

                    return false
                }

            }
            autoComplete.setOnQueryTextListener(QueryTextChangedListener())

            return view
        }
    }

    override fun onResume() {
        super.onResume()

        val view = requireView()

        val frame = view.findViewById<FrameLayout>(R.id.frame_exerciseAndComponentsContent)
        val tabs = view.findViewById<TabLayout>(R.id.tabs_exercisesAndComponents)

        class TabSelectedListener : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                val fragmentTransaction = childFragmentManager.beginTransaction()

                val fragmentType: Fragment? = when (tab?.position) {
                    0 -> ExerciseFragment() // Exercises
                    1 -> ComponentFragment(ExerciseComponentType.EQUIPMENT)
                    2 -> ComponentFragment(ExerciseComponentType.LOCATION)
                    3 -> ComponentFragment(ExerciseComponentType.MOVEMENT)
                    else -> null
                }

                fragmentTransaction.replace(frame.id, fragmentType!!)
                fragmentTransaction.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
                fragmentTransaction.commit()
            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {
                // TODO: free memory of the old fragment?
            }

            override fun onTabReselected(tab: TabLayout.Tab?) {
                // TODO: refresh page/db for funsies?
            }
        }

        tabs.addOnTabSelectedListener(TabSelectedListener())
        tabs.selectTab(tabs.getTabAt(0))

        val fragmentTransaction = childFragmentManager.beginTransaction()
        fragmentTransaction.replace(frame.id, ExerciseFragment())
        fragmentTransaction.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
        fragmentTransaction.commit()
    }
}