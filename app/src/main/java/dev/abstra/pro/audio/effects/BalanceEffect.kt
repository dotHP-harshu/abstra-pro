package dev.abstra.pro.audio.effects

/**
 * Calculates stereo left/right volume balance.
 * [balance]: 0.0f (Full Left) .. 0.5f (Center) .. 1.0f (Full Right)
 */
object BalanceEffect {
    fun volumesForBalance(balance: Float): Pair<Float, Float> {
        val b = balance.coerceIn(0f, 1f)
        val left = if (b <= 0.5f) 1f else 1f - (b - 0.5f) * 2f
        val right = if (b >= 0.5f) 1f else b * 2f
        return left.coerceIn(0f, 1f) to right.coerceIn(0f, 1f)
    }
}
