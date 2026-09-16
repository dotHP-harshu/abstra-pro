package dev.abstra.pro.ui.components

import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.zIndex
import kotlinx.coroutines.CoroutineScope

/**
 * Lightweight long-press drag-to-reorder for LazyColumn, ported from the same
 * self-contained ~70-line pattern used by Lune (pointerInput + graphicsLayer only —
 * GPU-cheap, no third-party dependency).
 */
@Composable
fun rememberReorderableState(
    listState: LazyListState,
    canDragOver: ((Int) -> Boolean)? = null,
    onDragEnd: (() -> Unit)? = null,
    onMove: (Int, Int) -> Unit
): ReorderableState {
    val scope = rememberCoroutineScope()
    return remember(listState) {
        ReorderableState(listState, scope, onMove, canDragOver, onDragEnd)
    }
}

class ReorderableState(
    val listState: LazyListState,
    private val scope: CoroutineScope,
    private val onMove: (Int, Int) -> Unit,
    private val canDragOver: ((Int) -> Boolean)? = null,
    private val onDragEndCallback: (() -> Unit)? = null
) {
    var draggedIndex by mutableStateOf<Int?>(null)
        private set
    var dragOffset by mutableStateOf(0f)
        private set

    fun onDragStart(offset: Offset) {
        listState.layoutInfo.visibleItemsInfo
            .firstOrNull { item -> offset.y.toInt() in item.offset..(item.offset + item.size) }
            ?.let { item ->
                if (canDragOver == null || canDragOver.invoke(item.index)) {
                    draggedIndex = item.index
                }
            }
    }

    fun onDrag(dragAmount: Offset) {
        draggedIndex?.let { index ->
            dragOffset += dragAmount.y

            val draggedItem = listState.layoutInfo.visibleItemsInfo.firstOrNull { it.index == index } ?: return
            val targetItem = listState.layoutInfo.visibleItemsInfo.find { item ->
                val isValid = canDragOver == null || canDragOver.invoke(item.index)
                val threshold = item.size / 2
                isValid && if (dragOffset > 0) {
                    item.index > index && dragOffset > (item.offset - draggedItem.offset - threshold)
                } else {
                    item.index < index && dragOffset < (item.offset - draggedItem.offset + threshold)
                }
            }

            if (targetItem != null) {
                onMove(index, targetItem.index)
                draggedIndex = targetItem.index
                dragOffset -= (targetItem.offset - draggedItem.offset)
            }
        }
    }

    fun onDragEnd() {
        if (draggedIndex != null) {
            onDragEndCallback?.invoke()
        }
        draggedIndex = null
        dragOffset = 0f
    }
}

fun Modifier.reorderableItem(
    state: ReorderableState,
    index: Int
): Modifier = this.then(
    Modifier
        .graphicsLayer {
            val isDragged = state.draggedIndex == index
            translationY = if (isDragged) state.dragOffset else 0f
            scaleX = if (isDragged) 1.05f else 1f
            scaleY = if (isDragged) 1.05f else 1f
            alpha = if (isDragged) 0.8f else 1f
        }
        .zIndex(if (state.draggedIndex == index) 1f else 0f)
)

fun Modifier.reorderable(
    state: ReorderableState
): Modifier = this.then(
    Modifier.pointerInput(Unit) {
        detectDragGesturesAfterLongPress(
            onDragStart = { offset -> state.onDragStart(offset) },
            onDrag = { change, dragAmount ->
                change.consume()
                state.onDrag(dragAmount)
            },
            onDragEnd = { state.onDragEnd() },
            onDragCancel = { state.onDragEnd() }
        )
    }
)
