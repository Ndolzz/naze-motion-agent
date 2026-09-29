package com.naze.motion.core.ai

import com.naze.motion.core.domain.Action
import com.naze.motion.core.domain.ActionTarget
import com.naze.motion.core.domain.RetryPolicy
import com.naze.motion.core.domain.VerificationRule
import com.naze.motion.core.domain.VerificationType
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * Parses raw provider output into domain Actions (NMA-AI-004 step 2).
 * Structural schema violations are PlanningError.SchemaViolation; anything
 * ambiguous is rejected, never guessed (NMA-AI-005).
 */
object PlannerJsonParser {

    private val json = Json { ignoreUnknownKeys = true }

    fun parse(raw: String): Result<Pair<String, List<Action>>> {
        val root = try {
            json.parseToJsonElement(raw).jsonObject
        } catch (e: Exception) {
            return Result.failure(PlanningError.MalformedJson("output is not a JSON object: " + e.message))
        }
        val task = root.string("task")
            ?: return Result.failure(PlanningError.SchemaViolation("missing required string field 'task'"))
        val actionsElement = root["actions"]
            ?: return Result.failure(PlanningError.SchemaViolation("missing required field 'actions'"))
        val actionsArray = (actionsElement as? JsonArray)
            ?: return Result.failure(PlanningError.SchemaViolation("'actions' must be an array"))
        val actions = mutableListOf<Action>()
        actionsArray.forEachIndexed { index, element ->
            val action = parseAction(element, index)
                ?: return Result.failure(
                    PlanningError.SchemaViolation("actions[$index] is not a valid action object")
                )
            actions.add(action)
        }
        return Result.success(task to actions)
    }

    private fun parseAction(element: JsonElement, index: Int): Action? {
        val obj = element as? JsonObject ?: return null
        val id = obj.string("id") ?: return null
        val type = obj.string("type") ?: return null
        val actionResult = Action.of(
            id = id,
            rawType = type,
            target = parseTarget(obj["target"]),
            parameters = parseParameters(obj["parameters"]),
            timeoutMs = obj.long("timeoutMs") ?: 5_000L,
            retryPolicy = parseRetryPolicy(obj["retryPolicy"]),
            verification = parseVerification(obj["verification"]),
        )
        return actionResult.getOrNull()
    }

    private fun parseTarget(element: JsonElement?): ActionTarget? {
        val obj = element as? JsonObject ?: return null
        val coordinateX = obj.int("coordinateX")
        val coordinateY = obj.int("coordinateY")
        val fallbackAllowed = obj.boolean("coordinateFallbackAllowed") ?: false
        if (coordinateX != null && coordinateY != null && !fallbackAllowed) return null
        return ActionTarget(
            resourceId = obj.string("resourceId"),
            contentDescription = obj.string("contentDescription"),
            text = obj.string("text"),
            normalizedText = obj.string("normalizedText"),
            coordinateX = coordinateX,
            coordinateY = coordinateY,
            coordinateFallbackAllowed = fallbackAllowed,
        )
    }

    private fun parseParameters(element: JsonElement?): Map<String, String> {
        val obj = element as? JsonObject ?: return emptyMap()
        val out = mutableMapOf<String, String>()
        for ((key, value) in obj) {
            val primitive = (value as? kotlinx.serialization.json.JsonPrimitive) ?: continue
            out[key] = primitive.content
        }
        return out
    }

    private fun parseRetryPolicy(element: JsonElement?): RetryPolicy {
        val obj = element as? JsonObject ?: return RetryPolicy()
        val maxAttempts = obj.int("maxAttempts") ?: 2
        return runCatching { RetryPolicy(maxAttempts) }.getOrElse { RetryPolicy() }
    }

    private fun parseVerification(element: JsonElement?): VerificationRule? {
        val obj = element as? JsonObject ?: return null
        val type = obj.string("type") ?: return null
        val verificationType = VerificationType.entries.firstOrNull { it.name == type } ?: return null
        return VerificationRule(
            type = verificationType,
            target = parseTarget(obj["target"]),
            expectedText = obj.string("expectedText"),
            expectedPackage = obj.string("expectedPackage"),
        )
    }

    private fun JsonObject.string(key: String): String? {
        val v = (this[key] as? kotlinx.serialization.json.JsonPrimitive) ?: return null
        return if (v.isString || v.content.toLongOrNull() != null || v.content.toDoubleOrNull() != null) v.content else null
    }

    private fun JsonObject.int(key: String): Int? = string(key)?.toIntOrNull()

    private fun JsonObject.long(key: String): Long? = string(key)?.toLongOrNull()?.takeIf { it > 0 }

    private fun JsonObject.boolean(key: String): Boolean? {
        val v = (this[key] as? kotlinx.serialization.json.JsonPrimitive) ?: return null
        return v.content.toBooleanStrictOrNull()
    }
}
