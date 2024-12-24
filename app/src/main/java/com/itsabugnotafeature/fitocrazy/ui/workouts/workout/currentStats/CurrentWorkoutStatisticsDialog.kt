package com.itsabugnotafeature.fitocrazy.ui.workouts.workout.currentStats

import android.graphics.drawable.Drawable
import android.graphics.drawable.LayerDrawable
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager.LayoutParams
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.constraintlayout.helper.widget.Flow
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.setPadding
import androidx.core.view.updateLayoutParams
import androidx.fragment.app.DialogFragment
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.itsabugnotafeature.fitocrazy.R
import com.itsabugnotafeature.fitocrazy.ui.workouts.workout.ExerciseListViewAdapter.ExerciseView
import kotlin.math.max
import kotlin.reflect.typeOf

class CurrentWorkoutStatisticsDialog(val exercises: List<ExerciseView>) : DialogFragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.dialog_current_workout_summary, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val baseImage = ResourcesCompat.getDrawable(resources, R.drawable.muscles_bodylines, null)
        val exercisesBySets =
            exercises.map {
                (it.tags.joinToString(";") + ";").repeat(max(1, it.sets.size)).split(";").filterNot { it.isEmpty() }
            }
                .flatten().groupingBy { it }.eachCount()
        val values = exercisesBySets.map { it.value }
        val min = values.min()
        val max = values.max()
        val leastAlpha = 100
        val lowerBoundary = (max - min) / 3.0 + min
        val mediumAlpha = 200
        val upperBoundary = (max - min) / 3.0 * 2 + min
        val mostAlpha = 255

        val bodyparts: Array<Drawable?> = arrayOf(
            *exercisesBySets.map {
                when (it.key) {
                    "shoulders" -> ResourcesCompat.getDrawable(resources, R.drawable.muscles_shoulders, null)
                    "biceps" -> ResourcesCompat.getDrawable(resources, R.drawable.muscles_biceps, null)
                    "triceps" -> ResourcesCompat.getDrawable(resources, R.drawable.muscles_triceps, null)
                    "chest" -> ResourcesCompat.getDrawable(resources, R.drawable.muscles_chest, null)
                    "back" -> ResourcesCompat.getDrawable(resources, R.drawable.muscles_back, null)
                    "legs" -> ResourcesCompat.getDrawable(resources, R.drawable.muscles_legs, null)
                    "core" -> ResourcesCompat.getDrawable(resources, R.drawable.muscles_core, null)
                    else -> ResourcesCompat.getDrawable(resources, R.drawable.muscles_bodylines, null)
                }.also { drawable ->
                    drawable?.alpha =
                        if (it.value <= lowerBoundary) leastAlpha else if (it.value <= upperBoundary) mediumAlpha else mostAlpha
                    Log.i("TEST", "${it.key} has ${it.value} and so has ${drawable?.alpha}")
                }
            }.toTypedArray(),
            baseImage
        )

        val imageView = view.findViewById<ImageView>(R.id.image_bodypartsInWorkout)
        val finalComposite = LayerDrawable(bodyparts)
        imageView.setImageDrawable(finalComposite)

        dialog?.setCanceledOnTouchOutside(true)
        dialog?.window?.setLayout(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)

        val parent = view.findViewById<ConstraintLayout>(R.id.layout_parent)
        val chipGroup = view.findViewById<Flow>(R.id.chipgroup_Bodyparts2)
        exercises.map { it.tags }.flatten().toSet().forEach { chipName ->
            val newChip = Chip(chipGroup.context)
            newChip.text = chipName
            newChip.id = View.generateViewId()
            newChip.setChipBackgroundColorResource(R.color.blue_accent_light)
//            newChip.layoutParams = FrameLayout.LayoutParams(
//                FrameLayout.LayoutParams.WRAP_CONTENT,
//                FrameLayout.LayoutParams.WRAP_CONTENT,
//                Gravity.END
//            )
            parent.addView(newChip)
            chipGroup.addView(newChip)
        }
        chipGroup.invalidate()
        chipGroup.requestLayout()
    }

    companion object {
        const val TAG = "CurrentWorkoutStatisticsDialog"
    }
}