package com.itsabugnotafeature.fitocrazy.ui.home.workout.addExercise

import android.animation.ArgbEvaluator
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.os.Bundle
import android.transition.AutoTransition
import android.transition.TransitionManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.SearchView
import android.widget.Spinner
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentTransaction
import androidx.fragment.app.setFragmentResultListener
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.itsabugnotafeature.fitocrazy.R
import com.itsabugnotafeature.fitocrazy.common.Converters
import com.itsabugnotafeature.fitocrazy.common.ExerciseComponentModel
import com.itsabugnotafeature.fitocrazy.common.ExerciseComponentType
import com.itsabugnotafeature.fitocrazy.common.ExerciseDatabase
import com.itsabugnotafeature.fitocrazy.common.ExerciseExerciseComponentCrossRef
import com.itsabugnotafeature.fitocrazy.common.ExerciseModel
import com.itsabugnotafeature.fitocrazy.common.MostCommonExerciseView
import kotlinx.coroutines.runBlocking
import java.time.LocalDate


class AddNewExerciseToWorkoutActivity : AppCompatActivity(), AdapterView.OnItemSelectedListener {

    private var selectedEquipment: Long = -1
    private var selectedLocation: Long = -1
    private var selectedMovement: Long = -1
    private var selectedBasePoints: Int = -1

    private lateinit var equipmentAdapter: ArrayAdapter<ExerciseComponentModel>
    private lateinit var locationAdapter: ArrayAdapter<ExerciseComponentModel>
    private lateinit var movementAdapter: ArrayAdapter<ExerciseComponentModel>

    private lateinit var listOfSuggestedExercises: List<MostCommonExerciseView>
    private lateinit var suggestedExerciseAdapter: SuggestedExercisesListAdapter

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
            val ft: FragmentTransaction = supportFragmentManager.beginTransaction()
            val prev: Fragment? = supportFragmentManager.findFragmentByTag(textFrag.tag)
            if (prev != null) {
                ft.remove(prev)
            }
            ft.addToBackStack(null)

            textFrag.show(ft, textFrag.tag)
            textFrag.setFragmentResultListener("exerciseTextEntered") { _, bundle ->
                val enteredText: String? = bundle.getString("userInputtedString")
                if (!enteredText.isNullOrBlank()) {
                    runBlocking {
                        val db = ExerciseDatabase.getInstance(applicationContext)
                        val existing = db.exerciseDao().getExerciseComponent(enteredText, exerciseComponentType)
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
        val db = ExerciseDatabase.getInstance(this)
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

    class SuggestedExercisesListAdapter(
        var exerciseList: List<MostCommonExerciseView>,
        val selectedItems: MutableList<Long> = mutableListOf(),
    ) : RecyclerView.Adapter<SuggestedExercisesListAdapter.ViewHolder>() {
        inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            fun bind(suggestion: MostCommonExerciseView) {
                itemView.findViewById<TextView>(R.id.label_exerciseSuggestionName).text = suggestion.displayName

                val lastSeenLabel = itemView.findViewById<TextView>(R.id.label_exerciseSuggestionLastSeen)
                if (suggestion.date == null) {
                    lastSeenLabel.visibility = TextView.INVISIBLE
                } else {
                    lastSeenLabel.text = itemView.context.getString(
                        R.string.add_exercise_days_ago,
                        suggestion.date.format(Converters.dateFormatter),
                        LocalDate.now().toEpochDay() - suggestion.date.toEpochDay()
                    )
                }

                val btn = itemView.findViewById<Button>(R.id.btn_exerciseSuggestionAddExercise)
                btn.setOnClickListener {
                    if (suggestion.exerciseModelId in selectedItems) {
                        selectedItems.remove(suggestion.exerciseModelId)
                        itemView.isSelected = false

                    } else {
                        selectedItems.add(suggestion.exerciseModelId)
                        itemView.isSelected = true
                    }

                    notifyItemChanged(adapterPosition)
                }

                if (suggestion.exerciseModelId in selectedItems) {
                    btn.text = itemView.context.getString(R.string.btn_minus)
                    itemView.setBackgroundColor(itemView.context.getColor(R.color.blue_tertiary))
                    itemView.elevation = 0f
                } else {
                    btn.text = itemView.context.getString(R.string.btn_add)
                    itemView.setBackgroundColor(itemView.context.getColor(R.color.white))
                    itemView.elevation = 4f
                }

            }
        }

        override fun getItemCount() = exerciseList.size

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view: View = LayoutInflater.from(parent.context)
                .inflate(R.layout.activity_row_exercise_suggestion_add_new_exercise_to_workout, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            holder.bind(exerciseList[position])
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_new_exercise_to_workout)

        val addExerciseButton: Button = findViewById(R.id.btn_addExercise)

        val chipGroup = findViewById<ChipGroup>(R.id.chipGroup_exerciseTags)
        val chipList = resources.getStringArray(R.array.arrayOfExerciseTagChips)
        chipList.forEach { chipName ->
            val newChip = Chip(this)
            newChip.text = chipName
            newChip.setChipBackgroundColorResource(R.color.blue_accent_light)
            newChip.isCheckable = true
            newChip.setCheckedIconTintResource(R.color.blue_main)
            newChip.setTextColor(ContextCompat.getColor(this, R.color.black))
            newChip.id = View.generateViewId()
            newChip.setOnCheckedChangeListener { compoundButton, checked ->
                if (checked) {
                    if (chipGroup.checkedChipIds.size > 3) {
                        val saved = chipGroup.checkedChipIds
                        saved.remove(compoundButton.id)
                        chipGroup.clearCheck()
                        saved.forEach { chipGroup.check(it) } // seriously? this is the best way??
                    }
                }
            }

            chipGroup.addView(newChip)
        }

        val detailLayout = findViewById<ConstraintLayout>(R.id.layout_addExerciseToWorkout)
        val labelExpandExerciseGroup = findViewById<TextView>(R.id.label_ExpandNewExerciseGroup)
        labelExpandExerciseGroup.setCompoundDrawablesWithIntrinsicBounds(R.drawable.drawer_closed, 0, 0, 0)

        val listExerciseSuggestions = findViewById<RecyclerView>(R.id.list_addExerciseSuggestions)
        val autoComplete = findViewById<SearchView>(R.id.search_addExercise)

        labelExpandExerciseGroup.setOnClickListener {
            val visible: Int = if (detailLayout.visibility == View.GONE) {
                listExerciseSuggestions.visibility = RecyclerView.GONE
                autoComplete.clearFocus()

                labelExpandExerciseGroup.setCompoundDrawablesWithIntrinsicBounds(R.drawable.drawer_opened, 0, 0, 0)

                View.VISIBLE
            } else {
                listExerciseSuggestions.visibility = RecyclerView.VISIBLE
                autoComplete.requestFocus()

                labelExpandExerciseGroup.setCompoundDrawablesWithIntrinsicBounds(R.drawable.drawer_closed, 0, 0, 0)

                View.GONE
            }

            val autoTransition = AutoTransition()
            autoTransition.setDuration(100) // probably not needed, default is 300 if not set
            TransitionManager.beginDelayedTransition(detailLayout, autoTransition)
            detailLayout.visibility = visible
        }

        val equipmentSpinner = findViewById<Spinner>(R.id.spinner_equipment)
        val positionSpinner = findViewById<Spinner>(R.id.spinner_position)
        val movementSpinner = findViewById<Spinner>(R.id.spinner_movement)

        runBlocking {
            equipmentAdapter = ArrayAdapter(applicationContext, android.R.layout.simple_dropdown_item_1line)
            locationAdapter = ArrayAdapter(applicationContext, android.R.layout.simple_dropdown_item_1line)
            movementAdapter = ArrayAdapter(applicationContext, android.R.layout.simple_dropdown_item_1line)
            updateSpinnerData()
            equipmentSpinner.adapter = equipmentAdapter
            positionSpinner.adapter = locationAdapter
            movementSpinner.adapter = movementAdapter

            val db = ExerciseDatabase.getInstance(applicationContext)
            listOfSuggestedExercises = db.exerciseDao().getMostCommonExercises(
                LocalDate.now(),
                intent.getLongArrayExtra("currentExercisesInWorkout")?.toList() ?: emptyList()
            )
            suggestedExerciseAdapter = SuggestedExercisesListAdapter(listOfSuggestedExercises)
        }
        listExerciseSuggestions.adapter = suggestedExerciseAdapter
        listExerciseSuggestions.layoutManager = LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)

        class QueryTextChangedListener() : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                return false
            }

            // could change any number of items on filter, could be more efficient with SortedListAdapter
            //          https://stackoverflow.com/questions/30398247/how-to-filter-a-recyclerview-with-a-searchview
            @SuppressLint("NotifyDataSetChanged")
            override fun onQueryTextChange(query: String?): Boolean {
                if (query != null) {
                    val upperQ = query.uppercase()
                    suggestedExerciseAdapter.exerciseList = listOfSuggestedExercises.filter { it.displayName.contains(upperQ) }
                    suggestedExerciseAdapter.notifyDataSetChanged()
                    listExerciseSuggestions.scrollToPosition(0)
                    return true
                }

                return false
            }

        }
        autoComplete.setOnQueryTextListener(QueryTextChangedListener())

        equipmentSpinner.onItemSelectedListener = this
        positionSpinner.onItemSelectedListener = this
        movementSpinner.onItemSelectedListener = this

        setupEnterCustomTextDialog(
            ExerciseComponentType.EQUIPMENT,
            findViewById(R.id.btn_addNewEquipment),
            equipmentSpinner
        )
        setupEnterCustomTextDialog(
            ExerciseComponentType.LOCATION,
            findViewById(R.id.btn_addNewLocation),
            positionSpinner
        )
        setupEnterCustomTextDialog(
            ExerciseComponentType.MOVEMENT,
            findViewById(R.id.btn_addNewMovement),
            movementSpinner
        )

        findViewById<RadioButton>(R.id.radio_isCompoundExercise)
            .setOnCheckedChangeListener { _, isChecked -> if (isChecked) selectedBasePoints = 15 }
        findViewById<RadioButton>(R.id.radio_isFreeWeightExercise)
            .setOnCheckedChangeListener { _, isChecked -> if (isChecked) selectedBasePoints = 10 }
        findViewById<RadioButton>(R.id.radio_isMachineExercise)
            .setOnCheckedChangeListener { _, isChecked -> if (isChecked) selectedBasePoints = 8 }

        addExerciseButton.setOnClickListener {
            if (detailLayout.visibility == View.VISIBLE) {
                val radioGroup = findViewById<RadioGroup>(R.id.radioGroup_exerciseType)
                if (selectedBasePoints < 0) {
                    val colorFrom: Int = applicationContext.getColor(R.color.blue_tertiary)
                    val colorTo: Int = applicationContext.getColor(R.color.white)

                    val colorAnimation = ValueAnimator.ofObject(ArgbEvaluator(), colorFrom, colorTo)
                    colorAnimation.setDuration(500)
                    colorAnimation.addUpdateListener { animator -> radioGroup.setBackgroundColor(animator.animatedValue as Int) }
                    colorAnimation.start()
                    return@setOnClickListener
                }
                if (chipGroup.checkedChipIds.isEmpty()) {
                    val colorFrom: Int = applicationContext.getColor(R.color.blue_tertiary)
                    val colorTo: Int = applicationContext.getColor(R.color.white)

                    val colorAnimation = ValueAnimator.ofObject(ArgbEvaluator(), colorFrom, colorTo)
                    colorAnimation.setDuration(500)
                    colorAnimation.addUpdateListener { animator -> chipGroup.setBackgroundColor(animator.animatedValue as Int) }
                    colorAnimation.start()
                    return@setOnClickListener
                }

                // if adding a new activity
                runBlocking {
                    val db = ExerciseDatabase.getInstance(applicationContext).exerciseDao()
                    val exercise: ExerciseExerciseComponentCrossRef? =
                        db.getExercise(selectedEquipment, selectedLocation, selectedMovement)
                    if (exercise == null) {
                        val displayName = listOf(
                            db.getExercise(selectedEquipment)?.name,
                            db.getExercise(selectedLocation)?.name,
                            db.getExercise(selectedMovement)?.name
                        ).joinToString(" ")
                        val chips = chipGroup.checkedChipIds.joinToString(" ") { findViewById<Chip>(it).text }
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
                        intent.putExtra("exerciseIDs", longArrayOf(newExerciseId))
                    } else {
                        intent.putExtra("exerciseIDs", longArrayOf(exercise.exerciseId))
                    }
                }
            } else {
                // selected some things from the list
                intent.putExtra("exerciseIDs", suggestedExerciseAdapter.selectedItems.toLongArray())
            }

            setResult(RESULT_OK, intent)
            finish()
        }
    }

}