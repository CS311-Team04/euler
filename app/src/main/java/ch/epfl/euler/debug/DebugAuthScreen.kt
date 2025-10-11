package ch.epfl.euler.debug

import android.widget.Toast
import android.util.Log
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.lifecycleScope
import ch.epfl.euler.data.ensureProfile
import com.google.firebase.ktx.Firebase
import com.google.firebase.auth.ktx.auth
import kotlinx.coroutines.launch

// <-- importe tes classes du data layer
import ch.epfl.euler.data.models
import ch.epfl.euler.data.UserRepository

@Composable
fun DebugCreateUserAndProfileButton() {
    val ctx = LocalContext.current
    val owner = LocalLifecycleOwner.current
    val repo = remember { UserRepository() }

    Button(onClick = {
        val auth = Firebase.auth
        auth.createUserWithEmailAndPassword("test+1@euler.app", "Passw0rd!")
            .addOnSuccessListener {
                owner.lifecycleScope.launch {
                    try {
                        ensureProfile(repo, auth)
                        repo.upsertSettings(models.UserSettings(language = "fr", theme = "dark"))
                        Toast.makeText(ctx, "Profil + settings créés", Toast.LENGTH_SHORT).show()
                    } catch (e: Exception) {
                        Log.e("Firestore", "ensureProfile/upsertSettings failed", e)
                        Toast.makeText(ctx, "Firestore fail: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .addOnFailureListener { e ->
                Log.e("Auth", "createUser failed", e)
                Toast.makeText(ctx, "Auth fail: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }) { Text("Créer user + profil (émulateur)") }
}
