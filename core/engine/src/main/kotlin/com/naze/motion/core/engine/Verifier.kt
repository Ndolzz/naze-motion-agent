package com.naze.motion.core.engine

import com.naze.motion.core.domain.Observation
import com.naze.motion.core.domain.VerificationResult
import com.naze.motion.core.domain.VerificationRule
import com.naze.motion.core.domain.VerificationType

/**
 * Evaluates a VerificationRule against an Observation (NMA-DOMAIN-006,
 * NMA-DOMAIN-007). Pure function, no side effects. Honest: when an
 * observation cannot prove a rule, verification fails with a reason.
 */
object Verifier {

    fun verify(
        rule: VerificationRule?,
        observation: Observation,
        previous: Observation? = null,
    ): VerificationResult {
        val effective = rule ?: return VerificationResult(VerificationType.ACTION_COMPLETED, true)
        return when (effective.type) {
            VerificationType.ACTION_COMPLETED ->
                VerificationResult(effective.type, true)

            VerificationType.ELEMENT_VISIBLE ->
                elementVisible(effective, observation)

            VerificationType.ELEMENT_NOT_VISIBLE -> {
                val visible = elementVisible(effective, observation)
                if (visible.passed) {
                    VerificationResult(effective.type, false, "target is visible but must not be")
                } else {
                    VerificationResult(effective.type, true)
                }
            }

            VerificationType.TEXT_EXISTS -> {
                val expected = effective.expectedText
                if (expected == null) {
                    fail(effective.type, "TEXT_EXISTS requires expectedText")
                } else if (observation.visibleText.any { normalize(it).contains(normalize(expected)) }) {
                    VerificationResult(effective.type, true)
                } else {
                    fail(effective.type, "expected text not found on screen")
                }
            }

            VerificationType.TEXT_NOT_EXISTS -> {
                val expected = effective.expectedText
                if (expected == null) {
                    fail(effective.type, "TEXT_NOT_EXISTS requires expectedText")
                } else if (observation.visibleText.any { normalize(it).contains(normalize(expected)) }) {
                    fail(effective.type, "forbidden text is still on screen")
                } else {
                    VerificationResult(effective.type, true)
                }
            }

            VerificationType.PACKAGE_ACTIVE -> {
                val expected = effective.expectedPackage
                if (expected == null) {
                    fail(effective.type, "PACKAGE_ACTIVE requires expectedPackage")
                } else if (observation.currentPackage == expected) {
                    VerificationResult(effective.type, true)
                } else {
                    fail(effective.type, "active package is " + observation.currentPackage)
                }
            }

            VerificationType.SCREEN_CHANGED -> {
                val prev = previous
                if (prev == null) {
                    fail(effective.type, "no previous observation to compare")
                } else if (prev.visibleText != observation.visibleText) {
                    VerificationResult(effective.type, true)
                } else {
                    fail(effective.type, "screen visible text unchanged")
                }
            }

            VerificationType.CUSTOM ->
                VerificationResult(effective.type, true, "custom rules are evaluated by the caller")
        }
    }

    private fun elementVisible(rule: VerificationRule, observation: Observation): VerificationResult {
        val target = rule.target
            ?: return fail(rule.type, "ELEMENT_VISIBLE requires a target")
        val hasTextSignal = target.text != null || target.normalizedText != null || target.contentDescription != null
        if (!hasTextSignal) {
            return fail(
                rule.type,
                "resourceId only targets cannot be verified from an observation; use text or contentDescription",
            )
        }
        val byText = target.text != null &&
            observation.visibleText.any { normalize(it).contains(normalize(target.text)) }
        val byNormalized = target.normalizedText != null &&
            observation.visibleText.any { normalize(it).contains(normalize(target.normalizedText)) }
        val byDescription = target.contentDescription != null &&
            observation.contentDescriptions.any { normalize(it).contains(normalize(target.contentDescription)) }
        return if (byText || byNormalized || byDescription) {
            VerificationResult(rule.type, true)
        } else {
            fail(rule.type, "target not visible in the observation")
        }
    }

    private fun fail(type: VerificationType, reason: String): VerificationResult =
        VerificationResult(type, false, reason)

    private fun normalize(value: String): String = value.trim().lowercase()
}
