package dev.abstra.pro.ui.util

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.positionChange

suspend fun PointerInputScope.detectVerticalSwipes(
    thresholdPx: Float = 120f,
    onSwipeUp: () -> Unit,
    onSwipeDown: () -> Unit,
    onHorizontalDrag: (Float) -> Unit = {}
) {
    awaitEachGesture {
        val down = awaitFirstDown(requireUnconsumed = false)
        var totalDy = 0f
        var totalDx = 0f
        var direction = Direction.UNKNOWN

        while (true) {
            val event = awaitPointerEvent()
            val change = event.changes.firstOrNull() ?: break
            if (!change.pressed) {
                if (direction == Direction.VERTICAL) {
                    if (totalDy < -thresholdPx) onSwipeUp()
                    else if (totalDy > thresholdPx) onSwipeDown()
                }
                break
            }
            val dy = change.positionChange().y
            val dx = change.positionChange().x

            if (direction == Direction.UNKNOWN) {
                if (kotlin.math.abs(dy) > kotlin.math.abs(dx) + 4f) {
                    direction = Direction.VERTICAL
                    change.consume()
                } else if (kotlin.math.abs(dx) > 4f) {
                    direction = Direction.HORIZONTAL
                }
            } else if (direction == Direction.VERTICAL) {
                totalDy += dy
                change.consume()
            } else if (direction == Direction.HORIZONTAL) {
                totalDx += dx
                onHorizontalDrag(dx)
            }
        }
    }
}

private enum class Direction { UNKNOWN, VERTICAL, HORIZONTAL }
