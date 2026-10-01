package com.naze.motion.core.ai

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import java.net.HttpURLConnection
import java.net.URL

/**
 * Kinds of network AI backend the planner can talk to (NMA-AI-003).
 * OPENAI_COMPATIBLE covers OpenAI, Groq, OpenRouter, and any
 * /chat/completions style endpoint including self hosted ones.
 */
enum class AiProviderKind { OPENAI_COMPATIBLE, ANTHROPIC, GEMINI }

/**
 * Network provider configuration. The API key is supplied by the user from
 * inside the app (Phase 12) and stored on device; it is never baked into
 * the build, never logged, and never sent anywhere except the configured
 * endpoint.
 */
data class AiProviderConfig(
    val kind: AiProviderKind,
    val baseUrl: String,
    val apiKey: String,
    val model: String,
) {
    init {
        require(apiKey.isNotBlank()) { "apiKey must not be blank" }
        require(model.isNotBlank()) { "model must not be blank" }
        require(baseUrl.startsWith("http")) { "baseUrl must be an http(s) URL" }
    }
}

/**
 * NetworkAiProvider: an AIProvider implementation over a chat completion
 * API. Request building, endpoint building, header building, and response
 * extraction are pure companion functions so they are unit tested on the
 * JVM without any network (NMA-TEST-002). Raw model output is sanitized to
 * a bare JSON object before the planner parser sees it; malformed output is
 * still rejected downstream, never guessed (NMA-AI-005). Every call runs on
 * Dispatchers.IO and is bounded by the AiPlanner timeout (NMA-AI-008).
 */
class NetworkAiProvider(private val config: AiProviderConfig) : AIProvider {

    override val name: String = "network-" + config.kind.name.lowercase()

    /**
     * One retry for transient provider trouble (429 and 5xx such as the
     * model overloaded 503): demand spikes are usually short, so waiting
     * briefly and trying once more often saves the run. Anything else
     * fails immediately with the original error.
     */
    override suspend fun complete(request: PlanningRequest): Result<String> =
        withContext(Dispatchers.IO) {
            runCatching { post(request) }.recoverCatching { error ->
                if (!isTransient(error)) throw error
                delay(RETRY_DELAY_MS)
                post(request)
            }
        }

    private fun isTransient(error: Throwable): Boolean {
        val message = error.message ?: return false
        if (!message.startsWith("provider http ")) return false
        val code = message.removePrefix("provider http ").substringBefore(":").trim().toIntOrNull() ?: return false
        return code == 429 || (code in 500..599)
    }

    private fun post(request: PlanningRequest): String {
        val connection = URL(endpointUrl(config)).openConnection() as HttpURLConnection
        connection.requestMethod = "POST"
        connection.connectTimeout = 15_000
        connection.readTimeout = 20_000
        for ((key, value) in headers(config)) {
            connection.setRequestProperty(key, value)
        }
        connection.doOutput = true
        connection.outputStream.use { stream ->
            stream.write(requestBody(config, request).toByteArray(Charsets.UTF_8))
        }
        val code = connection.responseCode
        val body = if (code in 200..299) {
            connection.inputStream.bufferedReader().use { it.readText() }
        } else {
            val error = runCatching {
                connection.errorStream?.bufferedReader()?.use { it.readText() }
            }.getOrNull() ?: ""
            throw IllegalStateException("provider http " + code + ": " + error.take(300))
        }
        val content = plannerJson(extractContent(config.kind, body))
        if (content.isBlank()) throw IllegalStateException("provider returned empty content")
        return content
    }

    companion object {
        private val json = Json { ignoreUnknownKeys = true }

        /** Wait before the single retry of a transient provider error. */
        const val RETRY_DELAY_MS = 3_000L

        /** The endpoint a config points at, per provider kind. */
        fun endpointUrl(config: AiProviderConfig): String {
            val base = trimSlash(config.baseUrl)
            return when (config.kind) {
                AiProviderKind.OPENAI_COMPATIBLE -> base + "/chat/completions"
                AiProviderKind.ANTHROPIC -> base + "/messages"
                AiProviderKind.GEMINI ->
                    base + "/v1beta/models/" + config.model + ":generateContent?key=" + config.apiKey
            }
        }

        private fun trimSlash(url: String): String =
            if (url.endsWith("/")) url.dropLast(1) else url

        /** Auth headers per kind. The API key only ever goes here or the query for Gemini. */
        fun headers(config: AiProviderConfig): List<Pair<String, String>> = when (config.kind) {
            AiProviderKind.OPENAI_COMPATIBLE -> listOf(
                "Authorization" to "Bearer " + config.apiKey,
                "Content-Type" to "application/json",
            )
            AiProviderKind.ANTHROPIC -> listOf(
                "x-api-key" to config.apiKey,
                "anthropic-version" to "2023-06-01",
                "Content-Type" to "application/json",
            )
            AiProviderKind.GEMINI -> listOf("Content-Type" to "application/json")
        }

        /** Chat request body per kind, built with the same system prompt. */
        fun requestBody(config: AiProviderConfig, request: PlanningRequest): String {
            val system = systemPrompt()
            val user = userMessage(request)
            val element = when (config.kind) {
                AiProviderKind.OPENAI_COMPATIBLE -> buildJsonObject {
                    put("model", config.model)
                    put("temperature", 0.0)
                    put("messages", buildJsonArray {
                        add(buildJsonObject {
                            put("role", "system")
                            put("content", system)
                        })
                        add(buildJsonObject {
                            put("role", "user")
                            put("content", user)
                        })
                    })
                }
                AiProviderKind.ANTHROPIC -> buildJsonObject {
                    put("model", config.model)
                    put("max_tokens", 2048)
                    put("temperature", 0.0)
                    put("system", system)
                    put("messages", buildJsonArray {
                        add(buildJsonObject {
                            put("role", "user")
                            put("content", user)
                        })
                    })
                }
                AiProviderKind.GEMINI -> buildJsonObject {
                    put("contents", buildJsonArray {
                        add(buildJsonObject {
                            put("parts", buildJsonArray {
                                add(buildJsonObject {
                                    put("text", system + "\n\n" + user)
                                })
                            })
                        })
                    })
                    put("generationConfig", buildJsonObject {
                        put("temperature", 0.0)
                    })
                }
            }
            return element.toString()
        }

        /**
         * System prompt describing the exact planner JSON schema. Ambiguity
         * is rejected downstream, so the prompt forbids prose and fences and
         * forbids coordinate targets outright.
         */
        fun systemPrompt(): String = trimEachLine(
            """
            You are the planning module of a mobile automation agent. Convert the user instruction into an execution plan for the target app.
            Respond with ONLY a JSON object, no prose, no markdown code fences.
            Schema:
            {"task":"short task name","actions":[{"id":"a1","type":"OPEN_APP","parameters":{"packageName":"com.alightmotion.motion"}}]}
            Rules:
            - "id" must be unique within the plan.
            - "type" must be one of: OPEN_APP, WAIT, TAP, LONG_PRESS, SWIPE, SCROLL, TYPE_TEXT, PRESS_BACK, SCREENSHOT, FIND_ELEMENT, CREATE_PROJECT, ADD_MEDIA, ADD_TEXT, EXPORT.
            - The first action must be OPEN_APP with parameters {"packageName":"<target app package>"}.
            - "target" may use "resourceId", "contentDescription", "text", or "normalizedText". Never use coordinates: coordinate targets are rejected.
            - TAP, LONG_PRESS, FIND_ELEMENT require "target". WAIT requires parameters {"durationMs":"100"}. TYPE_TEXT requires parameters {"text":"..."}.
            - Keep plans short and concrete.
            """
        )

        fun userMessage(request: PlanningRequest): String {
            val sb = StringBuilder()
            sb.append("Target app package: ").append(request.targetAppPackage)
            if (request.observationSummary.isNotBlank()) {
                sb.append("\nObservation: ").append(request.observationSummary)
            }
            sb.append("\nInstruction: ").append(request.instruction)
            return sb.toString()
        }

        /** Pulls the assistant text out of a provider response body. */
        fun extractContent(kind: AiProviderKind, body: String): String = try {
            val root = json.parseToJsonElement(body).jsonObject
            val content: String? = when (kind) {
                AiProviderKind.OPENAI_COMPATIBLE ->
                    root["choices"]?.jsonArray?.firstOrNull()
                        ?.jsonObject?.get("message")?.jsonObject?.get("content")
                        ?.jsonPrimitive?.content
                AiProviderKind.ANTHROPIC ->
                    root["content"]?.jsonArray?.firstOrNull()
                        ?.jsonObject?.get("text")?.jsonPrimitive?.content
                AiProviderKind.GEMINI ->
                    root["candidates"]?.jsonArray?.firstOrNull()?.jsonObject
                        ?.get("content")?.jsonObject?.get("parts")?.jsonArray
                        ?.firstOrNull()?.jsonObject?.get("text")?.jsonPrimitive?.content
            }
            content ?: ""
        } catch (e: Exception) {
            ""
        }

        /**
         * Sanitizes raw model text to a bare JSON object: strips code
         * fences and any prose before or after the object. The planner
         * parser still validates everything (NMA-AI-005).
         */
        fun plannerJson(raw: String): String {
            var text = raw.trim()
            if (text.startsWith("```")) {
                text = text.removePrefix("```json").removePrefix("```").trim()
                text = text.removeSuffix("```").trim()
            }
            val start = text.indexOf('{')
            val end = text.lastIndexOf('}')
            if (start >= 0 && end > start) return text.substring(start, end + 1)
            return text
        }

        private fun trimEachLine(text: String): String =
            text.trimIndent().trim().replace("\n    ", "\n")
    }
}
