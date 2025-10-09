package ch.epfl.euler

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChatBubble
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.*
import ch.epfl.euler.resources.C
import ch.epfl.euler.theme.SampleAppTheme
import ch.epfl.euler.auth.MicrosoftSignInScreen
import com.google.firebase.auth.ktx.BuildConfig
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
// ⚠️ Supprimer: import com.google.firebase.BuildConfig

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { SampleAppTheme { AuthGate() } } // <-- au lieu de AppScaffold()
    }
}

/**
 * Affiche l'écran de connexion Microsoft si non connecté,
 * sinon affiche ton app (tabs Home/Chat/Settings).
 */
@Composable
private fun AuthGate() {
    val user = Firebase.auth.currentUser
    if (user == null) {
        MicrosoftSignInScreen()            // <-- fichier auth/SignInScreen.kt
    } else {
        AppScaffold()
    }
}

@Composable
private fun AppScaffold() {
    val nav = rememberNavController()
    val tabs = listOf(
        TabSpec(C.Route.home, "Home", Icons.Outlined.Home, C.Tag.tabHome),
        TabSpec(C.Route.chat, "Chat", Icons.Outlined.ChatBubble, C.Tag.tabChat),
        TabSpec(C.Route.settings, "Settings", Icons.Outlined.Settings, C.Tag.tabSettings),
    )

    Scaffold(
        bottomBar = {
            NavigationBar(Modifier.testTag(C.Tag.navBar)) {
                val backStack by nav.currentBackStackEntryAsState()
                val current = backStack?.destination?.route
                tabs.forEach { t ->
                    NavigationBarItem(
                        selected = current == t.route,
                        onClick = {
                            nav.navigate(t.route) {
                                popUpTo(nav.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true; restoreState = true
                            }
                        },
                        icon = { Icon(t.icon, contentDescription = t.label) },
                        label = { Text(t.label) },
                        modifier = Modifier.testTag(t.testTag)
                    )
                }
            }
        }
    ) { padding ->
        NavHost(
            navController = nav,
            startDestination = C.Route.home,
            modifier = Modifier.padding(padding)
        ) {
            if (BuildConfig.DEBUG) {
                composable("debug-auth") { ch.epfl.euler.debug.DebugCreateUserAndProfileButton() }
            }
            composable(C.Route.home) { HomeScreen() }
            composable(C.Route.chat) { ChatScreen() }
            composable(C.Route.settings) { SettingsScreen() }
        }
    }
}

private data class TabSpec(
    val route: String,
    val label: String,
    val icon: ImageVector,
    val testTag: String
)
