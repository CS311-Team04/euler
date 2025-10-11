package ch.epfl.euler.auth

import android.app.Activity
import android.widget.Toast
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.lifecycleScope
import ch.epfl.euler.data.UserRepository
import ch.epfl.euler.data.ensureProfile
import ch.epfl.euler.data.models
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.launch

@Composable
fun MicrosoftSignInScreen() {
    val ctx = LocalContext.current
    val owner = LocalLifecycleOwner.current
    val repo = remember { UserRepository() }

    Button(onClick = {
        val act = ctx as? Activity
        if (act == null) {
            Toast.makeText(ctx, "Activity non disponible", Toast.LENGTH_SHORT).show()
            return@Button
        }

        signInWithMicrosoft(
            activity = act,
            onSuccess = {
                owner.lifecycleScope.launch {
                    try {
                        ensureProfile(repo, Firebase.auth)
                        repo.upsertSettings(models.UserSettings(language = "fr"))
                        Toast.makeText(ctx, "Connecté et profil créé", Toast.LENGTH_SHORT).show()
                    } catch (e: Exception) {
                        Toast.makeText(ctx, "Post-login: ${e.message}", Toast.LENGTH_LONG).show()
                    }
                }
            },
            onError = { e ->
                Toast.makeText(ctx, "Erreur Microsoft: ${e.message}", Toast.LENGTH_LONG).show()
            }
        )
    }) {
        Text("Se connecter avec Microsoft")
    }
}
