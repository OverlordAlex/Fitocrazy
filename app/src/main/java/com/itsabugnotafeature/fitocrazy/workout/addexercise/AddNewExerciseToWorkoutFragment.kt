package com.itsabugnotafeature.fitocrazy.workout.addexercise

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.Spinner
import androidx.core.os.bundleOf
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentTransaction
import androidx.fragment.app.setFragmentResult
import androidx.fragment.app.setFragmentResultListener
import androidx.lifecycle.lifecycleScope
import com.itsabugnotafeature.fitocrazy.R
import com.itsabugnotafeature.fitocrazy.common.Equipment
import com.itsabugnotafeature.fitocrazy.common.ExerciseDatabase
import com.itsabugnotafeature.fitocrazy.common.Movement
import com.itsabugnotafeature.fitocrazy.common.Position
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

class AddNewExerciseToWorkoutFragment : DialogFragment(), AdapterView.OnItemSelectedListener {

    private enum class EXERCISE_COMPONENTS {
        EQUIPMENT, POSITION, MOVEMENT
    }

    private var equipment: String = ""
    private var position: String = ""
    private var movement: String = ""

    private lateinit var equipmentAdapter: ArrayAdapter<Equipment>
    private lateinit var positionAdapter: ArrayAdapter<Position>
    private lateinit var movementAdapter: ArrayAdapter<Movement>

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

    private fun setupEnterCustomTextDialog(hint:EXERCISE_COMPONENTS, btn: Button, spinner: Spinner) {
        val textFrag: DialogFragment = EnterTextForNewExerciseFragment(hint.toString())

        btn.setOnClickListener {
            val ft: FragmentTransaction = childFragmentManager.beginTransaction()
            val prev: Fragment? = childFragmentManager.findFragmentByTag(textFrag.tag)
            if (prev != null) {
                ft.remove(prev)
            }
            ft.addToBackStack(null)

            textFrag.show(ft, textFrag.tag)
            textFrag.setFragmentResultListener("exerciseTextEntered") { _, bundle ->
                val enteredText: String? = bundle.getString("newText")?.trim()?.uppercase()
                Log.i("test", "$hint ${bundle.getString("newText")}")
                if (!enteredText.isNullOrBlank()) {
                    viewLifecycleOwner.lifecycleScope.launch {
                        val db = ExerciseDatabase.getInstance(requireContext())

                        when (hint) {
                            EXERCISE_COMPONENTS.MOVEMENT -> {
                                val movement = db.exerciseComponentsDao().getMovement(enteredText)
                                if (movement == null) {
                                    db.exerciseComponentsDao().addMovement(Movement(0, enteredText))
                                } else {
                                    spinner.setSelection((spinner.adapter as ArrayAdapter<Movement>).getPosition(movement), false)
                                } // TODO the same for below
                            }

                            EXERCISE_COMPONENTS.POSITION -> db.exerciseComponentsDao()
                                .addPosition(Position(0, enteredText))

                            EXERCISE_COMPONENTS.EQUIPMENT -> db.exerciseComponentsDao()
                                .addEquipment(
                                    Equipment(0, enteredText)
                                )
                        }
                        updateSpinnerData()
                        spinner.setSelection(spinner.adapter.count - 1 )
                    }
                }
            }
        }
    }

    private suspend fun updateSpinnerData() {
        val db = ExerciseDatabase.getInstance(requireContext())
        equipmentAdapter.clear()
        equipmentAdapter.addAll(db.exerciseComponentsDao().getAllEquipment())
        positionAdapter.clear()
        positionAdapter.addAll(db.exerciseComponentsDao().getAllPosition())
        movementAdapter.clear()
        movementAdapter.addAll(db.exerciseComponentsDao().getAllMovement())
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val equipmentSpinner = view.findViewById<Spinner>(R.id.equipment)
        val positionSpinner = view.findViewById<Spinner>(R.id.position)
        val movementSpinner = view.findViewById<Spinner>(R.id.movement)

        runBlocking {
            equipmentAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item);
            positionAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item);
            movementAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item);
            updateSpinnerData()
            equipmentSpinner.adapter = equipmentAdapter
            positionSpinner.adapter = positionAdapter
            movementSpinner.adapter = movementAdapter
        }

        equipmentSpinner.onItemSelectedListener = this
        positionSpinner.onItemSelectedListener = this
        movementSpinner.onItemSelectedListener = this

        setupEnterCustomTextDialog(EXERCISE_COMPONENTS.EQUIPMENT, view.findViewById<Button>(R.id.newEquipment), equipmentSpinner)
        setupEnterCustomTextDialog(EXERCISE_COMPONENTS.POSITION, view.findViewById<Button>(R.id.newPosition), positionSpinner)
        setupEnterCustomTextDialog(EXERCISE_COMPONENTS.MOVEMENT, view.findViewById<Button>(R.id.newMovement), movementSpinner)

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