package ru.kontur.swipe.button.implementation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.unit.dp
import ru.kontur.swipe.button.notOptimalSwipeButton.Anchor
import ru.kontur.swipe.button.notOptimalSwipeButton.NotOptimalSwipeButton

@Composable
fun NotOptimalSwipeButtonContent() {
    var isLoading by rememberSaveable { mutableStateOf(false) }
    NotOptimalSwipeButton(
        progressColor = MaterialTheme.colors.primary,
        thumbBackgroundColor = MaterialTheme.colors.primary,
        backgroundColor = MaterialTheme.colors.primary.copy(
            alpha = 0.1f
        ),
        centerContent = { progress, currentAnchor ->
            val correctProgress = when (currentAnchor) {
                Anchor.Start -> {
                    if (progress == 1f) 0f else progress
                }

                Anchor.End -> progress
            }
            Box(
                contentAlignment = Alignment.Center
            ) {
                AnimatedVisibility(
                    visible = isLoading,
                    enter = fadeIn() + scaleIn(),
                    exit = fadeOut() + scaleOut()
                ) {
                    CircularProgressIndicator(
                        color = MaterialTheme.colors.onPrimary,
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp
                    )
                }
                AnimatedVisibility(
                    visible = !isLoading,
                    enter = fadeIn() + scaleIn(),
                    exit = fadeOut() + scaleOut()
                ) {
                    Text(
                        modifier = Modifier.alpha(correctProgress.coerceAtLeast(0.4f)),
                        text = "Принять",
                        color = MaterialTheme.colors.onSurface,
                    )
                }
            }
        },
        thumbContent = { progress, targetAnchor ->
            val imageVector = when (targetAnchor) {
                Anchor.Start -> Icons.AutoMirrored.Filled.ArrowForward
                Anchor.End -> Icons.Filled.Check
            }
            Box(
                modifier = Modifier.size(56.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = imageVector,
                    contentDescription = null,
                    tint = MaterialTheme.colors.onPrimary
                )
            }
        },
        endContent = { progress, currentAnchor ->
            val correctProgress = when (currentAnchor) {
                Anchor.Start -> {
                    if (progress == 1f) 0f else progress
                }

                Anchor.End -> progress
            }
            Row {
                Icon(
                    modifier = Modifier.alpha(1 - correctProgress),
                    imageVector = Icons.Filled.Check,
                    contentDescription = null,
                    tint = MaterialTheme.colors.onSurface.copy()
                )
                Spacer(modifier = Modifier.width(16.dp))
            }
        },
        onSwiped = {
            isLoading = when (it) {
                Anchor.Start -> false
                Anchor.End -> true
            }
        }
    )
}