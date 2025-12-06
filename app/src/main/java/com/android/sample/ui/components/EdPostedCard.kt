package com.android.sample.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.android.sample.home.EdPostCard
import com.android.sample.home.EdPostStatus
import com.android.sample.ui.theme.ed1
import com.android.sample.ui.theme.ed2

@Composable
fun EdPostedCard(card: EdPostCard, modifier: Modifier = Modifier) {
  val statusColor =
      when (card.status) {
        EdPostStatus.Published -> Color(0xFF2ECC71) // green
        EdPostStatus.Cancelled -> Color(0xFFE74C3C) // red
      }
  val gradient = Brush.horizontalGradient(listOf(ed1, ed2))

  Column(
      modifier = modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp),
      verticalArrangement = Arrangement.spacedBy(10.dp)) {
        // Gradient frame
        EdPostGradientFrame(gradient = gradient) {
          Column(
              modifier = Modifier.fillMaxWidth().padding(16.dp),
              verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // Title and status row
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                      Text(
                          text = card.title.ifBlank { "ED post" },
                          color = Color.White,
                          fontWeight = FontWeight.Bold,
                          style = MaterialTheme.typography.titleLarge.copy(fontSize = 18.sp))
                      Spacer(Modifier.weight(1f))
                      Box(
                          modifier =
                              Modifier.background(
                                      statusColor.copy(alpha = 0.14f), RoundedCornerShape(10.dp))
                                  .padding(horizontal = 10.dp, vertical = 6.dp)) {
                            Text(
                                text =
                                    if (card.status == EdPostStatus.Published) "Published"
                                    else "Cancelled",
                                color = statusColor,
                                fontWeight = FontWeight.SemiBold,
                                style = MaterialTheme.typography.labelMedium)
                          }
                    }

                // Ed Discussion footer
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                      Icon(
                          imageVector = Icons.Default.CheckCircle,
                          contentDescription = null,
                          tint = statusColor,
                          modifier = Modifier.size(16.dp))
                      Text(
                          text = "Ed Discussion",
                          color = statusColor,
                          style = MaterialTheme.typography.labelMedium,
                          fontWeight = FontWeight.Medium)
                    }
              }
        }
      }
}
