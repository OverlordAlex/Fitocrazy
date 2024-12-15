package com.itsabugnotafeature.fitocrazy.ui.exercises_and_components.exercises

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager.LayoutParams
import android.widget.TextView
import androidx.fragment.app.DialogFragment
import com.github.mikephil.charting.animation.ChartAnimator
import com.github.mikephil.charting.charts.CombinedChart
import com.github.mikephil.charting.charts.CombinedChart.DrawOrder
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.charts.ScatterChart
import com.github.mikephil.charting.components.Description
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.components.YAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.data.CombinedData
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.data.ScatterData
import com.github.mikephil.charting.data.ScatterDataSet
import com.github.mikephil.charting.formatter.ValueFormatter
import com.github.mikephil.charting.interfaces.dataprovider.BarDataProvider
import com.github.mikephil.charting.renderer.BarChartRenderer
import com.github.mikephil.charting.renderer.CombinedChartRenderer
import com.github.mikephil.charting.utils.MPPointF
import com.github.mikephil.charting.utils.Utils
import com.github.mikephil.charting.utils.ViewPortHandler
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

    class CombinedChartRenderer(chart: CombinedChart, animator: ChartAnimator, viewPortHandler: ViewPortHandler) :
        com.github.mikephil.charting.renderer.CombinedChartRenderer(chart, animator, viewPortHandler) {
        override fun createRenderers() {
            super.createRenderers()
            val removedChart = mRenderers.indexOfFirst { it is BarChartRenderer }
            mRenderers.removeIf { it is BarChartRenderer }
            mRenderers.add(removedChart, MyBarChartRenderer(mChart.get() as CombinedChart, mAnimator, mViewPortHandler))
        }

        class MyBarChartRenderer(
            chart: BarDataProvider,
            animator: ChartAnimator,
            viewPortHandler: ViewPortHandler,
        ) : BarChartRenderer(chart, animator, viewPortHandler) {

            init {
                initBuffers() // Initialize the buffers in the constructor, chart will crash without it
            }

            override fun drawValue(canvas: Canvas?, valueText: String?, x: Float, y: Float, color: Int) {
                val paint = mDrawPaint
                paint.textSize = 32F
                paint.textAlign = Paint.Align.LEFT
                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                paint.color = color

                valueText?.let {
                    Utils.drawXAxisValue(
                        canvas,
                        it,
                        x - 16F,
                        // bar buffers contains the coordinates for the rectangles, we're interested in the bottom right
                        //      this should be the zero line
                        this.mBarBuffers.first().buffer[3] - paint.textSize * 1.5F,
                        paint,
                        MPPointF.getInstance(),
                        0f
                    )
                }
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.dialog_exercise_history_chart, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val exerciseName = view.findViewById<TextView>(R.id.label_exerciseName)
        exerciseName.text = exercise.displayName

        val setData: SortedMap<Exercise, List<Set>> = runBlocking {
            val db = ExerciseDatabase.getInstance(requireContext()).exerciseDao()
            db.getHistoricalSets(exercise.exerciseId)
        }.toList().groupBy { it.first.exerciseId }.map { it.value.first().first to it.value.flatMap { v -> v.second } }
            .toMap().toSortedMap()

        val sets = setData.map { it.key.date to it.value }.toMap()
        if (sets.isEmpty()) return

        val setDates = sets.map { it.key }.sorted().toMutableList()
        val setWeights = sets.map { set -> set.value.maxOf { it.weight } }
        val setTotals = sets.map { set -> set.value.sumOf { it.weight * it.reps } }

        val chart = view.findViewById<CombinedChart>(R.id.chart_exerciseHistory)
        chart.description.isEnabled = false
        chart.setDrawGridBackground(false)
        chart.setScaleEnabled(false)
        chart.setBackgroundColor(requireContext().getColor(R.color.white))
        chart.setTouchEnabled(false)
        chart.setOnTouchListener { view, motionEvent -> dismiss(); true }

        val totalWeightData = BarDataSet(List(setDates.size) { i ->
            BarEntry(
                i.toFloat(),
                setTotals[i].toFloat()
            )
        }, "Total Moved")
        totalWeightData.color = requireContext().getColor(R.color.blue_accent_light)
        totalWeightData.formLineWidth = 4f
        totalWeightData.axisDependency = YAxis.AxisDependency.RIGHT
        totalWeightData.setDrawValues(true)

        val barData = BarData(totalWeightData)
        barData.setValueTextSize(16f)
        //barData.setDrawValues(true)

        val maxWeightData = LineDataSet(List(setDates.size) { i ->
            Entry(
                i.toFloat(),
                setWeights[i].toFloat()
            )
        }, "KG")
        maxWeightData.color = requireContext().getColor(R.color.blue_accent)
        maxWeightData.setCircleColor(requireContext().getColor(R.color.blue_accent))
        maxWeightData.setDrawCircleHole(false)
        maxWeightData.lineWidth = 2f
        maxWeightData.setDrawValues(true)
        maxWeightData.axisDependency = YAxis.AxisDependency.LEFT
        maxWeightData.mode = LineDataSet.Mode.LINEAR

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
        combinedData.setData(barData)
        combinedData.setData(lineData)
        combinedData.setData(scatterData)

        chart.data = combinedData
        chart.renderer = CombinedChartRenderer(chart, chart.animator, chart.viewPortHandler)
        //chart.setExtraOffsets(50F, 0F, 50F, 0F)

        val xAxis = chart.xAxis
        xAxis.axisMinimum = barData.xMin - 1
        xAxis.axisMaximum = barData.xMax + 1

        setDates.add(0, LocalDate.ofEpochDay((setDates.first().toEpochDay().toFloat() - 0.864).toLong()))
        setDates.add(LocalDate.ofEpochDay((setDates.last().toEpochDay().toFloat() + 0.864).toLong()))

        xAxis.valueFormatter = object : ValueFormatter() {
            override fun getFormattedValue(value: Float): String {
                if ((value.toInt() < 0) || (value.toInt() >= setDates.size - 2)) return ""
                return Converters.dateFormatter.format(setDates[value.toInt() + 1])
            }
        }
        xAxis.setDrawGridLines(false)
        xAxis.granularity = 1F
        xAxis.position = XAxis.XAxisPosition.BOTTOM
        //xAxis.setCenterAxisLabels(true)
        xAxis.labelRotationAngle = 30f

        val yAxis = chart.axisLeft
        //yAxis.axisMinimum = 0F
        //yAxis.setDrawZeroLine(true)
        yAxis.granularity = 5f

        val rightAxis = chart.axisRight
        rightAxis.isEnabled = false
        //rightAxis.axisMinimum = 0f
        rightAxis.isGranularityEnabled = false

        dialog?.setCanceledOnTouchOutside(true)
        //dialog?.window?.setLayout(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
        dialog?.window?.setLayout(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
    }

    companion object {
        const val TAG = "ExerciseHistoryDialog"
    }
}