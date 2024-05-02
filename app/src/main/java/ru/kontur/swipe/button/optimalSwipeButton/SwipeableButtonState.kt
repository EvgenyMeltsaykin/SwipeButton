package ru.kontur.swipe.button.optimalSwipeButton

import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.MutatePriority
import androidx.compose.foundation.gestures.AnchoredDraggableState
import androidx.compose.foundation.gestures.DraggableAnchors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.saveable.rememberSaveable

@OptIn(ExperimentalFoundationApi::class)
internal class SwipeableButtonState(
    val initialValue: SwipeButtonAnchor,
    positionalThreshold: (totalDistance: Float) -> Float,
    snapAnimationSpec: AnimationSpec<Float>,
    private val velocityThreshold: () -> Float,
    confirmValueChange: (newValue: SwipeButtonAnchor) -> Boolean = { true },
) {
    internal val anchoredDraggableState = AnchoredDraggableState(
        initialValue = initialValue,
        anchors = DraggableAnchors {
            SwipeButtonAnchor.Start at 0f
            SwipeButtonAnchor.End at Float.MAX_VALUE
        },
        positionalThreshold = positionalThreshold,
        velocityThreshold = velocityThreshold,
        animationSpec = snapAnimationSpec,
        confirmValueChange = confirmValueChange,
    )

    val currentValue: SwipeButtonAnchor by derivedStateOf {
        anchoredDraggableState.currentValue
    }

    val targetValue: SwipeButtonAnchor by derivedStateOf {
        anchoredDraggableState.targetValue
    }

    suspend fun dragTo(anchor: SwipeButtonAnchor) {
        anchoredDraggableState.anchoredDrag(
            dragPriority = MutatePriority.PreventUserInput
        ) { anchors ->
            dragTo(newOffset = anchors.positionOf(anchor))
        }
    }

    companion object {
        fun Saver(
            snapAnimationSpec: AnimationSpec<Float>,
            positionalThreshold: (distance: Float) -> Float,
            velocityThreshold: () -> Float,
            confirmValueChange: (SwipeButtonAnchor) -> Boolean = { true },
        ) = androidx.compose.runtime.saveable.Saver<SwipeableButtonState, SwipeButtonAnchor>(
            save = { it.currentValue },
            restore = {
                SwipeableButtonState(
                    initialValue = it,
                    snapAnimationSpec = snapAnimationSpec,
                    confirmValueChange = confirmValueChange,
                    positionalThreshold = positionalThreshold,
                    velocityThreshold = velocityThreshold,
                )
            }
        )
    }
}

@Composable
internal fun rememberSwipeableButtonState(
    initialValue: SwipeButtonAnchor,
    velocityThreshold: () -> Float,
    positionalThreshold: (totalDistance: Float) -> Float = { distance -> distance * 0.8f },
    snapAnimationSpec: AnimationSpec<Float> = tween(),
    confirmValueChange: (newValue: SwipeButtonAnchor) -> Boolean = { true },
): SwipeableButtonState {
    return rememberSaveable(
        saver = SwipeableButtonState.Saver(
            snapAnimationSpec = snapAnimationSpec,
            positionalThreshold = positionalThreshold,
            velocityThreshold = velocityThreshold,
            confirmValueChange = confirmValueChange,
        )
    ) {
        SwipeableButtonState(
            initialValue = initialValue,
            positionalThreshold = positionalThreshold,
            velocityThreshold = velocityThreshold,
            snapAnimationSpec = snapAnimationSpec,
            confirmValueChange = confirmValueChange
        )
    }
}