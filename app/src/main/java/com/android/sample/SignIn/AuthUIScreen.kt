package com.android.sample.authentification

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowForward
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.android.sample.R

/** Tags de test */
object AuthTags {
  const val Root = "auth_root"
  const val Card = "auth_card"
  const val LogosRow = "auth_logos_row"
  const val LogoEpfl = "auth_logo_epfl"
  const val LogoPoint = "auth_logo_point"
  const val LogoEuler = "auth_logo_euler"
  const val Title = "auth_title"
  const val Subtitle = "auth_subtitle"
  const val OrSeparator = "auth_or_separator"
  const val BtnMicrosoft = "auth_btn_ms"
  const val BtnSwitchEdu = "auth_btn_switch"
  const val MsProgress = "auth_ms_progress"
  const val SwitchProgress = "auth_switch_progress"
  const val TermsText = "auth_terms_text"
  const val ByEpflText = "auth_by_epfl_text"
}

@Composable
fun AuthUIScreen(
    state: AuthUiState,
    onMicrosoftLogin: () -> Unit,
    onSwitchEduLogin: () -> Unit,
    modifier: Modifier = Modifier
) {
  Box(modifier = modifier.fillMaxSize().background(Color(0xFF000000)).testTag(AuthTags.Root)) {
    AnimatedVisibility(
        visible = true,
        enter =
            fadeIn(animationSpec = tween(800)) +
                slideInVertically(
                    initialOffsetY = { it / 8 },
                    animationSpec = tween(800, easing = FastOutSlowInEasing))) {
          Box(modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp).testTag(AuthTags.Card)) {

                // Central content pinned to true center
               Column(
                  modifier = Modifier.align(Alignment.Center).fillMaxWidth(),
                  horizontalAlignment = Alignment.CenterHorizontally) {
                  // Title split on two lines
                   val screenHeight = LocalConfiguration.current.screenHeightDp.dp
                   val titleUpOffset = (25).dp
                   Text(
                       text = "Ask anything,\ndo everything",
                      style =
                          MaterialTheme.typography.headlineLarge.copy(
                              fontFamily = FontFamily.Serif,
                              fontWeight = FontWeight.Normal,
                               fontSize = 40.sp,
                               lineHeight = 44.sp,
                              color = Color.White),
                      textAlign = TextAlign.Center,
                       modifier = Modifier.fillMaxWidth().offset(y = -titleUpOffset).testTag(AuthTags.Title))

                  Spacer(modifier = Modifier.height(12.dp))

                      // Subtitle
                      Text(
                          text = "Welcome to EULER",
                          style =
                              MaterialTheme.typography.bodyLarge.copy(
                                  fontFamily = FontFamily.SansSerif,
                                  fontWeight = FontWeight.Normal,
                                  fontSize = 16.sp,
                                  color = Color(0xFF9CA3AF)),
                          textAlign = TextAlign.Center,
                          modifier = Modifier.fillMaxWidth().testTag(AuthTags.Subtitle))

                      Spacer(modifier = Modifier.height(40.dp))

                      // Buttons section
                      val isMicrosoftLoading =
                          state is AuthUiState.Loading && state.provider == AuthProvider.MICROSOFT
                      val isGuestLoading =
                          state is AuthUiState.Loading && state.provider == AuthProvider.SWITCH_EDU

                      // Microsoft Entra ID button
                      MicrosoftEntraButton(
                          enabled = !isMicrosoftLoading,
                          isLoading = isMicrosoftLoading,
                          onClick = onMicrosoftLogin,
                          modifier = Modifier.testTag(AuthTags.BtnMicrosoft))

                      Spacer(modifier = Modifier.height(16.dp))

                      // OR separator
                      OrSeparator()

                      Spacer(modifier = Modifier.height(16.dp))

                      // Guest button
                      GuestButton(
                          enabled = !isGuestLoading,
                          isLoading = isGuestLoading,
                          onClick = onSwitchEduLogin,
                          modifier = Modifier.testTag(AuthTags.BtnSwitchEdu))

                      // Terms just below the guest button
                      Spacer(modifier = Modifier.height(12.dp))
                      PrivacyPolicyText(modifier = Modifier.testTag(AuthTags.TermsText))
                      Spacer(modifier = Modifier.height(8.dp))
                    }

                // Footer pinned to bottom-center: terms above BY EPFL
                Column(
                    modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally) {
                  Text(
                      text = "BY EPFL",
                      style =
                          MaterialTheme.typography.bodySmall.copy(
                              fontSize = 12.sp, color = Color(0xFF9CA3AF)),
                      modifier = Modifier.testTag(AuthTags.ByEpflText))
                }
              }
        }
  }
}

@Composable
private fun LogosRow() {
  Row(
      modifier = Modifier.fillMaxWidth().testTag(AuthTags.LogosRow),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.Start) {
        Image(
            painter = painterResource(id = R.drawable.epfl_logo),
            contentDescription = "EPFL Logo",
            modifier = Modifier.size(48.dp).testTag(AuthTags.LogoEpfl).offset(y =2.dp),
            contentScale = ContentScale.Fit)

        Spacer(modifier = Modifier.width(12.dp))
        Box(
            modifier =
                Modifier.width(1.dp)
                    .height(14.dp)
                    .background(Color(0xFF9CA3AF))
                    .testTag(AuthTags.LogoPoint))
        Spacer(modifier = Modifier.width(12.dp))

        Image(
            painter = painterResource(id = R.drawable.euler_logo),
            contentDescription = "Euler Logo",
            modifier =
                Modifier.size(32.dp).testTag(AuthTags.LogoEuler),
            contentScale = ContentScale.Fit)
      }
}

@Composable
private fun MicrosoftEntraButton(
    enabled: Boolean,
    isLoading: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
  val interaction = remember { MutableInteractionSource() }
  val pressed by interaction.collectIsPressedAsState()
  val scale by
      animateFloatAsState(
          targetValue = if (pressed) 0.98f else 1.0f, animationSpec = tween(120), label = "scale")

  Surface(
      color = Color.White,
      shape = RoundedCornerShape(12.dp),
      modifier = modifier.fillMaxWidth().scale(scale).clip(RoundedCornerShape(12.dp))) {
        TextButton(
            onClick = onClick,
            enabled = enabled,
            interactionSource = interaction,
            modifier = Modifier.fillMaxWidth()) {
              Row(
                  modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.Center) {
                    // Microsoft logo
                    Image(
                        painter = painterResource(id = R.drawable.microsoft_logo),
                        contentDescription = "Microsoft Logo",
                        modifier = Modifier.size(24.dp))

                    Spacer(modifier = Modifier.width(8.dp))

                    Text(
                        text = "Continue with Microsoft Entra ID",
                        color = Color.Black,
                        style =
                            MaterialTheme.typography.bodyLarge.copy(
                                fontFamily = FontFamily.SansSerif,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold))

                    if (isLoading) {
                      Spacer(modifier = Modifier.width(12.dp))
                      CircularProgressIndicator(
                          modifier = Modifier.size(18.dp).testTag(AuthTags.MsProgress),
                          color = Color.Black,
                          strokeWidth = 2.dp)
                    }
                  }
            }
      }
}

@Composable
private fun OrSeparator() {
  Row(
      modifier = Modifier.fillMaxWidth().testTag(AuthTags.OrSeparator),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.Center) {
        // Left line
        Box(modifier = Modifier.width(48.dp).height(1.dp).background(Color(0xFF9CA3AF)))
        Spacer(modifier = Modifier.width(16.dp))
        // OR text
        Text(
            text = "OR",
            style =
                MaterialTheme.typography.bodyMedium.copy(
                    fontSize = 14.sp, color = Color(0xFF9CA3AF), fontFamily = FontFamily.SansSerif))
        Spacer(modifier = Modifier.width(16.dp))
        // Right line
        Box(modifier = Modifier.width(48.dp).height(1.dp).background(Color(0xFF9CA3AF)))
      }
}

@Composable
private fun GuestButton(
    enabled: Boolean,
    isLoading: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
  val interaction = remember { MutableInteractionSource() }
  val pressed by interaction.collectIsPressedAsState()
  val scale by
      animateFloatAsState(
          targetValue = if (pressed) 0.98f else 1.0f, animationSpec = tween(120), label = "scale2")

  val epflRed = Color(0xFFFF0000)

  Surface(
      color = epflRed,
      shape = RoundedCornerShape(12.dp),
      modifier = modifier.fillMaxWidth().scale(scale).clip(RoundedCornerShape(12.dp))) {
        TextButton(
            onClick = onClick,
            enabled = enabled,
            interactionSource = interaction,
            modifier = Modifier.fillMaxWidth()) {
              Row(
                  modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.Center) {
                    Text(
                        text = "Continue as a guest",
                        color = Color.White,
                        style =
                            MaterialTheme.typography.bodyLarge.copy(
                                fontFamily = FontFamily.SansSerif,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold))

                    if (isLoading) {
                      Spacer(modifier = Modifier.width(12.dp))
                      CircularProgressIndicator(
                          modifier = Modifier.size(18.dp).testTag(AuthTags.SwitchProgress),
                          color = Color.White,
                          strokeWidth = 2.dp)
                    } else {
                      Spacer(modifier = Modifier.width(8.dp))
                      Icon(
                          imageVector = Icons.Outlined.ArrowForward,
                          contentDescription = "Continue",
                          tint = Color.White,
                          modifier = Modifier.size(20.dp))
                    }
                  }
            }
      }
}

@Composable
private fun PrivacyPolicyText(modifier: Modifier = Modifier) {
  val annotatedText = buildAnnotatedString {
    withStyle(
        style =
            SpanStyle(
                color = Color(0xFF9CA3AF), fontFamily = FontFamily.SansSerif, fontSize = 11.sp)) {
          append("By continuing, you acknowledge EPFL's ")
        }
    pushStringAnnotation(
        tag = "privacy_policy", annotation = "https://www.epfl.ch/about/overview/regulations-and-guidelines/epfl-privacy-policy/")
    withStyle(
        style =
            SpanStyle(
                color = Color(0xFF9CA3AF),
                fontFamily = FontFamily.SansSerif,
                fontSize = 11.sp,
                textDecoration = TextDecoration.Underline)) {
          append("Privacy Policy")
        }
    pop()
  }

  Text(
      text = annotatedText,
      modifier = modifier.clickable { /* Handle privacy policy click */},
      style = MaterialTheme.typography.bodySmall)
}
