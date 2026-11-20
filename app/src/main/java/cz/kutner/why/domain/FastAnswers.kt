package cz.kutner.why.domain

/** An answer faster than reading the choices comes from the thumb's memory, not from a decision. */
object FastAnswers {
    const val SAMPLE = 30
    const val FAST_MS = 1_000L
    const val MIN_FAST = 20

    /**
     * The number of fast answers among the last [SAMPLE] when annoying mode should be offered now, otherwise null.
     * [recent] is newest first, includes [latestMs], and holds only answers given without a trick.
     */
    fun offer(latestMs: Long, recent: List<Long>, annoyingMode: Boolean, offerAfter: Long, now: Long): Int? {
        if (annoyingMode || now < offerAfter || latestMs >= FAST_MS || recent.size < SAMPLE) return null
        return recent.take(SAMPLE).count { it < FAST_MS }.takeIf { it >= MIN_FAST }
    }
}
