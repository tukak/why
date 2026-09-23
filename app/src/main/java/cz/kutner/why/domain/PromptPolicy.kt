package cz.kutner.why.domain

data class UnlockDecision(val resumePrevious: Boolean, val prompt: Boolean)

object PromptPolicy {
    fun decide(
        now: Long,
        lastLockAt: Long?,
        /** An unlock this soon after screen off continues the previous session. */
        resumeWindowMs: Long,
        previousOpen: Boolean,
        previousAnswered: Boolean,
        pausedUntil: Long,
        canShowOverlay: Boolean,
        inCall: Boolean,
    ): UnlockDecision {
        // System UI can send the unlock signal more than once per unlock.
        if (previousOpen) return UnlockDecision(resumePrevious = true, prompt = false)
        val resume = lastLockAt != null && now - lastLockAt in 0..resumeWindowMs
        val prompt = canShowOverlay && !inCall && now >= pausedUntil && !(resume && previousAnswered)
        return UnlockDecision(resumePrevious = resume, prompt = prompt)
    }
}
