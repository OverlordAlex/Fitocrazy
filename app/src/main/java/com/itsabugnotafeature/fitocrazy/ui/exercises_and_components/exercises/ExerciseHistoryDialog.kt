package com.itsabugnotafeature.fitocrazy.ui.exercises_and_components.exercises

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager.LayoutParams
import androidx.fragment.app.DialogFragment
import com.github.mikephil.charting.charts.CombinedChart
import com.github.mikephil.charting.charts.CombinedChart.DrawOrder
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.charts.ScatterChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.components.YAxis
import com.github.mikephil.charting.data.CombinedData
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.data.ScatterData
import com.github.mikephil.charting.data.ScatterDataSet
import com.github.mikephil.charting.formatter.ValueFormatter
import com.itsabugnotafeature.fitocrazy.R
import com.itsabugnotafeature.fitocrazy.common.Converters
import com.itsabugnotafeature.fitocrazy.common.Exercise
import com.itsabugnotafeature.fitocrazy.common.ExerciseDatabase
import com.itsabugnotafeature.fitocrazy.common.ExerciseModel
import com.itsabugnotafeature.fitocrazy.common.Set
import kotlinx.coroutines.runBlocking
import java.time.LocalDate
import java.util.SortedMap

class ExerciseHistoryDialog(val exercise: ExerciseModel) : DialogFragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.dialog_exercise_history_chart, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val setData: SortedMap<Exercise, List<Set>> = runBlocking {
            val db = ExerciseDatabase.getInstance(requireContext()).exerciseDao()
            db.getHistoricalSets(exercise.exerciseId)
        }.toSortedMap()

        val sets = setData.map { it.key.date to it.value }.toMap()
        if (sets.isEmpty()) return

        val setDates = sets.map { it.key }.sorted()
        val setWeights = sets.map { set -> set.value.maxOf { it.weight } }

        val chart = view.findViewById<CombinedChart>(R.id.chart_exerciseHistory)
        chart.description.isEnabled = false
        chart.setDrawGridBackground(false)
        chart.setScaleEnabled(false)
        chart.setBackgroundColor(requireContext().getColor(R.color.white))
        chart.setTouchEnabled(false)
        chart.setOnTouchListener { view, motionEvent -> dismiss(); true }

        val maxWeightData = LineDataSet(setDates.mapIndexed { i, date ->
            Entry(
                date.toEpochDay().toFloat(),
                setWeights[i].toFloat()
            )
        }, "KG")
        maxWeightData.color = requireContext().getColor(R.color.blue_accent)
        maxWeightData.setCircleColor(requireContext().getColor(R.color.blue_accent))
        maxWeightData.setDrawCircleHole(false)
        maxWeightData.lineWidth = 2f
        maxWeightData.setDrawValues(true)
        maxWeightData.axisDependency = YAxis.AxisDependency.LEFT
        //maxWeightData.mode = LineDataSet.Mode.CUBIC_BEZIER;

        val lineData = LineData(maxWeightData)
        lineData.setValueTextSize(16f)

        val recordData = setData
            .filter { it.key.recordsAchieved?.isNotEmpty() == true }
            .map { Entry(it.key.date.toEpochDay().toFloat(), setWeights[setDates.indexOf(it.key.date)].toFloat()) }

        val prData = ScatterDataSet(recordData, "PRs")
        prData.color = requireContext().getColor(R.color.blue_main_light)
        prData.setScatterShape(ScatterChart.ScatterShape.TRIANGLE)
        prData.scatterShapeSize = 50f
        prData.setDrawValues(false)
        prData.axisDependency = YAxis.AxisDependency.LEFT
        val scatterData = ScatterData()
        scatterData.addDataSet(prData)

        val combinedData = CombinedData()
        combinedData.setData(lineData)
        combinedData.setData(scatterData)
        chart.drawOrder = arrayOf(DrawOrder.SCATTER, DrawOrder.LINE)
        chart.data = combinedData

        val xAxis = chart.xAxis
        xAxis.valueFormatter = object: ValueFormatter() {
            override fun getFormattedValue(value: Float): String {
                return Converters.dateFormatter.format(LocalDate.ofEpochDay(value.toLong()))
            }
        }
        xAxis.axisMinimum = setDates.first().toEpochDay().toFloat() - 0.864f
        xAxis.axisMaximum = setDates.last().toEpochDay().toFloat() + 0.864f
        xAxis.setDrawGridLines(false)
        xAxis.granularity = 0.864f
        xAxis.position = XAxis.XAxisPosition.BOTTOM
        xAxis.setCenterAxisLabels(true)
        xAxis.labelRotationAngle = 30f

        val yAxis = chart.axisLeft
        yAxis.axisMinimum = setWeights.min().toFloat() - 5
        yAxis.axisMaximum = setWeights.max().toFloat() + 5
        yAxis.granularity = 5f

        val rightAxis = chart.axisRight
        rightAxis.isEnabled = false

        dialog?.setCanceledOnTouchOutside(true)
        //dialog?.window?.setLayout(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
        dialog?.window?.setLayout(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
    }

    companion object {
        const val TAG = "ExerciseHistoryDialog"
    }
}