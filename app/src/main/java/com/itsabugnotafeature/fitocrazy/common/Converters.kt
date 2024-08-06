package com.itsabugnotafeature.fitocrazy.common

import androidx.room.TypeConverter
import org.json.JSONArray
import org.json.JSONObject
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.Date

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
    }
}