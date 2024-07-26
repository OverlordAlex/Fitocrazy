package com.itsabugnotafeature.fitocrazy.common

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Fts4
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase

@Fts4
@Entity
data class Exercise(
    @PrimaryKey(autoGenerate = true) val rowid: Int,
    val equipment: String,
    val position: String,
    val movement: String,
) {
    override fun toString(): String {
        return "$equipment $position $movement"
    }
}

@Dao
interface ExerciseDao {
    @Query("SELECT * FROM exercise")
    fun getAll(): List<Exercise>

    @Insert
    fun addExercise(exercise: Exercise)

    @Query("SELECT * FROM exercise_fts WHERE exercise_fts MATCH :query")
    fun searchExercises(query: String)
}

@Database(entities = [Exercise::class], version = 1)
abstract class ExerciseDatabase : RoomDatabase() {
    abstract fun exerciseDao(): ExerciseDao
}