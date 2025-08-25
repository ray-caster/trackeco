package com.trackeco.trackeco.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.trackeco.trackeco.ui.theme.AccentColor
import com.trackeco.trackeco.ui.theme.PrimaryColor

@Composable
fun MapScreen() {
    Box(modifier = Modifier.fillMaxSize()) {
        // In a real app, this would be the GoogleMap composable.
        // For now, it's a placeholder to show the layout.
        Text(
            text = "Google Map Placeholder",
            modifier = Modifier.align(Alignment.Center)
        )

        FloatingActionButton(
            onClick = { 
                // Navigate to crisis record screen for emergency waste disposal
                // This will be handled by the navigation controller in MainApp
            },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = 16.dp, end = 16.dp),
            shape = CircleShape,
            containerColor = AccentColor,
            contentColor = PrimaryColor,
            elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 8.dp)
        ) {
            Icon(
                imageVector = Icons.Default.PhotoCamera,
                contentDescription = "Record Disposal",
                modifier = Modifier.size(24.dp)
            )
        }
    }
}