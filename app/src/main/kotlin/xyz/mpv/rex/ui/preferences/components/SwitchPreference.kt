package xyz.mpv.rex.ui.preferences.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Switch
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import me.zhanghai.compose.preference.Preference

@Composable
fun SwitchPreference(
  value: Boolean,
  onValueChange: (Boolean) -> Unit,
  title: @Composable () -> Unit,
  modifier: Modifier = Modifier,
  summary: (@Composable () -> Unit)? = null,
  icon: (@Composable () -> Unit)? = null,
  enabled: Boolean = true,
) {
  Preference(
    title = title,
    summary = summary,
    icon = icon,
    enabled = enabled,
    onClick = { onValueChange(!value) },
    widgetContainer = {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Spacer(modifier = Modifier.width(8.dp))
        Switch(
          checked = value,
          onCheckedChange = onValueChange,
          enabled = enabled,
        )
      }
    },
    modifier = modifier,
  )
}
