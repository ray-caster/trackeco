package com.trackeco.trackeco.ui.screens

import android.Manifest
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.trackeco.trackeco.AppUiState
import kotlinx.coroutines.delay
import java.io.File

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun CrisisRecordScreen(
    uiState: AppUiState,
    onVideoRecorded: (Uri) -> Unit
) {
    val context = LocalContext.current
    val userData = uiState.userData
    var tempVideoUri by remember { mutableStateOf<Uri?>(null) }

    // Crisis counter animation
    var bottleCounter by remember { mutableStateOf(0) }
    var co2Counter by remember { mutableStateOf(0f) }
    
    // Simulate real-time crisis counters
    LaunchedEffect(Unit) {
        while (true) {
            delay(100) // Update every 100ms
            bottleCounter += (1..3).random() // 1-3 bottles per 100ms
            co2Counter += (0.01f..0.05f).random() // CO2 emissions
        }
    }

    // --- CAMERA AND PERMISSION LOGIC ---
    val cameraPermissionState = rememberPermissionState(permission = Manifest.permission.CAMERA)

    val videoCaptureLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CaptureVideo(),
        onResult = { success ->
            if (success && tempVideoUri != null) {
                // Pass the valid URI up to the ViewModel for processing
                onVideoRecorded(tempVideoUri!!)
            } else {
                println("Video recording cancelled or failed.")
            }
        }
    )

    val handleRecordClick = {
        if (cameraPermissionState.status.isGranted) {
            val newVideoFile = File(context.cacheDir, "temp_video_${System.currentTimeMillis()}.mp4")
            val newVideoUri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.provider",
                newVideoFile
            )
            tempVideoUri = newVideoUri
            videoCaptureLauncher.launch(newVideoUri)
        } else {
            cameraPermissionState.launchPermissionRequest()
        }
    }
    // --- END OF CAMERA LOGIC ---

    // Crisis-oriented environmental gradient: Pollution → Action → Transform → Thrive
    val crisisGradient = Brush.verticalGradient(
        colors = listOf(
            Color(0xFF8B4513), // Pollution brown
            Color(0xFFFF6B35), // Action orange  
            Color(0xFF4CAF50), // Transform green
            Color(0xFF009688)  // Thriving teal
        )
    )
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(brush = crisisGradient),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp),
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
        ) {
            Spacer(modifier = Modifier.height(16.dp))
            
            // CRISIS INTERVENTION HEADER
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = Color.Black.copy(alpha = 0.7f)
                ),
                elevation = CardDefaults.cardElevation(8.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "🚨 CRISIS INTERVENTION MODE 🚨",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFFF6B35), // Action orange
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "YOUR ACTION STOPS THIS COUNTER",
                        fontSize = 14.sp,
                        color = Color.White,
                        textAlign = TextAlign.Center
                    )
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    // Real-time crisis counters
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        CrisisCounter(
                            value = bottleCounter.toString(),
                            label = "Plastic Bottles\nSold Today",
                            icon = "🍶"
                        )
                        CrisisCounter(
                            value = String.format("%.1f", co2Counter),
                            label = "Tons CO₂\nEmitted Today",
                            icon = "☁️"
                        )
                    }
                }
            }

            // PLANET HERO STATUS
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(180.dp)
                    .shadow(elevation = 20.dp, shape = CircleShape, ambientColor = Color(0xFF4CAF50))
                    .clip(CircleShape)
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                Color(0xFF4CAF50), // Transform green center
                                Color(0xFF009688)  // Thriving teal edge
                            )
                        )
                    )
                    .border(4.dp, Color(0xFFFFD700), CircleShape) // Gold border for hero status
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "🌍",
                        fontSize = 32.sp
                    )
                    Text(
                        text = userData?.points?.toString() ?: "0",
                        fontSize = 36.sp, 
                        fontWeight = FontWeight.Bold, 
                        color = Color.White
                    )
                    Text(
                        text = "PLANET HERO\nPOINTS",
                        fontSize = 12.sp, 
                        color = Color.White.copy(alpha = 0.9f),
                        textAlign = TextAlign.Center,
                        lineHeight = 14.sp
                    )
                }
            }

            // ACTIVIST STATS
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                ActivistStatItem(
                    value = userData?.streak?.toString() ?: "0",
                    label = "Days of\nCrisis Response",
                    icon = "🔥"
                )
                ActivistStatItem(
                    value = userData?.ecoRank ?: "Recruit",
                    label = "Activist\nRank",
                    icon = "⭐"
                )
            }

            // CRISIS INTERVENTION ACTION BUTTON (72dp for glove-friendly outdoor use)
            Button(
                onClick = handleRecordClick,
                enabled = !uiState.isProcessingDisposal,
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .height(72.dp), // Glove-friendly height
                shape = RoundedCornerShape(36.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFFF6B35), // Action orange
                    disabledContainerColor = Color.Gray
                ),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 12.dp)
            ) {
                if (uiState.isProcessingDisposal) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = Color.White,
                            strokeWidth = 3.dp
                        )
                        Text(
                            text = "PROCESSING\nINTERVENTION...",
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            lineHeight = 18.sp
                        )
                    }
                } else {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CameraAlt,
                            contentDescription = "Crisis Intervention",
                            tint = Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "🚨 RESCUE NOW! 🚨",
                                color = Color.White,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Crisis Intervention Report",
                                color = Color.White.copy(alpha = 0.9f),
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.weight(1f))
        }
    }
}

@Composable
fun CrisisCounter(
    value: String,
    label: String,
    icon: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(horizontal = 8.dp)
    ) {
        Text(
            text = icon,
            fontSize = 20.sp
        )
        Text(
            text = value,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFFFF6B35) // Action orange
        )
        Text(
            text = label,
            fontSize = 10.sp,
            color = Color.White.copy(alpha = 0.8f),
            textAlign = TextAlign.Center,
            lineHeight = 12.sp
        )
    }
}

@Composable
fun ActivistStatItem(
    value: String,
    label: String,
    icon: String
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.Black.copy(alpha = 0.6f)
        ),
        modifier = Modifier.defaultMinSize(minWidth = 120.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = icon,
                fontSize = 24.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF4CAF50), // Transform green
                textAlign = TextAlign.Center
            )
            Text(
                text = label,
                fontSize = 11.sp,
                color = Color.White.copy(alpha = 0.9f),
                textAlign = TextAlign.Center,
                lineHeight = 13.sp,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}