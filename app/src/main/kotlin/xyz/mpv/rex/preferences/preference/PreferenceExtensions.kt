package xyz.mpv.rex.preferences.preference

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.rememberCoroutineScope

@Composable
fun <T> Preference<T>.collectAsState(): State<T> {
  val scope = rememberCoroutineScope()
  return stateIn(scope).collectAsState(initial = get())
}
