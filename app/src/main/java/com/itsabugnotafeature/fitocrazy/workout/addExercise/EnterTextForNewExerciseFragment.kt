package com.itsabugnotafeature.fitocrazy.workout.addExercise

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.core.os.bundleOf
import androidx.core.widget.doOnTextChanged
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.setFragmentResult
import com.itsabugnotafeature.fitocrazy.R


class EnterTextForNewExerciseFragment(private val hintForExerciseComponentType: String) : DialogFragment() {

    private var userInputtedString: String? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.new_exercise_fragment_enter_text_for_new_exercise, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        view.findViewById<TextView>(R.id.label_enterNewExerciseComponentText).text = getString(R.string.label_enterNewExerciseComponentText, hintForExerciseComponentType.lowercase())
        val editText = view.findViewById<EditText>(R.id.textEntry_newExerciseComponentText)

        editText.hint = hintForExerciseComponentType
        editText.requestFocus()
        requireDialog().window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE)

        editText.doOnTextChanged { text, _, _, _ ->
            userInputtedString = text?.toString()
        }

        view.findViewById<Button>(R.id.btn_EnterExerciseComponentText).setOnClickListener {
            dismiss()
        }
    }

    override fun dismiss() {
        setFragmentResult("exerciseTextEntered", bundleOf("userInputtedString" to userInputtedString?.trim()?.uppercase()))
        super.dismiss()
    }

    companion object {
        const val TAG = "EnterTextForNewExerciseFragment_dialog"
    }
}