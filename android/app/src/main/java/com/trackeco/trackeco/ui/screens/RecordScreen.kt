package com.trackeco.trackeco.ui.screens

import android.Manifest
import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PhotoCamera
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.trackeco.trackeco.AppUiState
import com.trackeco.trackeco.ui.theme.AccentColor
import com.trackeco.trackeco.ui.theme.HeaderGradientEnd
import com.trackeco.trackeco.ui.theme.PrimaryColor
import java.io.File

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun RecordScreen(
    uiState: AppUiState,
    onVideoRecorded: (Uri) -> Unit
) {
    val context = LocalContext.current
    val userData = uiState.userData
    var tempVideoUri by remember { mutableStateOf<Uri?>(null) }

    // --- CORRECTED CAMERA AND PERMISSION LOGIC ---

    val cameraPermissionState = rememberPermissionState(permission = Manifest.permission.CAMERA)

    val videoCaptureLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CaptureVideo(),
        onResult = { success ->
            if (success && tempVideoUri != null) {
                // If the camera successfully saved the video to our temporary Uri,
                // we pass that Uri up to the ViewModel for processing.
                onVideoRecorded(tempVideoUri!!)
            } else {
                println("Video recording cancelled or failed.")
            }
        }
    )

    val handleRecordClick = {
        if (cameraPermissionState.status.isGranted) {
            // **THE FIX:** We must create a temporary file and get its secure URI first.
            val newVideoFile = File(context.cacheDir, "temp_video_${System.currentTimeMillis()}.mp4")
            val newVideoUri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.provider",
                newVideoFile
            )
            tempVideoUri = newVideoUri
            // Now we launch the camera, giving it the URI where it should save the video.
            videoCaptureLauncher.launch(newVideoUri)
        } else {
            cameraPermissionState.launchPermissionRequest()
        }
    }
    // --- END OF CAMERA LOGIC ---

    Box(
        modifier = Modifier.fillMaxSize().background(
            brush = Brush.linearGradient(colors = listOf(MaterialTheme.colorScheme.background, Color(0xFFF0F8FF)))
        ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(32.dp)
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(200.dp)
                    .shadow(elevation = 16.dp, shape = CircleShape, ambientColor = PrimaryColor)
                    .clip(CircleShape)
                    .background(brush = Brush.linearGradient(colors = listOf(PrimaryColor, HeaderGradientEnd)))
                    .border(6.dp, AccentColor, CircleShape)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = userData?.points?.toString() ?: "0",
                        fontSize = 48.sp, fontWeight = FontWeight.Bold, color = Color.White, lineHeight = 48.sp
                    )
                    Text(
                        text = "Points",
                        fontSize = 16.sp, color = Color.White.copy(alpha = 0.9f), modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(48.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.padding(bottom = 48.dp)
            ) {
                StatItem(value = userData?.streak?.toString() ?: "0", label = "Day Streak")
                StatItem(value = userData?.eco_rank ?: "Novice", label = "Eco Rank")
            }
            Button(
                onClick = handleRecordClick,
                enabled = !uiState.isSimulatingDisposal,
                modifier = Modifier.width(200.dp).height(80.dp),
                shape = RoundedCornerShape(40.dp),
                colors = ButtonDefaults.buttonColors(containerColor = AccentColor),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 8.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(
                        imageVector = Icons.Default.PhotoCamera,
                        contentDescription = "Record",
                        tint = PrimaryColor,
                        modifier = Modifier.size(24.dp)
                    )
                    Text(text = "Record", color = PrimaryColor, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun StatItem(value: String, label: String) {
    Card(
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 24.dp, vertical = 16.dp)
                .defaultMinSize(minWidth = 100.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = PrimaryColor
            )
            Text(
                text = label,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}