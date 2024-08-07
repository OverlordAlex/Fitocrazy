package com.itsabugnotafeature.fitocrazy.common

import android.content.Context
import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Database
import androidx.room.DatabaseView
import androidx.room.Delete
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.Junction
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Relation
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Transaction
import androidx.room.TypeConverters
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

enum class ExerciseComponentType {
    EQUIPMENT, LOCATION, MOVEMENT
}

@Entity
data class ExerciseComponentModel(
    @PrimaryKey(autoGenerate = true) val componentId: Long,
    val name: String,
    val type: ExerciseComponentType
) : Comparable<ExerciseComponentModel> {
    override fun toString() = name
    override fun compareTo(other: ExerciseComponentModel) =
        this.type.ordinal.compareTo(other.type.ordinal)
}

@Entity
data class ExerciseModel(
    @PrimaryKey(autoGenerate = true) val exerciseId: Long,
    val displayName: String,
    @ColumnInfo(defaultValue = "10") val basePoints: Int,
    val bodyPartChips: String?,
) {
    override fun toString() = displayName
    fun getChips(): List<String> = bodyPartChips?.split(" ") ?: emptyList()
}

@Entity(primaryKeys = ["componentId", "exerciseId"])
data class ExerciseExerciseComponentCrossRef(
    val componentId: Long,
    val exerciseId: Long,
)

data class ExerciseWithComponentModel(
    @Embedded val exercise: ExerciseModel,

    @Relation(
        parentColumn = "exerciseId",
        entityColumn = "componentId",
        associateBy = Junction(ExerciseExerciseComponentCrossRef::class)
    )
    val components: List<ExerciseComponentModel>,
) {
    override fun toString(): String {
        return components.sorted().joinToString(" ")
    }
}

@Entity(primaryKeys = ["exerciseModelId", "exerciseId"])
data class ExerciseAndExerciseModelCrossRef(
    val exerciseModelId: Long,
    val exerciseId: Long,
)

@Entity
data class Exercise(
    @PrimaryKey(autoGenerate = true) var exerciseId: Long,
    val exerciseModelId: Long,
    val date: LocalDate,
    val order: Int,
) : Comparable<Exercise> {
    override fun compareTo(other: Exercise): Int {
        return if (this.date == other.date) this.order.compareTo(other.order) else this.date.compareTo(other.date)
    }

    fun toTimeStamp(): Long? = Converters.dateToTimestamp(date)
}

@Entity
data class Set(
    @PrimaryKey(autoGenerate = true) var setID: Long,
    val exerciseId: Long,
    val weight: Double,
    val reps: Int,
    val order: Int,
) : Comparable<Set> {
    override fun compareTo(other: Set): Int = this.order.compareTo(other.order)
}

@DatabaseView("SELECT exerciseModelId, max(weight) as maxWeight, max(reps) as maxReps, max(weight*reps) as mostWeightMoved FROM `Set` s JOIN exercise e ON s.exerciseId = e.exerciseId GROUP BY exerciseModelId")
data class SetRecordView(
    @PrimaryKey val exerciseModelId: Long,
    val maxWeight: Double,
    val maxReps: Int,
    val mostWeightMoved: Double,
)

@Entity
data class Workout(
    @PrimaryKey(autoGenerate = true) var workoutId: Long,
    val date: LocalDate,
    var totalPoints: Int = 0,
    var totalExercises: Int = 0,

    var totalWeight: Double = 0.0,
    var totalReps: Int = 0,
    var totalSets: Int = 0,

    var totalTime: Long = 0,

    var topTags: String = "",
)

@Dao
interface ExerciseDao {
    @Transaction
    @Query("SELECT * FROM ExerciseModel")
    suspend fun getAllExercises(): List<ExerciseWithComponentModel>

    @Query("SELECT * FROM ExerciseModel WHERE exerciseId = :id")
    suspend fun getExerciseDetails(id: Long): ExerciseWithComponentModel?

    @Query("SELECT * FROM ExerciseComponentModel WHERE componentId = :id")
    suspend fun getExercise(id: Long): ExerciseComponentModel?

    @Query("SELECT * FROM ExerciseExerciseComponentCrossRef WHERE componentId IN (:firstComponentId, :secondComponentId, :thirdComponentId) GROUP BY exerciseId HAVING COUNT(exerciseId) = 3 LIMIT 1")
    suspend fun getExercise(
        firstComponentId: Long,
        secondComponentId: Long,
        thirdComponentId: Long
    ): ExerciseExerciseComponentCrossRef?

    @Query("SELECT * FROM ExerciseComponentModel WHERE name = :name AND type = :type")
    suspend fun getExerciseComponent(
        name: String,
        type: ExerciseComponentType
    ): ExerciseComponentModel?

    @Query("SELECT * FROM ExerciseComponentModel WHERE type = :type")
    suspend fun getExerciseComponent(type: ExerciseComponentType): List<ExerciseComponentModel>

    @Insert
    suspend fun addExercise(exercise: ExerciseModel): Long

    @Insert
    suspend fun addExerciseComponent(exerciseComponent: ExerciseComponentModel): Long

    @Insert
    suspend fun addExerciseExerciseComponentCrossRef(crossRef: ExerciseExerciseComponentCrossRef): Long

    @Insert
    suspend fun addExerciseSet(exercise: Exercise): Long

    @Insert
    suspend fun addSetToExercise(set: Set): Long

    @Delete
    suspend fun deleteSetFromExercise(set: Set)

    @Delete
    suspend fun deleteExercise(exercise: Exercise)

    @Query("SELECT * FROM `Set` s JOIN (SELECT * FROM Exercise ORDER BY date DESC LIMIT 1, :nSets) as E ON s.exerciseId=E.exerciseId WHERE E.exerciseModelId=:exerciseModelId AND E.date < :excludeDate")
    suspend fun getHistoricalSets(exerciseModelId: Long, nSets: Int, excludeDate: Long? = 0): Map<Exercise, List<Set>>

    @Query("SELECT * FROM Exercise WHERE date = :date")
    suspend fun getListOfExercise(date: LocalDate): List<Exercise>

    @Query("SELECT * FROM `Set` WHERE exerciseId = :exerciseId")
    suspend fun getSets(exerciseId: Long): List<Set>

    @Query("SELECT * FROM SetRecordView WHERE exerciseModelId = :exerciseId")
    suspend fun getRecord(exerciseId: Long): SetRecordView?

    @Insert
    suspend fun addWorkout(workout: Workout): Long

    @Query("SELECT * FROM Workout WHERE workoutId = :workoutId")
    suspend fun getWorkout(workoutId: Long): Workout?

    @Update
    suspend fun updateWorkout(workout: Workout)

    @Query("SELECT * FROM Workout ORDER BY date DESC")
    suspend fun listWorkouts(): List<Workout>
}

@Database(
    entities = [ExerciseModel::class, ExerciseComponentModel::class, ExerciseExerciseComponentCrossRef::class, Exercise::class, Set::class, Workout::class],
    views = [SetRecordView::class],
    version = 5
)
@TypeConverters(Converters::class)
abstract class ExerciseDatabase : RoomDatabase() {
    abstract fun exerciseDao(): ExerciseDao

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
                    ).fallbackToDestructiveMigration().build()

                    INSTANCE = instance
                }
                return instance
            }
        }
    }
}