package xyz.mpv.rex.presentation.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

enum class GroupPosition {
  FIRST, MIDDLE, LAST, ONLY
}

@Composable
fun GroupedListColumn(
  modifier: Modifier = Modifier,
  content: @Composable () -> Unit,
) {
  Column(
    modifier = modifier.fillMaxWidth(),
    content = { content() }
  )
}

@Composable
fun GroupedListItem(
  position: GroupPosition,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  content: @Composable () -> Unit,
) {
  val shape = when (position) {
    GroupPosition.FIRST -> RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = 4.dp, bottomEnd = 4.dp)
    GroupPosition.MIDDLE -> RoundedCornerShape(CornerSize(4.dp))
    GroupPosition.LAST -> RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp, bottomStart = 16.dp, bottomEnd = 16.dp)
    GroupPosition.ONLY -> RoundedCornerShape(16.dp)
  }

  Card(
    onClick = onClick,
    shape = shape,
    colors = CardDefaults.cardColors(
      containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
    ),
    modifier = modifier.fillMaxWidth(),
  ) {
    content()
  }
}
