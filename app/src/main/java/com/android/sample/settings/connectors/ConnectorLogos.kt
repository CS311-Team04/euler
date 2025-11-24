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
import com.android.sample.R
import com.android.sample.settings.connectors.ConnectorsDimensions as Dimens
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
    else -> Box(modifier = Modifier.size(Dimens.LogoSize).background(textConnectors))
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
      modifier =
          Modifier.size(Dimens.LogoSize)
              .background(backgroundColor, RoundedCornerShape(Dimens.LogoCornerRadius)),
      contentAlignment = Alignment.Center) {
        Box(
            modifier = Modifier.padding(horizontal = Dimens.LogoPadding),
            contentAlignment = Alignment.Center) {
              // Graduation cap on top of "m"
              Column(
                  horizontalAlignment = Alignment.CenterHorizontally,
                  verticalArrangement = Arrangement.Center) {
                    // Graduation cap
                    Box(
                        modifier =
                            Modifier.width(Dimens.MoodleGraduationCapWidth)
                                .height(Dimens.MoodleGraduationCapHeight)
                                .background(
                                    EulerGrayDark,
                                    RoundedCornerShape(Dimens.MoodleGraduationCapCornerRadius))
                                .offset(y = Dimens.MoodleGraduationCapOffsetY),
                        contentAlignment = Alignment.TopCenter) {
                          // Tassel
                          Box(
                              modifier =
                                  Modifier.width(Dimens.MoodleTasselWidth)
                                      .height(Dimens.MoodleTasselHeight)
                                      .background(EulerGrayDark)
                                      .offset(
                                          x = Dimens.MoodleTasselOffsetX,
                                          y = Dimens.MoodleTasselOffsetY))
                        }
                    // Letter "m"
                    Text(
                        text = "m",
                        color = LightBackground,
                        fontSize = Dimens.MoodleTextFontSize,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.offset(y = Dimens.MoodleTextOffsetY))
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
      modifier =
          Modifier.size(Dimens.LogoSize)
              .background(backgroundColor, RoundedCornerShape(Dimens.LogoCornerRadius)),
      contentAlignment = Alignment.Center) {
        Text(
            text = "ed",
            color = LightBackground,
            fontSize = Dimens.EdTextFontSize,
            fontWeight = FontWeight.Bold,
            letterSpacing = Dimens.EdTextLetterSpacing) // Letters overlap slightly
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
        modifier = Modifier.size(Dimens.LogoSize),
        contentScale = ContentScale.Fit)
  } else {

    Text(
        text = "EPFL",
        color = if (isConnected) EulerNewChatCircleRed else MoodleGray1,
        fontSize = Dimens.EPFLTextFontSize,
        fontWeight = FontWeight.Bold,
        letterSpacing = Dimens.EPFLTextLetterSpacing)
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
      modifier =
          Modifier.size(Dimens.LogoSize, Dimens.ISAcademiaHeight)
              .background(surfaceColor, RoundedCornerShape(Dimens.LogoCornerRadius)),
      contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(horizontal = Dimens.ISAcademiaPadding)) {
              // "IS academia" text
              Row(
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.Center) {
                    Text(
                        text = "IS",
                        color = isColor,
                        fontSize = Dimens.ISAcademiaTextFontSize,
                        fontWeight = FontWeight.Bold)
                    Text(
                        text = "academia",
                        color = academiaColor,
                        fontSize = Dimens.ISAcademiaTextFontSize,
                        fontWeight = FontWeight.Bold)
                  }
              // Underline
              Box(
                  modifier =
                      Modifier.width(Dimens.ISAcademiaUnderlineWidth)
                          .height(Dimens.ISAcademiaUnderlineHeight)
                          .background(underlineColor)
                          .offset(y = Dimens.ISAcademiaUnderlineOffsetY))
            }
      }
}
