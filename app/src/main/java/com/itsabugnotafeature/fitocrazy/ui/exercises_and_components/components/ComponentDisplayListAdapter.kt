package com.itsabugnotafeature.fitocrazy.ui.exercises_and_components.components

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentTransaction
import androidx.fragment.app.setFragmentResultListener
import androidx.recyclerview.widget.RecyclerView
import com.itsabugnotafeature.fitocrazy.R
import com.itsabugnotafeature.fitocrazy.common.ExerciseComponentModel
import com.itsabugnotafeature.fitocrazy.common.ExerciseComponentType
import com.itsabugnotafeature.fitocrazy.common.ExerciseDatabase
import com.itsabugnotafeature.fitocrazy.common.DisplayListAdapter
import com.itsabugnotafeature.fitocrazy.ui.exercises_and_components.components.ComponentFragment.ComponentView
import com.itsabugnotafeature.fitocrazy.ui.home.workout.addExercise.EnterTextForNewExerciseFragment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext

class ComponentDisplayListAdapter(val type: ExerciseComponentType) :
    RecyclerView.Adapter<ComponentDisplayListAdapter.ViewHolder>(), DisplayListAdapter<ComponentView> {
    override var dataList = emptyList<ComponentView>().toMutableList()
    override var displayList = emptyList<ComponentView>().toMutableList()

    override suspend fun loadData(applicationContext: Context, arguments: Map<String, Any>?) {
        if (dataList.isNotEmpty()) throw InstantiationException("Component list already populated!")

        withContext(Dispatchers.IO) {
            val db = ExerciseDatabase.getInstance(applicationContext).exerciseDao()
            dataList = db.getExerciseComponent(type).map {
                ComponentView(
                    it.componentId,
                    it.name,
                    db.getExerciseDetailsWithComponent(it.componentId)
                )
            }.sorted().toMutableList()
            displayList.addAll(dataList)
            notifyItemRangeInserted(0, displayList.size)
        }
    }

    override fun filterDataList(filter: String): List<ComponentView> {
        return dataList.filter { componentView ->
            componentView.name.contains(filter) || (componentView.exercises.find {
                filter.split(" ").all { filterWord -> it.displayName.contains(filterWord) }
            } != null)
        }
    }

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
                    removeItemAt(adapterPosition)
                }
            } else {
                actionButton.text = itemView.context.getString(R.string.btn_rename)
                actionButton.setOnClickListener {
                    val application = itemView.context as AppCompatActivity
                    val textFrag: DialogFragment = EnterTextForNewExerciseFragment(type.toString(), component.name)
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
                                val db = ExerciseDatabase.getInstance(itemView.context).exerciseDao()
                                val newComponent = ExerciseComponentModel(component.id, enteredText, type)
                                withContext(Dispatchers.IO) {
                                    db.updateExerciseComponent(newComponent)

                                    val exercises = db.getExercisesUsingComponent(component.id)
                                    for (exerciseId in exercises) {
                                        val newDisplayName =
                                            db.getExerciseDetails(exerciseId)!!.components.joinToString(" ") {
                                                if (it.type == type) enteredText else it.name
                                            }
                                        db.updateExerciseDisplayName(exerciseId, newDisplayName)
                                    }
                                }
                                replaceItemAt(
                                    adapterPosition,
                                    ComponentView(
                                        newComponent,
                                        db.getExerciseDetailsWithComponent(newComponent.componentId)
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view: View = LayoutInflater.from(parent.context).inflate(R.layout.row_component, parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount() = displayList.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(displayList[position])
    }

}
