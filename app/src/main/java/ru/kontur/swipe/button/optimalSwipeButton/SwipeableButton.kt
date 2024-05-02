package ru.kontur.swipe.button.optimalSwipeButton

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.DraggableAnchors
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.anchoredDraggable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.MaterialTheme
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
import androidx.compose.ui.Alignment
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
import kotlin.math.roundToInt

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
    onSwiped: (target: SwipeButtonAnchor) -> Unit = {},
) {
    val progressState = remember { mutableFloatStateOf(0f) }
    var lastAnchor by remember { mutableStateOf(state.initialValue) }
    val currentAnchorState = remember { derivedStateOf { state.anchoredDraggableState.currentValue } }
    val targetAnchorState = remember { derivedStateOf { state.anchoredDraggableState.targetValue } }
    val endOfTrackState = remember { mutableIntStateOf(0) }
    LaunchedEffect(state.currentValue) {
        if (state.currentValue != lastAnchor) {
            onSwiped(state.currentValue)
        }
        lastAnchor = state.currentValue
    }

    LaunchedEffect(endOfTrackState.intValue) {
        if (endOfTrackState.intValue != 0) {
            state.anchoredDraggableState.updateAnchors(
                newAnchors = DraggableAnchors {
                    SwipeButtonAnchor.Start at 0f
                    SwipeButtonAnchor.End at endOfTrackState.intValue.toFloat()
                },
                newTarget = state.currentValue
            )
        }
    }
    Layout(
        {
            // Помещаем элемент-якорь
            Box(
                modifier = Modifier
                    .layoutId(SwipeableButtonLayout.ThumbLayout)
                    .clip(shape)
                    .background(colors.thumbBackgroundColor(), shape)
                    .anchoredDraggable(
                        state = state.anchoredDraggableState,
                        orientation = Orientation.Horizontal,
                        enabled = enabled,
                        startDragImmediately = false
                    ),
                contentAlignment = Alignment.Center
            ) {
                thumbContent(progressState, targetAnchorState)
            }
            // Помещаем элемент отвечающий за прогресс свайпа
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
            // Помещаем правый элемент
            Box(
                modifier = Modifier.layoutId(SwipeableButtonLayout.EndLayout)
            ) {
                endContent(progressState)
            }
            // Помещаем центральный элемент
            Box(
                modifier = Modifier.layoutId(SwipeableButtonLayout.CenterLayout)
            ) {
                centerContent(progressState, currentAnchorState)
            }
        },
        modifier = modifier
            .clip(shape)
            .background(colors.backgroundColor(), shape),
        measurePolicy = swipeableMeasure(
            size = size,
            progressState = progressState,
            draggableOffsetProvider = { state.anchoredDraggableState.requireOffset().roundToInt() },
            maxAnchorProvider = { state.anchoredDraggableState.anchors.maxAnchor().roundToInt() },
            endOfTrackState = endOfTrackState
        )
    )
}

@Composable
private fun swipeableMeasure(
    size: SwipeableButtonSize,
    draggableOffsetProvider: () -> Int,
    maxAnchorProvider: () -> Int,
    endOfTrackState: MutableIntState,
    progressState: MutableFloatState,
): MeasureScope.(measurables: List<Measurable>, constraints: Constraints) -> MeasureResult {
    return { measurables, constraints ->
        // Измеряем слайд-якорь
        val thumbPlaceable = measurables.first { it.layoutId == SwipeableButtonLayout.ThumbLayout }.measure(
            constraints.copy(
                minHeight = constraints.minHeight.coerceAtLeast(size.minHeight.roundToPx()),
                minWidth = constraints.minWidth.coerceAtLeast(size.minWidth.roundToPx())
            )
        )
        // Высота кнопки
        val height = thumbPlaceable.height
        // Ширина якоря
        val thumbWidth = thumbPlaceable.width
        // Рассчитываем длину свайпа
        val endOfTrackWidth = constraints.maxWidth - thumbWidth

        if (maxAnchorProvider() != endOfTrackWidth && endOfTrackWidth != 0) {
            endOfTrackState.intValue = endOfTrackWidth
        }

        // Рассчитываем отступ по икс для якоря
        val thumbX = draggableOffsetProvider().coerceAtLeast(0).coerceAtMost(endOfTrackWidth)

        // Измеряем элемент прогресса
        val progressPlaceable = measurables.first { it.layoutId == SwipeableButtonLayout.ProcessLayout }.measure(
            constraints.copy(
                minWidth = thumbX + thumbWidth / 2,
                minHeight = height
            )
        )

        // Измеряем правый элемент
        val endContentPlaceable = measurables.first { it.layoutId == SwipeableButtonLayout.EndLayout }.measure(constraints)
        // Измеряем центральный элемент
        val centerContentPlaceable = measurables.first { it.layoutId == SwipeableButtonLayout.CenterLayout }.measure(constraints)
        progressState.floatValue = thumbX.toFloat() / endOfTrackWidth
        layout(constraints.maxWidth, height) {
            // Размещаем элемент прогресса
            progressPlaceable.placeRelative(x = 0, y = 0)
            // Размещаем центральный элемент
            centerContentPlaceable.placeRelative(
                x = constraints.maxWidth / 2 - centerContentPlaceable.width / 2,
                y = height / 2 - centerContentPlaceable.height / 2
            )
            // Размещаем элемент-якорь
            thumbPlaceable.placeRelative(
                x = thumbX,
                y = height / 2 - thumbPlaceable.height / 2
            )
            // Размещаем правый элемент
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
    ThumbLayout, ProcessLayout, EndLayout, CenterLayout
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