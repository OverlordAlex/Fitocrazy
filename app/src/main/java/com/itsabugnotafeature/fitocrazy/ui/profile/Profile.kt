package com.itsabugnotafeature.fitocrazy.ui.profile

import android.content.Context
import android.content.res.ColorStateList
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.github.mikephil.charting.charts.CombinedChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.components.YAxis
import com.github.mikephil.charting.data.CombinedData
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.ValueFormatter
import com.itsabugnotafeature.fitocrazy.R
import com.itsabugnotafeature.fitocrazy.common.BodyWeightRecord
import com.itsabugnotafeature.fitocrazy.common.Converters
import com.itsabugnotafeature.fitocrazy.common.ExerciseDatabase
import kotlinx.coroutines.runBlocking
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

class Profile : Fragment() {

    private fun loadChartData(chart: CombinedChart) {
        val weightRecordings = runBlocking {
            val db = ExerciseDatabase.getInstance(requireContext()).exerciseDao()
            db.getBodyWeightRecords()
        }
        if (weightRecordings.isNullOrEmpty()) return

        val recordingDates = weightRecordings.map { it.timestamp }.toMutableList()
        val weights = weightRecordings.map { it.weight }

        val weightDataSet = LineDataSet(List(recordingDates.size) { i ->
            Entry(
                i.toFloat(),
                weights[i].toFloat()
            )
        }, "KG")

        weightDataSet.color = requireContext().getColor(R.color.blue_accent)
        weightDataSet.setCircleColor(requireContext().getColor(R.color.blue_accent))
        weightDataSet.setDrawCircleHole(false)
        weightDataSet.lineWidth = 2f
        weightDataSet.setDrawValues(true)
        weightDataSet.axisDependency = YAxis.AxisDependency.LEFT
        weightDataSet.mode = LineDataSet.Mode.HORIZONTAL_BEZIER

        val lineData = LineData(weightDataSet)
        lineData.setValueTextSize(16f)
        lineData.setValueFormatter(object : ValueFormatter() {
            override fun getFormattedValue(value: Float): String {
                return Converters.formatDoubleWeight(value.toDouble())
            }
        })

        val combinedData = CombinedData()
        combinedData.setData(lineData)

        chart.data = combinedData

        val xAxis = chart.xAxis
        xAxis.axisMinimum = lineData.xMin - 1
        xAxis.axisMaximum = lineData.xMax + 1

        recordingDates.add(0, (recordingDates.first() - 0.864).toLong())
        recordingDates.add((recordingDates.last() + 0.864).toLong())
        xAxis.valueFormatter = object : ValueFormatter() {
            override fun getFormattedValue(value: Float): String {
                if ((value.toInt() < 0) || (value.toInt() >= recordingDates.size - 2)) return ""

                return Converters.dateFormatter.format(
                    LocalDateTime.ofInstant(
                        Instant.ofEpochMilli(recordingDates[value.toInt() + 1]),
                        ZoneId.systemDefault()
                    )
                )
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

    }


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_profile, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

        val dateLabel = view.findViewById<TextView>(R.id.label_date)
        dateLabel.text = Converters.dateFormatter.format(
            Instant.ofEpochMilli(Instant.now().toEpochMilli()).atZone(ZoneId.systemDefault())
        )

        val chart = view.findViewById<CombinedChart>(R.id.chart_weightHistory)

        val btnEnterWeight = view.findViewById<Button>(R.id.btn_enterWeight)
        btnEnterWeight.setOnClickListener {
            val weightEditText = view.findViewById<EditText>(R.id.textEntry_currentWeight)
            val weightValue = weightEditText.text.toString().toDoubleOrNull() ?: return@setOnClickListener

            runBlocking {
                val db = ExerciseDatabase.getInstance(requireContext()).exerciseDao()
                db.addBodyWeightRecord(BodyWeightRecord(Instant.now().toEpochMilli(), weightValue))
                loadChartData(view.findViewById<CombinedChart>(R.id.chart_weightHistory))
            }

            weightEditText.clearComposingText()
            weightEditText.text.clear()
            val imm = weightEditText.context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(weightEditText.windowToken, 0)

            chart.invalidate()
        }

        val states = arrayOf(
            intArrayOf(android.R.attr.state_pressed), // selected
            intArrayOf(-android.R.attr.state_active), // notselected
        )
        val colors = intArrayOf(
            requireContext().getColor(R.color.purple_accent),
            requireContext().getColor(R.color.blue_main_light),
        )
        val myList = ColorStateList(states, colors)
        btnEnterWeight.backgroundTintList = myList

        btnEnterWeight.setOnLongClickListener {
            runBlocking {
                val db = ExerciseDatabase.getInstance(requireContext()).exerciseDao()
                db.deleteLastBodyWeightRecord()
                loadChartData(view.findViewById<CombinedChart>(R.id.chart_weightHistory))
            }

            chart.invalidate()
            Toast.makeText(requireContext(), "Deleted!", Toast.LENGTH_SHORT).show()
            true
        }

        chart.description.isEnabled = false
        chart.setDrawGridBackground(false)
        //chart.setScaleEnabled(false)
        chart.setBackgroundColor(requireContext().getColor(R.color.white))
        //chart.setTouchEnabled(true)

        loadChartData(chart)
    }
}