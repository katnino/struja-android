package ba.noniboy.struja

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.view.WindowInsetsControllerCompat
import ba.noniboy.struja.ui.navigation.NavGraph
import ba.noniboy.struja.ui.theme.StrujaTheme
import dagger.hilt.android.AndroidEntryPoint

/**
 * Main activity — single-activity architecture using Jetpack Compose Navigation.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // Light status bar icons for dark theme
        val insetsController = WindowInsetsControllerCompat(
            window,
            window.decorView
        )
        insetsController.isAppearanceLightStatusBars = false

        setContent {
            StrujaAppContent()
        }
    }
}

@Composable
fun StrujaAppContent() {
    StrujaTheme {
        NavGraph()
    }
}

@Preview(showBackground = true, name = "Struja App Preview")
@Composable
fun StrujaAppPreview() {
    StrujaTheme {
        NavGraph()
    }
}
