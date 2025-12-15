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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import com.android.sample.R
import com.android.sample.home.EdCourse
import com.android.sample.ui.theme.DarkBackground
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
 * @param courses List of available courses to post to
 * @param selectedCourseId The currently selected course ID, or null if none selected
 * @param onPublish Called when user clicks "Post" button with final title, body, courseId, and
 *   isAnonymous
 * @param onCancel Called when user clicks "Cancel" or dismisses the modal
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EdPostConfirmationModal(
    modifier: Modifier = Modifier,
    title: String,
    body: String,
    courses: List<EdCourse> = emptyList(),
    selectedCourseId: Long? = null,
    isLoading: Boolean = false,
    isLoadingCourses: Boolean = false,
    onPublish: (String, String, Long?, Boolean) -> Unit,
    onCancel: () -> Unit
) {
  var editedTitle by remember(title) { mutableStateOf(title.replace("\\n", "\n")) }
  var editedBody by remember(body) { mutableStateOf(body.replace("\\n", "\n")) }
  var currentSelectedCourseId by
      remember(selectedCourseId) { mutableStateOf<Long?>(selectedCourseId) }
  var courseDropdownExpanded by remember { mutableStateOf(false) }
  var isAnonymous by remember { mutableStateOf(false) }

  val colorScheme = MaterialTheme.colorScheme
  val isDark = colorScheme.background == DarkBackground
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
                // Course selection dropdown
                if (courses.isNotEmpty() || isLoadingCourses) {
                  ExposedDropdownMenuBox(
                      expanded = courseDropdownExpanded && !isLoadingCourses,
                      onExpandedChange = {
                        if (!isLoadingCourses) courseDropdownExpanded = !courseDropdownExpanded
                      }) {
                        val selectedCourse = courses.find { it.id == currentSelectedCourseId }
                        val displayText =
                            selectedCourse?.let { "${it.code ?: ""} ${it.name}".trim() }
                                ?: stringResource(R.string.select_course)

                        OutlinedTextField(
                            value = displayText,
                            onValueChange = {},
                            readOnly = true,
                            enabled = !isLoading && !isLoadingCourses,
                            modifier = Modifier.fillMaxWidth().menuAnchor(),
                            placeholder = {
                              Text(
                                  text = stringResource(R.string.select_course),
                                  color = textSecondary,
                                  fontSize = EdPostDimensions.TextFieldPlaceholderFontSize)
                            },
                            textStyle =
                                MaterialTheme.typography.bodyLarge.copy(
                                    fontSize = EdPostDimensions.TextFieldBodyFontSize,
                                    color = EdPostTextPrimary),
                            colors =
                                OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = EdPostTextPrimary,
                                    unfocusedTextColor = EdPostTextPrimary,
                                    focusedBorderColor = EdPostTransparent,
                                    unfocusedBorderColor = EdPostTransparent,
                                    focusedContainerColor = EdPostTextFieldContainer,
                                    unfocusedContainerColor = EdPostTextFieldContainer),
                            shape = RoundedCornerShape(EdPostDimensions.TextFieldBodyCornerRadius),
                            trailingIcon = {
                              if (isLoadingCourses) {
                                CircularProgressIndicator(
                                    color = EdPostIconSecondary,
                                    strokeWidth = EdPostDimensions.IconLoadingSpinnerStrokeWidth,
                                    modifier =
                                        Modifier.size(EdPostDimensions.IconLoadingSpinnerSize))
                              } else {
                                ExposedDropdownMenuDefaults.TrailingIcon(
                                    expanded = courseDropdownExpanded)
                              }
                            })

                        ExposedDropdownMenu(
                            expanded = courseDropdownExpanded,
                            onDismissRequest = { courseDropdownExpanded = false },
                            modifier = Modifier.background(EdPostTextFieldContainer)) {
                              courses.forEach { course ->
                                DropdownMenuItem(
                                    text = {
                                      Text(
                                          text = "${course.code ?: ""} ${course.name}".trim(),
                                          color = EdPostTextPrimary)
                                    },
                                    onClick = {
                                      currentSelectedCourseId = course.id
                                      courseDropdownExpanded = false
                                    })
                              }
                            }
                      }
                }

                // Title capsule row
                OutlinedTextField(
                    value = editedTitle,
                    onValueChange = { editedTitle = it },
                    enabled = !isLoading,
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = {
                      Text(
                          text = stringResource(R.string.ed_post_title_placeholder),
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
                    enabled = !isLoading,
                    modifier =
                        Modifier.fillMaxWidth()
                            .heightIn(
                                min = EdPostDimensions.TextFieldBodyMinHeight,
                                max = EdPostDimensions.TextFieldBodyMaxHeight),
                    placeholder = {
                      Text(
                          text = stringResource(R.string.ed_post_body_placeholder),
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

                // Anonymous post toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween) {
                      Text(
                          text = stringResource(R.string.post_anonymously),
                          style =
                              MaterialTheme.typography.bodyMedium.copy(
                                  fontSize = EdPostDimensions.TextFieldBodyFontSize,
                                  color = EdPostTextPrimary))
                      Switch(
                          checked = isAnonymous,
                          onCheckedChange = { isAnonymous = it },
                          enabled = !isLoading,
                          colors =
                              SwitchDefaults.colors(
                                  checkedThumbColor = ed1,
                                  checkedTrackColor = ed1.copy(alpha = 0.5f),
                                  uncheckedThumbColor =
                                      if (isDark) colorScheme.outline
                                      else colorScheme.outlineVariant,
                                  uncheckedTrackColor =
                                      if (isDark) colorScheme.surfaceVariant
                                      else colorScheme.surfaceVariant.copy(alpha = 0.6f)))
                    }

                // Action buttons row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(EdPostDimensions.ButtonSpacing)) {
                      // Cancel button (outline)
                      OutlinedButton(
                          onClick = onCancel,
                          enabled = !isLoading,
                          modifier = Modifier.weight(1f),
                          colors =
                              ButtonDefaults.outlinedButtonColors(contentColor = EdPostTextPrimary),
                          shape = RoundedCornerShape(EdPostDimensions.ButtonCancelCornerRadius),
                          border =
                              BorderStroke(
                                  EdPostDimensions.ButtonBorderWidth, EdPostBorderSecondary)) {
                            Text(
                                text = stringResource(R.string.ed_post_cancel_button),
                                fontSize = EdPostDimensions.ButtonTextFontSize,
                                fontWeight = FontWeight.Medium)
                          }

                      // Post button (gradient purple)
                      // Disable button if no course is selected
                      val canPost = !isLoading && currentSelectedCourseId != null
                      Button(
                          onClick = {
                            onPublish(editedTitle, editedBody, currentSelectedCourseId, isAnonymous)
                          },
                          enabled = canPost,
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
                                            text = stringResource(R.string.ed_post_post_button),
                                            fontSize = EdPostDimensions.ButtonTextFontSize,
                                            fontWeight = FontWeight.Bold,
                                            color = EdPostTextPrimary)
                                        Spacer(
                                            modifier =
                                                Modifier.width(
                                                    EdPostDimensions.ButtonIconSpacerWidth))
                                        if (isLoading) {
                                          CircularProgressIndicator(
                                              color = EdPostTextPrimary,
                                              strokeWidth = EdPostDimensions.ButtonBorderWidth,
                                              modifier =
                                                  Modifier.size(EdPostDimensions.IconSendSize))
                                        } else {
                                          Icon(
                                              imageVector = Icons.AutoMirrored.Rounded.Send,
                                              contentDescription = null,
                                              modifier =
                                                  Modifier.size(EdPostDimensions.IconSendSize),
                                              tint = EdPostTextPrimary)
                                        }
                                      }
                                }
                          }
                    }
              }
        }
      }
}
