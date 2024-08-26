package com.itsabugnotafeature.fitocrazy.ui.home

import android.animation.ArgbEvaluator
import android.animation.ValueAnimator
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.itsabugnotafeature.fitocrazy.R
import com.itsabugnotafeature.fitocrazy.common.ExerciseDatabase
import com.itsabugnotafeature.fitocrazy.common.Workout
import com.itsabugnotafeature.fitocrazy.common.WorkoutRecordView
import com.itsabugnotafeature.fitocrazy.ui.home.workout.WorkoutActivity
import com.itsabugnotafeature.fitocrazy.ui.home.workout.WorkoutActivity.Companion.NOTIFICATION_ID
import kotlinx.coroutines.runBlocking
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlin.time.DurationUnit
import kotlin.time.toDuration


class WorkoutListFragment : Fragment() {

    private val channelId = "FitocrazyCurrentExerciseChannel"
    private lateinit var workoutList: MutableList<Workout>
    private lateinit var workoutListViewAdapter: WorkoutListViewAdapter
    private var workoutStats: WorkoutRecordView? = null

    class WorkoutListViewAdapter(
        var workoutList: MutableList<Workout>,
        val workoutStats: WorkoutRecordView?,
    ) : RecyclerView.Adapter<WorkoutListViewAdapter.ViewHolder>() {
        var lastOpened: ViewHolder? = null

        inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

            private fun showDelete() {
                itemView.findViewById<LinearLayout>(R.id.layout_workoutOtherStats).visibility = LinearLayout.GONE
                itemView.findViewById<TextView>(R.id.label_workoutNumberExercises).visibility = TextView.GONE

                val deleteFrame = itemView.findViewById<FrameLayout>(R.id.frame_deleteWorkout)
                deleteFrame.alpha = 0f
                deleteFrame.visibility = FrameLayout.VISIBLE
                deleteFrame.animate().setDuration(200).alpha(1f)
            }

            private fun hideDelete() {
                val deleteFrame = itemView.findViewById<FrameLayout>(R.id.frame_deleteWorkout)
                if (deleteFrame.visibility == FrameLayout.GONE) return

                deleteFrame.animate().setDuration(150).alpha(0f).withEndAction {
                    deleteFrame.visibility = FrameLayout.GONE
                    itemView.findViewById<LinearLayout>(R.id.layout_workoutOtherStats).visibility = LinearLayout.VISIBLE
                    itemView.findViewById<TextView>(R.id.label_workoutNumberExercises).visibility = TextView.VISIBLE
                }
            }

            fun bind(
                currentWorkout: Workout,
            ) {
                itemView.findViewById<TextView>(R.id.label_workoutDate).text =
                    if (LocalDate.now() == currentWorkout.date) {
                        itemView.context.getString(R.string.today)
                    } else {
                        currentWorkout.date.format(DateTimeFormatter.ofPattern("dd LLLL yyyy"))
                    }

                itemView.findViewById<TextView>(R.id.label_workoutNumberExercises).text =
                    itemView.context.getString(R.string.number_of_exercises_in_workout, currentWorkout.totalExercises)

                val labelWorkoutPoints = itemView.findViewById<TextView>(R.id.label_workoutPoints)
                // do a nice color animation on points more than the last 10 average
                if (currentWorkout.totalPoints > (workoutStats?.avgTotalPoints ?: 0.0)) {
                    val colorFrom: Int = itemView.context.getColor(R.color.blue_main)
                    val colorTo: Int = itemView.context.getColor(R.color.blue_accent)

                    val colorAnimation = ValueAnimator.ofObject(ArgbEvaluator(), colorFrom, colorTo)
                    colorAnimation.setDuration(1000) // milliseconds
                    colorAnimation.repeatCount = ValueAnimator.INFINITE
                    colorAnimation.repeatMode = ValueAnimator.REVERSE
                    colorAnimation.addUpdateListener { animator -> labelWorkoutPoints.setTextColor(animator.animatedValue as Int) }
                    colorAnimation.start()
                } else {
                    labelWorkoutPoints.clearAnimation()
                }

                labelWorkoutPoints.text =
                    itemView.context.getString(R.string.total_points_in_workout, currentWorkout.totalPoints)

                itemView.findViewById<TextView>(R.id.label_workoutTotalWeightValue).text =
                    "%.0f kg".format(currentWorkout.totalWeight)

                itemView.findViewById<TextView>(R.id.label_workoutTotalRepsValue).text =
                    currentWorkout.totalReps.toString()

                itemView.findViewById<TextView>(R.id.label_workoutTotalSetsValue).text =
                    currentWorkout.totalSets.toString()

                val totalTime = currentWorkout.totalTime.toDuration(DurationUnit.MILLISECONDS)
                    .toComponents { hours, minutes, _, _ -> "%02d:%02d".format(hours, minutes) }
                itemView.findViewById<TextView>(R.id.label_workoutTotalTimeValue).text = totalTime

                val chipGroup = itemView.findViewById<ChipGroup>(R.id.chipGroup_workoutTopTags)
                chipGroup.removeAllViews()
                val tags = currentWorkout.topTags
                if (tags.isNotEmpty()) {
                    chipGroup.visibility = ChipGroup.VISIBLE
                    val colourGradient =
                        listOf(R.color.orange_gradient1, R.color.orange_gradient2, R.color.orange_gradient3)

                    tags.trim().split(" ").forEachIndexed { index, chipName ->
                        val newChip = Chip(itemView.context)
                        newChip.text = chipName
                        newChip.setChipBackgroundColorResource(colourGradient[index % 3])

                        newChip.setTextColor(itemView.context.getColor(R.color.black))
                        chipGroup.addView(newChip)
                    }
                }

                val deleteFrame = itemView.findViewById<FrameLayout>(R.id.frame_deleteWorkout)
                hideDelete()
                itemView.setOnClickListener {
                    if (deleteFrame.visibility == FrameLayout.VISIBLE) {
                        hideDelete()
                        return@setOnClickListener
                    }

                    ContextCompat.startActivity(
                        itemView.context, Intent(
                            itemView.context.applicationContext, WorkoutActivity::class.java
                        ).setAction("oldWorkoutStartedFromHome").putExtra("workoutId", currentWorkout.workoutId), null
                    )
                }

                itemView.setOnLongClickListener {
                    if (deleteFrame.visibility == FrameLayout.GONE) {
                        lastOpened?.hideDelete()
                        showDelete()
                        lastOpened = this
                    } else {
                        hideDelete()
                        lastOpened = null
                    }

                    true
                }

                itemView.findViewById<Button>(R.id.btnDeleteWorkout).setOnClickListener {
                    val db = ExerciseDatabase.getInstance(itemView.context)
                    runBlocking {
                        db.exerciseDao().deleteWorkout(workoutList[adapterPosition])
                        db.exerciseDao().deleteExercisesInWorkout(workoutList[adapterPosition].workoutId)
                    }
                    workoutList.removeAt(adapterPosition)
                    notifyItemRemoved(adapterPosition)
                    lastOpened = null
                }
            }
        }

        override fun getItemCount() = workoutList.size

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view: View = LayoutInflater.from(parent.context).inflate(R.layout.row_workout, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            holder.bind(workoutList[position])
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_workout_list_home, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        // Create the NotificationChannel.
        val name = getString(R.string.channel_name)
        val descriptionText = getString(R.string.channel_description)
        val importance = NotificationManager.IMPORTANCE_LOW
        val mChannel = NotificationChannel(channelId, name, importance)
        mChannel.description = descriptionText
        // Register the channel with the system.
        (requireActivity().getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).createNotificationChannel(mChannel)

        super.onViewCreated(view, savedInstanceState)
    }

    override fun onResume() {
        super.onResume()
        // no workouts may be running on this screen
        (requireActivity().getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).cancel(NOTIFICATION_ID)

        val db = ExerciseDatabase.getInstance(requireContext())
        workoutList = runBlocking {
            workoutStats = db.exerciseDao().getWorkoutStats()
            db.exerciseDao().listWorkouts().toMutableList()
        }
        val workoutListView = requireView().findViewById<RecyclerView>(R.id.list_allWorkoutsHomepage)
        workoutListViewAdapter = WorkoutListViewAdapter(workoutList, workoutStats)
        workoutListView.adapter = workoutListViewAdapter
        workoutListView.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false)

        requireView().findViewById<Button>(R.id.btn_AddNewWorkout)?.setOnClickListener {
            startActivity(
                Intent(
                    requireActivity(), WorkoutActivity::class.java
                ).setAction("newWorkoutStartedFromHome")
            )
        }
    }

}