package com.itsabugnotafeature.fitocrazy.common

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Relation
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Transaction
import androidx.room.TypeConverters
import java.util.Date

@Entity
data class Workout(
    @PrimaryKey(autoGenerate = true) val id: Int,
    val date: Date,
    val exerciseSets: List<ExerciseSets>
)

data class ExerciseSets(
    val exercise: ExerciseWithComponents,
    val sets: List<Set>
)

data class Set(
    val weight: Double,
    val reps: Int,
) {
    override fun toString(): String {
        return "${weight}kg * $reps"
    }
}

@Entity
data class Exercise(
    @PrimaryKey(autoGenerate = true) val rowid: Int,

    val equipmentId: Long,
    val positionId: Long,
    val movementId: Long,
)

data class ExerciseWithComponents(
    @Embedded val exercise: Exercise,
    @Relation(
        parentColumn = "equipmentId",
        entityColumn = "id"
    )
    val equipment: Equipment,
    @Relation(
        parentColumn = "positionId",
        entityColumn = "id"
    )
    val position: Position,
    @Relation(
        parentColumn = "movementId",
        entityColumn = "id"
    )
    val movement: Movement
) {
    override fun toString(): String {
        return "$equipment $position $movement"
    }
}

abstract class ExerciseComponent {
    abstract val id: Int
    abstract val name: String

     override fun toString(): String {
        return name
    }

}

@Entity
data class Equipment(
    @PrimaryKey(autoGenerate = true) override val id: Int,
    override val name: String
): ExerciseComponent() {
    override fun toString(): String {
        return super.toString()
    }
}

@Entity
data class Position(
    @PrimaryKey(autoGenerate = true) override val id: Int,
    override val name: String
): ExerciseComponent() {
    override fun toString(): String {
        return name
    }
}

@Entity
data class Movement(
    @PrimaryKey(autoGenerate = true) override val id: Int,
    override val name: String
): ExerciseComponent() {
    override fun toString(): String {
        return name
    }
}

@Dao
interface ExerciseDao {
    @Transaction
    @Query("SELECT * FROM exercise")
    suspend fun getAll(): List<ExerciseWithComponents>

    @Transaction
    @Query("SELECT * FROM exercise WHERE equipmentId=:equipmentId AND positionId=:positionId AND movementId=:movementId")
    suspend fun getExactExercise(equipmentId: Long, positionId: Long, movementId: Long): ExerciseWithComponents?

    @Insert
    suspend fun addExercise(exercise: Exercise): Long

    /*@Query("SELECT * FROM exercise_fts WHERE exercise_fts MATCH :query")
    fun searchExercises(query: String): List<Exercise>*/
}

@Dao
interface ExerciseComponentsDao {
    @Query("Select * FROM equipment")
    suspend fun getAllEquipment(): List<Equipment>

    @Query("Select * FROM equipment WHERE name = :name")
    suspend fun getEquipment(name: String): Equipment?

    @Query("Select * FROM position")
    suspend fun getAllPosition(): List<Position>

    @Query("Select * FROM position WHERE name = :name")
    suspend fun getPosition(name: String): Position?

    @Query("Select * FROM movement")
    suspend fun getAllMovement(): List<Movement>

    @Query("Select * FROM movement WHERE name = :name")
    suspend fun getMovement(name: String): Movement?

    @Insert
    suspend fun addEquipment(equipment: Equipment): Long

    @Insert
    suspend fun addPosition(position: Position): Long

    @Insert
    suspend fun addMovement(movement: Movement): Long
}

interface WorkoutDao {
    @Transaction
    @Query("SELECT * FROM workout ORDER by date DESC LIMIT 3")
    suspend fun getRecentWorkouts(): List<Workout>

    // TODO check and get relationships working
    /*@Query("SELECT * FROM workout WHERE exerciseSets = :exerciseId ORDER by date DESC LIMIT 1")
    suspend fun getLastSetsByExercise(exerciseId: Long): List<Set>*/

    @Transaction
    @Insert
    suspend fun addWorkout(workout: Workout): Long
}

@Database(entities = [Exercise::class, Equipment::class, Movement::class, Position::class], version = 1)
@TypeConverters(Converters::class)
abstract class ExerciseDatabase : RoomDatabase() {
    abstract fun exerciseDao(): ExerciseDao
    abstract fun exerciseComponentsDao(): ExerciseComponentsDao

    companion object {
        private const val DATABASE_NAME = "exercises.db"

        /**
         * As we need only one instance of db in our app will use to store
         * This is to avoid memory leaks in android when there exist multiple instances of db
         */
        @Volatile
        private var INSTANCE: ExerciseDatabase? = null

        fun getInstance(context: Context): ExerciseDatabase {

            synchronized(this) {
                var instance = INSTANCE

                if (instance == null) {
                    instance = Room.databaseBuilder(
                        context.applicationContext,
                        ExerciseDatabase::class.java,
                        DATABASE_NAME
                    ).build()

                    INSTANCE = instance
                }
                return instance
            }
        }
    }
}