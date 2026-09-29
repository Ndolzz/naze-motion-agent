package com.naze.motion.core.engine

/** Structured engine log levels (NMA-ACTION-011). */
enum class EngineLogLevel { DEBUG, INFO, WARNING, ERROR }

/**
 * One structured log event. Machine readable, never free-form blobs.
 * Fields: timestamp, level, event type, message, optional task and action ids,
 * and string detail pairs.
 */
data class EngineLogEvent(
    val timestampMs: Long,
    val level: EngineLogLevel,
    val type: String,
    val message: String,
    val taskId: String? = null,
    val actionId: String? = null,
    val detail: Map<String, String> = emptyMap(),
)

/**
 * In-memory structured log with an optional live sink for UI streaming.
 * Events are immutable once appended; snapshot gives a stable copy.
 */
class EngineLog(private val clock: () -> Long = { System.currentTimeMillis() }) {
    private val lock = Any()
    private val events = mutableListOf<EngineLogEvent>()

    @Volatile
    private var sink: ((EngineLogEvent) -> Unit)? = null

    fun onEvent(listener: (EngineLogEvent) -> Unit) {
        sink = listener
    }

    fun debug(type: String, message: String, taskId: String? = null, actionId: String? = null, detail: Map<String, String> = emptyMap()) =
        append(EngineLogLevel.DEBUG, type, message, taskId, actionId, detail)

    fun info(type: String, message: String, taskId: String? = null, actionId: String? = null, detail: Map<String, String> = emptyMap()) =
        append(EngineLogLevel.INFO, type, message, taskId, actionId, detail)

    fun warning(type: String, message: String, taskId: String? = null, actionId: String? = null, detail: Map<String, String> = emptyMap()) =
        append(EngineLogLevel.WARNING, type, message, taskId, actionId, detail)

    fun error(type: String, message: String, taskId: String? = null, actionId: String? = null, detail: Map<String, String> = emptyMap()) =
        append(EngineLogLevel.ERROR, type, message, taskId, actionId, detail)

    private fun append(
        level: EngineLogLevel,
        type: String,
        message: String,
        taskId: String?,
        actionId: String?,
        detail: Map<String, String>,
    ) {
        val event = EngineLogEvent(clock(), level, type, message, taskId, actionId, detail)
        synchronized(lock) { events.add(event) }
        sink?.invoke(event)
    }

    val snapshot: List<EngineLogEvent>
        get() = synchronized(lock) { events.toList() }

    val size: Int get() = synchronized(lock) { events.size }
}
