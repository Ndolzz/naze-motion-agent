package com.naze.motion.core.ai

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class NetworkAiProviderTest {

    private fun config(kind: AiProviderKind) = AiProviderConfig(
        kind = kind,
        baseUrl = "https://api.example.com/v1",
        apiKey = "sk-test-key",
        model = "test-model",
    )

    private val request = PlanningRequest("buat video baru", "com.alightmotion.motion")

    @Test
    fun configRejectsBlankApiKey() {
        assertFailsWith<IllegalArgumentException> {
            AiProviderConfig(AiProviderKind.OPENAI_COMPATIBLE, "https://api.example.com", " ", "m")
        }
    }

    @Test
    fun configRejectsBlankModel() {
        assertFailsWith<IllegalArgumentException> {
            AiProviderConfig(AiProviderKind.ANTHROPIC, "https://api.example.com", "key", " ")
        }
    }

    @Test
    fun configRejectsNonHttpBaseUrl() {
        assertFailsWith<IllegalArgumentException> {
            AiProviderConfig(AiProviderKind.GEMINI, "ftp://bad", "key", "m")
        }
    }

    @Test
    fun providerNameReflectsKind() {
        assertEquals("network-openai_compatible", NetworkAiProvider(config(AiProviderKind.OPENAI_COMPATIBLE)).name)
    }

    @Test
    fun openAiEndpointAndHeaders() {
        val c = config(AiProviderKind.OPENAI_COMPATIBLE)
        assertEquals("https://api.example.com/v1/chat/completions", NetworkAiProvider.endpointUrl(c))
        val headers = NetworkAiProvider.headers(c)
        assertTrue(headers.contains("Authorization" to "Bearer sk-test-key"))
    }

    @Test
    fun trailingSlashBaseUrlIsTrimmed() {
        val c = config(AiProviderKind.OPENAI_COMPATIBLE).copy(baseUrl = "https://api.example.com/v1/")
        assertEquals("https://api.example.com/v1/chat/completions", NetworkAiProvider.endpointUrl(c))
    }

    @Test
    fun anthropicEndpointAndHeaders() {
        val c = config(AiProviderKind.ANTHROPIC)
        assertEquals("https://api.example.com/v1/messages", NetworkAiProvider.endpointUrl(c))
        val headers = NetworkAiProvider.headers(c)
        assertTrue(headers.contains("x-api-key" to "sk-test-key"))
        assertTrue(headers.any { it.first == "anthropic-version" })
    }

    @Test
    fun geminiEndpointContainsModelAndKey() {
        val url = NetworkAiProvider.endpointUrl(config(AiProviderKind.GEMINI))
        assertTrue(url.contains("/v1beta/models/test-model:generateContent"))
        assertTrue(url.contains("key=sk-test-key"))
    }

    @Test
    fun openAiBodyCarriesModelAndMessages() {
        val body = NetworkAiProvider.requestBody(config(AiProviderKind.OPENAI_COMPATIBLE), request)
        assertTrue(body.contains("\"model\":\"test-model\""))
        assertTrue(body.contains("OPEN_APP"))
        assertTrue(body.contains("com.alightmotion.motion"))
        assertTrue(body.contains("buat video baru"))
    }

    @Test
    fun anthropicBodyCarriesSystemField() {
        val body = NetworkAiProvider.requestBody(config(AiProviderKind.ANTHROPIC), request)
        assertTrue(body.contains("\"system\":"))
        assertTrue(body.contains("\"max_tokens\":"))
    }

    @Test
    fun geminiBodyCarriesPartsText() {
        val body = NetworkAiProvider.requestBody(config(AiProviderKind.GEMINI), request)
        assertTrue(body.contains("\"parts\":"))
        assertTrue(body.contains("\"text\":"))
    }

    @Test
    fun systemPromptForbidsCoordinatesAndFences() {
        val prompt = NetworkAiProvider.systemPrompt()
        assertTrue(prompt.contains("OPEN_APP"))
        assertTrue(prompt.contains("Never use coordinates"))
        assertTrue(prompt.contains("no markdown code fences"))
    }

    @Test
    fun extractContentOpenAiStyle() {
        val body = """
            {"choices":[{"message":{"role":"assistant","content":"{\"task\":\"x\"}"}}]}
        """.trimIndent()
        assertEquals("{\"task\":\"x\"}", NetworkAiProvider.extractContent(AiProviderKind.OPENAI_COMPATIBLE, body))
    }

    @Test
    fun extractContentAnthropicStyle() {
        val body = """
            {"content":[{"type":"text","text":"hello plan"}]}
        """.trimIndent()
        assertEquals("hello plan", NetworkAiProvider.extractContent(AiProviderKind.ANTHROPIC, body))
    }

    @Test
    fun extractContentGeminiStyle() {
        val body = """
            {"candidates":[{"content":{"parts":[{"text":"gemini plan"}]}}]}
        """.trimIndent()
        assertEquals("gemini plan", NetworkAiProvider.extractContent(AiProviderKind.GEMINI, body))
    }

    @Test
    fun extractContentReturnsEmptyOnGarbage() {
        assertEquals("", NetworkAiProvider.extractContent(AiProviderKind.OPENAI_COMPATIBLE, "not json"))
    }

    @Test
    fun plannerJsonStripsFencesAndProse() {
        val raw = "Here is the plan:\n```json\n{\"task\":\"x\"}\n```\nDone."
        assertEquals("{\"task\":\"x\"}", NetworkAiProvider.plannerJson(raw))
    }

    @Test
    fun plannerJsonKeepsBareObjectUnchanged() {
        assertEquals("{\"task\":\"x\"}", NetworkAiProvider.plannerJson("{\"task\":\"x\"}"))
    }
}
