package com.trackeco.trackeco.ui.screens

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.trackeco.trackeco.ui.theme.LightBackground
import com.trackeco.trackeco.ui.theme.PrimaryColor
import com.trackeco.trackeco.ui.theme.SuccessColor

@Composable
fun WasteDexScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Daily Challenge Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Daily Challenge",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = PrimaryColor
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Dispose of 5 Plastic items today",
                    style = MaterialTheme.typography.bodyLarge
                )
                Spacer(modifier = Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = { 0.6f }, // Mocked progress
                    modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp))
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Waste Categories Title - No more card
        Text(
            text = "Waste-Dex",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        val categories = listOf(
            Category("Plastic", Icons.Default.Circle, true),
            Category("Paper", Icons.Default.Newspaper, true),
            Category("Glass", Icons.Default.WineBar, false),
            Category("Metal", Icons.Default.Hardware, true),
            Category("Organic", Icons.Default.Eco, false),
            Category("E-Waste", Icons.Default.ElectricalServices, false),
            Category("General", Icons.Default.Delete, true)
        )

        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 120.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            items(categories) { category ->
                CategoryCard(category = category)
            }
        }
    }
}

data class Category(val name: String, val icon: ImageVector, val isUnlocked: Boolean)

@Composable
fun CategoryCard(category: Category) {
    val borderColor = if (category.isUnlocked) SuccessColor else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
    val iconColor = if (category.isUnlocked) SuccessColor else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
    val backgroundColor = if (category.isUnlocked) MaterialTheme.colorScheme.surface else LightBackground

    Card(
        modifier = Modifier
            .aspectRatio(1f)
            .border(2.dp, borderColor, RoundedCornerShape(12.dp)),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = category.icon,
                contentDescription = category.name,
                tint = iconColor,
                modifier = Modifier.size(40.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = category.name,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
        }
    }
}