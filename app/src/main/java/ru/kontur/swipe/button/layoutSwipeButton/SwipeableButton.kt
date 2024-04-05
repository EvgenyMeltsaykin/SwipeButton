package ru.kontur.swipe.button.layoutSwipeButton

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.DraggableAnchors
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.anchoredDraggable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.FloatState
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableFloatState
import androidx.compose.runtime.MutableIntState
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.layout.layoutId
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.theapache64.rebugger.Rebugger
import kotlin.math.roundToInt

/**
 * SwipeableButton
 *
 * Button with the ability to move the thumb.
 *
 * @param state The state object that will be used to control or monitor the button's state.
 * @param initialAnchor The thumb initial position.
 * @param progressColor The color that will fill the area behind the thumb.
 * @param thumbBackgroundColor The thumb background color.
 * @param backgroundColor The button background color.
 * @param thumbContent The thumb composable content.
 * @param centerContent The content in the center of the button.
 * @param endContent The content in the end of the button.
 * @param modifier The modifier to apply to this button.
 * @param shape Button and thumb shape.
 * @param size Defines the button's size.
 * @param enabled Drag and drop accessibility.
 * @param onTargetAnchorChange The target anchor change listener. Called when it reaches the desired positionalThreshold specified in [state]
 * @param onSwiped The current anchor change listener. Called when the current anchor changes
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun SwipeableButton(
    state: SwipeableButtonState,
    thumbContent: @Composable (progress: FloatState, targetAnchor: State<SwipeButtonAnchor>) -> Unit,
    centerContent: @Composable (progress: FloatState, currentAnchor: State<SwipeButtonAnchor>) -> Unit,
    endContent: @Composable (progress: FloatState) -> Unit,
    modifier: Modifier = Modifier,
    colors: SwipeableButtonColors = SwipeableButtonDefaults.colors(),
    shape: Shape = CircleShape,
    size: SwipeableButtonSize = SwipeableButtonSize.Large,
    enabled: Boolean = true,
    onSwiped: (target: State<SwipeButtonAnchor>) -> Unit = {},
) {
    val progressState = remember { mutableFloatStateOf(0f) }
    var lastAnchor by remember { mutableStateOf(state.initialValue) }
    val anchoredDraggableState = remember { state.anchoredDraggableState }
    val currentAnchorState = remember { derivedStateOf { anchoredDraggableState.currentValue } }
    val targetAnchorState = remember { derivedStateOf { anchoredDraggableState.targetValue } }
    val draggableOffsetState = remember { derivedStateOf { anchoredDraggableState.requireOffset() } }
    val maxAnchorState = remember { derivedStateOf { anchoredDraggableState.anchors.maxAnchor() } }
    val endOfTrackState = remember { mutableIntStateOf(0) }
    LaunchedEffect(currentAnchorState.value) {
        if (currentAnchorState.value != lastAnchor) {
            onSwiped(currentAnchorState)
        }
        lastAnchor = currentAnchorState.value
    }

    LaunchedEffect(endOfTrackState.intValue) {
        if (endOfTrackState.intValue != 0) {
            anchoredDraggableState.updateAnchors(
                newAnchors = DraggableAnchors {
                    SwipeButtonAnchor.Start at 0f
                    SwipeButtonAnchor.End at endOfTrackState.intValue.toFloat()
                },
                newTarget = currentAnchorState.value
            )
        }
    }
    Surface(
        modifier = modifier,
        shape = shape,
        color = colors.backgroundColor(),
    ) {
        Layout(
            {
                Box(modifier = Modifier.layoutId(SwipeableButtonLayout.ThumbLayout)) {
                    thumbContent(progressState, targetAnchorState)
                }
                Box(
                    modifier = Modifier
                        .layoutId(SwipeableButtonLayout.ProcessLayout)
                        .drawWithCache {
                            onDrawBehind {
                                drawRect(
                                    color = colors.progressColor(),
                                    size = Size(width = this.size.width, height = this.size.height),
                                )
                            }
                        }
                )
                Box(modifier = Modifier.layoutId(SwipeableButtonLayout.EndLayout)) {
                    endContent(progressState)
                }
                Box(modifier = Modifier.layoutId(SwipeableButtonLayout.CenterLayout)) {
                    centerContent(progressState, currentAnchorState)
                }
                Box(
                    modifier = Modifier
                        .layoutId(SwipeableButtonLayout.BackgroundThumbLayout)
                        .clip(shape)
                        .background(colors.thumbBackgroundColor(), shape)
                        .anchoredDraggable(
                            state = anchoredDraggableState,
                            orientation = Orientation.Horizontal,
                            enabled = enabled && !state.anchoredDraggableState.isAnimationRunning,
                            startDragImmediately = false
                        )
                )
            },
            modifier = Modifier,
            measurePolicy = swipeableMeasure(
                size = size,
                progress = progressState,
                draggableOffset = draggableOffsetState,
                maxAnchor = maxAnchorState,
                endOfTrackState = endOfTrackState
            )
        )
    }
}

@Composable
private fun swipeableMeasure(
    size: SwipeableButtonSize,
    draggableOffset: State<Float>,
    maxAnchor: State<Float>,
    endOfTrackState: MutableIntState,
    progress: MutableFloatState,
): MeasureScope.(measurables: List<Measurable>, constraints: Constraints) -> MeasureResult {
    return { measurables, constraints ->
        val thumbPlaceable = measurables.first { it.layoutId == SwipeableButtonLayout.ThumbLayout }.measure(constraints)
        val thumbHeight = thumbPlaceable.height
        val thumbWidth = thumbPlaceable.width
        val height = thumbHeight.coerceAtLeast(size.minHeight.roundToPx())
        val endOfTrack = constraints.maxWidth - thumbWidth
        val backgroundThumbPlaceable = measurables.first { it.layoutId == SwipeableButtonLayout.BackgroundThumbLayout }.measure(
            constraints.copy(minWidth = thumbWidth.coerceAtLeast(size.minWidth.roundToPx()), minHeight = height)
        )

        if (maxAnchor.value.roundToInt() != endOfTrack && endOfTrack != 0) {
            endOfTrackState.intValue = endOfTrack
        }

        val maxProgressWidth = constraints.maxWidth - backgroundThumbPlaceable.width

        val backgroundThumbX = draggableOffset.value.roundToInt().coerceAtLeast(0).coerceAtMost(maxProgressWidth)

        val progressPlaceable = measurables.first { it.layoutId == SwipeableButtonLayout.ProcessLayout }.measure(
            constraints.copy(
                minWidth = backgroundThumbX + backgroundThumbPlaceable.width / 2,
                minHeight = height
            )
        )

        val endContentPlaceable = measurables.first {
            it.layoutId == SwipeableButtonLayout.EndLayout
        }.measure(constraints)
        val centerContentPlaceable = measurables.first {
            it.layoutId == SwipeableButtonLayout.CenterLayout
        }.measure(constraints)
        progress.floatValue = backgroundThumbX.toFloat() / maxProgressWidth
        layout(constraints.maxWidth, height) {
            progressPlaceable.placeRelative(x = 0, y = 0)
            centerContentPlaceable.placeRelative(
                x = constraints.maxWidth / 2 - centerContentPlaceable.width / 2,
                y = height / 2 - centerContentPlaceable.height / 2
            )
            backgroundThumbPlaceable.placeRelative(x = backgroundThumbX, y = 0)
            thumbPlaceable.placeRelative(
                x = backgroundThumbX + backgroundThumbPlaceable.width / 2 - thumbWidth / 2,
                y = height / 2 - thumbPlaceable.height / 2
            )
            endContentPlaceable.placeRelative(
                x = constraints.maxWidth - endContentPlaceable.width,
                y = height / 2 - endContentPlaceable.height / 2
            )
        }
    }

}

enum class SwipeButtonAnchor { Start, End }

enum class SwipeableButtonSize(
    val minWidth: Dp,
    val minHeight: Dp,
) {
    Small(minWidth = 40.dp, minHeight = 40.dp),
    Medium(minWidth = 48.dp, minHeight = 48.dp),
    Large(minWidth = 56.dp, minHeight = 56.dp)
}

private enum class SwipeableButtonLayout {
    BackgroundThumbLayout, ThumbLayout, ProcessLayout, EndLayout, CenterLayout
}

@Stable
interface SwipeableButtonColors {
    fun progressColor(): Color
    fun thumbBackgroundColor(): Color
    fun backgroundColor(): Color
}

@Immutable
object SwipeableButtonDefaults {

    @Composable
    fun colors(
        progressColor: Color = MaterialTheme.colors.primary,
        thumbBackgroundColor: Color = MaterialTheme.colors.primary,
        backgroundColor: Color = MaterialTheme.colors.primary.copy(alpha = 0.1f),
    ): SwipeableButtonColors {
        return DefaultSwipeableButtonColors(
            backgroundColor = backgroundColor,
            thumbBackgroundColor = thumbBackgroundColor,
            progressColor = progressColor
        )
    }
}

@Immutable
private class DefaultSwipeableButtonColors(
    private val backgroundColor: Color,
    private val thumbBackgroundColor: Color,
    private val progressColor: Color,
) : SwipeableButtonColors {
    override fun progressColor(): Color = progressColor
    override fun thumbBackgroundColor(): Color = thumbBackgroundColor
    override fun backgroundColor(): Color = backgroundColor

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as DefaultSwipeableButtonColors
        return when {
            backgroundColor != other.backgroundColor -> false
            thumbBackgroundColor != other.thumbBackgroundColor -> false
            progressColor != other.progressColor -> false
            else -> true
        }
    }

    override fun hashCode(): Int {
        var result = backgroundColor.hashCode()
        result = 31 * result + thumbBackgroundColor.hashCode()
        result = 31 * result + progressColor.hashCode()
        return result
    }
}