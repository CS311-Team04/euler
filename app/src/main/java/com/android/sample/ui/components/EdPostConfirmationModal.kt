package com.android.sample.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.rounded.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.android.sample.ui.theme.ed1
import com.android.sample.ui.theme.ed2

/**
 * Modal confirmation dialog for ED Discussion posts.
 *
 * Displays a preview of the post with editable title and body fields. User can review and edit
 * before publishing or cancel.
 *
 * @param title The initial title for the post (editable)
 * @param body The initial body text for the post (editable)
 * @param onPublish Called when user clicks "Post" button with final title and body
 * @param onCancel Called when user clicks "Cancel" or dismisses the modal
 */
@Composable
fun EdPostConfirmationModal(
    modifier: Modifier = Modifier,
    title: String,
    body: String,
    onPublish: (String, String) -> Unit,
    onCancel: () -> Unit
) {
  var editedTitle by remember(title) { mutableStateOf(title.replace("\\n", "\n")) }
  var editedBody by remember(body) { mutableStateOf(body.replace("\\n", "\n")) }

  val colorScheme = MaterialTheme.colorScheme
  val textPrimary = colorScheme.onSurface
  val textSecondary = colorScheme.onSurfaceVariant
  val gradient = Brush.horizontalGradient(listOf(ed1, ed2))
  val pillBg = Color(0xFF1C1C1E)

  Column(
      modifier = modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp),
      verticalArrangement = Arrangement.spacedBy(10.dp)) {

        // Gradient frame
        Box(
            modifier =
                Modifier.fillMaxWidth()
                    .border(BorderStroke(2.dp, gradient), RoundedCornerShape(18.dp))
                    .background(Color(0xFF0F0F0F), RoundedCornerShape(18.dp))) {
              // Main card container
              Card(
                  modifier =
                      Modifier.fillMaxWidth()
                          .padding(2.dp) // keep gradient visible
                          .background(Color(0xFF0F0F0F), RoundedCornerShape(16.dp)),
                  colors = CardDefaults.cardColors(containerColor = Color(0xFF0F0F0F)),
                  shape = RoundedCornerShape(16.dp),
                  elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)) {
                          // Title capsule row
                          OutlinedTextField(
                              value = editedTitle,
                              onValueChange = { editedTitle = it },
                              modifier = Modifier.fillMaxWidth(),
                              placeholder = {
                                Text(text = "Titre", color = textSecondary, fontSize = 15.sp)
                              },
                              textStyle =
                                  MaterialTheme.typography.titleLarge.copy(
                                      fontSize = 18.sp,
                                      fontWeight = FontWeight.Bold,
                                      color = Color.White),
                              colors =
                                  OutlinedTextFieldDefaults.colors(
                                      focusedTextColor = Color.White,
                                      unfocusedTextColor = Color.White,
                                      cursorColor = Color.White,
                                      focusedBorderColor = Color.Transparent,
                                      unfocusedBorderColor = Color.Transparent,
                                      focusedContainerColor = Color(0xFF16161A),
                                      unfocusedContainerColor = Color(0xFF16161A)),
                              shape = RoundedCornerShape(12.dp),
                              singleLine = true,
                              trailingIcon = {
                                Icon(
                                    imageVector = Icons.Outlined.Edit,
                                    contentDescription = "Edit title",
                                    tint = Color.White.copy(alpha = 0.7f),
                                    modifier = Modifier.size(18.dp))
                              })

                          // Body text field (multiline)
                          OutlinedTextField(
                              value = editedBody,
                              onValueChange = { editedBody = it },
                              modifier =
                                  Modifier.fillMaxWidth().heightIn(min = 200.dp, max = 360.dp),
                              placeholder = {
                                Text(
                                    text = "Votre question pour ED…",
                                    color = textSecondary,
                                    fontSize = 15.sp)
                              },
                              textStyle =
                                  MaterialTheme.typography.bodyLarge.copy(
                                      fontSize = 16.sp, color = Color.White, lineHeight = 23.sp),
                              colors =
                                  OutlinedTextFieldDefaults.colors(
                                      focusedTextColor = Color.White,
                                      unfocusedTextColor = Color.White,
                                      cursorColor = Color.White,
                                      focusedBorderColor = Color.Transparent,
                                      unfocusedBorderColor = Color.Transparent,
                                      focusedContainerColor = Color(0xFF16161A),
                                      unfocusedContainerColor = Color(0xFF16161A)),
                              shape = RoundedCornerShape(14.dp),
                              maxLines = 14,
                              keyboardOptions = KeyboardOptions(imeAction = ImeAction.Default))

                          // Action buttons row
                          Row(
                              modifier = Modifier.fillMaxWidth(),
                              horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                // Cancel button (outline)
                                OutlinedButton(
                                    onClick = onCancel,
                                    modifier = Modifier.weight(1f),
                                    colors =
                                        ButtonDefaults.outlinedButtonColors(
                                            contentColor = Color.White),
                                    shape = RoundedCornerShape(10.dp),
                                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.4f))) {
                                      Text(
                                          text = "Cancel",
                                          fontSize = 14.sp,
                                          fontWeight = FontWeight.Medium)
                                    }

                                // Post button (gradient purple)
                                Button(
                                    onClick = { onPublish(editedTitle, editedBody) },
                                    modifier = Modifier.weight(1f),
                                    colors =
                                        ButtonDefaults.buttonColors(
                                            containerColor = Color.Transparent),
                                    contentPadding = PaddingValues(),
                                    shape = RoundedCornerShape(12.dp)) {
                                      Box(
                                          modifier =
                                              Modifier.background(
                                                      brush = gradient,
                                                      shape = RoundedCornerShape(12.dp))
                                                  .fillMaxWidth()
                                                  .padding(vertical = 12.dp),
                                          contentAlignment = Alignment.Center) {
                                            Row(
                                                horizontalArrangement = Arrangement.Center,
                                                verticalAlignment = Alignment.CenterVertically) {
                                                  Text(
                                                      text = "Post",
                                                      fontSize = 14.sp,
                                                      fontWeight = FontWeight.Bold,
                                                      color = Color.White)
                                                  Spacer(modifier = Modifier.width(10.dp))
                                                  Icon(
                                                      imageVector = Icons.Rounded.Send,
                                                      contentDescription = null,
                                                      modifier = Modifier.size(18.dp),
                                                      tint = Color.White)
                                                }
                                          }
                                    }
                              }
                        }
                  }
            }
      }
}
