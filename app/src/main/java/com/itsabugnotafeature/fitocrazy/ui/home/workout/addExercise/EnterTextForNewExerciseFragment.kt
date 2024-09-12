package com.itsabugnotafeature.fitocrazy.ui.home.workout.addExercise

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.WindowManager.LayoutParams
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.core.os.bundleOf
import androidx.core.widget.doOnTextChanged
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.setFragmentResult
import com.itsabugnotafeature.fitocrazy.R


class EnterTextForNewExerciseFragment(
    private val hintForExerciseComponentType: String,
    private val existingComponentText: String? = null
) : DialogFragment() {

    private var userInputtedString: String? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.dialog_enter_text_for_new_exercise, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        dialog?.window?.setLayout(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)

        view.findViewById<TextView>(R.id.label_enterNewExerciseComponentText).text =
            getString(R.string.label_enterNewExerciseComponentText, hintForExerciseComponentType.lowercase())
        val editText = view.findViewById<EditText>(R.id.textEntry_newExerciseComponentText)

        if (!existingComponentText.isNullOrBlank()) editText.setText(existingComponentText)

        editText.hint = hintForExerciseComponentType
        editText.requestFocus()
        requireDialog().window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE)

        editText.doOnTextChanged { text, _, _, _ ->
            userInputtedString = text?.toString()
        }

        view.findViewById<Button>(R.id.btn_EnterExerciseComponentText).setOnClickListener {
            dismiss()
        }

        dialog?.setCanceledOnTouchOutside(true)
    }

    override fun dismiss() {
        setFragmentResult(
            "exerciseTextEntered",
            bundleOf("userInputtedString" to userInputtedString?.trim()?.uppercase())
        )
        super.dismiss()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NO_TITLE, R.style.Dialog_NewExercise)
    }

    companion object {
        const val TAG = "EnterTextForNewExerciseFragment_dialog"
    }
}