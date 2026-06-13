package com.jarvis.android.data.local

import android.content.Context
import androidx.room.*

@Entity(tableName = "memory_entries")
data class MemoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val userMessage: String,
    val response: String,
    val timestamp: Long
)

@Dao
interface MemoryDao {
    @Query("SELECT * FROM memory_entries ORDER BY timestamp DESC LIMIT :limit")
    suspend fun getRecent(limit: Int): List<MemoryEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entry: MemoryEntity)

    @Query("DELETE FROM memory_entries WHERE timestamp < :before")
    suspend fun deleteOlderThan(before: Long)
}

@Database(entities = [MemoryEntity::class], version = 1, exportSchema = false)
abstract class JarvisDatabase : RoomDatabase() {
    abstract fun memoryDao(): MemoryDao

    companion object {
        @Volatile private var INSTANCE: JarvisDatabase? = null

        fun getInstance(context: Context): JarvisDatabase {
            return INSTANCE ?: synchronized(this) {
                Room.databaseBuilder(
                    context.applicationContext,
                    JarvisDatabase::class.java,
                    "jarvis_db"
                ).build().also { INSTANCE = it }
            }
        }
    }
}

// Extension para converter entity → MemoryEntry
fun MemoryEntity.toMemoryEntry() = com.jarvis.android.memory.MemoryEntry(
    id = id,
    userMessage = userMessage,
    response = response,
    timestamp = timestamp
)

fun com.jarvis.android.memory.MemoryEntry.toEntity() = MemoryEntity(
    id = id,
    userMessage = userMessage,
    response = response,
    timestamp = timestamp
)
