package com.itsabugnotafeature.fitocrazy.workout.addexercise

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


class EnterTextForNewExerciseFragment(val enterText: String) : DialogFragment() {

    private var returnString: String? = ""

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_enter_text_for_new_exercise, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        view.findViewById<TextView>(R.id.enterTextText).text = "Create a new ${enterText.lowercase()}:"
        val editText = view.findViewById<EditText>(R.id.newExerciseTextEntry)

        editText.hint = enterText
        editText.requestFocus()
        requireDialog().window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE)

        editText.doOnTextChanged { text, _, _, _ ->
            returnString = text?.toString()
        }

        view.findViewById<Button>(R.id.EnterExerciseTextButton).setOnClickListener {
            dismiss()
        }
    }

    override fun dismiss() {
        setFragmentResult("exerciseTextEntered", bundleOf("newText" to returnString))
        super.dismiss()
    }

    companion object {
        const val TAG = "EnterTextForNewExerciseFragment_dialog"
    }
}