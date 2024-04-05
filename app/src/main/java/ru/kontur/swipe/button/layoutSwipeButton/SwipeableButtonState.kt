package ru.kontur.swipe.button.layoutSwipeButton

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

/**
 * @param initialValue The initial value of the state.
 * @param snapAnimationSpec - The default animation spec that will be used to animate to a new state.
 * @param confirmValueChange Optional callback invoked to confirm or veto a pending state
 * change.
 * @param positionalThreshold The positional threshold, in px, to be used when calculating the
 * target state while a drag is in progress and when settling after the drag ends. This is the
 * distance from the start of a transition. It will be, depending on the direction of the
 * interaction, added or subtracted from/to the origin offset. It should always be a positive
 * value.
 * @param velocityThreshold The velocity threshold (in px per second) that the end velocity has
 * to exceed in order to animate to the next state, even if the [positionalThreshold] has not
 * been reached.
 */
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

/**
 * Creates a [SwipeableButtonState] that is remembered across compositions.
 *
 * @param initialValue The initial value of the state.
 * @param snapAnimationSpec - The default animation spec that will be used to animate to a new state.
 * @param decayAnimationSpec - The animation spec that will be used when flinging with a large enough velocity to reach or cross the target state.
 * @param confirmValueChange Optional callback invoked to confirm or veto a pending state
 * change.
 * @param positionalThreshold The positional threshold, in px, to be used when calculating the
 * target state while a drag is in progress and when settling after the drag ends. This is the
 * distance from the start of a transition. It will be, depending on the direction of the
 * interaction, added or subtracted from/to the origin offset. It should always be a positive
 * value.
 * @param velocityThreshold The velocity threshold (in px per second) that the end velocity has
 * to exceed in order to animate to the next state, even if the [positionalThreshold] has not
 * been reached.
 */
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