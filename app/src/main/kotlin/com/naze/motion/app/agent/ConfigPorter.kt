package com.naze.motion.app.agent

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/**
 * ConfigPorter (Phase 24): exports the on-device agent configuration into
 * one JSON document and imports it back. The document carries the safety
 * profile, the allowed applications list, the selected AI provider, and
 * every stored provider configuration (key, model, base URL). The values
 * live on this device only; exporting hands the API keys to whatever the
 * user shares the document with, so the UI must always let the user pick
 * the destination explicitly. Import goes through the same stores the
 * Settings screens use, so validation and coercion stay in one place:
 * invalid package names are skipped instead of failing the whole import,
 * and the safety profile is coerced by ExecutionProfile.of before it is
 * persisted.
 */
class ConfigPorter(context: Context) {

    private val safety = SafetySettingsStore(context)
    private val allowedApps = AllowedAppsStore(context)
    private val keys = ApiKeyStore(context)

    /** Serializes the full configuration. Never throws. */
    fun export(): String {
        val profile = safety.load()
        val providers = JSONArray()
        for (entry in ApiKeyStore.catalog) {
            if (entry.id == ApiKeyStore.LOCAL) continue
            val stored = keys.load(entry.id)
            if (stored.apiKey.isBlank() && stored.model.isBlank() &&
                stored.baseUrl.isBlank()
            ) {
                continue
            }
            providers.put(
                JSONObject()
                    .put("id", entry.id)
                    .put("apiKey", stored.apiKey)
                    .put("model", stored.model)
                    .put("baseUrl", stored.baseUrl),
            )
        }
        val root = JSONObject()
            .put(SCHEMA_KEY, SCHEMA_VERSION)
            .put(
                "safety",
                JSONObject()
                    .put("actionTimeoutMs", profile.actionTimeoutMs)
                    .put("maxAttempts", profile.maxAttempts)
                    .put("recoveryMaxAttempts", profile.recoveryMaxAttempts)
                    .put("recoveryBackoffBaseMs", profile.recoveryBackoffBaseMs),
            )
            .put("allowedApps", JSONArray(allowedApps.load()))
            .put("selectedProvider", keys.selectedId())
            .put("providers", providers)
        return root.toString(2)
    }

    /**
     * Applies an exported document. Fails with a clear message when the
     * document is not a Phase 24 export; partially valid sections are
     * applied, malformed entries inside a section are skipped.
     */
    fun import(json: String): Result<Unit> = runCatching {
        val root = JSONObject(json)
        if (root.optInt(SCHEMA_KEY, -1) != SCHEMA_VERSION) {
            error("not a supported configuration document")
        }
        root.optJSONObject("safety")?.let { section ->
            safety.save(
                section.optLong("actionTimeoutMs", 5_000L),
                section.optInt("maxAttempts", 2),
                section.optInt("recoveryMaxAttempts", 2),
                section.optLong("recoveryBackoffBaseMs", 400L),
            )
        }
        root.optJSONArray("allowedApps")?.let { section ->
            val incoming = (0 until section.length())
                .map { index -> section.optString(index, "") }
                .filter { it.isNotBlank() }
            for (name in allowedApps.load()) {
                if (name !in incoming) allowedApps.remove(name)
            }
            for (name in incoming) {
                // add() validates the package name and skips bad entries.
                allowedApps.add(name)
            }
        }
        root.optJSONArray("providers")?.let { section ->
            for (index in 0 until section.length()) {
                val entry = section.optJSONObject(index) ?: continue
                val id = entry.optString("id", "")
                if (id.isBlank()) continue
                keys.save(
                    id,
                    entry.optString("apiKey", ""),
                    entry.optString("model", ""),
                    entry.optString("baseUrl", ""),
                )
            }
        }
        val selected = root.optString("selectedProvider", "")
        if (selected.isNotBlank()) keys.setSelected(selected)
    }

    companion object {
        private const val SCHEMA_KEY = "schemaVersion"
        private const val SCHEMA_VERSION = 1
    }
}
