package com.naze.motion.app.agent

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import com.naze.motion.app.ui.components.TimelineItemState
import kotlinx.coroutines.flow.Flow

/**
 * One finished agent run (Phase 13/17). Persisted locally so History shows
 * real executions instead of mock data. The log is stored as compact
 * "time|event" lines and the plan timeline as "label|state" lines so the
 * detail screen can replay both.
 */
@Entity(tableName = "agent_runs")
data class AgentRunEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val instruction: String,
    val outcome: String,
    val reason: String?,
    val actionCount: Int,
    val completedCount: Int,
    val durationMs: Long,
    val endedAtMs: Long,
    val logText: String,
    val planText: String,
)

@Dao
interface AgentRunDao {
    @Insert
    suspend fun insert(run: AgentRunEntity): Long

    @Query("SELECT * FROM agent_runs ORDER BY endedAtMs DESC")
    fun observeRuns(): Flow<List<AgentRunEntity>>

    @Query("DELETE FROM agent_runs WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM agent_runs")
    suspend fun clearAll()
}

@Database(entities = [AgentRunEntity::class], version = 2, exportSchema = false)
abstract class HistoryDatabase : RoomDatabase() {
    abstract fun agentRunDao(): AgentRunDao

    companion object {
        @Volatile
        private var instance: HistoryDatabase? = null

        fun get(context: Context): HistoryDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    HistoryDatabase::class.java,
                    "naze_history.db",
                )
                    // Phase 17: plan steps column added. Run history is local
                    // diagnostics data, so the schema bump recreates the table
                    // instead of carrying a hand written migration.
                    .fallbackToDestructiveMigration()
                    .build().also { instance = it }
            }
    }
}

/** Read model for the History and Workflow detail screens. */
data class AgentRunUi(
    val id: Long,
    val instruction: String,
    val outcome: String,
    val reason: String?,
    val actionCount: Int,
    val completedCount: Int,
    val durationMs: Long,
    val endedAtMs: Long,
    val planSteps: List<Pair<String, TimelineItemState>>,
    val logLines: List<Pair<String, String>>,
)
