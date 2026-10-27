package xyz.mpv.rex.ui.utils

import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import xyz.mpv.rex.presentation.Screen

val LocalBackStack = compositionLocalOf<SnapshotStateList<Screen>> {
  error("No backstack provided")
}
