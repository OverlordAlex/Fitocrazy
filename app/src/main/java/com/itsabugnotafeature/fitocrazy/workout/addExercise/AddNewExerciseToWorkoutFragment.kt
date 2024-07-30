package com.itsabugnotafeature.fitocrazy.workout.addExercise

import android.graphics.Color
import android.os.Bundle
import android.transition.AutoTransition
import android.transition.TransitionManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager.LayoutParams
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Button
import android.widget.Spinner
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.os.bundleOf
import androidx.core.widget.doOnTextChanged
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentTransaction
import androidx.fragment.app.setFragmentResult
import androidx.fragment.app.setFragmentResultListener
import com.itsabugnotafeature.fitocrazy.R
import com.itsabugnotafeature.fitocrazy.common.Equipment
import com.itsabugnotafeature.fitocrazy.common.Exercise
import com.itsabugnotafeature.fitocrazy.common.ExerciseComponent
import com.itsabugnotafeature.fitocrazy.common.ExerciseDatabase
import com.itsabugnotafeature.fitocrazy.common.ExerciseWithComponents
import com.itsabugnotafeature.fitocrazy.common.Movement
import com.itsabugnotafeature.fitocrazy.common.Position
import kotlinx.coroutines.runBlocking


class AddNewExerciseToWorkoutFragment : DialogFragment(), AdapterView.OnItemSelectedListener {

    private enum class ExerciseComponents {
        EQUIPMENT, POSITION, MOVEMENT
    }

    private var selectedEquipment: Long = -1
    private var selectedPosition: Long = -1
    private var selectedMovement: Long = -1

    private lateinit var equipmentAdapter: ArrayAdapter<Equipment>
    private lateinit var positionAdapter: ArrayAdapter<Position>
    private lateinit var movementAdapter: ArrayAdapter<Movement>
    private lateinit var autocompleteAdapter: ArrayAdapter<ExerciseWithComponents>

    override fun onItemSelected(parent: AdapterView<*>, view: View?, pos: Int, id: Long) {
        // When a spinner selects an item, we get the update and capture it
        parent.getItemAtPosition(pos)
        when (parent.id) {
            R.id.spinner_equipment -> selectedEquipment = (parent.getItemAtPosition(pos) as ExerciseComponent).id.toLong()
            R.id.spinner_position -> selectedPosition = (parent.getItemAtPosition(pos) as ExerciseComponent).id.toLong()
            R.id.spinner_movement -> selectedMovement = (parent.getItemAtPosition(pos) as ExerciseComponent).id.toLong()
        }
    }

    override fun onNothingSelected(parent: AdapterView<*>) {
        // Do nothing when a spinner does not select an item
    }

    private fun setupEnterCustomTextDialog(hint: ExerciseComponents, btn: Button, spinner: Spinner) {
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
                val enteredText: String? = bundle.getString("userInputtedString")
                if (!enteredText.isNullOrBlank()) {
                    runBlocking {
                        val db = ExerciseDatabase.getInstance(requireContext())

                        when (hint) {
                            ExerciseComponents.MOVEMENT -> {
                                val movement = db.exerciseComponentsDao().getMovement(enteredText)
                                if (movement == null) {
                                    db.exerciseComponentsDao().addMovement(Movement(0, enteredText))
                                } else {
                                    spinner.setSelection((spinner.adapter as ArrayAdapter<Movement>).getPosition(movement), false)
                                }
                            }

                            ExerciseComponents.POSITION -> {
                                val position = db.exerciseComponentsDao().getPosition(enteredText)
                                if (position == null ) {
                                    db.exerciseComponentsDao()
                                        .addPosition(Position(0, enteredText))
                                } else {
                                    spinner.setSelection((spinner.adapter as ArrayAdapter<Position>).getPosition(position), false)
                                }

                            }

                            ExerciseComponents.EQUIPMENT -> {
                                val equipment = db.exerciseComponentsDao().getEquipment(enteredText)
                                if (equipment == null) {
                                    db.exerciseComponentsDao()
                                        .addEquipment(
                                            Equipment(0, enteredText)
                                        )
                                } else {
                                    spinner.setSelection((spinner.adapter as ArrayAdapter<Equipment>).getPosition(equipment), false)
                                }

                            }
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
        // make it always show full-screen
        dialog?.window?.setLayout(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)

        val addExerciseButton: Button = view.findViewById(R.id.btn_addExercise)
        val autocomplete = view.findViewById<AutoCompleteTextView>(R.id.autocomplete_addExercise)

        val detailLayout = view.findViewById<ConstraintLayout>(R.id.layout_addExerciseToWorkout)
        view.findViewById<TextView>(R.id.label_ExpandNewExerciseGroup).setOnClickListener {
            val visible: Int = if (detailLayout.visibility == View.GONE) {
                autocomplete.isEnabled = false
                addExerciseButton.isEnabled = true
                View.VISIBLE
            } else {
                autocomplete.isEnabled = true
                autocomplete.requestFocus()
                View.GONE
            }

            val autoTransition = AutoTransition()
            autoTransition.setDuration(100) // probably not needed, default is 300 if not set
            TransitionManager.beginDelayedTransition(detailLayout, autoTransition)
            detailLayout.visibility = visible
        }

        val equipmentSpinner = view.findViewById<Spinner>(R.id.spinner_equipment)
        val positionSpinner = view.findViewById<Spinner>(R.id.spinner_position)
        val movementSpinner = view.findViewById<Spinner>(R.id.spinner_movement)

        runBlocking {
            equipmentAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item)
            positionAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item)
            movementAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item)
            updateSpinnerData()
            equipmentSpinner.adapter = equipmentAdapter
            positionSpinner.adapter = positionAdapter
            movementSpinner.adapter = movementAdapter

            autocompleteAdapter = ArrayAdapter<ExerciseWithComponents>(requireContext(), android.R.layout.simple_dropdown_item_1line, ExerciseDatabase.getInstance(requireContext()).exerciseDao().getAll())
            autocomplete.setAdapter(autocompleteAdapter)
            autocomplete.threshold = 1
            autocompleteAdapter.notifyDataSetChanged()
        }
        var selectedItem: ExerciseWithComponents? = null
        autocomplete.setOnItemClickListener { _, _, position, _ ->
            selectedItem = autocompleteAdapter.getItem(position)
            addExerciseButton.isEnabled = true
            autocomplete.setBackgroundColor(Color.parseColor("#FFFFFF"))
        }
        autocomplete.doOnTextChanged { text, _, _, _ ->
            if (!text.isNullOrBlank()) {
                if (selectedItem?.toString() != text) {
                    selectedItem = null
                    addExerciseButton.isEnabled = false
                    autocomplete.setBackgroundColor(Color.parseColor("#FFAAAA"))
                }
            }
        }
        autocomplete.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) autocomplete.text = null
        }

        equipmentSpinner.onItemSelectedListener = this
        positionSpinner.onItemSelectedListener = this
        movementSpinner.onItemSelectedListener = this

        setupEnterCustomTextDialog(ExerciseComponents.EQUIPMENT, view.findViewById<Button>(R.id.btn_addNewEquipment), equipmentSpinner)
        setupEnterCustomTextDialog(ExerciseComponents.POSITION, view.findViewById<Button>(R.id.btn_addNewPosition), positionSpinner)
        setupEnterCustomTextDialog(ExerciseComponents.MOVEMENT, view.findViewById<Button>(R.id.btn_addNewMovement), movementSpinner)

        addExerciseButton.setOnClickListener {
            if (detailLayout.visibility == View.VISIBLE) {
                // if adding a new activity
                runBlocking {
                    val db = ExerciseDatabase.getInstance(requireContext()).exerciseDao()
                    val exercise: ExerciseWithComponents? =
                        db.getExactExercise(selectedEquipment, selectedPosition, selectedMovement)
                    if (exercise == null) {
                        val newID = db.addExercise(Exercise(0, selectedEquipment, selectedPosition, selectedMovement))
                        setFragmentResult("exerciseAdded", bundleOf("exerciseID" to newID))
                    } else {
                        setFragmentResult("exerciseAdded", bundleOf("exerciseID" to exercise.exercise.rowid))
                    }
                }
            } else {
                // selected something from autocomplete
                setFragmentResult("exerciseAdded", bundleOf("exerciseID" to selectedItem?.exercise?.rowid))
            }
            dismiss()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.new_exercise_fragment_add_new_exercise_to_workout, container, false)
    }

    companion object {
        const val TAG = "AddNewExerciseToWorkoutFragment_dialog"
    }

}