package com.itsabugnotafeature.fitocrazy.common

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.core.graphics.blue
import androidx.core.net.toFile
import androidx.room.AutoMigration
import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Database
import androidx.room.DatabaseView
import androidx.room.Delete
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.Insert
import androidx.room.Junction
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Relation
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.Update
import com.itsabugnotafeature.fitocrazy.ui.workouts.workout.ExerciseListViewAdapter
import java.io.BufferedOutputStream
import java.io.DataInputStream
import java.io.File
import java.io.IOException
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.time.Instant
import java.time.LocalDate
import java.util.EnumSet
import java.util.Scanner
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import kotlin.math.max
import kotlin.math.pow

enum class ExerciseComponentType {
    EQUIPMENT, LOCATION, MOVEMENT
}

enum class RecordType {
    MAX_WEIGHT, MAX_REPS, MAX_WEIGHT_MOVED
}

data class ExerciseRecord(val oldBest: Number, val newBest: Number, val recordType: RecordType)
data class PointsResult(val points: Int, val records: List<ExerciseRecord>)
data class MostCommonExercisesAtWorkoutPosition(val order: Int, val exerciseIds: String, val counts: String) {
    // TODO: could this be done with getters and setters?
    private fun getExerciseIds() = exerciseIds.split(",").map { it.toLong() }
    private fun getCounts() = counts.split(",").map { it.toInt() }
    fun getCountPerExercise(): List<Pair<Long, Int>> {
        val countCache = getCounts()
        return getExerciseIds().mapIndexed { index, item -> Pair(item, countCache[index]) }
    }
}

@Entity
data class ExerciseComponentModel(
    @PrimaryKey(autoGenerate = true) var componentId: Long, val name: String, val type: ExerciseComponentType
) : Comparable<ExerciseComponentModel> {
    override fun toString() = name
    override fun compareTo(other: ExerciseComponentModel) = this.type.ordinal.compareTo(other.type.ordinal)

    override fun equals(other: Any?): Boolean {
        if (other is ExerciseComponentModel) return componentId == other.componentId
        return super.equals(other)
    }

    override fun hashCode(): Int {
        var result = componentId.hashCode()
        result = 31 * result + name.hashCode()
        result = 31 * result + type.hashCode()
        return result
    }
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

@Entity(primaryKeys = ["componentId", "exerciseId"]) // , indices = [Index("exerciseId", unique = false)]
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
    ) val components: List<ExerciseComponentModel>,
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
class Exercise(
    @PrimaryKey(autoGenerate = true) var exerciseId: Long,
    val exerciseModelId: Long,
    date: LocalDate,
    order: Int,
    val workoutId: Long,
    recordsAchieved: EnumSet<RecordType>? = null
) : Comparable<Exercise> {
    var date: LocalDate = date
        set(value) {
            dirty = true
            field = value
        }
    var order: Int = order
        set(value) {
            dirty = true
            field = value
        }

    @ColumnInfo(defaultValue = "0.0")
    var recordsAchieved: EnumSet<RecordType>? = recordsAchieved
        set(value) {
            dirty = true
            field = value
        }

    fun addRecords(records: List<ExerciseRecord>) {
        if (records.isEmpty()) return
        if (recordsAchieved == null) {
            recordsAchieved = EnumSet.copyOf(records.map { it.recordType })
        } else {
            recordsAchieved?.addAll(records.map { it.recordType }) ?: {}
        }
        dirty = true
    }

    @Ignore
    private var dirty: Boolean = false
    fun isDirty() = dirty
    fun clearDirty() {
        dirty = false
    }

    override fun compareTo(other: Exercise): Int {
        return if (this.date == other.date) this.order.compareTo(other.order) else this.date.compareTo(other.date)
    }

    fun toTimeStamp(): Long? = Converters.dateToTimestamp(date)

    override fun toString(): String {
        return "[$order] $exerciseId in $workoutId"
    }
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

@DatabaseView("SELECT avg(totalPoints) AS avgTotalPoints FROM (SELECT totalPoints FROM Workout ORDER BY date DESC LIMIT 10)")
data class WorkoutRecordView(
    val avgTotalPoints: Double
)

@DatabaseView("SELECT E.exerciseId AS exerciseModelId, displayName, max(date) as date, COUNT(DISTINCT workoutId) AS count, bodyPartChips FROM (SELECT * FROM ExerciseModel) AS E LEFT JOIN Exercise on Exercise.exerciseModelId=E.exerciseId GROUP BY E.exerciseId ORDER BY count DESC, date DESC")
data class MostCommonExerciseView(
    val exerciseModelId: Long,
    val displayName: String,
    val date: LocalDate?,
    val count: Int,
    val bodyPartChips: String,
) : Comparable<MostCommonExerciseView> {
    override fun compareTo(other: MostCommonExerciseView): Int {
        return if (count == other.count) return date?.compareTo(other.date) ?: displayName.compareTo(other.displayName)
        else count.compareTo(other.count)
    }
}

@DatabaseView("SELECT DISTINCT strftime('%Y-%m', date / 1000, 'unixepoch') AS display FROM workout ORDER BY display DESC")
class WorkoutDatesView {
    @Ignore
    lateinit var month: String

    @Ignore
    lateinit var year: String

    var display: String = ""
        set(value) {
            value.split("-").let {
                year = it[0]
                month = it[1]
            }
            field = value
        }

    fun toDate(): LocalDate = LocalDate.of(year.toInt(), month.toInt(), 1)
}

@Entity
data class Workout(
    @PrimaryKey(autoGenerate = true) var workoutId: Long,
    var date: Long,
    var totalPoints: Int = 0,
    var totalExercises: Int = 0,

    var totalWeight: Double = 0.0,
    var totalReps: Int = 0,
    var totalSets: Int = 0,

    var totalTime: Long = 0,

    var topTags: String = "",
) : Comparable<Workout> {
    override fun compareTo(other: Workout): Int {
        return -this.date.compareTo(other.date)
    }

    fun recalculateWorkoutTotals(exerciseList: List<ExerciseListViewAdapter.ExerciseView>) {
        totalWeight = 0.0
        totalReps = 0
        totalSets = 0
        totalPoints = 0
        totalExercises = exerciseList.size

        exerciseList.forEach { exercise ->
            totalSets += exercise.sets.size
            totalReps += exercise.sets.fold(0) { acc, set -> acc + set.reps }
            totalWeight += exercise.sets.fold(0.0) { acc, set -> acc + (set.reps * set.weight) }
            totalPoints += calculatePoints(exercise).points
        }
    }

    companion object {
        fun calculatePoints(exercise: ExerciseListViewAdapter.ExerciseView): PointsResult {
            if (exercise.sets.isEmpty()) return PointsResult(0, emptyList())

            var points = 0.0
            val records: MutableList<ExerciseRecord> = mutableListOf()

            val maxWeightThisSet = exercise.sets.maxOf { it.weight }
            val maxRepsThisSet = exercise.sets.maxOf { it.reps }
            val maxMovedThisSet = exercise.sets.maxOf { it.weight * it.reps }

            val exerciseMaxWeight = exercise.record?.maxWeight ?: maxWeightThisSet
            val exerciseMaxReps = exercise.record?.maxReps ?: maxRepsThisSet
            val exerciseMaxMoved = exercise.record?.mostWeightMoved ?: maxMovedThisSet

            if (maxWeightThisSet > exerciseMaxWeight) {
                points += 100
                records.add(
                    ExerciseRecord(
                        exercise.record?.maxWeight ?: 0.0, maxWeightThisSet, RecordType.MAX_WEIGHT
                    )
                )
            }

            if (maxRepsThisSet > exerciseMaxReps) {
                points += 75
                records.add(
                    ExerciseRecord(
                        exercise.record?.maxReps ?: 0.0, maxRepsThisSet, RecordType.MAX_REPS
                    )
                )
            }

            if (maxMovedThisSet > exerciseMaxMoved) {
                points += 100
                records.add(
                    ExerciseRecord(
                        exercise.record?.mostWeightMoved ?: 0.0, maxMovedThisSet, RecordType.MAX_WEIGHT_MOVED
                    )
                )
            }

            var maxWeightSoFar = exercise.sets.firstOrNull()?.weight ?: 0.0
            exercise.sets.forEach {
                val repMultiplier: Double = when {
                    it.reps <= 4 -> 0.9
                    it.reps <= 8 -> 1.0
                    else -> 1.05
                }

                maxWeightSoFar = max(maxWeightSoFar, it.weight)
                points += exercise.basePoints.toDouble().pow((it.weight / maxWeightSoFar)) * (it.reps * repMultiplier)
            }

            return PointsResult(points.toInt(), records)
        }
    }
}

@Entity
data class BodyWeightRecord(
    @PrimaryKey val timestamp: Long,
    val weight: Double,
)

@Dao
interface ExerciseDao {
// do joins need @Transaction ?

    @Query("SELECT EM.exerciseId, displayName, basePoints, bodyPartChips FROM exerciseexercisecomponentcrossref CR JOIN exercisemodel EM ON EM.exerciseId=CR.exerciseId WHERE CR.componentId = :componentId")
    suspend fun getExerciseDetailsWithComponent(componentId: Long): List<ExerciseModel>

    @Query("SELECT * FROM ExerciseModel WHERE exerciseId = :id")
    suspend fun getExerciseDetails(id: Long): ExerciseWithComponentModel?

    @Query("SELECT * FROM ExerciseModel")
    suspend fun getExercises(): List<ExerciseWithComponentModel>

    @Query("SELECT COUNT(*) FROM Exercise WHERE exerciseModelId = :exerciseModelId LIMIT 1")
    suspend fun getExerciseCount(exerciseModelId: Long): Int?

    @Query("SELECT * FROM ExerciseComponentModel WHERE componentId = :id")
    suspend fun getExercise(id: Long): ExerciseComponentModel?

    @Update
    suspend fun updateExerciseComponent(exerciseComponent: ExerciseComponentModel)

    @Query("SELECT exerciseId FROM ExerciseExerciseComponentCrossRef WHERE componentId = :exerciseComponentId")
    suspend fun getExercisesUsingComponent(exerciseComponentId: Long): List<Long>

    @Query("UPDATE ExerciseModel SET displayName = :displayName WHERE exerciseId = :exerciseId")
    suspend fun updateExerciseDisplayName(exerciseId: Long, displayName: String)

    @Query("DELETE FROM ExerciseModel WHERE exerciseId = :exerciseModelId")
    suspend fun deleteExerciseModel(exerciseModelId: Long)

    @Query("SELECT * FROM ExerciseExerciseComponentCrossRef WHERE componentId IN (:firstComponentId, :secondComponentId, :thirdComponentId) GROUP BY exerciseId HAVING COUNT(exerciseId) = 3 LIMIT 1")
    suspend fun getExercise(
        firstComponentId: Long, secondComponentId: Long, thirdComponentId: Long
    ): ExerciseExerciseComponentCrossRef?

    @Query("SELECT * FROM ExerciseComponentModel WHERE name = :name AND type = :type")
    suspend fun getExerciseComponent(
        name: String, type: ExerciseComponentType
    ): ExerciseComponentModel?

    @Query("SELECT * FROM ExerciseComponentModel WHERE type = :type")
    suspend fun getExerciseComponent(type: ExerciseComponentType): List<ExerciseComponentModel>

    @Query("DELETE FROM ExerciseComponentModel WHERE componentId = :exerciseComponentId")
    suspend fun deleteExerciseComponent(exerciseComponentId: Long)

    @Insert
    suspend fun addExercise(exercise: ExerciseModel): Long

    @Insert
    suspend fun addExerciseComponent(exerciseComponent: ExerciseComponentModel): Long

    @Insert
    suspend fun addExerciseExerciseComponentCrossRef(crossRef: ExerciseExerciseComponentCrossRef): Long

    @Query("DELETE FROM ExerciseExerciseComponentCrossRef WHERE exerciseId = :exerciseId")
    suspend fun deleteExerciseComponentCrossRefByExerciseId(exerciseId: Long)

    @Query("SELECT * FROM Exercise WHERE exerciseModelId = :exerciseId ORDER BY date DESC LIMIT 1")
    suspend fun getLastExerciseOccurrence(exerciseId: Long): Exercise?

    @Insert
    suspend fun addExerciseSet(exercise: Exercise): Long

    @Insert
    suspend fun addSetToExercise(set: Set): Long

    @Delete
    suspend fun deleteSetFromExercise(set: Set)

    @Delete
    suspend fun deleteExercise(exercise: Exercise)

    @Update
    suspend fun updateExercise(exercise: Exercise)

    @Query("DELETE from Exercise WHERE workoutId = :workoutId")
    suspend fun deleteExercisesInWorkout(workoutId: Long)

    @Query("SELECT * FROM `Set` as s JOIN (SELECT * FROM Exercise ORDER BY date DESC) as E ON s.exerciseId=E.exerciseId WHERE E.exerciseModelId=:exerciseModelId AND workoutId in (SELECT workoutId FROM Exercise EX WHERE EX.exerciseModelId = :exerciseModelId AND EX.date < :excludeDate GROUP BY EX.workoutId ORDER BY EX.date DESC LIMIT :nSets)")
    suspend fun getHistoricalSets(
        exerciseModelId: Long, nSets: Int = Int.MAX_VALUE, excludeDate: Long? = Long.MAX_VALUE
    ): Map<Exercise, List<Set>>

    @Query("SELECT * FROM Exercise WHERE workoutId = :workoutId")
    suspend fun getListOfExerciseInWorkout(workoutId: Long): List<Exercise>

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

    @Query("SELECT * FROM Workout WHERE date >= :start AND date < :end ORDER BY date DESC")
    suspend fun listWorkouts(start: Long = Long.MAX_VALUE, end: Long = 0): List<Workout>

    @Delete
    suspend fun deleteWorkout(workout: Workout)

    @Query("SELECT * FROM WorkoutRecordView LIMIT 1")
    suspend fun getWorkoutStats(): WorkoutRecordView?

    @Query("SELECT * FROM MostCommonExerciseView WHERE (date < :today OR date IS NULL) AND exerciseModelId NOT IN (:existingExercises) ORDER BY count DESC, date DESC, displayName ASC")
    suspend fun getMostCommonExercises(
        today: LocalDate, existingExercises: List<Long> = emptyList()
    ): List<MostCommonExerciseView>

    @Query("SELECT * FROM WorkoutDatesView")
    suspend fun getMonthsPresentInData(): List<WorkoutDatesView>

    @Insert
    suspend fun addBodyWeightRecord(record: BodyWeightRecord)

    @Query("SELECT * FROM BodyWeightRecord ORDER BY timestamp ASC")
    suspend fun getBodyWeightRecords(): List<BodyWeightRecord>

    @Query("DELETE FROM BodyWeightRecord WHERE timestamp = (SELECT MAX(timestamp) FROM BodyWeightRecord)")
    suspend fun deleteLastBodyWeightRecord()

    // TODO this should be moved to a view for performance
    @Query("select `order`, group_concat(IDs) as exerciseIds, group_concat(Count) as counts  from (select  `order`, group_concat(distinct exerciseModelId) as IDs , count(exerciseModelId) as Count from Exercise E group by `order`, exerciseModelId order by Count desc)  group by `order` ")
    suspend fun getExercisesWithOrders(): List<MostCommonExercisesAtWorkoutPosition>

}

@Database(
    entities = [ExerciseModel::class, ExerciseComponentModel::class, ExerciseExerciseComponentCrossRef::class, Exercise::class, Set::class, Workout::class, BodyWeightRecord::class],
    views = [SetRecordView::class, WorkoutRecordView::class, MostCommonExerciseView::class, WorkoutDatesView::class],
    version = 18,
    exportSchema = true,
    autoMigrations = [
        AutoMigration(from = 13, to = 14),
        AutoMigration(from = 14, to = 15),
        AutoMigration(from = 15, to = 16),
        AutoMigration(from = 16, to = 17),
        AutoMigration(from = 17, to = 18),
    ],
)
@TypeConverters(Converters::class)
abstract class ExerciseDatabase : RoomDatabase() {
    abstract fun exerciseDao(): ExerciseDao

    companion object {
        private const val DATABASE_NAME = "exercises.db"
        private const val DATABASE_BACKUP_SUFFIX = "-backup"
        private const val SQLITE_WALFILE_SUFFIX = "-wal"
        private const val SQLITE_SHMFILE_SUFFIX = "-shm"

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
                        context.applicationContext, ExerciseDatabase::class.java, DATABASE_NAME
                    ).fallbackToDestructiveMigrationOnDowngrade().build()

                    INSTANCE = instance
                }
                return instance
            }
        }

        fun backupDatabase(context: Context, resultUri: Uri): Long {
            val dbFile = context.getDatabasePath(DATABASE_NAME)
            val dbFilePath = dbFile.path
            val dbWalFile = File(dbFilePath + SQLITE_WALFILE_SUFFIX)
            val dbShmFile = File(dbFilePath + SQLITE_SHMFILE_SUFFIX)

            val backupFile = context.contentResolver.openOutputStream(resultUri, "w")
            //val backupFile = File(resultUri.path ?: (dbFilePath + DATABASE_BACKUP_SUFFIX))
            /*val backupWalFile = File(dbFilePath + DATABASE_BACKUP_SUFFIX + SQLITE_WALFILE_SUFFIX)
            val backupShmFile = File(dbFilePath + DATABASE_BACKUP_SUFFIX + SQLITE_SHMFILE_SUFFIX)*/

            // Remove existing backup
            // TODO: ask user if overwrite okay
            //if (backupFile.exists()) backupFile.delete()
            /*if (backupWalFile.exists()) backupWalFile.delete()
            if (backupShmFile.exists()) backupShmFile.delete()*/

            synchronized(this) {
                INSTANCE?.close()
                INSTANCE = null
                val zipFile = ZipOutputStream(BufferedOutputStream(backupFile))
                // TODO: ask user for location
                try {
                    zipFile.putNextEntry(ZipEntry("db_file"))
                    zipFile.write(dbFile.readBytes())
                    zipFile.closeEntry()
                    Log.i("test", "wrote db_file")

                    if (dbWalFile.exists()) {
                        zipFile.putNextEntry(ZipEntry("db_wal_file"))
                        zipFile.write(dbWalFile.readBytes())
                        zipFile.closeEntry()
                        Log.i("test", "wrote db_wal_file")
                    }

                    if (dbShmFile.exists()) {
                        zipFile.putNextEntry(ZipEntry("db_shm_file"))
                        zipFile.write(dbShmFile.readBytes())
                        zipFile.closeEntry()
                        Log.i("test", "wrote db_shm_file")
                    }

                    //dbFile.copyTo(backupFile, true)
                    /*if (dbWalFile.exists()) dbWalFile.copyTo(backupWalFile)
                if (dbShmFile.exists()) dbShmFile.copyTo(backupShmFile)*/
                } catch (e: IOException) {
                    Log.e("test", "Failed to backup", e)
                    return -1
                } finally {
                    zipFile.close()
                    //backupFile?.close()
                }
                getInstance(context)
            }
            return Instant.now().toEpochMilli()
            //return backupFile.lastModified()
        }

        fun restoreDatabase(context: Context, resultUri: Uri): Long {
            synchronized(this) {
                INSTANCE?.close()
                INSTANCE = null

                val dbFile = context.getDatabasePath(DATABASE_NAME)
                val dbFilePath = dbFile.path
                val dbBackupFile = File(dbFilePath + DATABASE_BACKUP_SUFFIX)
                val dbBackupWalFile = File(dbFilePath + SQLITE_WALFILE_SUFFIX + DATABASE_BACKUP_SUFFIX)
                val dbBackupShmFile = File(dbFilePath + SQLITE_SHMFILE_SUFFIX + DATABASE_BACKUP_SUFFIX)

                val zipFile = ZipInputStream(context.contentResolver.openInputStream(resultUri))
                while (zipFile.available() > 0) {
                    val next = zipFile.nextEntry
                    Log.i("test", next.name)

                    when (next.name) {
                        "db_file" -> {
                            val output = dbBackupFile.outputStream()
                            val b = ByteArray(2048)
                            var len = zipFile.read(b)
                            while (len > 0) {
                                output.write(b, 0, len)
                                len = zipFile.read(b)
                            }
                            output.close()
                            Log.i("test", "wrote db_file")
                            //
                            //
                            //
                            zipFile.closeEntry()
                        }

                        "db_wal_file" -> {
                            val output = dbBackupWalFile.outputStream()
                            val b = ByteArray(2048)
                            var len = zipFile.read(b)
                            while (len > 0) {
                                output.write(b, 0, len)
                                len = zipFile.read(b)
                            }
                            output.close()
                            Log.i("test", "wrote db_wal_file")
                            /*val b = ByteArray(next.size.toInt())
                        zipFile.read(b, 0, next.size.toInt())
                        dbWalFile.outputStream().write(b)*/
                            //dbWalFile.outputStream().write(zipFile.readBytes())
                            zipFile.closeEntry()
                        }

                        "db_shm_file" -> {
                            val output = dbBackupShmFile.outputStream()
                            val b = ByteArray(2048)
                            var len = zipFile.read(b)
                            while (len > 0) {
                                output.write(b, 0, len)
                                len = zipFile.read(b)
                            }
                            output.close()
                            Log.i("test", "wrote db_shm_file")
                            //zipFile.closeEntry()
                            /*val b = ByteArray(next.size.toInt())
                        zipFile.read(b, 0, next.size.toInt())
                        dbShmFile.outputStream().write(b)*/
                            //dbShmFile.outputStream().write(zipFile.readBytes())
                            zipFile.closeEntry()
                        }

                    }
                }
                zipFile.close()

                val dbWalFile = File(dbFilePath + SQLITE_WALFILE_SUFFIX)
                val dbShmFile = File(dbFilePath + SQLITE_SHMFILE_SUFFIX)

                //dbBackupFile.renameTo(dbFile)
                if (dbBackupWalFile.exists()) {
                    //dbBackupWalFile.renameTo(dbWalFile)
                    Files.copy(dbBackupWalFile.toPath(), dbWalFile.toPath(), StandardCopyOption.REPLACE_EXISTING)
                }
                if (dbBackupShmFile.exists()) {
                    //dbBackupShmFile.renameTo(dbShmFile)
                    Files.copy(dbBackupShmFile.toPath(), dbShmFile.toPath(), StandardCopyOption.REPLACE_EXISTING)
                }

                Files.copy(dbBackupFile.toPath(), dbFile.toPath(), StandardCopyOption.REPLACE_EXISTING)

                // TODO: restart app?
                // TODO: ask user for location
                //temp.copyTo(dbFile, overwrite = true)

                /*synchronized(this) {
                try {
                    if (backupFile != null) {
                        //dbFile.delete()
                        //dbFile.createNewFile()
                        //dbFile.writeBytes(backupFile.readBytes())
                        val bytes = ByteArray(backupFile.available())

                        DataInputStream(backupFile).readFully(bytes)
                        dbFile.outputStream().write(bytes)
                    }
                    backupFile.copyTo(dbFile, overwrite = true)
                } catch (e: IOException) {
                    Log.e("test", "Failed to restore", e)
                    return -1
                } finally {
                    backupFile?.close()
                    dbFile.outputStream().close()
                }
            }*/


                /*try {
                if (backupWalFile.exists()) {
                    backupWalFile.copyTo(dbWalFile, overwrite = true)
                }
            } catch (e: IOException) {
                Log.e("test", "Failed to restore", e)
                //return -1
            }

            try {
                if (backupShmFile.exists()) {
                    backupShmFile.copyTo(dbShmFile, overwrite = true)
                }
            } catch (e: IOException) {
                Log.e("test", "Failed to restore", e)
                //return -1
            }*/

                // recreate the DB


            }
            getInstance(context)
            return Instant.now().toEpochMilli()
            //return backupFile.lastModified()
        }

    }
}