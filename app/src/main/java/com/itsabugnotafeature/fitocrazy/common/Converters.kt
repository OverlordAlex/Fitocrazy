package com.itsabugnotafeature.fitocrazy.common

import androidx.room.TypeConverter
import org.json.JSONArray
import org.json.JSONObject
import java.math.RoundingMode
import java.text.DecimalFormat
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Date
import java.util.Locale

class Converters {
    @TypeConverter
    fun fromTimestamp(value: Long?): LocalDate? {
        return value?.let { Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalDate() }
    }

    @TypeConverter
    fun dateToTimestamp(date: LocalDate?): Long? {
        return date?.atStartOfDay(ZoneId.systemDefault())?.toInstant()?.toEpochMilli()
    }

    companion object {
        fun dateToTimestamp(date: LocalDate): Long? {
            return date.atStartOfDay(ZoneId.systemDefault())?.toInstant()?.toEpochMilli()
        }

        val dateFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("dd LLL yyyy")

        fun formatDoubleWeight(weight: Double): String {
            return if (weight % 1 == 0.0) {
                // there is nothing behind the .
                weight.toInt().toString()
            } else {
                // prevent rounding and just drop the extra behind 2 decimal places
                String.format(Locale.US, "%.2f", (weight * 100).toInt() / 100.0)
            }
        }

        val decimalFormatter = DecimalFormat("###.##").apply { roundingMode = RoundingMode.FLOOR }
    }

}