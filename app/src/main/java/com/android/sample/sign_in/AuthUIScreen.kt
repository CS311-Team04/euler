package com.android.sample.authentification

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.material.icons.outlined.AccountBox
import androidx.compose.material.icons.outlined.ArrowForward
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.android.sample.R

/** Tags de test */
object AuthTags {
    const val Root = "auth_root"
    const val Card = "auth_card"
    const val LogosRow = "auth_logos_row"
    const val LogoEpfl = "auth_logo_epfl"
    const val LogoPoint = "auth_logo_point"
    const val LogoEuler = "auth_logo_euler"
    const val BtnMicrosoft = "auth_btn_ms"
    const val BtnSwitchEdu = "auth_btn_switch"
    const val MsProgress = "auth_ms_progress"
    const val SwitchProgress = "auth_switch_progress"
    const val TermsText = "auth_terms_text"
}

@Composable
fun AuthUIScreen(
    state: AuthUiState,
    onMicrosoftLogin: () -> Unit,
    onSwitchEduLogin: () -> Unit,
    modifier: Modifier = Modifier
) {
    val backgroundColor = Color(0xFFC63F3F)

    Box(
        modifier = modifier.fillMaxSize().background(backgroundColor).testTag(AuthTags.Root),
        contentAlignment = Alignment.Center
    ) {
        Accents()

        AnimatedVisibility(
            visible = true,
            enter =
                fadeIn(animationSpec = tween(800)) +
                    slideInVertically(
                        initialOffsetY = { it / 8 },
                        animationSpec = tween(800, easing = FastOutSlowInEasing)
                    )
        ) {
            Surface(
                modifier =
                    Modifier.padding(horizontal = 16.dp).fillMaxWidth().testTag(AuthTags.Card),
                tonalElevation = 8.dp,
                shadowElevation = 24.dp,
                color = Color.White.copy(alpha = 0.98f),
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(3.dp, Color(0x994B5563))
            ) {
                Column(
                    modifier =
                        Modifier.padding(horizontal = 24.dp, vertical = 28.dp).fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    LogosRow()

                    Spacer(modifier = Modifier.height(8.dp))
                    Spacer(modifier = Modifier.height(4.dp))

                    val isMicrosoftLoading =
                        state is AuthUiState.Loading && state.provider == AuthProvider.MICROSOFT
                    val isSwitchLoading =
                        state is AuthUiState.Loading && state.provider == AuthProvider.SWITCH_EDU

                    AuthPrimaryButton(
                        text = "Sign in with Microsoft Entra ID",
                        background = Color(0xFFB51F1F),
                        icon = {
                            Icon(
                                imageVector = Icons.Outlined.AccountBox,
                                contentDescription = "Microsoft Icon",
                                tint = Color.White
                            )
                        },
                        trailing = {
                            if (isMicrosoftLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp).testTag(AuthTags.MsProgress),
                                    color = Color.White,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Outlined.ArrowForward,
                                    contentDescription = "Continue",
                                    tint = Color.White.copy(alpha = 0.95f)
                                )
                            }
                        },
                        enabled = !isMicrosoftLoading && state !is AuthUiState.Loading,
                        onClick = onMicrosoftLogin,
                        modifier = Modifier.testTag(AuthTags.BtnMicrosoft)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    AuthSecondaryButton(
                        text = "Sign in with Switch edu ID",
                        leading = {
                            Icon(
                                imageVector = Icons.Outlined.AccountBox,
                                contentDescription = "Switch Icon",
                                tint = Color(0xFF374151)
                            )
                        },
                        trailing = {
                            if (isSwitchLoading) {
                                CircularProgressIndicator(
                                    modifier =
                                        Modifier.size(18.dp).testTag(AuthTags.SwitchProgress),
                                    color = Color(0xFF374151),
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Outlined.ArrowForward,
                                    contentDescription = "Continue",
                                    tint = Color(0xFF6B7280)
                                )
                            }
                        },
                        enabled = !isSwitchLoading && state !is AuthUiState.Loading,
                        onClick = onSwitchEduLogin,
                        modifier = Modifier.testTag(AuthTags.BtnSwitchEdu)
                    )

                    Spacer(modifier = Modifier.height(18.dp))

                    Text(
                        text =
                            "By signing in, you agree to our Terms of Service and Privacy Policy",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF6B7280),
                        modifier = Modifier.testTag(AuthTags.TermsText)
                    )
                }
            }
        }
    }
}

@Composable
private fun LogosRow() {
    Column(
        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp).testTag(AuthTags.LogosRow),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Box {
                Image(
                    painter = painterResource(id = R.drawable.epfl_logo),
                    contentDescription = "EPFL Logo",
                    modifier = Modifier.size(72.dp).testTag(AuthTags.LogoEpfl),
                    contentScale = ContentScale.Fit
                )
            }
            Image(
                painter = painterResource(id = R.drawable.point),
                contentDescription = "Separator Dot",
                modifier = Modifier.size(60.dp).offset(x = (20).dp).testTag(AuthTags.LogoPoint),
                contentScale = ContentScale.Fit
            )
            Image(
                painter = painterResource(id = R.drawable.euler_logo),
                contentDescription = "Euler Logo",
                modifier = Modifier.size(120.dp).offset(x = (-10).dp).testTag(AuthTags.LogoEuler),
                contentScale = ContentScale.Fit
            )
        }
    }
}

@Composable
private fun Accents() {
    val transition = rememberInfiniteTransition(label = "accent")
    val alpha1 by
        transition.animateFloat(
            initialValue = 0.025f,
            targetValue = 0.04f,
            animationSpec =
                infiniteRepeatable(animation = tween(14000, easing = FastOutSlowInEasing)),
            label = "alpha1"
        )
    val alpha2 by
        transition.animateFloat(
            initialValue = 0.02f,
            targetValue = 0.035f,
            animationSpec =
                infiniteRepeatable(animation = tween(16000, easing = FastOutSlowInEasing)),
            label = "alpha2"
        )
    AccentCircle(size = 96.dp, brushAlpha = alpha1)
    AccentCircle(size = 128.dp, brushAlpha = alpha2)
}

@Composable
private fun AccentCircle(size: Dp, brushAlpha: Float) {
    val brush =
        Brush.radialGradient(
            colors = listOf(Color.White.copy(alpha = brushAlpha), Color.Transparent)
        )
    Box(Modifier.fillMaxSize()) {
        Box(modifier = Modifier.size(size).clip(RoundedCornerShape(100)).background(brush))
    }
}

@Composable
private fun AuthPrimaryButton(
    text: String,
    background: Color,
    icon: @Composable () -> Unit,
    trailing: @Composable () -> Unit,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by
        animateFloatAsState(
            targetValue = if (pressed) 0.98f else 1.0f,
            animationSpec = tween(120),
            label = "scale"
        )
    Surface(
        color = background,
        shape = RoundedCornerShape(14.dp),
        shadowElevation = 8.dp,
        modifier = modifier.fillMaxWidth().scale(scale).clip(RoundedCornerShape(14.dp))
    ) {
        TextButton(
            onClick = onClick,
            enabled = enabled,
            interactionSource = interaction,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    icon()
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(text = text, color = Color.White)
                }
                trailing()
            }
        }
    }
}

@Composable
private fun AuthSecondaryButton(
    text: String,
    leading: @Composable () -> Unit,
    trailing: @Composable () -> Unit,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by
        animateFloatAsState(
            targetValue = if (pressed) 0.98f else 1.0f,
            animationSpec = tween(120),
            label = "scale2"
        )
    Surface(
        color = Color(0xFFFAFAFA),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, Color(0xFFE5E7EB)),
        modifier = modifier.fillMaxWidth().scale(scale).clip(RoundedCornerShape(14.dp))
    ) {
        TextButton(
            onClick = onClick,
            enabled = enabled,
            interactionSource = interaction,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    leading()
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(text = text, color = Color(0xFF374151))
                }
                trailing()
            }
        }
    }
}

@Composable
private fun MutableInteractionSource.collectIsPressedAsState(): State<Boolean> {
    return remember { androidx.compose.runtime.mutableStateOf(false) }
}
