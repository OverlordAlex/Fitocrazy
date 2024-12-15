package com.itsabugnotafeature.fitocrazy.common

import androidx.room.TypeConverter
import java.math.RoundingMode
import java.text.DecimalFormat
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.EnumSet
import java.util.Locale
import kotlin.math.pow

class Converters {
    @TypeConverter
    fun fromTimestamp(value: Long?): LocalDate? {
        return value?.let { Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalDate() }
    }

    @TypeConverter
    fun dateToTimestamp(date: LocalDate?): Long? {
        return date?.atStartOfDay(ZoneId.systemDefault())?.toInstant()?.toEpochMilli()
    }

    @TypeConverter
    fun fromLong(value: Long?): EnumSet<RecordType> {
        if (value == null || value == 0L) return EnumSet.noneOf(RecordType::class.java)
        var result = mutableListOf<RecordType>()
        var bitflag = value ?: 0  // not required, except to make xor happy
        while (bitflag != 0L) {
            val ordinal = bitflag.takeLowestOneBit()

            result.add(RecordType.entries[ordinal.countTrailingZeroBits()])
            bitflag = bitflag.xor(ordinal)
        }
        return EnumSet.copyOf(result)
    }

    @TypeConverter
    fun enumSetToLong(values: EnumSet<RecordType>?): Long? {
        return values?.fold(0.0) {acc, next -> acc + 2.0.pow(next.ordinal) }?.toLong()
    }

    companion object {
        fun dateToTimestamp(date: LocalDate): Long? {
            return date.atStartOfDay(ZoneId.systemDefault())?.toInstant()?.toEpochMilli()
        }

        val dateFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("dd LLL yyyy")
        val monthFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy LLLL")

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