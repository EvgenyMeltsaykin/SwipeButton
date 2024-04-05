package ru.kontur.swipe.button.swipeButton


import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.AnchoredDraggableState
import androidx.compose.foundation.gestures.DraggableAnchors
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.anchoredDraggable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

enum class Anchor { Start, End }

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SwipeButton(
    modifier: Modifier,
    progressColor: Color,
    thumbBackgroundColor: Color,
    backgroundColor: Color,
    centerContent: @Composable (progress: Float, currentAnchor: Anchor) -> Unit,
    thumbContent: @Composable (progress: Float, targetAnchor: Anchor) -> Unit,
    endContent: @Composable (progress: Float, currentAnchor: Anchor) -> Unit,
    onSwiped: (target: Anchor) -> Unit = {},
) {
    val density = LocalDensity.current
    var width by remember { mutableIntStateOf(0) }
    var thumbSize by remember { mutableStateOf(IntSize.Zero) }
    val endOfTrackPx = remember(width) { width - thumbSize.width }
    val heightPx = thumbSize.height
    val thumbWidthPx = thumbSize.width

    val state = remember {
        AnchoredDraggableState(
            initialValue = Anchor.Start,
            anchors = DraggableAnchors {
                Anchor.Start at 0f
                Anchor.End at endOfTrackPx.toFloat()
            },
            positionalThreshold = { distance -> distance * 0.8f },
            velocityThreshold = { with(density) { 400.dp.toPx() } },
            animationSpec = tween()
        )
    }

    var lastAnchor by remember { mutableStateOf(Anchor.Start) }

    LaunchedEffect(state.currentValue) {
        if (state.currentValue != lastAnchor) {
            onSwiped(state.currentValue)
        }
        lastAnchor = state.currentValue
    }

    LaunchedEffect(endOfTrackPx) {
        state.updateAnchors(
            DraggableAnchors {
                Anchor.Start at 0f
                Anchor.End at endOfTrackPx.toFloat()
            }
        )
    }
    Surface(
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .onSizeChanged { width = it.width }
                .background(backgroundColor, CircleShape)
                .clip(CircleShape)
                .drawWithCache {
                    val progressWidth = state.requireOffset() + thumbWidthPx / 2
                    if (state.offset == 0f) return@drawWithCache onDrawBehind { }
                    onDrawBehind {
                        drawRect(
                            color = progressColor,
                            size = Size(width = progressWidth, height = heightPx.toFloat()),
                        )
                    }
                }
        ) {
            Box(
                modifier = Modifier.align(Alignment.CenterEnd)
            ) {
                endContent(state.progress, state.currentValue)
            }

            Box(
                modifier = Modifier.align(Alignment.Center)
            ) {
                centerContent(state.progress, state.currentValue)
            }
            Box(
                Modifier
                    .anchoredDraggable(
                        state = state,
                        orientation = Orientation.Horizontal,
                    )
                    .offset {
                        IntOffset(
                            x = state
                                .requireOffset()
                                .roundToInt(),
                            y = 0
                        )
                    }
                    .onGloballyPositioned {
                        thumbSize = it.size
                    }
                    .background(thumbBackgroundColor, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                thumbContent(state.progress, state.targetValue)
            }
        }
    }
}
