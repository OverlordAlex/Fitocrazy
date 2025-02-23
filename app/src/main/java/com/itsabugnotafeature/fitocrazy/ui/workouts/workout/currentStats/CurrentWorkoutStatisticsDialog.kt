package com.itsabugnotafeature.fitocrazy.ui.workouts.workout.currentStats

import android.content.res.ColorStateList
import android.graphics.BlendMode
import android.graphics.BlendModeColorFilter
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.graphics.drawable.LayerDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager.LayoutParams
import android.widget.ImageView
import androidx.appcompat.content.res.AppCompatResources
import androidx.constraintlayout.helper.widget.Flow
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.drawable.toDrawable
import androidx.core.graphics.toColor
import androidx.fragment.app.DialogFragment
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.formatter.ValueFormatter
import com.google.android.material.chip.Chip
import com.itsabugnotafeature.fitocrazy.R
import com.itsabugnotafeature.fitocrazy.common.Converters
import com.itsabugnotafeature.fitocrazy.ui.workouts.workout.ExerciseListViewAdapter.ExerciseView
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.pow


class CurrentWorkoutStatisticsDialog(val exercises: List<ExerciseView>) : DialogFragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.dialog_current_workout_summary, container, false)
    }

    private fun ease(x: Float): Float {
        // https://easings.net/
        // return x
        return 1 - cos((x * Math.PI) / 2f).toFloat()
        // return (-(cos(Math.PI * x) - 1f) / 2f).toFloat()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        dialog?.setCanceledOnTouchOutside(true)
        // why is this required here, but not in EnterTextForNewExerciseFragment ??
        dialog?.window?.setBackgroundDrawable(
            AppCompatResources.getDrawable(
                view.context,
                R.drawable.rounded_corner_dialog
            )
        )
        dialog?.window?.setLayout(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)

        val chart = view.findViewById<PieChart>(R.id.chart_bodypartsInExercise)

        val colorList = listOf(
            R.color.blue_main,
            R.color.blue_secondary,
            R.color.blue_tertiary,
            R.color.blue_main_lighter,
            R.color.orange_main,
            R.color.purple_main,
            R.color.purple_accent,
        )

        val bodypartsByExercise = exercises.map { it.tags }.flatten().groupingBy { it }.eachCount().toSortedMap()

        val exercisesBySets =
            exercises.map { exercise ->
                (exercise.tags.joinToString(";") + ";").repeat(max(1, exercise.sets.size)).split(";")
                    .filterNot { it.isEmpty() }
            }.flatten().groupingBy { it }.eachCount().toSortedMap()

        val baseImage = ResourcesCompat.getDrawable(resources, R.drawable.muscles_bodylines, null)
        val values = exercisesBySets.map { it.value }
        val total = values.sum()
        val min = values.min()
        val max = values.max()

        val exerciseBySetsEased = exercisesBySets.map { (k, v) -> ease(v * 1f / total) to k }.sortedBy { it.first }.reversed().toMap()

        val leastAlpha = 100
        val lowerBoundary = (max - min) / 3.0 + min
        val mediumAlpha = 200
        val upperBoundary = (max - min) / 3.0 * 2 + min
        val mostAlpha = 255

        val bodyparts: Array<Drawable?> = arrayOf(
            *exercisesBySets.toSortedMap().toList().mapIndexed { index, (exercise, sets) ->
                when (exercise) {
                    "shoulders" -> ResourcesCompat.getDrawable(resources, R.drawable.muscles_shoulders, null)
                    "biceps" -> ResourcesCompat.getDrawable(resources, R.drawable.muscles_biceps, null)
                    "triceps" -> ResourcesCompat.getDrawable(resources, R.drawable.muscles_triceps, null)
                    "chest" -> ResourcesCompat.getDrawable(resources, R.drawable.muscles_chest, null)
                    "back" -> ResourcesCompat.getDrawable(resources, R.drawable.muscles_back, null)
                    "legs" -> ResourcesCompat.getDrawable(resources, R.drawable.muscles_legs, null)
                    "core" -> ResourcesCompat.getDrawable(resources, R.drawable.muscles_core, null)
                    else -> ResourcesCompat.getDrawable(resources, R.drawable.muscles_bodylines, null)
                }.also { drawable ->
                    drawable?.colorFilter =
                        BlendModeColorFilter(view.context.getColor(colorList[index]), BlendMode.SRC_ATOP)
                    drawable?.alpha =
                        if (sets < lowerBoundary) leastAlpha else if (sets < upperBoundary) mediumAlpha else mostAlpha
                }
            }.toTypedArray(),
            baseImage
        )

        val imageView = view.findViewById<ImageView>(R.id.image_bodypartsInWorkout)
        val finalComposite = LayerDrawable(bodyparts)
        imageView.setImageDrawable(finalComposite)

        val chipGroup = view.findViewById<Flow>(R.id.flowlayout_bodypartChips)
        val parent = chipGroup.parent as ConstraintLayout

        val states = arrayOf(
            intArrayOf(android.R.attr.state_checked), // selected
            intArrayOf(-android.R.attr.state_checked), // notselected
        )

        val chips = mutableListOf<Chip>()
        exercises.map { it.tags }.flatten().toSet().sorted().forEachIndexed { index, chipName ->
            val newChip = Chip(chipGroup.context)
            newChip.text = chipName
            newChip.id = View.generateViewId()

            val backgroundColor = view.context.getColor(colorList[index]).toColor().toDrawable()
            backgroundColor.alpha = 100
            val chipBackgroundColors = intArrayOf(
                view.context.getColor(colorList[index]),
                backgroundColor.color,
            )
            val chipBackgroundColorsList = ColorStateList(states, chipBackgroundColors)
            newChip.chipBackgroundColor = chipBackgroundColorsList
            newChip.isCheckable = true
            newChip.checkedIcon = null
            newChip.rippleColor = null

            newChip.setChipStrokeColorResource(colorList[index])
            newChip.chipStrokeWidth = 8f

            val chipTextColors = intArrayOf(
                view.context.getColor(R.color.white),
                view.context.getColor(R.color.black),
            )
            val chipTextColorsList = ColorStateList(states, chipTextColors)
            newChip.setTextColor(chipTextColorsList)
            newChip.setOnCheckedChangeListener { compoundButton, state ->
                //chart.highlightValue(0f, -1, false)
                //chart.highlightValue(-all f, -1, false)

                if (state) {
                    chips.forEach { if (it != newChip) it.isChecked = false }
                    chart.highlightValue(index.toFloat(), 0, false)
                } else {
                    chart.highlightValue(index.toFloat(), -1, false)
                    //chart.highlightValue(null)
                }

                //chart.highlightValue(Highlight(0f, 0f, 0))
            }
            chips.add(newChip)
            parent.addView(newChip)
            chipGroup.addView(newChip)
        }
        chipGroup.invalidate()
        chipGroup.requestLayout()

        val entries = exercisesBySets.map { PieEntry(ease(it.value.toFloat() / total), it.key) }
        val dataSet = PieDataSet(entries, null)
        dataSet.colors = colorList.map { view.context.getColor(it) }
        dataSet.selectionShift = 10f
        dataSet.setDrawValues(false)
        /*
        dataSet.valueTextSize = 12f
        dataSet.valueFormatter = object : ValueFormatter() {
            override fun getFormattedValue(value: Float): String {
                val bodypart = exerciseBySetsEased[value]
                return if (exerciseBySetsEased.values.take(3).contains(bodypart))
                    Converters.formatDoubleWeight(value.toDouble())
                else
                    ""
            }
        }*/

        val pieData = PieData(dataSet)
        pieData.setDrawValues(true)
        chart.data = pieData

        chart.isDrawHoleEnabled = false
        chart.legend.isEnabled = false
        chart.description.isEnabled = false
        chart.setTouchEnabled(false)
    }

    companion object {
        const val TAG = "CurrentWorkoutStatisticsDialog"
    }
}