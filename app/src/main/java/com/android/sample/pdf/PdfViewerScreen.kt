package com.android.sample.pdf

import android.annotation.SuppressLint
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.viewinterop.AndroidView
import androidx.navigation.NavController

/**
 * PDF Viewer Screen using Google Docs viewer. Displays a PDF in a full-screen view with a close
 * button.
 */
@SuppressLint("SetJavaScriptEnabled")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PdfViewerScreen(pdfUrl: String, filename: String, navController: NavController) {
  Scaffold(
      topBar = {
        TopAppBar(
            title = { Text(text = filename, maxLines = 1, overflow = TextOverflow.Ellipsis) },
            navigationIcon = {
              IconButton(
                  onClick = { navController.popBackStack() },
                  modifier = Modifier.testTag("pdf_viewer_close_button")) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = "Close",
                        tint = MaterialTheme.colorScheme.onSurface)
                  }
            },
            colors =
                TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface))
      }) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
          // WebView for PDF display using Google Docs viewer
          AndroidView(
              factory = { context ->
                android.webkit.WebView(context).apply {
                  settings.javaScriptEnabled = true
                  settings.loadWithOverviewMode = true
                  settings.useWideViewPort = true
                  settings.builtInZoomControls = true
                  settings.displayZoomControls = false
                  settings.domStorageEnabled = true
                  settings.allowFileAccess = true
                  settings.allowContentAccess = true

                  // Use Google Docs viewer for better PDF rendering
                  val encodedUrl = android.net.Uri.encode(pdfUrl, "")
                  val viewerUrl = "https://docs.google.com/viewer?url=$encodedUrl&embedded=true"
                  loadUrl(viewerUrl)
                }
              },
              modifier = Modifier.fillMaxSize().testTag("pdf_viewer_webview"))
        }
      }
}
