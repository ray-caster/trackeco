package com.trackeco.trackeco.ui.components

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog

@Composable
fun ShareImpactDialog(
    isVisible: Boolean,
    onDismiss: () -> Unit,
    userPoints: Int,
    userRank: String,
    bottlesPrevented: Int = 0,
    co2Prevented: Float = 0f
) {
    if (isVisible) {
        Dialog(onDismissRequest = onDismiss) {
            ShareImpactContent(
                onDismiss = onDismiss,
                userPoints = userPoints,
                userRank = userRank,
                bottlesPrevented = bottlesPrevented,
                co2Prevented = co2Prevented
            )
        }
    }
}

@Composable
private fun ShareImpactContent(
    onDismiss: () -> Unit,
    userPoints: Int,
    userRank: String,
    bottlesPrevented: Int,
    co2Prevented: Float
) {
    val context = LocalContext.current
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header with close button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "🔥 FLEX YOUR IMPACT! 🔥",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFFF6B35)
                )
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = Color.Gray
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Impact showcase with gradient background
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        brush = Brush.horizontalGradient(
                            colors = listOf(
                                Color(0xFF4CAF50),
                                Color(0xFF009688)
                            )
                        ),
                        shape = RoundedCornerShape(16.dp)
                    )
                    .padding(20.dp)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "🌍 PLANET RESCUE STATS 🌍",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        textAlign = TextAlign.Center
                    )
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        ImpactStat(
                            value = userPoints.toString(),
                            label = "Hero Points",
                            icon = "⭐"
                        )
                        ImpactStat(
                            value = userRank,
                            label = "Activist Rank",
                            icon = "🏆"
                        )
                    }
                    
                    if (bottlesPrevented > 0 || co2Prevented > 0) {
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            if (bottlesPrevented > 0) {
                                ImpactStat(
                                    value = bottlesPrevented.toString(),
                                    label = "Bottles Prevented",
                                    icon = "🍶"
                                )
                            }
                            if (co2Prevented > 0) {
                                ImpactStat(
                                    value = "${String.format("%.1f", co2Prevented)}kg",
                                    label = "CO₂ Saved",
                                    icon = "🌱"
                                )
                            }
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(20.dp))
            
            // Share buttons
            Text(
                text = "Share your environmental impact:",
                fontSize = 14.sp,
                color = Color.Gray,
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ShareButton(
                    text = "Share Story",
                    color = Color(0xFF4CAF50),
                    onClick = {
                        shareImpactStory(context, userPoints, userRank, bottlesPrevented, co2Prevented)
                    }
                )
                
                ShareButton(
                    text = "Challenge Friends",
                    color = Color(0xFFFF6B35),
                    onClick = {
                        shareChallengeMessage(context, userPoints, userRank)
                    }
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "Every share inspires action! 🚀",
                fontSize = 12.sp,
                color = Color.Gray,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun ImpactStat(
    value: String,
    label: String,
    icon: String
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = icon,
            fontSize = 20.sp
        )
        Text(
            text = value,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
        Text(
            text = label,
            fontSize = 10.sp,
            color = Color.White.copy(alpha = 0.9f),
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun ShareButton(
    text: String,
    color: Color,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(containerColor = color),
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.height(40.dp)
    ) {
        Icon(
            imageVector = Icons.Default.Share,
            contentDescription = "Share",
            tint = Color.White,
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = text,
            color = Color.White,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

private fun shareImpactStory(
    context: Context,
    points: Int,
    rank: String,
    bottlesPrevented: Int,
    co2Prevented: Float
) {
    val shareText = buildString {
        appendLine("🌍 PLANET RESCUE TEAM EARTH MEMBER ALERT! 🌍")
        appendLine()
        appendLine("I'm making a real difference in the climate crisis!")
        appendLine("⭐ $points Planet Hero Points")
        appendLine("🏆 $rank Activist Rank")
        
        if (bottlesPrevented > 0) {
            appendLine("🍶 $bottlesPrevented plastic bottles prevented from production")
        }
        if (co2Prevented > 0) {
            appendLine("🌱 ${String.format("%.1f", co2Prevented)}kg CO₂ emissions prevented")
        }
        
        appendLine()
        appendLine("Every piece of waste disposed properly prevents pollution at its source!")
        appendLine("Join me in stopping the crisis before it starts. 🚨")
        appendLine()
        appendLine("#ClimateAction #WasteDisposal #PlanetRescue #EnvironmentalActivist")
        appendLine("Download TrackEco and become a Planet Hero! 🦸‍♀️🦸‍♂️")
    }
    
    shareContent(context, shareText)
}

private fun shareChallengeMessage(
    context: Context,
    points: Int,
    rank: String
) {
    val challengeText = buildString {
        appendLine("🚨 CLIMATE CRISIS CHALLENGE! 🚨")
        appendLine()
        appendLine("I just hit $points Planet Hero Points as a $rank in TrackEco!")
        appendLine()
        appendLine("Think you can beat my environmental impact? 🌍")
        appendLine("Every disposal prevents pollution before it starts!")
        appendLine()
        appendLine("JOIN THE PLANET RESCUE TEAM and prove you're serious about saving our planet! 🔥")
        appendLine()
        appendLine("Download TrackEco - Let's see who's the real environmental hero! 💪")
        appendLine("#ClimateChallenge #PlanetHero #EnvironmentalActivist #TrackEco")
    }
    
    shareContent(context, challengeText)
}

private fun shareContent(context: Context, text: String) {
    val intent = Intent().apply {
        action = Intent.ACTION_SEND
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, text)
    }
    
    val shareIntent = Intent.createChooser(intent, "Share your environmental impact")
    context.startActivity(shareIntent)
}