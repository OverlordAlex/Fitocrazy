package com.itsabugnotafeature.fitocrazy.ui.profile

import android.content.Context
import android.content.Intent
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
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.findFragment
import com.github.mikephil.charting.charts.CombinedChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.components.YAxis
import com.github.mikephil.charting.data.CombinedData
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.ValueFormatter
import com.itsabugnotafeature.fitocrazy.R
import com.itsabugnotafeature.fitocrazy.common.ApplicationConfig
import com.itsabugnotafeature.fitocrazy.common.BodyWeightRecord
import com.itsabugnotafeature.fitocrazy.common.Converters
import com.itsabugnotafeature.fitocrazy.common.ExerciseDatabase
import com.itsabugnotafeature.fitocrazy.common.ExerciseDatabase.Companion.getInstance
import kotlinx.coroutines.runBlocking
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import kotlin.math.pow

class Profile : Fragment() {

    private lateinit var getBackupLocation: ActivityResultLauncher<Intent>
    private lateinit var getRestoreLocation: ActivityResultLauncher<Intent>

    private fun loadChartData(chart: CombinedChart) {
        val weightRecordings = runBlocking {
            val db = ExerciseDatabase.getInstance(requireContext()).exerciseDao()
            db.getBodyWeightRecords()
        }
        if (weightRecordings.isEmpty()) return

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

        if (weights.size > 2) {
            val lastFiveWeights = weightDataSet.values.takeLast(5).toList()
            // Calculations source: https://math.stackexchange.com/a/204021
            val slope =
                ((lastFiveWeights.size * lastFiveWeights.sumOf { it.x.toLong() * it.y.toLong() }) -
                        (lastFiveWeights.sumOf { it.x.toLong() } * lastFiveWeights.sumOf { it.y.toLong() })) /
                        ((lastFiveWeights.size * lastFiveWeights.sumOf { it.x.pow(2).toLong() }) -
                                (lastFiveWeights.sumOf { it.x.toDouble() }.pow(2))
                                )
            val offset =
                (lastFiveWeights.sumOf { it.y.toLong() } - slope * (lastFiveWeights.sumOf { it.x.toLong() })) / lastFiveWeights.size

            val trendLineData = LineDataSet(
                listOf(
                    Entry(
                        weightDataSet.values.first().x - 1,
                        (lastFiveWeights.first().x * slope + offset).toFloat()
                    ),
                    Entry(
                        lastFiveWeights.last().x + 1,
                        (lastFiveWeights.last().x * slope + offset).toFloat()
                    )
                ), "trend"
            )
            //Log.i("TEXT", "${trendLineData.values[0]} :: ${trendLineData.values[1]}")
            val trendLine = LineData(trendLineData)
            trendLine.setDrawValues(false)
            lineData.addDataSet(trendLineData)
        }

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
        getBackupLocation = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result: ActivityResult ->
            val resultUri = result.data?.data ?: return@registerForActivityResult
            val labelLastExport = view?.findViewById<TextView>(R.id.label_lastExportTimestamp)

            runBlocking {

                labelLastExport?.text = Instant.ofEpochMilli(ExerciseDatabase.backupDatabase(requireContext(), resultUri)).toString()
            }
        }

        getRestoreLocation = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result: ActivityResult ->
            val resultUri = result.data?.data ?: return@registerForActivityResult
            val labelLastExport = view?.findViewById<TextView>(R.id.label_lastExportTimestamp)

            runBlocking {
                labelLastExport?.text = Instant.ofEpochMilli(ExerciseDatabase.restoreDatabase(requireContext(), resultUri)).toString()
            }

            // refresh the current fragment
            val fragment = view?.findFragment<Profile>()
            if (fragment != null) {
                parentFragmentManager.beginTransaction().detach(fragment).commit()
                parentFragmentManager.beginTransaction().attach(fragment).commit()
            }
            //labelLastExport.text = ExerciseDatabase.backupDatabase(requireContext(), resultUri).toString()
        }

        return inflater.inflate(R.layout.fragment_profile, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

        val exportDatabaseButton = view.findViewById<Button>(R.id.btn_exportData)
        val importDatabaseButton = view.findViewById<Button>(R.id.btn_importData)
        val labelLastExport = view.findViewById<TextView>(R.id.label_lastExportTimestamp)

        exportDatabaseButton.setOnClickListener {
            val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                type = "application/zip"
                putExtra(Intent.EXTRA_TITLE, "exercises.zip")
            }
            getBackupLocation.launch(intent)
        }
        importDatabaseButton.setOnClickListener {
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                type = "application/zip"
            }
            getRestoreLocation.launch(intent)
        }

        runBlocking {
            val db = ExerciseDatabase.getInstance(requireContext()).exerciseDao()
            val appConfig = db.getApplicationConfig() ?: ApplicationConfig()
            labelLastExport.text = Instant.ofEpochMilli(appConfig.databaseLastBackupTime).toString()
        }



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
                ExerciseDatabase.getInstance(requireContext()).exerciseDao().deleteLastBodyWeightRecord()
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

    companion object {
        private const val TAG = "ProfileFragment"
    }
}