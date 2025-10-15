package com.android.sample

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.android.sample.resources.C

@Composable
fun HomeScreen() {
    Column(Modifier.fillMaxSize().padding(20.dp).testTag(C.Tag.screenHome)) {
        Text("🏠 Home — Placeholder", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(8.dp))
        Text("Ici on mettra l’overview / quick actions.")
    }
}

@Composable
fun ChatScreen() {
    var text by remember { mutableStateOf(TextFieldValue("")) }
    Column(Modifier.fillMaxSize().padding(16.dp).testTag(C.Tag.screenChat)) {
        Text("💬 Chat — Placeholder", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = text,
            onValueChange = { text = it },
            placeholder = { Text("Ask EULER…") },
            modifier = Modifier.fillMaxWidth().testTag(C.Tag.chatInput),
        )
        Spacer(Modifier.height(12.dp))
        Button(onClick = { /* TODO: call backend */}, enabled = text.text.isNotBlank()) {
            Text("Send")
        }
    }
}

@Composable
fun SettingsScreen() {
    Column(Modifier.fillMaxSize().padding(20.dp).testTag(C.Tag.screenSettings)) {
        Text("⚙️ Settings — Placeholder", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(8.dp))
        Text("Compte, thème, permissions…")
    }
}
