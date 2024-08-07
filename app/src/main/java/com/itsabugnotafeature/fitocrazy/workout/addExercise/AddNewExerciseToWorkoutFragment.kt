package com.itsabugnotafeature.fitocrazy.workout.addExercise

import android.graphics.Color
import android.os.Bundle
import android.transition.AutoTransition
import android.transition.TransitionManager
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager.LayoutParams
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Button
import android.widget.RadioButton
import android.widget.Spinner
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.core.view.ViewCompat
import androidx.core.widget.doOnTextChanged
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentTransaction
import androidx.fragment.app.setFragmentResult
import androidx.fragment.app.setFragmentResultListener
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.itsabugnotafeature.fitocrazy.R
import com.itsabugnotafeature.fitocrazy.common.ExerciseComponentModel
import com.itsabugnotafeature.fitocrazy.common.ExerciseComponentType
import com.itsabugnotafeature.fitocrazy.common.ExerciseDatabase
import com.itsabugnotafeature.fitocrazy.common.ExerciseExerciseComponentCrossRef
import com.itsabugnotafeature.fitocrazy.common.ExerciseModel
import com.itsabugnotafeature.fitocrazy.common.ExerciseWithComponentModel
import kotlinx.coroutines.runBlocking


class AddNewExerciseToWorkoutFragment : DialogFragment(), AdapterView.OnItemSelectedListener {

    private var selectedEquipment: Long = -1
    private var selectedLocation: Long = -1
    private var selectedMovement: Long = -1
    private var selectedBasePoints: Int = -1

    private lateinit var equipmentAdapter: ArrayAdapter<ExerciseComponentModel>
    private lateinit var locationAdapter: ArrayAdapter<ExerciseComponentModel>
    private lateinit var movementAdapter: ArrayAdapter<ExerciseComponentModel>
    private lateinit var autocompleteAdapter: ArrayAdapter<ExerciseWithComponentModel>

    override fun onItemSelected(parent: AdapterView<*>, view: View?, pos: Int, id: Long) {
        // When a spinner selects an item, we get the update and capture it
        parent.getItemAtPosition(pos)
        when (parent.id) {
            R.id.spinner_equipment -> selectedEquipment =
                (parent.getItemAtPosition(pos) as ExerciseComponentModel).componentId

            R.id.spinner_position -> selectedLocation =
                (parent.getItemAtPosition(pos) as ExerciseComponentModel).componentId

            R.id.spinner_movement -> selectedMovement =
                (parent.getItemAtPosition(pos) as ExerciseComponentModel).componentId
        }
    }

    override fun onNothingSelected(parent: AdapterView<*>) {
        // Do nothing when a spinner does not select an item
    }

    private fun setupEnterCustomTextDialog(
        exerciseComponentType: ExerciseComponentType,
        btn: Button,
        spinner: Spinner
    ) {
        val textFrag: DialogFragment =
            EnterTextForNewExerciseFragment(exerciseComponentType.toString())

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
                        val existing = db.exerciseDao()
                            .getExerciseComponent(enteredText, exerciseComponentType)
                        if (existing == null) {
                            db.exerciseDao().addExerciseComponent(
                                ExerciseComponentModel(
                                    0,
                                    enteredText,
                                    exerciseComponentType
                                )
                            )
                        } else {
                            spinner.setSelection(
                                (spinner.adapter as ArrayAdapter<ExerciseComponentModel>).getPosition(
                                    existing
                                ), false
                            )
                        }
                        updateSpinnerData()
                        spinner.setSelection(spinner.adapter.count - 1)
                    }
                }
            }
        }
    }

    private suspend fun updateSpinnerData() {
        val db = ExerciseDatabase.getInstance(requireContext())
        equipmentAdapter.clear()
        equipmentAdapter.addAll(
            db.exerciseDao().getExerciseComponent(ExerciseComponentType.EQUIPMENT)
        )
        locationAdapter.clear()
        locationAdapter.addAll(
            db.exerciseDao().getExerciseComponent(ExerciseComponentType.LOCATION)
        )
        movementAdapter.clear()
        movementAdapter.addAll(
            db.exerciseDao().getExerciseComponent(ExerciseComponentType.MOVEMENT)
        )
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // make it always show full-screen, not sure why it's not working via the xml
        dialog?.window?.setLayout(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)

        val chipGroup = view.findViewById<ChipGroup>(R.id.chipGroup_exerciseTags)
        val chipList = resources.getStringArray(R.array.arrayOfExerciseTagChips)
        chipList.forEach { chipName ->
            val newChip = Chip(context)
            newChip.text = chipName
            newChip.setChipBackgroundColorResource(R.color.purple_500)
            newChip.isCheckable = true
            newChip.setTextColor(context?.let { ContextCompat.getColor(it, R.color.white) } ?: R.color.white)

            chipGroup.addView(newChip)
        }

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
            locationAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item)
            movementAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item)
            updateSpinnerData()
            equipmentSpinner.adapter = equipmentAdapter
            positionSpinner.adapter = locationAdapter
            movementSpinner.adapter = movementAdapter

            autocompleteAdapter = ArrayAdapter<ExerciseWithComponentModel>(
                requireContext(),
                android.R.layout.simple_dropdown_item_1line,
                ExerciseDatabase.getInstance(requireContext()).exerciseDao().getAllExercises()
            )
            autocomplete.setAdapter(autocompleteAdapter)
            autocomplete.threshold = 1
            autocompleteAdapter.notifyDataSetChanged()
        }
        var selectedItem: ExerciseWithComponentModel? = null
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

        setupEnterCustomTextDialog(
            ExerciseComponentType.EQUIPMENT,
            view.findViewById(R.id.btn_addNewEquipment),
            equipmentSpinner
        )
        setupEnterCustomTextDialog(
            ExerciseComponentType.LOCATION,
            view.findViewById(R.id.btn_addNewLocation),
            positionSpinner
        )
        setupEnterCustomTextDialog(
            ExerciseComponentType.MOVEMENT,
            view.findViewById(R.id.btn_addNewMovement),
            movementSpinner
        )

        view.findViewById<RadioButton>(R.id.radio_isCompoundExercise)
            .setOnCheckedChangeListener { _, isChecked -> if (isChecked) selectedBasePoints = 15 }
        view.findViewById<RadioButton>(R.id.radio_isFreeWeightExercise)
            .setOnCheckedChangeListener { _, isChecked -> if (isChecked) selectedBasePoints = 10 }
        view.findViewById<RadioButton>(R.id.radio_isMachineExercise)
            .setOnCheckedChangeListener { _, isChecked -> if (isChecked) selectedBasePoints = 8 }

        addExerciseButton.setOnClickListener {
            if (detailLayout.visibility == View.VISIBLE) {
                // if adding a new activity
                runBlocking {
                    val db = ExerciseDatabase.getInstance(requireContext()).exerciseDao()
                    val exercise: ExerciseExerciseComponentCrossRef? =
                        db.getExercise(selectedEquipment, selectedLocation, selectedMovement)
                    if (exercise == null) {
                        val displayName = listOf(
                            db.getExercise(selectedEquipment)?.name,
                            db.getExercise(selectedLocation)?.name,
                            db.getExercise(selectedMovement)?.name
                        ).joinToString(" ")
                        val chips = chipGroup.checkedChipIds.joinToString(" ") { view.findViewById<Chip>(it).text }
                        val newExerciseId = db.addExercise(
                            ExerciseModel(0, displayName, selectedBasePoints, chips)
                        )
                        db.addExerciseExerciseComponentCrossRef(
                            ExerciseExerciseComponentCrossRef(
                                selectedEquipment,
                                newExerciseId
                            )
                        )
                        db.addExerciseExerciseComponentCrossRef(
                            ExerciseExerciseComponentCrossRef(
                                selectedLocation,
                                newExerciseId
                            )
                        )
                        db.addExerciseExerciseComponentCrossRef(
                            ExerciseExerciseComponentCrossRef(
                                selectedMovement,
                                newExerciseId
                            )
                        )
                        setFragmentResult("exerciseAdded", bundleOf("exerciseID" to newExerciseId))
                    } else {
                        setFragmentResult(
                            "exerciseAdded",
                            bundleOf("exerciseID" to exercise.exerciseId)
                        )
                    }
                }
            } else {
                // selected something from autocomplete
                setFragmentResult(
                    "exerciseAdded",
                    bundleOf("exerciseID" to selectedItem?.exercise?.exerciseId)
                )
            }
            dismiss()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(
            R.layout.new_exercise_fragment_add_new_exercise_to_workout,
            container,
            false
        )
    }

    companion object {
        const val TAG = "AddNewExerciseToWorkoutFragment_dialog"
    }

}