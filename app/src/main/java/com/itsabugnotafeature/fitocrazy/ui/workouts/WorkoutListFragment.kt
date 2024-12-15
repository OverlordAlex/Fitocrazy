package com.itsabugnotafeature.fitocrazy.ui.workouts

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentTransaction
import androidx.fragment.app.setFragmentResultListener
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.itsabugnotafeature.fitocrazy.R
import com.itsabugnotafeature.fitocrazy.common.AddSetNotificationManager
import com.itsabugnotafeature.fitocrazy.common.ExerciseDatabase
import com.itsabugnotafeature.fitocrazy.ui.workouts.filter.SelectWorkoutTimeFilterFragment
import com.itsabugnotafeature.fitocrazy.ui.workouts.workout.WorkoutActivity
import kotlinx.coroutines.runBlocking
import java.time.ZoneId


class WorkoutListFragment : Fragment() {

    private val channelId = "FitocrazyCurrentExerciseChannel"

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_workout_list_home, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        // Create the NotificationChannel.
        val name = getString(R.string.channel_name)
        val descriptionText = getString(R.string.channel_description)
        val importance = NotificationManager.IMPORTANCE_LOW
        val mChannel = NotificationChannel(channelId, name, importance)
        mChannel.description = descriptionText
        // Register the channel with the system.
        (requireActivity().getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).createNotificationChannel(
            mChannel
        )

        super.onViewCreated(view, savedInstanceState)
    }

    override fun onResume() {
        super.onResume()
        // no workouts may be running on this screen
        (requireActivity().getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).cancel(
            AddSetNotificationManager.NOTIFICATION_ID
        )

        val workoutListView = requireView().findViewById<RecyclerView>(R.id.list_allWorkoutsHomepage)
        workoutListView.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false)

        val yearLabel = requireView().findViewById<TextView>(R.id.label_year)
        //val monthLabel = requireView().findViewById<TextView>(R.id.label_month)

        val dates = runBlocking {
            val db = ExerciseDatabase.getInstance(requireContext()).exerciseDao()
            db.getMonthsPresentInData()
        }
        dates.first().let {
            yearLabel.text = it.year
            //monthLabel.text = it.toDate().month.name
        }
        yearLabel.setOnClickListener {
            val yearFragment: DialogFragment = SelectWorkoutTimeFilterFragment(dates.map { it.year }.toSet().toList(), yearLabel.text.toString())
            val ft: FragmentTransaction = childFragmentManager.beginTransaction()
            val prev: Fragment? = childFragmentManager.findFragmentByTag(yearFragment.tag)
            if (prev != null) {
                ft.remove(prev)
            }
            ft.addToBackStack(null)

            yearFragment.show(ft, yearFragment.tag)
            yearFragment.setFragmentResultListener("chosenItem") { _, bundle ->
                val chosenYear: String? = bundle.getString("chosenItem")
                Log.i("TEXT", "chose $chosenYear")
                val chosenDate = dates.first { it.year === chosenYear }
                yearLabel.text = chosenDate.year
                chosenDate.month = "1"

                runBlocking {
                    workoutListView.adapter = WorkoutListViewAdapter().apply {
                        loadData(
                            requireContext(),
                            mapOf(
                                "start" to chosenDate.toDate().atStartOfDay(ZoneId.systemDefault()).toEpochSecond()*1000,
                                "end" to chosenDate.toDate().plusMonths(12).atStartOfDay(ZoneId.systemDefault()).toEpochSecond()*1000
                            )
                        )
                    }
                }
            }
        }

        /*monthLabel.setOnClickListener {
            val monthFragment: DialogFragment =
                SelectWorkoutTimeFilterFragment(dates.filter { it.year == yearLabel.text }
                    .map { it.toDate().month.name }.toSet().toList())
            val ft: FragmentTransaction = childFragmentManager.beginTransaction()
            val prev: Fragment? = childFragmentManager.findFragmentByTag(monthFragment.tag)
            if (prev != null) {
                ft.remove(prev)
            }
            ft.addToBackStack(null)

            monthFragment.show(ft, monthFragment.tag)
            monthFragment.setFragmentResultListener("chosenItem") { _, bundle ->
                val chosenMonth: String? = bundle.getString("chosenItem")
                val chosenDate = dates.filter { it.year == yearLabel.text }
                    .first { it.toDate().month.name == chosenMonth }
                monthLabel.text = chosenDate.toDate().month.name

                runBlocking {
                    workoutListView.adapter = WorkoutListViewAdapter().apply {
                        loadData(
                            requireContext(),
                            mapOf(
                                "start" to chosenDate.toDate().atStartOfDay(ZoneId.systemDefault()).toEpochSecond()*1000,
                                "end" to chosenDate.toDate().plusMonths(1).atStartOfDay(ZoneId.systemDefault()).toEpochSecond()*1000
                            )
                        )
                    }
                }
            }
        }*/

        /*runBlocking {
            workoutListView.adapter = WorkoutListViewAdapter().apply {
                loadData(
                    requireContext(),
                    mapOf(
                        "start" to dates.first().toDate().atStartOfDay(ZoneId.systemDefault()).toEpochSecond()*1000,
                        "end" to dates.first().toDate().plusMonths(12).atStartOfDay(ZoneId.systemDefault()).toEpochSecond()*1000
                    )
                )
            }
        }*/
        runBlocking {
            val yearToLoad = dates.first()
            yearToLoad.month = "1"

            workoutListView.adapter = WorkoutListViewAdapter().apply {
                loadData(
                    requireContext(),
                    mapOf(
                        "start" to yearToLoad.toDate().atStartOfDay(ZoneId.systemDefault()).toEpochSecond()*1000,
                        "end" to yearToLoad.toDate().plusMonths(12).atStartOfDay(ZoneId.systemDefault()).toEpochSecond()*1000
                    )
                )
            }
        }

        requireView().findViewById<Button>(R.id.btn_AddNewWorkout)?.setOnClickListener {
            startActivity(
                Intent(
                    requireActivity(), WorkoutActivity::class.java
                ).setAction("newWorkoutStartedFromHome")
            )
        }
    }

}