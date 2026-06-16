package com.example

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.appcompat.app.AppCompatDelegate
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.ui.*
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.AppViewModel
import com.example.viewmodel.AuthState
import com.google.firebase.auth.FirebaseAuth

import com.google.firebase.FirebaseApp
import androidx.compose.runtime.LaunchedEffect
import androidx.navigation.NavOptions

class MainActivity : AppCompatActivity() {
    private val viewModel: AppViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        FirebaseApp.initializeApp(this)
        enableEdgeToEdge()

        setContent {
            MyApplicationTheme {
                MainAppHost(viewModel = viewModel)
            }
        }
        
        handleIntent(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        val action = intent?.action
        val data = intent?.dataString
        
        if (Intent.ACTION_VIEW == action && data != null) {
            val auth = FirebaseAuth.getInstance()
            if (auth.isSignInWithEmailLink(data)) {
                val prefs = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
                val email = prefs.getString("pending_email", null)
                
                if (email != null) {
                    auth.signInWithEmailLink(email, data)
                        .addOnCompleteListener { task ->
                            if (task.isSuccessful) {
                                prefs.edit().remove("pending_email").apply()
                                viewModel.markAuthenticated(email) {} 
                            } else {
                                Toast.makeText(this, "Login failed: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                            }
                        }
                } else {
                    Toast.makeText(this, "Email not found. Please request a new link from the app.", Toast.LENGTH_LONG).show()
                }
            }
        }
    }
}

@Composable
fun MainAppHost(viewModel: AppViewModel) {
    val navController = rememberNavController()
    val authState by viewModel.authState.collectAsState()

    // Observe configuration changes to trigger recomposition when locale changes
    val configuration = LocalConfiguration.current
    val currentLocale = remember(configuration) {
        AppCompatDelegate.getApplicationLocales().toLanguageTags().ifEmpty { "en" }
    }

    // Determine initial route once to avoid NavHost recomposition issues
    val initialRoute = remember { 
        if (com.google.firebase.auth.FirebaseAuth.getInstance().currentUser != null) "dashboard" else "login" 
    }

    CompositionLocalProvider(LocalLocale provides currentLocale) {
        Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
            NavHost(
                navController = navController,
                startDestination = initialRoute,
                modifier = Modifier.padding(innerPadding)
            ) {
            // Google authentication sign-in page
            composable("login") {
                LoginScreen(
                    viewModel = viewModel,
                    onLoginSuccess = {
                        navController.navigate("dashboard") {
                            popUpTo("login") { inclusive = true }
                        }
                    }
                )
            }

            // Editable main page profile
            composable("dashboard") {
                MainDashboardScreen(
                    viewModel = viewModel,
                    onNavigateToStudents = { navController.navigate("classes") },
                    onNavigateToMarksEntry = { navController.navigate("marks_entry") },
                    onNavigateToResultGenerator = { navController.navigate("result_generator") },
                    onNavigateToAnalysis = { navController.navigate("analysis") },
                    onNavigateToExamSettings = { navController.navigate("exam_settings") },
                    onLogout = {
                        navController.navigate("login") {
                            popUpTo("dashboard") { inclusive = true }
                        }
                    }
                )
            }

            // School administrator configurations
            composable("exam_settings") {
                ExamSettingsScreen(
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() }
                )
            }

            // Classes list selection
            composable("classes") {
                ClassSectionScreen(
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() }
                )
            }

            // Student grading module
            composable("marks_entry") {
                MarksEntryScreen(
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() }
                )
            }

            // Dynamic Report Card generator with working download / PDF print
            composable("result_generator") {
                ResultGeneratorScreen(
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() }
                )
            }

            // Performance Analytics Charts
            composable("analysis") {
                ResultAnalysisScreen(
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }
}
}
