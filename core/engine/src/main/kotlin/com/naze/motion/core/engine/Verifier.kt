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

            VerificationType.TEXT_EXISTS -> verifyTextExists(effective, observation)

            VerificationType.TEXT_NOT_EXISTS -> verifyTextNotExists(effective, observation)

            VerificationType.PACKAGE_ACTIVE -> verifyPackageActive(effective, observation)

            VerificationType.SCREEN_CHANGED -> verifyScreenChanged(effective, observation, previous)

            VerificationType.CUSTOM ->
                VerificationResult(effective.type, true, "custom rules are evaluated by the caller")
        }
    }

    private fun verifyTextExists(
        rule: VerificationRule,
        observation: Observation,
    ): VerificationResult {
        val expected = rule.expectedText
            ?: return fail(rule.type, "TEXT_EXISTS requires expectedText")
        val found = observation.visibleText.any { normalize(it).contains(normalize(expected)) }
        return if (found) {
            VerificationResult(rule.type, true)
        } else {
            fail(rule.type, "expected text not found on screen")
        }
    }

    private fun verifyTextNotExists(
        rule: VerificationRule,
        observation: Observation,
    ): VerificationResult {
        val expected = rule.expectedText
            ?: return fail(rule.type, "TEXT_NOT_EXISTS requires expectedText")
        val found = observation.visibleText.any { normalize(it).contains(normalize(expected)) }
        return if (found) {
            fail(rule.type, "forbidden text is still on screen")
        } else {
            VerificationResult(rule.type, true)
        }
    }

    private fun verifyPackageActive(
        rule: VerificationRule,
        observation: Observation,
    ): VerificationResult {
        val expected = rule.expectedPackage
            ?: return fail(rule.type, "PACKAGE_ACTIVE requires expectedPackage")
        return if (observation.currentPackage == expected) {
            VerificationResult(rule.type, true)
        } else {
            fail(rule.type, "active package is " + observation.currentPackage)
        }
    }

    private fun verifyScreenChanged(
        rule: VerificationRule,
        observation: Observation,
        previous: Observation?,
    ): VerificationResult {
        val prev = previous
            ?: return fail(rule.type, "no previous observation to compare")
        return if (prev.visibleText != observation.visibleText) {
            VerificationResult(rule.type, true)
        } else {
            fail(rule.type, "screen visible text unchanged")
        }
    }

    private fun elementVisible(rule: VerificationRule, observation: Observation): VerificationResult {
        val target = rule.target
            ?: return fail(rule.type, "ELEMENT_VISIBLE requires a target")
        // Copy cross module properties into locals so no smart cast is needed.
        val targetText = target.text
        val targetNormalized = target.normalizedText
        val targetDescription = target.contentDescription
        val hasTextSignal = targetText != null || targetNormalized != null || targetDescription != null
        if (!hasTextSignal) {
            return fail(
                rule.type,
                "resourceId only targets cannot be verified from an observation; use text or contentDescription",
            )
        }
        val byText = targetText != null &&
            observation.visibleText.any { normalize(it).contains(normalize(targetText)) }
        val byNormalized = targetNormalized != null &&
            observation.visibleText.any { normalize(it).contains(normalize(targetNormalized)) }
        val byDescription = targetDescription != null &&
            observation.contentDescriptions.any { normalize(it).contains(normalize(targetDescription)) }
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
