package com.android.sample.VoiceChat

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import kotlinx.coroutines.delay as coroutinesDelay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

@Composable
fun VoiceScreen(onClose: () -> Unit, modifier: Modifier = Modifier) {
  val context = androidx.compose.ui.platform.LocalContext.current
  val alreadyGranted =
      ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
          PackageManager.PERMISSION_GRANTED

  var hasMic by remember { mutableStateOf(alreadyGranted) }

  // Runtime permission
  val launcher =
      rememberLauncherForActivityResult(
          contract = ActivityResultContracts.RequestPermission(),
          onResult = { granted ->
            hasMic = granted
            android.util.Log.d("VoiceScreen", "Permission RECORD_AUDIO: $granted")
            if (!granted) {
              android.util.Log.w("VoiceScreen", "Microphone permission was denied by user")
            }
          })

  LaunchedEffect(Unit) {
    if (!alreadyGranted) {
      android.util.Log.d("VoiceScreen", "Requesting RECORD_AUDIO permission...")
      launcher.launch(Manifest.permission.RECORD_AUDIO)
    } else {
      android.util.Log.d("VoiceScreen", "RECORD_AUDIO permission already granted")
    }
  }

  // État pour contrôler si le microphone est actif
  var isMicActive by remember { mutableStateOf(false) }
  val mic = remember { AndroidMicLevelSource() }
  val mockSource = remember { MockLevelSource(0f) }

  // Détection d'inactivité vocale pour désactiver automatiquement
  var lastVoiceTime by remember { mutableStateOf(System.currentTimeMillis()) }
  val silenceThreshold = 0.05f // Seuil pour considérer qu'il y a du silence
  val silenceDuration = 2500L // 2.5 secondes de silence avant désactivation

  // Surveillance du niveau audio pour détecter l'inactivité
  var currentLevel by remember { mutableStateOf(0f) }

  // Contrôle du microphone (start/stop)
  LaunchedEffect(isMicActive, hasMic) {
    if (isMicActive && hasMic) {
      try {
        android.util.Log.d("VoiceScreen", "Démarrage du microphone...")
        mic.start()
        lastVoiceTime = System.currentTimeMillis()
        // Petit délai pour laisser le micro s'initialiser
        coroutinesDelay(100)
        android.util.Log.d("VoiceScreen", "Microphone démarré avec succès")
      } catch (e: Exception) {
        android.util.Log.e("VoiceScreen", "Erreur démarrage micro", e)
        e.printStackTrace()
        isMicActive = false
      }
    } else {
      try {
        android.util.Log.d("VoiceScreen", "Arrêt du microphone...")
        mic.stop()
        android.util.Log.d("VoiceScreen", "Microphone arrêté")
      } catch (e: Exception) {
        android.util.Log.e("VoiceScreen", "Erreur arrêt micro", e)
      }
      currentLevel = 0f
    }
  }

  // Nettoyage quand on quitte l'écran
  DisposableEffect(Unit) {
    onDispose {
      if (isMicActive) {
        mic.stop()
        android.util.Log.d("VoiceScreen", "Microphone arrêté lors de la fermeture")
      }
    }
  }

  // Collecter les niveaux audio quand le micro est actif + détection de silence
  LaunchedEffect(isMicActive, hasMic) {
    if (isMicActive && hasMic) {
      try {
        android.util.Log.d("VoiceScreen", "Début collecte des niveaux audio")
        var frameCount = 0
        mic.levels.collect { level ->
          currentLevel = level
          frameCount++

          // Log périodique pour débugger
          if (frameCount % 30 == 0) { // Toutes les ~0.5 secondes
            android.util.Log.d(
                "VoiceScreen",
                "Niveau audio: ${String.format("%.3f", level)} (seuil silence: $silenceThreshold)")
          }

          // Si le niveau est au-dessus du seuil, on considère qu'il y a de la voix
          if (level > silenceThreshold) {
            lastVoiceTime = System.currentTimeMillis()
            if (frameCount % 10 == 0) { // Log moins souvent quand on détecte la voix
              android.util.Log.d(
                  "VoiceScreen", "Voix détectée! Niveau: ${String.format("%.3f", level)}")
            }
          }
        }
      } catch (e: Exception) {
        android.util.Log.e("VoiceScreen", "Error collecting audio levels", e)
        currentLevel = 0f
      }
    } else {
      currentLevel = 0f
    }
  }

  // Vérifier périodiquement si on doit désactiver après silence
  LaunchedEffect(isMicActive) {
    if (isMicActive && hasMic) {
      while (isMicActive) {
        coroutinesDelay(500) // Vérifier toutes les 500ms
        val timeSinceLastVoice = System.currentTimeMillis() - lastVoiceTime
        if (timeSinceLastVoice > silenceDuration && currentLevel <= silenceThreshold) {
          android.util.Log.d(
              "VoiceScreen",
              "Silence détecté (${timeSinceLastVoice}ms), désactivation automatique du micro")
          isMicActive = false
          break
        }
      }
    }
  }

  Box(modifier = modifier.fillMaxSize().background(Color.Black)) {

    // Visualiseur au centre - utilise le vrai micro si actif, sinon mock silencieux
    // On utilise ManagedLevelSource pour empêcher VoiceVisualizer de démarrer/arrêter
    // automatiquement
    VoiceVisualizer(
        levelSource =
            remember(isMicActive, hasMic) {
              if (isMicActive && hasMic) {
                ManagedLevelSource(mic)
              } else {
                mockSource
              }
            },
        preset = VisualPreset.Bloom, // Visualiseur Bloom
        color = Color(0x961A03),
        petals = 4,
        size = 1500.dp,
        modifier = Modifier.align(Alignment.Center).offset(y = -10.dp))

    // Boutons bas
    Column(
        modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally) {
          Row(
              horizontalArrangement = Arrangement.spacedBy(50.dp),
              verticalAlignment = Alignment.CenterVertically) {
                RoundIconButton(
                    onClick = {
                      if (hasMic) {
                        isMicActive = !isMicActive
                        android.util.Log.d(
                            "VoiceScreen",
                            "Microphone ${if (isMicActive) "activé" else "désactivé"}")
                        if (isMicActive) {
                          lastVoiceTime = System.currentTimeMillis()
                        }
                      }
                    },
                    iconVector = Icons.Filled.Mic,
                    size = 100.dp,
                    background =
                        if (isMicActive) Color(0x33FF0000) else Color(0x0), // Indicateur visuel
                    iconTint = if (isMicActive) Color(0xFFFF0000) else Color.White)
                RoundIconButton(
                    onClick = onClose,
                    iconVector = Icons.Default.Close,
                    size = 100.dp,
                    background = Color(0x0),
                    iconTint = Color.White)
              }
          Spacer(Modifier.height(14.dp))

          // Indicateur de niveau audio pour débugger
          if (isMicActive && hasMic) {
            Text(
                text =
                    "Niveau: ${String.format("%.2f", currentLevel)} ${if (currentLevel > silenceThreshold) "✓ Voix" else "Silence"}",
                color =
                    if (currentLevel > silenceThreshold) Color(0xFF2CD510) else Color(0xFF9A9A9A),
                fontSize = 11.sp,
                modifier = Modifier.alpha(0.8f))
            Spacer(Modifier.height(8.dp))
          }

          Text(
              text = "Powered by APERTUS Swiss LLM",
              color = Color(0xFF9A9A9A),
              fontSize = 12.sp,
              modifier = Modifier.alpha(0.9f))
        }
  }
}

@Composable
private fun RoundIconButton(
    onClick: () -> Unit,
    iconVector: ImageVector,
    size: Dp,
    background: Color,
    iconTint: Color
) {
  Surface(
      color = background,
      shape = CircleShape,
      tonalElevation = 0.dp,
      shadowElevation = 0.dp,
      modifier = Modifier.size(size).clickable { onClick() }) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
          Icon(
              imageVector = iconVector,
              contentDescription = null,
              tint = iconTint,
              modifier = Modifier.size(size * 0.5f))
        }
      }
}

// Mock LevelSource pour les previews
private class MockLevelSource(private val level: Float) : LevelSource {
  override val levels = flow {
    while (true) {
      emit(level.coerceIn(0f, 1f))
      kotlinx.coroutines.delay(16)
    }
  }

  override fun start() {}

  override fun stop() {}
}

// Wrapper pour empêcher VoiceVisualizer de démarrer automatiquement le micro
// Mais on passe quand même le flux pour que VoiceVisualizer puisse collecter les niveaux
private class ManagedLevelSource(private val delegate: LevelSource) : LevelSource {
  override val levels: Flow<Float> = delegate.levels

  override fun start() {
    // Ne pas démarrer, car c'est géré dans VoiceScreen
    // Le micro est déjà démarré dans VoiceScreen
    android.util.Log.d("ManagedLevelSource", "start() appelé mais ignoré (géré par VoiceScreen)")
  }

  override fun stop() {
    // Ne pas arrêter, car c'est géré dans VoiceScreen
    android.util.Log.d("ManagedLevelSource", "stop() appelé mais ignoré (géré par VoiceScreen)")
  }
}

@Preview(showBackground = true, backgroundColor = 0x0)
@Composable
private fun VoiceChatScreenPreview() {
  MaterialTheme { VoiceScreen(onClose = {}) }
}
