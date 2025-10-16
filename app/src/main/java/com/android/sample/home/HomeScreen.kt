package com.android.sample.home

import androidx.compose.foundation.Image
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.android.sample.R
import kotlinx.coroutines.launch

import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items

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
    onSignOut: () -> Unit = {}
) {
  val ui by viewModel.uiState.collectAsState()
  val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
  val scope = rememberCoroutineScope()

  // Synchronize ViewModel state <-> Drawer component
  LaunchedEffect(ui.isDrawerOpen) {
    if (ui.isDrawerOpen && !drawerState.isOpen) {
      drawerState.open()
    } else if (!ui.isDrawerOpen && drawerState.isOpen) {
      drawerState.close()
    }
  }

  ModalNavigationDrawer(drawerState = drawerState, drawerContent = {}) {
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
                    modifier =
                        Modifier.size(40.dp)
                            .background(Color(0x22FFFFFF), CircleShape)
                            .testTag(HomeTags.TopRightBtn)) {
                      Icon(Icons.Default.MoreVert, contentDescription = "More", tint = Color.White)
                    }

                // Top-right menu (placeholder)
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
                // Action buttons
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.padding(horizontal = 16.dp)) {
                      ActionButton(
                          label = "Find CS220 past exams in Drive EPFL",
                          modifier = Modifier.weight(1f).height(50.dp).testTag(HomeTags.Action1Btn),
                          onClick = onAction1Click)
                      ActionButton(
                          label = "Check Ed Discussion",
                          modifier = Modifier.weight(1f).height(50.dp).testTag(HomeTags.Action2Btn),
                          onClick = onAction2Click)
                    }

                Spacer(Modifier.height(16.dp))

                // Message field connected to ViewModel
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
                                imageVector = Icons.Filled.Send,
                                contentDescription = "Send",
                                tint = Color.Gray)
                          }
                    },
                    shape = RoundedCornerShape(50),
                    colors =
                        OutlinedTextFieldDefaults.colors(
                            // text
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            disabledTextColor = Color.LightGray,
                            cursorColor = Color.White,

                            // placeholder
                            focusedPlaceholderColor = Color.Gray,
                            unfocusedPlaceholderColor = Color.Gray,

                            // borders + background
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
          // Central content (visual placeholder)
          Box(
              modifier = Modifier.fillMaxSize().padding(padding).background(Color.Black),
              contentAlignment = Alignment.Center)
          {
              LazyColumn(
                  modifier = Modifier.fillMaxSize().padding(16.dp)
              ) {
                  items(ui.recent) { item ->
                      Text(
                          text = item.title,
                          color = Color.White,
                          fontSize = 14.sp,
                          modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
                      )
                  }
              }
              if (ui.isLoading) {
                  CircularProgressIndicator(
                      modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 96.dp),
                      color = Color.Gray
                  )
              }
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

/* ----- Placeholders for external components (drawer + top-right panel) ----- */

@Composable
private fun TopRightPanelPlaceholder(onDismiss: () -> Unit) {
  DropdownMenuItem(text = { Text("Example item 1") }, onClick = onDismiss)
  DropdownMenuItem(text = { Text("Example item 2") }, onClick = onDismiss)
}

@Preview(showBackground = true, backgroundColor = 0x000000)
@Composable
private fun HomeScreenPreview() {
  MaterialTheme { HomeScreen() }
}
