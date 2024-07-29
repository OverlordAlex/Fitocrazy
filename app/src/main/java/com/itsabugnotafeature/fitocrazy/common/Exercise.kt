package com.itsabugnotafeature.fitocrazy.common

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.Fts4
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Relation
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Transaction

//@Fts4
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
)

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

    @Insert
    suspend fun addExercise(exercise: Exercise)

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

@Database(entities = [Exercise::class, Equipment::class, Movement::class, Position::class], version = 1)
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