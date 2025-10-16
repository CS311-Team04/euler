package com.android.sample.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
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
                        modifier =
                            Modifier.size(40.dp)
                                .background(Color(0x22FFFFFF), CircleShape)
                                .testTag(HomeTags.MenuBtn)) {
                          Icon(Icons.Default.Menu, contentDescription = "Menu", tint = Color.White)
                        }
                  },
                  title = {},
                  actions = {
                    IconButton(
                        onClick = { viewModel.setTopRightOpen(true) },
                        modifier =
                            Modifier.size(40.dp)
                                .background(Color(0x22FFFFFF), CircleShape)
                                .testTag(HomeTags.TopRightBtn)) {
                          Icon(
                              Icons.Default.MoreVert,
                              contentDescription = "More",
                              tint = Color.White)
                        }

                    // Menu haut-droite (placeholder)
                    DropdownMenu(
                        expanded = ui.isTopRightOpen,
                        onDismissRequest = { viewModel.setTopRightOpen(false) },
                        modifier = Modifier.testTag(HomeTags.TopRightMenu)) {
                          TopRightPanelPlaceholder(onDismiss = { viewModel.setTopRightOpen(false) })
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
                              label = "Find CS220 past exams in Drive EPFL",
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
                                .height(56.dp)
                                .testTag(HomeTags.MessageField),
                        trailingIcon = {
                          IconButton(
                              onClick = {
                                onSendMessage(ui.messageDraft)
                                viewModel.sendMessage()
                              },
                              modifier = Modifier.testTag(HomeTags.SendBtn)) {
                                Icon(
                                    Icons.Default.Send,
                                    contentDescription = "Send",
                                    tint = Color.Gray)
                              }
                        },
                        shape = RoundedCornerShape(50),
                        colors =
                            OutlinedTextFieldDefaults.colors(
                                // texte
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                disabledTextColor = Color.LightGray,
                                cursorColor = Color.White,

                                // placeholder
                                focusedPlaceholderColor = Color.Gray,
                                unfocusedPlaceholderColor = Color.Gray,

                                // bordures + fond
                                focusedBorderColor = Color.DarkGray,
                                unfocusedBorderColor = Color.DarkGray,
                                focusedContainerColor = Color(0xFF121212),
                                unfocusedContainerColor = Color(0xFF121212),
                            ))

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
                  }
            }
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
private fun TopRightPanelPlaceholder(onDismiss: () -> Unit) {
  DropdownMenuItem(text = { Text("Share") }, onClick = onDismiss)
  DropdownMenuItem(text = { Text("Delete") }, onClick = onDismiss)
}

@Preview(showBackground = true, backgroundColor = 0x000000)
@Composable
private fun HomeScreenPreview() {
  MaterialTheme { HomeScreen() }
}
