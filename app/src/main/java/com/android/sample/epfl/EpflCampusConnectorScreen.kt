package com.android.sample.epfl

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.LinkOff
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Snackbar
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.viewmodel.compose.viewModel
import com.android.sample.settings.Localization
import com.android.sample.ui.theme.EulerGreen
import com.android.sample.ui.theme.EulerRed

/**
 * EPFL Campus Schedule Connector Screen
 * 
 * Allows users to:
 * - Connect their EPFL schedule via ICS URL
 * - View connection status
 * - Disconnect schedule
 */
@Composable
fun EpflCampusConnectorScreen(
    onBackClick: () -> Unit = {},
    viewModel: EpflCampusViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    
    val colorScheme = MaterialTheme.colorScheme
    val background = colorScheme.background
    val surface = colorScheme.surface
    val textPrimary = colorScheme.onBackground
    val textSecondary = colorScheme.onSurfaceVariant
    val accentRed = EulerRed
    val accentGreen = EulerGreen
    
    // Check clipboard when screen becomes visible
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.checkClipboard(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }
    
    Box(modifier = Modifier.fillMaxSize().background(background)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 100.dp)
        ) {
            // Top bar
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBackClick, modifier = Modifier.size(36.dp)) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = Localization.t("close"),
                        tint = textPrimary
                    )
                }
                Text(
                    text = Localization.t("epfl_campus_title"),
                    color = textPrimary,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center
                )
                // Spacer for symmetry
                Spacer(modifier = Modifier.size(36.dp))
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Main content
            Column(
                modifier = Modifier.padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header card with EPFL branding
                EpflHeaderCard(surface, textPrimary, textSecondary)
                
                if (uiState.isLoading) {
                    // Loading state
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = accentRed)
                    }
                } else if (uiState.isConnected) {
                    // Connected state
                    ConnectedCard(
                        weeklySlots = uiState.weeklySlots,
                        finalExams = uiState.finalExams,
                        lastSync = uiState.lastSync,
                        surface = surface,
                        textPrimary = textPrimary,
                        textSecondary = textSecondary,
                        accentGreen = accentGreen
                    )
                    
                    // Disconnect button
                    OutlinedButton(
                        onClick = { viewModel.disconnect() },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = accentRed),
                        border = ButtonDefaults.outlinedButtonBorder.copy(
                            brush = Brush.horizontalGradient(listOf(accentRed, accentRed))
                        )
                    ) {
                        Icon(
                            Icons.Filled.LinkOff,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(Localization.t("epfl_disconnect"))
                    }
                } else {
                    // Not connected - show instructions and input
                    InstructionsCard(surface, textPrimary, textSecondary)
                    
                    // Open EPFL Campus button
                    Button(
                        onClick = { viewModel.openEpflCampus(context) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = accentRed)
                    ) {
                        Icon(
                            Icons.Filled.OpenInNew,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(Localization.t("epfl_open_campus"))
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // ICS URL input
                    IcsUrlInput(
                        value = uiState.icsUrlInput,
                        onValueChange = { viewModel.updateIcsUrl(it) },
                        isValid = uiState.isValidUrl,
                        isLikelyEpfl = uiState.isLikelyEpflUrl,
                        isSyncing = uiState.isSyncing,
                        onSync = { viewModel.syncSchedule() },
                        surface = surface,
                        textPrimary = textPrimary,
                        textSecondary = textSecondary,
                        accentRed = accentRed
                    )
                }
            }
        }
        
        // Clipboard suggestion banner
        AnimatedVisibility(
            visible = uiState.showClipboardSuggestion,
            modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp),
            enter = slideInVertically { it } + fadeIn(),
            exit = slideOutVertically { it } + fadeOut()
        ) {
            ClipboardSuggestionBanner(
                url = uiState.detectedClipboardUrl ?: "",
                onAccept = { viewModel.acceptClipboardUrl() },
                onDismiss = { viewModel.dismissClipboardSuggestion() },
                surface = surface,
                textPrimary = textPrimary,
                accentGreen = accentGreen
            )
        }
        
        // Error snackbar
        uiState.error?.let { error ->
            Snackbar(
                modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp),
                action = {
                    TextButton(onClick = { viewModel.clearError() }) {
                        Text("OK", color = colorScheme.inversePrimary)
                    }
                }
            ) {
                Text(error)
            }
        }
        
        // Success snackbar
        uiState.successMessage?.let { message ->
            Snackbar(
                modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp),
                containerColor = accentGreen.copy(alpha = 0.9f),
                action = {
                    TextButton(onClick = { viewModel.clearSuccessMessage() }) {
                        Text("OK", color = colorScheme.onPrimary)
                    }
                }
            ) {
                Text(message, color = colorScheme.onPrimary)
            }
        }
    }
}

@Composable
private fun EpflHeaderCard(
    surface: androidx.compose.ui.graphics.Color,
    textPrimary: androidx.compose.ui.graphics.Color,
    textSecondary: androidx.compose.ui.graphics.Color
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = surface),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // EPFL Logo placeholder
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(EulerRed.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Filled.CalendarMonth,
                    contentDescription = null,
                    tint = EulerRed,
                    modifier = Modifier.size(32.dp)
                )
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column {
                Text(
                    text = "EPFL Campus",
                    color = textPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = Localization.t("epfl_campus_subtitle"),
                    color = textSecondary,
                    fontSize = 14.sp
                )
            }
        }
    }
}

@Composable
private fun ConnectedCard(
    weeklySlots: Int,
    finalExams: Int,
    lastSync: String?,
    surface: androidx.compose.ui.graphics.Color,
    textPrimary: androidx.compose.ui.graphics.Color,
    textSecondary: androidx.compose.ui.graphics.Color,
    accentGreen: androidx.compose.ui.graphics.Color
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = surface),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(accentGreen.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Filled.Check,
                        contentDescription = null,
                        tint = accentGreen,
                        modifier = Modifier.size(24.dp)
                    )
                }
                
                Spacer(modifier = Modifier.width(12.dp))
                
                Column {
                    Text(
                        text = Localization.t("epfl_connected"),
                        color = accentGreen,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "$weeklySlots ${Localization.t("epfl_weekly_classes")} • $finalExams ${Localization.t("epfl_exams")}",
                        color = textSecondary,
                        fontSize = 14.sp
                    )
                }
            }
            
            lastSync?.let {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "${Localization.t("epfl_last_sync")}: $it",
                    color = textSecondary.copy(alpha = 0.7f),
                    fontSize = 12.sp
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Info about what EULER can do now
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = textPrimary.copy(alpha = 0.05f),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        Icons.Filled.Schedule,
                        contentDescription = null,
                        tint = textSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = Localization.t("epfl_connected_info"),
                        color = textSecondary,
                        fontSize = 13.sp,
                        lineHeight = 18.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun InstructionsCard(
    surface: androidx.compose.ui.graphics.Color,
    textPrimary: androidx.compose.ui.graphics.Color,
    textSecondary: androidx.compose.ui.graphics.Color
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = surface),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = Localization.t("epfl_instructions_title"),
                color = textPrimary,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            InstructionStep(1, Localization.t("epfl_step_1"), textPrimary, textSecondary)
            InstructionStep(2, Localization.t("epfl_step_2"), textPrimary, textSecondary)
            InstructionStep(3, Localization.t("epfl_step_3"), textPrimary, textSecondary)
            InstructionStep(4, Localization.t("epfl_step_4"), textPrimary, textSecondary)
        }
    }
}

@Composable
private fun InstructionStep(
    number: Int,
    text: String,
    textPrimary: androidx.compose.ui.graphics.Color,
    textSecondary: androidx.compose.ui.graphics.Color
) {
    Row(
        modifier = Modifier.padding(vertical = 6.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(CircleShape)
                .background(textPrimary.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = number.toString(),
                color = textPrimary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
        }
        
        Spacer(modifier = Modifier.width(12.dp))
        
        Text(
            text = text,
            color = textSecondary,
            fontSize = 14.sp,
            lineHeight = 20.sp,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun IcsUrlInput(
    value: String,
    onValueChange: (String) -> Unit,
    isValid: Boolean,
    isLikelyEpfl: Boolean,
    isSyncing: Boolean,
    onSync: () -> Unit,
    surface: androidx.compose.ui.graphics.Color,
    textPrimary: androidx.compose.ui.graphics.Color,
    textSecondary: androidx.compose.ui.graphics.Color,
    accentRed: androidx.compose.ui.graphics.Color
) {
    Column {
        Text(
            text = Localization.t("epfl_paste_url"),
            color = textPrimary,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = {
                Text(
                    "https://campus.epfl.ch/deploy/...",
                    color = textSecondary.copy(alpha = 0.5f)
                )
            },
            leadingIcon = {
                Icon(
                    Icons.Filled.Link,
                    contentDescription = null,
                    tint = if (isValid) accentRed else textSecondary
                )
            },
            trailingIcon = {
                if (value.isNotEmpty()) {
                    IconButton(onClick = { onValueChange("") }) {
                        Icon(
                            Icons.Filled.Close,
                            contentDescription = "Clear",
                            tint = textSecondary
                        )
                    }
                }
            },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = accentRed,
                unfocusedBorderColor = textSecondary.copy(alpha = 0.3f),
                focusedTextColor = textPrimary,
                unfocusedTextColor = textPrimary
            ),
            shape = RoundedCornerShape(12.dp),
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Uri,
                imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(
                onDone = { if (isValid) onSync() }
            )
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Button(
            onClick = onSync,
            modifier = Modifier.fillMaxWidth(),
            enabled = isValid && !isSyncing,
            colors = ButtonDefaults.buttonColors(
                containerColor = accentRed,
                disabledContainerColor = accentRed.copy(alpha = 0.4f)
            )
        ) {
            if (isSyncing) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    color = MaterialTheme.colorScheme.onPrimary,
                    strokeWidth = 2.dp
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(
                if (isSyncing) Localization.t("epfl_syncing") 
                else Localization.t("epfl_connect")
            )
        }
    }
}

@Composable
private fun ClipboardSuggestionBanner(
    url: String,
    onAccept: () -> Unit,
    onDismiss: () -> Unit,
    surface: androidx.compose.ui.graphics.Color,
    textPrimary: androidx.compose.ui.graphics.Color,
    accentGreen: androidx.compose.ui.graphics.Color
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = surface),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Filled.ContentPaste,
                    contentDescription = null,
                    tint = accentGreen,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = Localization.t("epfl_clipboard_detected"),
                    color = textPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = url.take(50) + if (url.length > 50) "..." else "",
                color = textPrimary.copy(alpha = 0.7f),
                fontSize = 12.sp
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = onDismiss) {
                    Text(Localization.t("not_now"), color = textPrimary.copy(alpha = 0.6f))
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = onAccept,
                    colors = ButtonDefaults.buttonColors(containerColor = accentGreen)
                ) {
                    Text(Localization.t("use_this_url"))
                }
            }
        }
    }
}

