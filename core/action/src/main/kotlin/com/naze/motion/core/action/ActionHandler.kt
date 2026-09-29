package com.naze.motion.core.action

import com.naze.motion.core.domain.Action
import com.naze.motion.core.domain.ActionResult
import com.naze.motion.core.domain.ExecutionContext

/**
 * Strategy contract: one handler per ActionType. Extensible without giant
 * conditionals. Handlers only touch AutomationDriver and TargetResolver.
 */
interface ActionHandler {
    val actionType: String
    suspend fun execute(
        action: Action,
        driver: AutomationDriver,
        resolver: TargetResolver,
        context: ExecutionContext,
    ): ActionResult
}
