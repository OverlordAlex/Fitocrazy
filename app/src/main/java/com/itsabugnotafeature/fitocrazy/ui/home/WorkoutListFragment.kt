package com.itsabugnotafeature.fitocrazy.ui.home

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.itsabugnotafeature.fitocrazy.R
import com.itsabugnotafeature.fitocrazy.common.AddSetNotificationManager
import com.itsabugnotafeature.fitocrazy.ui.home.workout.WorkoutActivity
import kotlinx.coroutines.runBlocking


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
            AddSetNotificationManager.NOTIFICATION_ID)

        val workoutListView = requireView().findViewById<RecyclerView>(R.id.list_allWorkoutsHomepage)
        val workoutListViewAdapter = WorkoutListViewAdapter()
        workoutListView.adapter = workoutListViewAdapter
        workoutListView.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false)

        runBlocking {
            workoutListViewAdapter.loadData(requireContext())
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