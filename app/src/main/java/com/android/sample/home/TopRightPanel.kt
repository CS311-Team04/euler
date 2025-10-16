package com.android.sample.home

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun TopRightPanel(
    expanded: Boolean,
    onDismiss: () -> Unit,
    ui: HomeUiState,
    onToggleSystem: (String) -> Unit,
    onOpenSettings: () -> Unit
) {
  DropdownMenu(
      expanded = expanded,
      onDismissRequest = onDismiss,
  ) {
    // --- Title
    DropdownMenuItem(
        text = {
          Text(
              "Quick actions",
              style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold))
        },
        onClick = { /* no-op */},
        enabled = false)

    // --- Settings
    DropdownMenuItem(
        text = { Text("Settings") },
        onClick = {
          onDismiss()
          onOpenSettings()
        })

    Divider()

    // --- Systems (2 premiers pour rester compact)
    ui.systems.take(2).forEach { s ->
      DropdownMenuItem(
          text = {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
              Text(s.name)
              Switch(checked = s.isConnected, onCheckedChange = { onToggleSystem(s.id) })
            }
          },
          onClick = { /* handled by Switch */})
    }

    Divider()

    // --- Recent actions (3 dernières)
    val recent = ui.recent.take(3)
    if (recent.isEmpty()) {
      DropdownMenuItem(text = { Text("No recent actions", color = Color.Gray) }, onClick = {})
    } else {
      recent.forEach { a ->
        DropdownMenuItem(
            text = {
              Column {
                Text(a.title, style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(2.dp))
                Text(a.time, style = MaterialTheme.typography.labelSmall, color = Color.Gray)
              }
            },
            onClick = { /* no-op */})
      }
    }
  }
}
