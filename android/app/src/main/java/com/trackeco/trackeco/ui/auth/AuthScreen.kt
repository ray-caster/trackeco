package com.trackeco.trackeco.ui.auth

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController

@Composable
fun AuthScreen() {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = "login") {
        composable("login") { LoginScreen(navController) }
        composable("signup") { SignUpScreen(navController) }
    }
}

@Composable
fun LoginScreen(navController: NavController, authViewModel: AuthViewModel = viewModel()) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    val uiState by authViewModel.uiState.collectAsState()

    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Welcome to EcoTrack", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(32.dp))
        OutlinedTextField(value = email, onValueChange = { email = it }, label = { Text("Email") }, isError = uiState.errorMessage != null)
        Spacer(modifier = Modifier.height(16.dp))
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            visualTransformation = PasswordVisualTransformation(),
            isError = uiState.errorMessage != null
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = { authViewModel.login(email, password) },
            enabled = !uiState.isLoading,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (uiState.isLoading) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp))
            } else {
                Text("Login")
            }
        }
        TextButton(onClick = { navController.navigate("signup") }, enabled = !uiState.isLoading) {
            Text("Don't have an account? Sign Up")
        }
        if (uiState.errorMessage != null) {
            Text(uiState.errorMessage!!, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 8.dp))
        }
    }
}

@Composable
fun SignUpScreen(navController: NavController, authViewModel: AuthViewModel = viewModel()) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    val uiState by authViewModel.uiState.collectAsState()
    
    // When registration is successful, show a message and navigate back to login
    LaunchedEffect(uiState.registrationSuccess) {
        if (uiState.registrationSuccess) {
            navController.navigate("login") {
                popUpTo("login") { inclusive = true }
            }
            // It's good practice to reset the state in the ViewModel
            authViewModel.resetRegistrationStatus()
        }
    }
    
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Create Your Account", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(32.dp))
        OutlinedTextField(value = email, onValueChange = { email = it }, label = { Text("Email") }, isError = uiState.errorMessage != null)
        Spacer(modifier = Modifier.height(16.dp))
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            visualTransformation = PasswordVisualTransformation(),
            isError = uiState.errorMessage != null
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = { authViewModel.signUp(email, password) },
            enabled = !uiState.isLoading,
            modifier = Modifier.fillMaxWidth()
        ) {
             if (uiState.isLoading) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp))
            } else {
                Text("Sign Up")
            }
        }
        TextButton(onClick = { navController.popBackStack() }, enabled = !uiState.isLoading) {
            Text("Already have an account? Login")
        }
        if (uiState.errorMessage != null) {
            Text(uiState.errorMessage!!, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 8.dp))
        }
    }
}