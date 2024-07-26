package com.itsabugnotafeature.fitocrazy.workout.addexercise

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.Button
import android.widget.Spinner
import androidx.core.os.bundleOf
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentTransaction
import androidx.fragment.app.setFragmentResult
import androidx.fragment.app.setFragmentResultListener
import com.itsabugnotafeature.fitocrazy.R

class AddNewExerciseToWorkoutFragment : DialogFragment(), AdapterView.OnItemSelectedListener {

    private var equipment: String = ""
    private var position: String = ""
    private var movement: String = ""

    override fun onItemSelected(parent: AdapterView<*>, view: View?, pos: Int, id: Long) {
        parent.getItemAtPosition(pos)
        when (parent.id) {
            R.id.equipment -> equipment = parent.getItemAtPosition(pos).toString()
            R.id.position -> position = parent.getItemAtPosition(pos).toString()
            R.id.movement -> movement = parent.getItemAtPosition(pos).toString()
        }
    }

    override fun onNothingSelected(parent: AdapterView<*>) {
        // Another interface callback.
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        view.findViewById<Spinner>(R.id.equipment).onItemSelectedListener = this
        view.findViewById<Spinner>(R.id.position).onItemSelectedListener = this
        view.findViewById<Spinner>(R.id.movement).onItemSelectedListener = this

        val textfrag: DialogFragment = EnterTextForNewExerciseFragment();
        val ft: FragmentTransaction = childFragmentManager.beginTransaction()
        val prev: Fragment? = childFragmentManager.findFragmentByTag(textfrag.tag)
        if (prev != null) {
            ft.remove(prev)
        }
        ft.addToBackStack(null)

        // Create and show the dialog.
        textfrag.show(ft, textfrag.tag)
        textfrag.setFragmentResultListener("exerciseTextEntered") { _, bundle ->
            Log.i("test", "${bundle.getString("equipment")} ${bundle.getString("position")} ${bundle.getString("movement")}")
        }


        val btn: Button = view.findViewById(R.id.addExerciseButton)
        btn.setOnClickListener {
            setFragmentResult("exerciseAdded", bundleOf("equipment" to equipment, "movement" to movement, "position" to position))
            dismiss()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_add_new_exercise_to_workout, container, false)
    }

    companion object {
        const val TAG = "AddNewExerciseToWorkoutFragment_dialog"
    }

}