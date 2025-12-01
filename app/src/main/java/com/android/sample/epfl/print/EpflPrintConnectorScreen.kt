package com.android.sample.epfl.print

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.LinkOff
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Snackbar
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.android.sample.settings.Localization
import com.android.sample.ui.theme.EulerGreen
import com.android.sample.ui.theme.EulerRed

/**
 * EPFL Print Connector Screen
 *
 * Allows users to:
 * - Connect to EPFL Print via OAuth
 * - Upload and print files
 * - Manage print settings
 */
@Composable
fun EpflPrintConnectorScreen(
    viewModel: EpflPrintViewModel,
    onBackClick: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    val colorScheme = MaterialTheme.colorScheme
    val background = colorScheme.background
    val surface = colorScheme.surface
    val textPrimary = colorScheme.onBackground
    val textSecondary = colorScheme.onSurfaceVariant
    val accentRed = EulerRed
    val accentGreen = EulerGreen

    // File picker launcher
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            val contentResolver = context.contentResolver
            val mimeType = contentResolver.getType(uri) ?: "application/pdf"
            val fileName = uri.lastPathSegment ?: "document"
            
            contentResolver.openInputStream(uri)?.let { inputStream ->
                viewModel.setFileToPrint(fileName, mimeType, inputStream)
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(background)) {
        // OAuth WebView overlay
        AnimatedVisibility(
            visible = uiState.showWebView,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            OAuthWebView(
                authUrl = viewModel.getOAuthUrl(),
                onCallback = { callbackUrl ->
                    viewModel.handleOAuthCallback(callbackUrl)
                },
                isCallbackUrl = { url -> viewModel.isOAuthCallback(url) },
                onCancel = { viewModel.cancelOAuthFlow() },
                modifier = Modifier.fillMaxSize()
            )
        }

        // Main content (hidden when WebView is shown)
        AnimatedVisibility(
            visible = !uiState.showWebView,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(bottom = 100.dp)
            ) {
                // Top bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBackClick, modifier = Modifier.size(36.dp)) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = "Close",
                            tint = textPrimary
                        )
                    }
                    Text(
                        text = "EPFL Print",
                        color = textPrimary,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.size(36.dp))
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Main content
                Column(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Header card
                    PrintHeaderCard(surface, textPrimary, textSecondary)

                    if (uiState.isLoading) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = accentRed)
                        }
                    } else if (uiState.isConnected) {
                        // Connected state
                        ConnectedCard(
                            email = uiState.connectedEmail,
                            creditBalance = uiState.creditBalance,
                            surface = surface,
                            textPrimary = textPrimary,
                            textSecondary = textSecondary,
                            accentGreen = accentGreen
                        )

                        // File upload section
                        FileUploadCard(
                            pendingFile = uiState.pendingFile,
                            onSelectFile = { filePickerLauncher.launch("application/pdf") },
                            onClearFile = { viewModel.clearPendingFile() },
                            surface = surface,
                            textPrimary = textPrimary,
                            textSecondary = textSecondary,
                            accentRed = accentRed
                        )

                        // Print options
                        if (uiState.pendingFile != null) {
                            PrintOptionsCard(
                                options = uiState.printOptions,
                                onOptionsChange = { viewModel.updatePrintOptions(it) },
                                surface = surface,
                                textPrimary = textPrimary,
                                textSecondary = textSecondary
                            )

                            // Print button
                            Button(
                                onClick = { viewModel.submitPrintJob() },
                                modifier = Modifier.fillMaxWidth(),
                                enabled = !uiState.isSubmitting,
                                colors = ButtonDefaults.buttonColors(containerColor = accentRed)
                            ) {
                                if (uiState.isSubmitting) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(18.dp),
                                        color = MaterialTheme.colorScheme.onPrimary,
                                        strokeWidth = 2.dp
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                }
                                Icon(Icons.Filled.Print, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(if (uiState.isSubmitting) "Sending..." else "Print")
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Disconnect button
                        OutlinedButton(
                            onClick = { viewModel.disconnect() },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = accentRed)
                        ) {
                            Icon(Icons.Filled.LinkOff, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Disconnect")
                        }
                    } else {
                        // Not connected
                        NotConnectedCard(
                            surface = surface,
                            textPrimary = textPrimary,
                            textSecondary = textSecondary
                        )

                        Button(
                            onClick = { viewModel.startOAuthFlow() },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = accentRed)
                        ) {
                            Text("Connect to EPFL Print")
                        }
                    }
                }
            }
        }

        // Error snackbar
        uiState.error?.let { error ->
            Snackbar(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp),
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
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp),
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

    // Load balance when connected
    LaunchedEffect(uiState.isConnected) {
        if (uiState.isConnected) {
            viewModel.loadCreditBalance()
        }
    }
}

// =============================================================================
// WEBVIEW COMPONENT
// =============================================================================

@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun OAuthWebView(
    authUrl: String,
    onCallback: (String) -> Unit,
    isCallbackUrl: (String) -> Boolean,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isLoading by remember { mutableStateOf(true) }

    Box(modifier = modifier.background(MaterialTheme.colorScheme.background)) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { context ->
                WebView(context).apply {
                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = true
                    
                    webViewClient = object : WebViewClient() {
                        override fun shouldOverrideUrlLoading(
                            view: WebView?,
                            request: WebResourceRequest?
                        ): Boolean {
                            val url = request?.url?.toString() ?: return false
                            
                            if (isCallbackUrl(url)) {
                                onCallback(url)
                                return true
                            }
                            return false
                        }

                        override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                            super.onPageStarted(view, url, favicon)
                            isLoading = true
                            
                            // Check if this is the callback
                            url?.let {
                                if (isCallbackUrl(it)) {
                                    onCallback(it)
                                }
                            }
                        }

                        override fun onPageFinished(view: WebView?, url: String?) {
                            super.onPageFinished(view, url)
                            isLoading = false
                        }
                    }
                    
                    loadUrl(authUrl)
                }
            }
        )

        // Loading indicator
        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = EulerRed)
            }
        }

        // Cancel button
        IconButton(
            onClick = onCancel,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp)
                .size(40.dp)
                .background(
                    MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                    CircleShape
                )
        ) {
            Icon(
                Icons.Filled.Close,
                contentDescription = "Cancel",
                tint = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

// =============================================================================
// CARD COMPONENTS
// =============================================================================

@Composable
private fun PrintHeaderCard(
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
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(EulerRed.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Filled.Print,
                    contentDescription = null,
                    tint = EulerRed,
                    modifier = Modifier.size(32.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column {
                Text(
                    text = "EPFL Print",
                    color = textPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Print documents on campus printers",
                    color = textSecondary,
                    fontSize = 14.sp
                )
            }
        }
    }
}

@Composable
private fun ConnectedCard(
    email: String?,
    creditBalance: Double?,
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
                        text = "Connected",
                        color = accentGreen,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    email?.let {
                        Text(
                            text = it,
                            color = textSecondary,
                            fontSize = 14.sp
                        )
                    }
                }
            }

            creditBalance?.let { balance ->
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Credit: CHF ${String.format("%.2f", balance)}",
                    color = textSecondary,
                    fontSize = 14.sp
                )
            }
        }
    }
}

@Composable
private fun NotConnectedCard(
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
                text = "Connect to print",
                color = textPrimary,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Sign in with your EPFL account to print documents directly from EULER.",
                color = textSecondary,
                fontSize = 14.sp
            )
        }
    }
}

@Composable
private fun FileUploadCard(
    pendingFile: PendingPrintFile?,
    onSelectFile: () -> Unit,
    onClearFile: () -> Unit,
    surface: androidx.compose.ui.graphics.Color,
    textPrimary: androidx.compose.ui.graphics.Color,
    textSecondary: androidx.compose.ui.graphics.Color,
    accentRed: androidx.compose.ui.graphics.Color
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = surface),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = "Document",
                color = textPrimary,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(12.dp))

            if (pendingFile != null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Filled.Description,
                        contentDescription = null,
                        tint = accentRed,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = pendingFile.fileName,
                            color = textPrimary,
                            fontSize = 14.sp
                        )
                        Text(
                            text = formatFileSize(pendingFile.sizeBytes),
                            color = textSecondary,
                            fontSize = 12.sp
                        )
                    }
                    IconButton(onClick = onClearFile) {
                        Icon(
                            Icons.Filled.Close,
                            contentDescription = "Remove",
                            tint = textSecondary
                        )
                    }
                }
            } else {
                OutlinedButton(
                    onClick = onSelectFile,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Filled.Upload, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Select PDF")
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PrintOptionsCard(
    options: PrintOptions,
    onOptionsChange: (PrintOptions) -> Unit,
    surface: androidx.compose.ui.graphics.Color,
    textPrimary: androidx.compose.ui.graphics.Color,
    textSecondary: androidx.compose.ui.graphics.Color
) {
    var copiesExpanded by remember { mutableStateOf(false) }
    var colorExpanded by remember { mutableStateOf(false) }
    var duplexExpanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = surface),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = "Print Options",
                color = textPrimary,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Copies
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Copies:", color = textSecondary, modifier = Modifier.width(100.dp))
                ExposedDropdownMenuBox(
                    expanded = copiesExpanded,
                    onExpandedChange = { copiesExpanded = it },
                    modifier = Modifier.weight(1f)
                ) {
                    OutlinedTextField(
                        value = options.copies.toString(),
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = copiesExpanded) },
                        modifier = Modifier.menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = copiesExpanded,
                        onDismissRequest = { copiesExpanded = false }
                    ) {
                        (1..10).forEach { count ->
                            DropdownMenuItem(
                                text = { Text(count.toString()) },
                                onClick = {
                                    onOptionsChange(options.copy(copies = count))
                                    copiesExpanded = false
                                }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Color
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Color:", color = textSecondary, modifier = Modifier.width(100.dp))
                ExposedDropdownMenuBox(
                    expanded = colorExpanded,
                    onExpandedChange = { colorExpanded = it },
                    modifier = Modifier.weight(1f)
                ) {
                    OutlinedTextField(
                        value = if (options.color == "COLOR") "Color" else "Black & White",
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = colorExpanded) },
                        modifier = Modifier.menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = colorExpanded,
                        onDismissRequest = { colorExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Black & White") },
                            onClick = {
                                onOptionsChange(options.copy(color = "BLACK_AND_WHITE"))
                                colorExpanded = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Color") },
                            onClick = {
                                onOptionsChange(options.copy(color = "COLOR"))
                                colorExpanded = false
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Duplex
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Sides:", color = textSecondary, modifier = Modifier.width(100.dp))
                ExposedDropdownMenuBox(
                    expanded = duplexExpanded,
                    onExpandedChange = { duplexExpanded = it },
                    modifier = Modifier.weight(1f)
                ) {
                    OutlinedTextField(
                        value = when (options.duplex) {
                            "ONE_SIDED" -> "One-sided"
                            "TWO_SIDED_LONG_EDGE" -> "Two-sided"
                            else -> "Two-sided"
                        },
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = duplexExpanded) },
                        modifier = Modifier.menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = duplexExpanded,
                        onDismissRequest = { duplexExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("One-sided") },
                            onClick = {
                                onOptionsChange(options.copy(duplex = "ONE_SIDED"))
                                duplexExpanded = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Two-sided") },
                            onClick = {
                                onOptionsChange(options.copy(duplex = "TWO_SIDED_LONG_EDGE"))
                                duplexExpanded = false
                            }
                        )
                    }
                }
            }
        }
    }
}

private fun formatFileSize(bytes: Long): String {
    return when {
        bytes < 1024 -> "$bytes B"
        bytes < 1024 * 1024 -> "${bytes / 1024} KB"
        else -> String.format("%.1f MB", bytes / (1024.0 * 1024.0))
    }
}

