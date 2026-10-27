package xyz.mpv.rex

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import xyz.mpv.rex.ui.preferences.CSCorePreferencesScreen
import xyz.mpv.rex.ui.preferences.PreferencesScreen
import xyz.mpv.rex.ui.utils.LocalBackStack

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContent {
      val backstack = remember { mutableStateListOf(PreferencesScreen, CSCorePreferencesScreen) }
      CompositionLocalProvider(LocalBackStack provides backstack) {
        val currentScreen = backstack.lastOrNull() ?: PreferencesScreen
        currentScreen.Content()
      }
    }
  }
}
