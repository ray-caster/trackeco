package com.trackeco.trackeco.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.trackeco.trackeco.MainViewModel
import com.trackeco.trackeco.ui.screens.MapScreen
import com.trackeco.trackeco.ui.screens.ProfileScreen
import com.trackeco.trackeco.ui.screens.RecordScreen
import com.trackeco.trackeco.ui.screens.WasteDexScreen
import com.trackeco.trackeco.ui.theme.HeaderGradientEnd
import com.trackeco.trackeco.ui.theme.PrimaryColor

// Sealed class for our bottom navigation routes
sealed class Screen(val route: String, val label: String, val icon: ImageVector) {
    object Map : Screen("map", "Map", Icons.Default.Map)
    object Record : Screen("record", "Record", Icons.Default.PhotoCamera)
    object WasteDex : Screen("wastedex", "Waste-Dex", Icons.Default.Book)
    object Profile : Screen("profile", "Profile", Icons.Default.Person)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(userId: String, mainViewModel: MainViewModel = viewModel()) {
    
    // Fetch user data when the MainScreen is first composed with a valid user ID.
    LaunchedEffect(userId) {
        mainViewModel.fetchUserData(userId)
    }

    val navController = rememberNavController()
    val navItems = listOf(Screen.Map, Screen.Record, Screen.WasteDex, Screen.Profile)
    val uiState by mainViewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("TrackEco", fontWeight = FontWeight.Bold, fontSize = 24.sp) },
                actions = {
                    if (uiState.userData != null) {
                        PointsDisplay(points = uiState.userData!!.points)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                ),
                modifier = Modifier.background(
                    brush = Brush.linearGradient(colors = listOf(PrimaryColor, HeaderGradientEnd))
                )
            )
        },
        bottomBar = {
            NavigationBar {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentDestination = navBackStackEntry?.destination

                navItems.forEach { screen ->
                    NavigationBarItem(
                        icon = { Icon(screen.icon, contentDescription = screen.label) },
                        label = { Text(screen.label) },
                        selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true,
                        onClick = {
                            navController.navigate(screen.route) {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                }
            }
        }
    ) { innerPadding ->
        if (uiState.isFetchingInitialData && uiState.userData == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            NavHost(
                navController,
                startDestination = Screen.Map.route, // Start on Map Screen
                Modifier.padding(innerPadding)
            ) {
                composable(Screen.Record.route) { RecordScreen(uiState, onVideoRecorded = { mainViewModel.simulateDisposal(userId) }) }
                composable(Screen.Map.route) { MapScreen() }
                composable(Screen.WasteDex.route) { WasteDexScreen() }
                composable(Screen.Profile.route) { ProfileScreen(uiState) }
            }
        }

        if (uiState.isSimulatingDisposal) {
            Dialog(onDismissRequest = {}) {
                Box(contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = Color.White)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Auditing Your Cleanup...", color = Color.White)
                    }
                }
            }
        }
        
        if (uiState.lastDisposalResult != null) {
            AlertDialog(
                onDismissRequest = { mainViewModel.clearLastDisposalResult() },
                title = { Text(if (uiState.lastDisposalResult!!.success) "Success!" else "Audit Failed") },
                text = { Text(uiState.lastDisposalResult!!.reason_string) },
                confirmButton = {
                    Button(onClick = { mainViewModel.clearLastDisposalResult() }) {
                        Text("OK")
                    }
                }
            )
        }
    }
}

@Composable
fun PointsDisplay(points: Int) {
    Box(
        modifier = Modifier
            .padding(end = 16.dp)
            .background(color = Color.White.copy(alpha = 0.2f), shape = RoundedCornerShape(50))
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Default.Star,
                contentDescription = "Points",
                tint = Color.Yellow,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = points.toString(),
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
        }
    }
}