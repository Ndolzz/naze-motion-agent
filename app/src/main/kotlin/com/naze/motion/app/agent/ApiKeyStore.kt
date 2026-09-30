package com.naze.motion.app.agent

import android.content.Context
import com.naze.motion.core.ai.AIProvider
import com.naze.motion.core.ai.AiProviderConfig
import com.naze.motion.core.ai.AiProviderKind
import com.naze.motion.core.agent.LocalTemplateProvider
import com.naze.motion.core.ai.NetworkAiProvider

/**
 * ApiKeyStore (Phase 12): in app API key storage. Keys are entered by the
 * user inside the Settings screen, kept in the app private storage on this
 * device only, and never baked into the build or logged. Multiple
 * providers can hold a key at once; one is selected as active.
 */
class ApiKeyStore(context: Context) {

    private val prefs = context.applicationContext
        .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    data class StoredKey(val apiKey: String, val model: String, val baseUrl: String)

    fun load(providerId: String): StoredKey = StoredKey(
        apiKey = prefs.getString(field(providerId, "apiKey"), "") ?: "",
        model = prefs.getString(field(providerId, "model"), "") ?: "",
        baseUrl = prefs.getString(field(providerId, "baseUrl"), "") ?: "",
    )

    fun save(providerId: String, apiKey: String, model: String, baseUrl: String) {
        prefs.edit()
            .putString(field(providerId, "apiKey"), apiKey.trim())
            .putString(field(providerId, "model"), model.trim())
            .putString(field(providerId, "baseUrl"), baseUrl.trim())
            .apply()
    }

    fun clear(providerId: String) {
        prefs.edit()
            .remove(field(providerId, "apiKey"))
            .remove(field(providerId, "model"))
            .remove(field(providerId, "baseUrl"))
            .apply()
    }

    fun selectedId(): String = prefs.getString(SELECTED, LOCAL) ?: LOCAL

    fun setSelected(providerId: String) {
        prefs.edit().putString(SELECTED, providerId).apply()
    }

    fun hasKey(providerId: String): Boolean = load(providerId).apiKey.isNotBlank()

    /**
     * Builds the active AIProvider from
 the stored keys. Falls back to the
     * deterministic LocalTemplateProvider whenever the selected provider
     * has no complete configuration, so a run never fails just because a
     * key is missing.
     */
    fun activeProvider(): AIProvider {
        val id = selectedId()
        val entry = catalog.firstOrNull { it.id == id } ?: return LocalTemplateProvider()
        val kind = entry.kind ?: return LocalTemplateProvider()
        val stored = load(id)
        val baseUrl = stored.baseUrl.ifBlank { entry.defaultBaseUrl ?: "" }
        if (stored.apiKey.isBlank() || stored.model.isBlank() || baseUrl.isBlank()) {
            return LocalTemplateProvider()
        }
        return runCatching {
            NetworkAiProvider(AiProviderConfig(kind, baseUrl, stored.apiKey, stored.model))
        }.getOrElse { LocalTemplateProvider() }
    }

    private fun field(providerId: String, name: String): String = "prov." + providerId + "." + name

    companion object {
        private const val PREFS_NAME = "naze_ai_keys"
        private const val SELECTED = "selected"
        const val LOCAL = "local"

        data class CatalogEntry(
            val id: String,
            val label: String,
            val kind: AiProviderKind?,
            val defaultBaseUrl: String? = null,
            val defaultModel: String? = null,
        )

        /** Providers the user can configure in app. */
        val catalog: List<CatalogEntry> = listOf(
            CatalogEntry(LOCAL, "Local (on device templates)", null),
            CatalogEntry(
                "openai", "OpenAI", AiProviderKind.OPENAI_COMPATIBLE,
                "https://api.openai.com/v1", "gpt-4o-mini",
            ),
            CatalogEntry(
                "anthropic", "Anthropic", AiProviderKind.ANTHROPIC,
                "https://api.anthropic.com/v1", "claude-3-5-haiku-latest",
            ),
            CatalogEntry(
                "gemini", "Google Gemini", AiProviderKind.GEMINI,
                "http
s://generativelanguage.googleapis.com", "gemini-1.5-flash",
            ),
            CatalogEntry(
                "custom", "Custom (OpenAI compatible)", AiProviderKind.OPENAI_COMPATIBLE,
            ),
        )
    }
}
