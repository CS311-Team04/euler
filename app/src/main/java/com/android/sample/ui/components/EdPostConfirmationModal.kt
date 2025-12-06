package com.android.sample.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import com.android.sample.ui.theme.EdPostBorderSecondary
import com.android.sample.ui.theme.EdPostDimensions
import com.android.sample.ui.theme.EdPostIconSecondary
import com.android.sample.ui.theme.EdPostTextFieldContainer
import com.android.sample.ui.theme.EdPostTextPrimary
import com.android.sample.ui.theme.EdPostTransparent
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
  val textSecondary = colorScheme.onSurfaceVariant
  val gradient = Brush.horizontalGradient(listOf(ed1, ed2))

  Column(
      modifier =
          modifier
              .fillMaxWidth()
              .padding(
                  horizontal = EdPostDimensions.ContainerHorizontalPadding,
                  vertical = EdPostDimensions.ContainerVerticalPadding),
      verticalArrangement = Arrangement.spacedBy(EdPostDimensions.ContainerVerticalSpacing)) {

        // Gradient frame
        EdPostGradientFrame(gradient = gradient) {
          Column(
              modifier = Modifier.fillMaxWidth().padding(EdPostDimensions.ContentHorizontalPadding),
              verticalArrangement = Arrangement.spacedBy(EdPostDimensions.ContentVerticalSpacing)) {
                // Title capsule row
                OutlinedTextField(
                    value = editedTitle,
                    onValueChange = { editedTitle = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = {
                      Text(
                          text = "Titre",
                          color = textSecondary,
                          fontSize = EdPostDimensions.TextFieldPlaceholderFontSize)
                    },
                    textStyle =
                        MaterialTheme.typography.titleLarge.copy(
                            fontSize = EdPostDimensions.TextFieldTitleFontSize,
                            fontWeight = FontWeight.Bold,
                            color = EdPostTextPrimary),
                    colors =
                        OutlinedTextFieldDefaults.colors(
                            focusedTextColor = EdPostTextPrimary,
                            unfocusedTextColor = EdPostTextPrimary,
                            cursorColor = EdPostTextPrimary,
                            focusedBorderColor = EdPostTransparent,
                            unfocusedBorderColor = EdPostTransparent,
                            focusedContainerColor = EdPostTextFieldContainer,
                            unfocusedContainerColor = EdPostTextFieldContainer),
                    shape = RoundedCornerShape(EdPostDimensions.TextFieldTitleCornerRadius),
                    singleLine = true,
                    trailingIcon = {
                      Icon(
                          imageVector = Icons.Outlined.Edit,
                          contentDescription = "Edit title",
                          tint = EdPostIconSecondary,
                          modifier = Modifier.size(EdPostDimensions.IconEditSize))
                    })

                // Body text field (multiline)
                OutlinedTextField(
                    value = editedBody,
                    onValueChange = { editedBody = it },
                    modifier =
                        Modifier.fillMaxWidth()
                            .heightIn(
                                min = EdPostDimensions.TextFieldBodyMinHeight,
                                max = EdPostDimensions.TextFieldBodyMaxHeight),
                    placeholder = {
                      Text(
                          text = "Votre question pour ED…",
                          color = textSecondary,
                          fontSize = EdPostDimensions.TextFieldPlaceholderFontSize)
                    },
                    textStyle =
                        MaterialTheme.typography.bodyLarge.copy(
                            fontSize = EdPostDimensions.TextFieldBodyFontSize,
                            color = EdPostTextPrimary,
                            lineHeight = EdPostDimensions.TextFieldBodyLineHeight),
                    colors =
                        OutlinedTextFieldDefaults.colors(
                            focusedTextColor = EdPostTextPrimary,
                            unfocusedTextColor = EdPostTextPrimary,
                            cursorColor = EdPostTextPrimary,
                            focusedBorderColor = EdPostTransparent,
                            unfocusedBorderColor = EdPostTransparent,
                            focusedContainerColor = EdPostTextFieldContainer,
                            unfocusedContainerColor = EdPostTextFieldContainer),
                    shape = RoundedCornerShape(EdPostDimensions.TextFieldBodyCornerRadius),
                    maxLines = EdPostDimensions.TextFieldBodyMaxLines,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Default))

                // Action buttons row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(EdPostDimensions.ButtonSpacing)) {
                      // Cancel button (outline)
                      OutlinedButton(
                          onClick = onCancel,
                          modifier = Modifier.weight(1f),
                          colors =
                              ButtonDefaults.outlinedButtonColors(contentColor = EdPostTextPrimary),
                          shape = RoundedCornerShape(EdPostDimensions.ButtonCancelCornerRadius),
                          border =
                              BorderStroke(
                                  EdPostDimensions.ButtonBorderWidth, EdPostBorderSecondary)) {
                            Text(
                                text = "Cancel",
                                fontSize = EdPostDimensions.ButtonTextFontSize,
                                fontWeight = FontWeight.Medium)
                          }

                      // Post button (gradient purple)
                      Button(
                          onClick = { onPublish(editedTitle, editedBody) },
                          modifier = Modifier.weight(1f),
                          colors = ButtonDefaults.buttonColors(containerColor = EdPostTransparent),
                          contentPadding = PaddingValues(),
                          shape = RoundedCornerShape(EdPostDimensions.ButtonPostCornerRadius)) {
                            Box(
                                modifier =
                                    Modifier.background(
                                            brush = gradient,
                                            shape =
                                                RoundedCornerShape(
                                                    EdPostDimensions.ButtonGradientCornerRadius))
                                        .fillMaxWidth()
                                        .padding(vertical = EdPostDimensions.ButtonVerticalPadding),
                                contentAlignment = Alignment.Center) {
                                  Row(
                                      horizontalArrangement = Arrangement.Center,
                                      verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = "Post",
                                            fontSize = EdPostDimensions.ButtonTextFontSize,
                                            fontWeight = FontWeight.Bold,
                                            color = EdPostTextPrimary)
                                        Spacer(
                                            modifier =
                                                Modifier.width(
                                                    EdPostDimensions.ButtonIconSpacerWidth))
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Rounded.Send,
                                            contentDescription = null,
                                            modifier = Modifier.size(EdPostDimensions.IconSendSize),
                                            tint = EdPostTextPrimary)
                                      }
                                }
                          }
                    }
              }
        }
      }
}
