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
 * One finished agent run (Phase 13/17/26). Persisted locally so History
 * shows real executions instead of mock data. The log is stored as
 * compact "time|event" lines and the plan timeline as "label|state" lines
 * so the detail screen can replay both. Since Phase 26 every run also
 * records the target application package it drove, so the detail screen
 * and saved workflows name the real target.
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
    val targetPackage: String,
    val logText: String,
    val planText: String,
)

/**
 * One saved workflow (Phase 26): an instruction kept for one tap reuse,
 * together with the target application it was saved for. Saved workflows
 * are disposable local convenience data, not cloud documents.
 */
@Entity(tableName = "saved_workflows")
data class SavedWorkflowEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val instruction: String,
    val targetPackage: String,
    val createdAtMs: Long,
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

@Dao
interface SavedWorkflowDao {
    @Insert
    suspend fun insert(workflow: SavedWorkflowEntity): Long

    @Query("SELECT * FROM saved_workflows ORDER BY createdAtMs DESC")
    fun observeAll(): Flow<List<SavedWorkflowEntity>>

    @Query("DELETE FROM saved_workflows WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM saved_workflows")
    suspend fun clearAll()
}

@Database(
    entities = [AgentRunEntity::class, SavedWorkflowEntity::class],
    version = 3,
    exportSchema = false,
)
abstract class HistoryDatabase : RoomDatabase() {
    abstract fun agentRunDao(): AgentRunDao
    abstract fun savedWorkflowDao(): SavedWorkflowDao

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
                    // Phase 17: plan steps column added. Phase 26: target
                    // package column and the saved workflows table. Run
                    // history and saved workflows are local disposable
                    // data, so the schema bump recreates the tables
                    // instead of carrying hand written migrations.
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
    val targetPackage: String,
    val planSteps: List<Pair<String, TimelineItemState>>,
    val logLines: List<Pair<String, String>>,
)

/** Read model for one saved workflow (Phase 26). */
data class SavedWorkflowUi(
    val id: Long,
    val instruction: String,
    val targetPackage: String,
    val createdAtMs: Long,
)
