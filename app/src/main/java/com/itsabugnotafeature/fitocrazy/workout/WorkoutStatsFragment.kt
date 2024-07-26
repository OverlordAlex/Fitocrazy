package com.itsabugnotafeature.fitocrazy.workout

import androidx.fragment.app.viewModels
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.itsabugnotafeature.fitocrazy.R

class WorkoutStatsFragment : Fragment() {

    companion object {
        fun newInstance() = WorkoutStatsFragment()
    }

    private val viewModel: WorkoutStatsViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // TODO: Use the ViewModel
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_workout_stats, container, false)
    }
}