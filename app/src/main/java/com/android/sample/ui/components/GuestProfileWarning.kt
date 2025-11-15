package com.android.sample.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun GuestProfileWarningModal(onContinueAsGuest: () -> Unit, onLogin: () -> Unit) {
  Box(
      modifier =
          Modifier.fillMaxSize()
              .background(Color.Black.copy(alpha = 0.7f))
              // Disable ripple/interaction to avoid Espresso AppNotIdleException
              .clickable(
                  indication = null, interactionSource = remember { MutableInteractionSource() }) {
                    onContinueAsGuest()
                  },
      contentAlignment = Alignment.Center) {
        Card(
            modifier = Modifier.wrapContentWidth().width(280.dp).padding(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E)),
            shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp)) {
              Column(
                  modifier = Modifier.padding(24.dp),
                  horizontalAlignment = Alignment.CenterHorizontally,
                  verticalArrangement = Arrangement.spacedBy(18.dp)) {
                    Text(
                        text = "Profile unavailable",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center)
                    Text(
                        text = "Sign in with Microsoft Entra ID to access your profile settings.",
                        color = Color.Gray,
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center)
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.padding(top = 4.dp)) {
                          Button(
                              onClick = onContinueAsGuest,
                              modifier = Modifier.weight(1f),
                              colors =
                                  ButtonDefaults.buttonColors(containerColor = Color(0xFF2A2A2A)),
                              shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
                              contentPadding =
                                  androidx.compose.foundation.layout.PaddingValues(
                                      horizontal = 16.dp, vertical = 12.dp)) {
                                Text(
                                    "Continue as guest",
                                    color = Color.White,
                                    fontWeight = FontWeight.Medium,
                                    textAlign = TextAlign.Center)
                              }
                          Button(
                              onClick = onLogin,
                              modifier = Modifier.weight(1f),
                              colors =
                                  ButtonDefaults.buttonColors(containerColor = Color(0xFFE53935)),
                              shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
                              contentPadding =
                                  androidx.compose.foundation.layout.PaddingValues(
                                      horizontal = 16.dp, vertical = 12.dp)) {
                                Text(
                                    "Log in now", color = Color.White, fontWeight = FontWeight.Bold)
                              }
                        }
                  }
            }
      }
}
