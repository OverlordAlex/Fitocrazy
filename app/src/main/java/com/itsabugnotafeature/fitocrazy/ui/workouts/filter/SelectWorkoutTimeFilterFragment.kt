package com.itsabugnotafeature.fitocrazy.ui.workouts.filter

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.NumberPicker
import androidx.core.os.bundleOf
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.setFragmentResult
import com.itsabugnotafeature.fitocrazy.R

class SelectWorkoutTimeFilterFragment(private val dataList: List<String>, private val selectedYear: String) : DialogFragment() {

        private var chosenItem: String? = null

        override fun onCreateView(
            inflater: LayoutInflater, container: ViewGroup?,
            savedInstanceState: Bundle?
        ): View {
            return inflater.inflate(R.layout.dialog_workout_list_month_selector, container, false)
        }

        override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
            super.onViewCreated(view, savedInstanceState)

            val selector = view.findViewById<NumberPicker>(R.id.picker_time)
            selector.minValue = 0
            selector.maxValue = dataList.size - 1
            selector.displayedValues = dataList.toTypedArray()
            selector.value = dataList.indexOf(selectedYear)

            selector.setOnClickListener {
                chosenItem = dataList[selector.value]
                dismiss()
            }

            dialog?.setCanceledOnTouchOutside(true)
            //dialog?.window?.setLayout(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
        }

        override fun dismiss() {
            setFragmentResult(
                "chosenItem",
                bundleOf("chosenItem" to chosenItem)
            )
            super.dismiss()
        }

        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            //setStyle(STYLE_NO_TITLE, R.style.Dialog_NewExercise)
        }

        companion object {
            const val TAG = "SelectWorkoutTimeFilterFragment_dialog"
        }
    }