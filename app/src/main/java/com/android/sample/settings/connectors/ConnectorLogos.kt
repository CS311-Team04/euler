package com.android.sample.settings.connectors

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.android.sample.R
import com.android.sample.ui.theme.ConnectorsLightSurface
import com.android.sample.ui.theme.DarkSurface
import com.android.sample.ui.theme.EulerGrayDark
import com.android.sample.ui.theme.EulerNewChatCircleRed
import com.android.sample.ui.theme.LightBackground
import com.android.sample.ui.theme.MoodleGray1
import com.android.sample.ui.theme.MoodleGray2
import com.android.sample.ui.theme.MoodleOrange
import com.android.sample.ui.theme.MoodleYellow
import com.android.sample.ui.theme.ed1
import com.android.sample.ui.theme.ed2
import com.android.sample.ui.theme.isAcademiaR
import com.android.sample.ui.theme.textConnectors
import com.android.sample.ui.theme.textConnectorsLight

/**
 * Connector logos - créés directement en Compose selon le design fourni Gris si non connecté,
 * coloré si connecté
 */
@Composable
fun ConnectorLogo(connectorId: String, isConnected: Boolean = true) {
  when (connectorId) {
    "moodle" -> MoodleLogo(isConnected = isConnected)
    "ed" -> EdLogo(isConnected = isConnected)
    "epfl_campus" -> EPFLLogo(isConnected = isConnected)
    "is_academia" -> ISAcademiaLogo(isConnected = isConnected)
    else -> Box(modifier = Modifier.size(40.dp).background(textConnectors))
  }
}

@Composable
fun MoodleLogo(isConnected: Boolean) {
  val backgroundColor =
      if (isConnected) {
        Brush.verticalGradient(
            colors =
                listOf(
                    MoodleOrange, // Orange
                    MoodleYellow // Yellow
                    ))
      } else {
        Brush.verticalGradient(colors = listOf(MoodleGray1, MoodleGray2))
      }

  Box(
      modifier = Modifier.size(40.dp).background(backgroundColor, RoundedCornerShape(8.dp)),
      contentAlignment = Alignment.Center) {
        Box(modifier = Modifier.padding(horizontal = 8.dp), contentAlignment = Alignment.Center) {
          // Graduation cap on top of "m"
          Column(
              horizontalAlignment = Alignment.CenterHorizontally,
              verticalArrangement = Arrangement.Center) {
                // Graduation cap
                Box(
                    modifier =
                        Modifier.width(14.dp)
                            .height(6.dp)
                            .background(EulerGrayDark, RoundedCornerShape(2.dp))
                            .offset(y = (-3).dp),
                    contentAlignment = Alignment.TopCenter) {
                      // Tassel
                      Box(
                          modifier =
                              Modifier.width(1.5.dp)
                                  .height(8.dp)
                                  .background(EulerGrayDark)
                                  .offset(x = 0.dp, y = 6.dp))
                    }
                // Letter "m"
                Text(
                    text = "m",
                    color = LightBackground,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.offset(y = (-6).dp))
              }
        }
      }
}

@Composable
fun EdLogo(isConnected: Boolean) {
  val backgroundColor =
      if (isConnected) {
        Brush.horizontalGradient(colors = listOf(ed1, ed2))
      } else {
        Brush.horizontalGradient(
            colors =
                listOf(
                    MoodleGray1, // Gris
                    MoodleGray2 // Gris clair
                    ))
      }

  Box(
      modifier = Modifier.size(40.dp).background(backgroundColor, RoundedCornerShape(8.dp)),
      contentAlignment = Alignment.Center) {
        Text(
            text = "ed",
            color = LightBackground,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = (-3).sp) // Letters overlap slightly
  }
}

@Composable
fun EPFLLogo(isConnected: Boolean) {
  // Utiliser epfl_logo.png si connecté, epfl_gray.png si non connecté
  val logoResId =
      try {
        if (isConnected) {
          R.drawable.epfl_logo
        } else {
          R.drawable.epfl_gray
        }
      } catch (e: Exception) {
        null
      }

  if (logoResId != null) {
    // Pas de fond blanc, juste l'image
    Image(
        painter = painterResource(id = logoResId),
        contentDescription = null,
        modifier = Modifier.size(40.dp),
        contentScale = ContentScale.Fit)
  } else {

    Text(
        text = "EPFL",
        color = if (isConnected) EulerNewChatCircleRed else MoodleGray1,
        fontSize = 16.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 2.sp)
  }
}

@Composable
fun ISAcademiaLogo(isConnected: Boolean) {
  val isDark = isSystemInDarkTheme()
  val isColor = if (isConnected) isAcademiaR else MoodleGray1
  val academiaColor =
      if (isConnected) {
        if (isDark) DarkSurface else ConnectorsLightSurface
      } else {
        MoodleGray2
      }
  val underlineColor = if (isConnected) textConnectorsLight else MoodleGray1 // to check
  val surfaceColor = if (isDark) DarkSurface else ConnectorsLightSurface

  Box(
      modifier = Modifier.size(40.dp, 28.dp).background(surfaceColor, RoundedCornerShape(8.dp)),
      contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(horizontal = 6.dp)) {
              // "IS academia" text
              Row(
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.Center) {
                    Text(
                        text = "IS",
                        color = isColor,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold)
                    Text(
                        text = "academia",
                        color = academiaColor,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold)
                  }
              // Underline
              Box(
                  modifier =
                      Modifier.width(35.dp)
                          .height(1.dp)
                          .background(underlineColor)
                          .offset(y = (-1).dp))
            }
      }
}
