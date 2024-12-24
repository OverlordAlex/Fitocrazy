package com.itsabugnotafeature.fitocrazy.ui.workouts.workout.currentStats

import android.graphics.BlendMode
import android.graphics.BlendModeColorFilter
import android.graphics.ColorFilter
import android.graphics.drawable.Drawable
import android.graphics.drawable.LayerDrawable
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager.LayoutParams
import android.widget.ImageView
import androidx.appcompat.content.res.AppCompatResources
import androidx.constraintlayout.helper.widget.Flow
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.marginEnd
import androidx.core.view.updateLayoutParams
import androidx.fragment.app.DialogFragment
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.charts.RadarChart
import com.github.mikephil.charting.components.Description
import com.github.mikephil.charting.components.Legend
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.data.RadarData
import com.github.mikephil.charting.data.RadarDataSet
import com.github.mikephil.charting.data.RadarEntry
import com.github.mikephil.charting.highlight.Highlight
import com.google.android.material.chip.Chip
import com.itsabugnotafeature.fitocrazy.R
import com.itsabugnotafeature.fitocrazy.ui.workouts.workout.ExerciseListViewAdapter.ExerciseView
import kotlin.math.max

class CurrentWorkoutStatisticsDialog(val exercises: List<ExerciseView>) : DialogFragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.dialog_current_workout_summary, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        dialog?.setCanceledOnTouchOutside(true)
        // why is this required here, but not in EnterTextForNewExerciseFragment ??
        dialog?.window?.setBackgroundDrawable(AppCompatResources.getDrawable(view.context, R.drawable.rounded_corner_dialog))
        dialog?.window?.setLayout(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)

        val colorList = listOf(
            R.color.blue_main,
            R.color.blue_secondary,
            R.color.blue_tertiary,
            R.color.blue_main_lighter,
            R.color.orange_main,
            R.color.orange_accent,
            R.color.purple_accent,
        )

        val bodypartsByExercise = exercises.map { it.tags }.flatten().groupingBy { it }.eachCount().toSortedMap()

        val exercisesBySets =
            exercises.map { exercise ->
                (exercise.tags.joinToString(";") + ";").repeat(max(1, exercise.sets.size)).split(";").filterNot { it.isEmpty() }
            }
                .flatten().groupingBy { it }.eachCount().toSortedMap()

        val baseImage = ResourcesCompat.getDrawable(resources, R.drawable.muscles_bodylines, null)
        val values = exercisesBySets.map { it.value }
        val total = values.sum()
        val min = values.min()
        val max = values.max()
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
                    drawable?.colorFilter = BlendModeColorFilter(view.context.getColor(colorList[index]), BlendMode.SRC_ATOP)
                    drawable?.alpha =
                        if (sets <= lowerBoundary) leastAlpha else if (sets <= upperBoundary) mediumAlpha else mostAlpha
                }
            }.toTypedArray(),
            baseImage
        )

        val imageView = view.findViewById<ImageView>(R.id.image_bodypartsInWorkout)
        val finalComposite = LayerDrawable(bodyparts)
        imageView.setImageDrawable(finalComposite)

        val chipGroup = view.findViewById<Flow>(R.id.flowlayout_bodypartChips)
        val parent = chipGroup.parent as ConstraintLayout

        exercises.map { it.tags }.flatten().toSet().sorted().forEachIndexed { index, chipName ->
            val newChip = Chip(chipGroup.context)
            newChip.text = chipName
            newChip.id = View.generateViewId()
            newChip.setChipBackgroundColorResource(colorList[index])
            newChip.setTextColor(view.context.getColor(R.color.white))
            parent.addView(newChip)
            chipGroup.addView(newChip)
        }
        chipGroup.invalidate()
        chipGroup.requestLayout()

        val datasets = mutableListOf<RadarDataSet>()
        bodypartsByExercise.toSortedMap().onEachIndexed { index, (part, _) ->
            val radarEntries = mutableListOf<RadarEntry>()

            bodypartsByExercise.forEach { (innerPart, innerCount) ->
                for (i in 1..innerCount) {
                    if (part == innerPart) {
                        radarEntries.add(RadarEntry(if (exercisesBySets[part] == null || exercisesBySets[part] == 0) 1f else exercisesBySets[part]!!.toFloat()))
                    } else {
                        radarEntries.add(RadarEntry(0f))
                    }
                }
                radarEntries.add(RadarEntry(if (exercisesBySets[innerPart] == null || exercisesBySets[innerPart] == 0) 1f else exercisesBySets[innerPart]!!.toFloat()))

            }
            val dataset = RadarDataSet(radarEntries, part)
            dataset.setDrawFilled(true)
            dataset.color = view.context.getColor(R.color.black)
            dataset.fillColor = view.context.getColor(colorList[index])
            dataset.fillAlpha = 255
            dataset.setDrawValues(false)
            datasets.add(dataset)
        }

        val chart = view.findViewById<RadarChart>(R.id.chart_bodypartsInExercise)
        val radarData = RadarData(datasets.toList())
        radarData.setDrawValues(false)
        radarData.labels = emptyList()

        chart.data = radarData
        chart.setDrawWeb(false)
        chart.setDrawMarkers(false)
        chart.yAxis.isEnabled = false
        chart.xAxis.isEnabled = false
        chart.legend.isEnabled = false
        chart.description.isEnabled = false
        chart.setTouchEnabled(false)
    }

    companion object {
        const val TAG = "CurrentWorkoutStatisticsDialog"
    }
}