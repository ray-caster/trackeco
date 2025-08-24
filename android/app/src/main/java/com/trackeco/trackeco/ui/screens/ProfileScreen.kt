package com.trackeco.trackeco.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Leaderboard
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.trackeco.trackeco.AppUiState
import com.trackeco.trackeco.ui.theme.PrimaryColor
import com.trackeco.trackeco.ui.theme.SuccessColor

@Composable
fun ProfileScreen(uiState: AppUiState) {
    val userData = uiState.userData

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Stats Cards
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            ProfileStatCard(
                label = "Total Points",
                value = userData?.points?.toString() ?: "0",
                color = PrimaryColor,
                modifier = Modifier.weight(1f)
            )
            ProfileStatCard(
                label = "Day Streak",
                value = userData?.streak?.toString() ?: "0",
                color = SuccessColor,
                modifier = Modifier.weight(1f)
            )
        }

        // XP Progress Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column {
                CardHeader(text = "Eco Rank Progress", icon = Icons.Default.Leaderboard)
                Column(modifier = Modifier.padding(16.dp)) {
                    val progress = calculateXpProgress(userData?.xp ?: 0)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(userData?.eco_rank ?: "Novice", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                        Text("${userData?.xp ?: 0} XP", style = MaterialTheme.typography.bodyLarge)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = { progress }, // Functional progress bar
                        modifier = Modifier.fillMaxWidth().height(8.dp).clip(MaterialTheme.shapes.small)
                    )
                }
            }
        }

        // User Info Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
             Column {
                CardHeader(text = "Profile Information", icon = Icons.Default.Info)
                Column(modifier = Modifier.padding(16.dp)) {
                    InfoRow(label = "User ID:", value = userData?.user_id ?: "Loading...")
                    InfoRow(label = "Member Since:", value = "August 20, 2025")
                }
            }
        }
    }
}

// Helper function to calculate XP progress to the next level
private fun calculateXpProgress(xp: Int): Float {
    // Define the XP thresholds for each level
    val levels = listOf(0, 100, 300, 600, 1000, 1500, 2500)
    if (xp >= levels.last()) return 1f

    var currentLevelXp = 0
    var nextLevelXp = levels[1]

    for (i in 0 until levels.size - 1) {
        if (xp >= levels[i] && xp < levels[i + 1]) {
            currentLevelXp = levels[i]
            nextLevelXp = levels[i + 1]
            break
        }
    }
    
    val xpInLevel = xp - currentLevelXp
    val xpForLevel = nextLevelXp - currentLevelXp
    return if (xpForLevel > 0) xpInLevel.toFloat() / xpForLevel.toFloat() else 0f
}

@Composable
fun ProfileStatCard(label: String, value: String, color: Color, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = color
            )
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(modifier = Modifier.padding(vertical = 4.dp)) {
        Text(
            text = label,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.width(120.dp)
        )
        Text(text = value)
    }
}