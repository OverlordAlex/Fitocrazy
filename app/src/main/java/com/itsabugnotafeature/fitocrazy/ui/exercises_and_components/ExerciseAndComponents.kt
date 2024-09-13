package com.itsabugnotafeature.fitocrazy.ui.exercises_and_components

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentTransaction
import com.google.android.material.tabs.TabLayout
import com.itsabugnotafeature.fitocrazy.R
import com.itsabugnotafeature.fitocrazy.common.ExerciseComponentType
import com.itsabugnotafeature.fitocrazy.ui.exercises_and_components.components.ComponentFragment
import com.itsabugnotafeature.fitocrazy.ui.exercises_and_components.exercises.ExerciseFragment

class ExerciseAndComponents : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_exercise_and_components, container, false)
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
        // TODO needed? fragmentTransaction.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
        fragmentTransaction.commit()
    }
}