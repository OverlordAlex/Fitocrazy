package com.itsabugnotafeature.fitocrazy.workout.addexercise

import androidx.fragment.app.viewModels
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import com.itsabugnotafeature.fitocrazy.R

class EnterTextForNewExerciseFragment : DialogFragment() {



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // TODO: Use the ViewModel
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_enter_text_for_new_exercise, container, false)
    }

    companion object {
        const val TAG = "EnterTextForNewExerciseFragment_dialog"
    }
}