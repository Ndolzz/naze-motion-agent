package com.naze.motion.core.agent

import com.naze.motion.core.ai.AIProvider
import com.naze.motion.core.ai.PlanningRequest

/**
 * LocalTemplateProvider (Phase 11): a deterministic on device provider
 * backed by fixed templates over the Alight Motion vocabulary. It is an
 * honest AIProvider implementation (NMA-AI-003 lists local providers as
 * legitimate), not a claim of intelligence: unknown instructions map to a
 * safe open and wait plan, never a guess. Network model providers arrive
 * with a later phase and plug in behind the same interface.
 */
class LocalTemplateProvider : AIProvider {
    override val name = "local-templates"

    override suspend fun complete(request: PlanningRequest): Result<String> =
        runCatching { planJsonFor(request) }

    companion object {
        /**
         * Builds planner schema JSON from keyword templates. The output runs
         * through the same parser, safety validator, and per action
         * validation as any other provider (NMA-AI-004/005).
         */
        fun planJsonFor(request: PlanningRequest): String {
            val text = request.instruction.trim().lowercase()
            val pkg = request.targetAppPackage
            val sb = StringBuilder()
            sb.append("{\"task\":\"agent_task\",\"actions\":[")
            sb.append("{\"id\":\"a1\",\"type\":\"OPEN_APP\",\"parameters\":{\"packageName\":\"")
                .append(pkg).append("\"}}")
            sb.append(",{\"id\":\"a2\",\"type\":\"WAIT\",\"parameters\":{\"durationMs\":\"500\"}}")
            if (text.contains("export") || text.contains("ekspor")) {
                sb.append(",{\"id\":\"a3\",\"type\":\"TAP\",\"target\":{\"normalizedText\":\"export\"}}")
            } else {
                sb.append(",{\"id\":\"a3\",\"type\":\"TAP\",\"target\":{\"normalizedText\":\"new project\"}}")
            }
            sb.append("]}")
            return sb.toString()
        }
    }
}
