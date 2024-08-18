package com.itsabugnotafeature.fitocrazy.ui.exercises_and_components

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentTransaction
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.tabs.TabLayout
import com.itsabugnotafeature.fitocrazy.R
import com.itsabugnotafeature.fitocrazy.common.ExerciseComponentModel
import com.itsabugnotafeature.fitocrazy.common.ExerciseComponentType
import com.itsabugnotafeature.fitocrazy.common.ExerciseDatabase
import com.itsabugnotafeature.fitocrazy.common.ExerciseModel
import kotlinx.coroutines.runBlocking

class ExerciseAndComponents : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_exercise_and_components, container, false)
    }

    data class ComponentView(
        val name: String,
        val exercises: List<ExerciseModel>,
    )

    class ComponentFragment(val type: ExerciseComponentType?) : Fragment() {
        class ComponentListAdapter(
            private var componentList: List<ComponentView>
        ) : RecyclerView.Adapter<ComponentListAdapter.ViewHolder>() {
            inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
                fun bind(component: ComponentView) {
                    itemView.findViewById<TextView>(R.id.label_componentName).text = component.name
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
            view.layoutParams = FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT)

            view.findViewById<TextView>(R.id.label_componentType).text = type?.name ?: "Unknown Type"

            if (type == null) return view

            val listOfComponents = view.findViewById<RecyclerView>(R.id.list_exerciseComponents)

            val listOfComponentsOfThisType: List<ComponentView>
            runBlocking {
                val db = ExerciseDatabase.getInstance(requireContext())
                listOfComponentsOfThisType = db.exerciseDao().getExerciseComponent(type).map { ComponentView(it.name, emptyList()) }
            }

            listOfComponents.adapter = ComponentListAdapter(listOfComponentsOfThisType)
            listOfComponents.layoutManager =  LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false)


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
                val fragmentType: ExerciseComponentType? = when (tab?.position) {
                    0 -> null // Exercises
                    1 -> ExerciseComponentType.EQUIPMENT // Equipment
                    2 -> ExerciseComponentType.LOCATION// Location
                    3 -> ExerciseComponentType.MOVEMENT// Movement
                    else -> null
                }

                val fragmentTransaction = childFragmentManager.beginTransaction()
                fragmentTransaction.replace(frame.id, ComponentFragment(fragmentType))
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
    }
}