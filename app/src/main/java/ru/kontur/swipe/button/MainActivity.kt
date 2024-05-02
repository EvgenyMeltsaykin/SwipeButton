package ru.kontur.swipe.button

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.vk.recompose.highlighter.RecomposeHighlighterConfig
import com.vk.recompose.logger.RecomposeLoggerConfig
import ru.kontur.swipe.button.implementation.NotOptimalSwipeButtonContent
import ru.kontur.swipe.button.implementation.OptimalSwipeButtonContent
import ru.kontur.swipe.button.ui.theme.SwipebuttonTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        RecomposeHighlighterConfig.isEnabled = true
        RecomposeLoggerConfig.isEnabled = true
        setContent {
            SwipebuttonTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colors.background) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                    ) {
                        OptimalSwipeButtonContent()
                        Spacer(modifier = Modifier.height(16.dp))
                        NotOptimalSwipeButtonContent()
                    }
                }
            }
        }
    }
}