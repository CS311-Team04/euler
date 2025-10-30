package com.android.sample.home

import androidx.compose.animation.*
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.rounded.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.android.sample.R
import kotlinx.coroutines.launch

object HomeTags {
  const val Root = "home_root"
  const val MenuBtn = "home_menu_btn"
  const val TopRightBtn = "home_topright_btn"
  const val Action1Btn = "home_action1_btn"
  const val Action2Btn = "home_action2_btn"
  const val MessageField = "home_message_field"
  const val SendBtn = "home_send_btn"
  const val Drawer = "home_drawer"
  const val TopRightMenu = "home_topright_menu"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = viewModel(),
    onAction1Click: () -> Unit = {},
    onAction2Click: () -> Unit = {},
    onSendMessage: (String) -> Unit = {},
    onSignOut: () -> Unit = {},
    onSettingsClick: () -> Unit = {},
    openDrawerOnStart: Boolean = false
) {
  val ui by viewModel.uiState.collectAsState()
  val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
  val scope = rememberCoroutineScope()

  // Synchronise l'état ViewModel <-> composant Drawer
  LaunchedEffect(ui.isDrawerOpen) {
    if (ui.isDrawerOpen && !drawerState.isOpen) {
      drawerState.open()
    } else if (!ui.isDrawerOpen && drawerState.isOpen) {
      drawerState.close()
    }
  }

  // Open drawer when returning from settings
  LaunchedEffect(openDrawerOnStart) {
    if (openDrawerOnStart) {
      drawerState.open()
      viewModel.toggleDrawer()
    }
  }

  ModalNavigationDrawer(
      drawerState = drawerState,
      drawerContent = {
        DrawerContent(
            ui = ui,
            onToggleSystem = { id -> viewModel.toggleSystemConnection(id) },
            onSignOut = {
              // TODO: brancher ton sign-out réel
              // Ferme le drawer visuellement + sync VM
              scope.launch { drawerState.close() }
              if (ui.isDrawerOpen) viewModel.toggleDrawer()
              onSignOut()
            },
            onSettingsClick = {
              scope.launch { drawerState.close() }
              if (ui.isDrawerOpen) viewModel.toggleDrawer()
              onSettingsClick()
            },
            onClose = {
              scope.launch { drawerState.close() }
              if (ui.isDrawerOpen) viewModel.toggleDrawer()
            })
      }) {
        Scaffold(
            modifier = modifier.fillMaxSize().background(Color.Black).testTag(HomeTags.Root),
            containerColor = Color.Black,
            topBar = {
              CenterAlignedTopAppBar(
                  navigationIcon = {
                    IconButton(
                        onClick = {
                          viewModel.toggleDrawer()
                          scope.launch {
                            if (!drawerState.isOpen) drawerState.open() else drawerState.close()
                          }
                        },
                        modifier = Modifier.size(48.dp).testTag(HomeTags.MenuBtn)) {
                          Icon(
                              Icons.Default.Menu,
                              contentDescription = "Menu",
                              tint = Color.White,
                              modifier = Modifier.size(24.dp))
                        }
                  },
                  title = {
                    Image(
                        painter = painterResource(R.drawable.euler_logo),
                        contentDescription = "Euler",
                        modifier = Modifier.height(100.dp),
                        contentScale = ContentScale.Fit)
                  },
                  actions = {
                    IconButton(
                        onClick = { viewModel.setTopRightOpen(true) },
                        modifier = Modifier.size(48.dp).testTag(HomeTags.TopRightBtn)) {
                          Icon(
                              Icons.Default.MoreVert,
                              contentDescription = "More",
                              tint = Color.White,
                              modifier = Modifier.size(24.dp))
                        }

                    // Menu haut-droite (placeholder)
                    DropdownMenu(
                        expanded = ui.isTopRightOpen,
                        onDismissRequest = { viewModel.setTopRightOpen(false) },
                        modifier = Modifier.testTag(HomeTags.TopRightMenu)) {
                          TopRightPanelPlaceholder(
                              onDismiss = { viewModel.setTopRightOpen(false) },
                              onDeleteClick = { viewModel.showDeleteConfirmation() })
                        }
                  },
                  colors =
                      TopAppBarDefaults.topAppBarColors(
                          containerColor = Color.Black,
                          titleContentColor = Color.White,
                          navigationIconContentColor = Color.White,
                          actionIconContentColor = Color.White))
            },
            bottomBar = {
              Column(
                  Modifier.fillMaxWidth().background(Color.Black).padding(bottom = 16.dp),
                  horizontalAlignment = Alignment.CenterHorizontally) {
                    // Boutons d'action
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.padding(horizontal = 16.dp)) {
                          ActionButton(
                              label = "Find CS220 past exams",
                              modifier =
                                  Modifier.weight(1f).height(50.dp).testTag(HomeTags.Action1Btn),
                              onClick = onAction1Click)
                          ActionButton(
                              label = "Check Ed Discussion",
                              modifier =
                                  Modifier.weight(1f).height(50.dp).testTag(HomeTags.Action2Btn),
                              onClick = onAction2Click)
                        }

                    Spacer(Modifier.height(16.dp))

                    // Champ de message branché au ViewModel
                    OutlinedTextField(
                        value = ui.messageDraft,
                        onValueChange = { viewModel.updateMessageDraft(it) },
                        placeholder = { Text("Message EULER", color = Color.Gray) },
                        modifier =
                            Modifier.fillMaxWidth()
                                .padding(horizontal = 16.dp)
                                .height(60.dp)
                                .testTag(HomeTags.MessageField),
                        enabled = !ui.isSending,
                        trailingIcon = {
                          val canSend = ui.messageDraft.isNotBlank() && !ui.isSending
                          BubbleSendButton(
                              enabled = canSend,
                              isSending = ui.isSending,
                              onClick = {
                                if (canSend) {
                                  onSendMessage(ui.messageDraft)
                                  viewModel.sendMessage()
                                }
                              })
                        },
                        shape = RoundedCornerShape(50),
                        colors =
                            OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                disabledTextColor = Color.LightGray,
                                cursorColor = Color.White,
                                focusedPlaceholderColor = Color.Gray,
                                unfocusedPlaceholderColor = Color.Gray,
                                focusedBorderColor = Color.DarkGray,
                                unfocusedBorderColor = Color.DarkGray,
                                focusedContainerColor = Color(0xFF121212),
                                unfocusedContainerColor = Color(0xFF121212),
                                disabledContainerColor = Color(0xFF121212)))

                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "Powered by APERTUS Swiss LLM · MCP-enabled for 6 EPFL systems",
                        color = Color.Gray,
                        fontSize = 11.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 16.dp))
                  }
            }) { padding ->
              // Contenu central (placeholder visuel)
              Box(
                  modifier = Modifier.fillMaxSize().padding(padding).background(Color.Black),
                  contentAlignment = Alignment.Center) {
                    // Ici tu pourras afficher un dashboard, une timeline, etc.
                    LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                      if (ui.isSending) {
                        item {
                          ThinkingIndicator(
                              modifier =
                                  Modifier.fillMaxWidth()
                                      .padding(vertical = 8.dp)
                                      .testTag("home_thinking_indicator"))
                        }
                      }
                      items(ui.recent) { item ->
                        Text(
                            text = item.title,
                            color = Color.White,
                            fontSize = 14.sp,
                            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp))
                      }
                    }
                  }
            }
      }

  // Delete Confirmation Modal
  AnimatedVisibility(
      visible = ui.showDeleteConfirmation,
      enter = fadeIn(tween(200)),
      exit = fadeOut(tween(200)),
      modifier = Modifier.fillMaxSize()) {
        DeleteConfirmationModal(
            onConfirm = {
              viewModel.clearChat()
              viewModel.hideDeleteConfirmation()
            },
            onCancel = { viewModel.hideDeleteConfirmation() })
      }
}

@Composable
private fun ActionButton(label: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
  Button(
      onClick = onClick,
      shape = RoundedCornerShape(50),
      colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E1E1E)),
      modifier = modifier) {
        Text(label, color = Color.White, textAlign = TextAlign.Center)
      }
}

/* ----- Placeholders pour compo externes (drawer + panneau top-right) ----- */

@Composable
private fun TopRightPanelPlaceholder(onDismiss: () -> Unit, onDeleteClick: () -> Unit) {
  DropdownMenuItem(text = { Text("Share") }, onClick = onDismiss)
  DropdownMenuItem(
      text = { Text("Delete") },
      onClick = {
        onDeleteClick()
        onDismiss()
      })
}

@Composable
private fun DeleteConfirmationModal(onConfirm: () -> Unit, onCancel: () -> Unit) {
  Box(
      modifier =
          Modifier.fillMaxSize()
              .background(Color.Black.copy(alpha = 0.7f))
              .clickable(onClick = onCancel),
      contentAlignment = Alignment.Center) {
        Card(
            modifier = Modifier.width(280.dp).padding(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E)),
            shape = RoundedCornerShape(16.dp)) {
              Column(
                  modifier = Modifier.padding(24.dp),
                  horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Clear Chat?",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold)

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "This will delete all messages. This action cannot be undone.",
                        color = Color.Gray,
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center)

                    Spacer(modifier = Modifier.height(24.dp))

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxWidth()) {
                          Button(
                              onClick = onCancel,
                              modifier = Modifier.weight(1f),
                              colors =
                                  ButtonDefaults.buttonColors(containerColor = Color(0xFF2A2A2A)),
                              shape = RoundedCornerShape(8.dp)) {
                                Text("Cancel", color = Color.White)
                              }

                          Button(
                              onClick = onConfirm,
                              modifier = Modifier.weight(1f),
                              colors = ButtonDefaults.buttonColors(containerColor = Color.Red),
                              shape = RoundedCornerShape(8.dp)) {
                                Text("Delete", color = Color.White, fontWeight = FontWeight.Bold)
                              }
                        }
                  }
            }
      }
}

@Composable
private fun ThinkingIndicator(modifier: Modifier = Modifier) {
  var dots by remember { mutableStateOf(0) }
  LaunchedEffect(Unit) {
    while (true) {
      kotlinx.coroutines.delay(450)
      dots = (dots + 1) % 4
    }
  }
  val text = "Euler is thinking" + ".".repeat(dots)
  Surface(
      modifier = modifier,
      shape = RoundedCornerShape(12.dp),
      color = Color(0x14FFFFFF),
      tonalElevation = 0.dp) {
        Row(
            Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically) {
              CircularProgressIndicator(
                  strokeWidth = 2.dp, modifier = Modifier.size(16.dp), color = Color.Gray)
              Spacer(Modifier.width(8.dp))
              Text(text = text, color = Color.LightGray, fontSize = 13.sp)
            }
      }
}

@Composable
private fun BubbleSendButton(
    enabled: Boolean,
    isSending: Boolean,
    onClick: () -> Unit,
) {
  val targetSize =
      when {
        isSending -> 40.dp
        enabled -> 42.dp
        else -> 40.dp
      }
  val size by animateDpAsState(targetValue = targetSize, label = "bubble-size")

  // Colors: bright red when enabled, deeper red while sending, neutral gray when disabled
  val targetContainer =
      when {
        isSending -> Color(0xFFC62828) // deeper red
        enabled -> Color(0xFFE53935) // bright red
        else -> Color(0xFF3C3C3C) // gray
      }
  val container by animateColorAsState(targetValue = targetContainer, label = "bubble-color")

  val borderColor =
      when {
        enabled || isSending -> Color(0x33FFFFFF) // subtle white ring for separation
        else -> Color(0x22000000)
      }
  val elevation by
      animateDpAsState(targetValue = if (enabled) 8.dp else 0.dp, label = "bubble-elev")

  val interaction = remember { MutableInteractionSource() }

  Surface(
      modifier = Modifier.size(size).padding(end = 6.dp).testTag(HomeTags.SendBtn),
      color = container,
      shape = CircleShape,
      tonalElevation = 0.dp,
      shadowElevation = elevation,
  ) {
    Box(
        modifier =
            Modifier.fillMaxSize()
                .padding(6.dp)
                .testTag(HomeTags.SendBtn)
                .then(
                    if (enabled && !isSending)
                        Modifier.clickable(interactionSource = interaction, indication = null) {
                          onClick()
                        }
                    else Modifier),
        contentAlignment = Alignment.Center) {
          if (isSending) {
            CircularProgressIndicator(
                strokeWidth = 2.dp, modifier = Modifier.size(20.dp), color = Color.White)
          } else {
            val icon =
                try {
                  androidx.compose.material.icons.Icons.Rounded.Send
                } catch (_: Throwable) {
                  androidx.compose.material.icons.Icons.Default.Send
                }
            Icon(
                imageVector = icon,
                contentDescription = "Send",
                tint = Color.White,
                modifier = Modifier.size(22.dp) // larger arrow for visibility
                )
          }
        }
  }
}

@Preview(showBackground = true, backgroundColor = 0x000000)
@Composable
private fun HomeScreenPreview() {
  MaterialTheme { HomeScreen() }
}
