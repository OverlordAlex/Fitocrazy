package com.itsabugnotafeature.fitocrazy.common

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
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
    val displayName: String
) {
    override fun toString() = displayName
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

@Dao
interface ExerciseDao {
    @Transaction
    @Query("SELECT * FROM ExerciseModel")
    suspend fun getAllExercises(): List<ExerciseWithComponentModel>

    @Query("SELECT * FROM ExerciseComponentModel WHERE componentId = :id")
    suspend fun getExercise(id: Long): ExerciseComponentModel?

    @Query("SELECT * FROM ExerciseExerciseComponentCrossRef GROUP BY exerciseId HAVING componentId IN (:firstComponentId, :secondComponentId, :thirdComponentId) AND COUNT(exerciseId) = 3 LIMIT 1")
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
}

@Database(
    entities = [ExerciseModel::class, ExerciseComponentModel::class, ExerciseExerciseComponentCrossRef::class],
    version = 3
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