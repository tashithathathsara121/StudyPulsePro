package com.studypulse.pro.data

import android.content.Context
import androidx.room.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

@Entity(tableName = "slots")
data class SlotEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val startMin: Int,
    val endMin: Int,
    val isBreak: Boolean
)

@Entity(tableName = "completions", primaryKeys = ["day", "slotId"])
data class Completion(val day: Long, val slotId: Int)

@Dao
interface StudyDao {
    @Query("SELECT * FROM slots ORDER BY startMin") fun slots(): Flow<List<SlotEntity>>
    @Query("SELECT * FROM slots ORDER BY startMin") suspend fun slotsNow(): List<SlotEntity>
    @Query("SELECT COUNT(*) FROM slots") suspend fun count(): Int
    @Query("DELETE FROM slots") suspend fun clear()
    @Insert suspend fun insert(s: SlotEntity)
    @Insert suspend fun insertAll(s: List<SlotEntity>)
    @Delete suspend fun delete(s: SlotEntity)
    @Insert(onConflict = OnConflictStrategy.IGNORE) suspend fun complete(c: Completion)
    @Query("SELECT slotId FROM completions WHERE day = :day") fun done(day: Long): Flow<List<Int>>
}

@Database(entities = [SlotEntity::class, Completion::class], version = 1, exportSchema = false)
abstract class Db : RoomDatabase() {
    abstract fun dao(): StudyDao

    companion object {
        @Volatile private var inst: Db? = null
        fun get(c: Context): Db = inst ?: synchronized(this) {
            inst ?: Room.databaseBuilder(c.applicationContext, Db::class.java, "studypulse.db")
                .build().also { inst = it }
        }
    }
}

object Seed {
    private val mutex = Mutex()
    private fun mm(x: String) = x.split(":").let { it[0].toInt() * 60 + it[1].toInt() }
    private fun s(t: String, a: String, b: String, br: Boolean = false) =
        SlotEntity(title = t, startMin = mm(a), endMin = mm(b), isBreak = br)

    val sample = listOf(
        s("IT2011 · AI & Machine Learning", "08:30", "09:30"),
        s("Break", "09:30", "09:45", true),
        s("SE2030 · Software Engineering", "09:45", "10:45"),
        s("Break", "10:45", "11:00", true),
        s("SE2072", "11:00", "12:00"),
        s("Lunch break", "12:00", "13:00", true),
        s("IT2011 · Practice & Notes", "13:00", "14:00"),
        s("Break", "14:00", "14:15", true),
        s("SE2030 · Assignments", "14:15", "15:15"),
        s("Break", "15:15", "15:30", true),
        s("SE2072 · Practice", "15:30", "16:30"),
        s("Rest", "16:30", "17:30", true),
        s("Self Study", "17:30", "18:30"),
        s("Dinner break", "18:30", "19:30", true),
        s("IT2011 · Revision", "19:30", "20:30"),
        s("Break", "20:30", "20:45", true),
        s("SE2030 · Revision", "20:45", "21:45"),
        s("Break", "21:45", "22:00", true),
        s("SE2072 · Revision", "22:00", "23:00"),
        s("Break", "23:00", "23:15", true),
        s("Daily Review", "23:15", "24:00")
    )

    suspend fun ifEmpty(dao: StudyDao) = mutex.withLock {
        if (dao.count() == 0) dao.insertAll(sample)
    }
}
