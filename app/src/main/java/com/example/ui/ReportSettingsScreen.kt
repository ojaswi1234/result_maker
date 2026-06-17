package com.example.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.viewmodel.AppViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportSettingsScreen(
    viewModel: AppViewModel,
    onBack: () -> Unit
) {
    val t1Weight by viewModel.term1Weight.collectAsState()
    val t2Weight by viewModel.term2Weight.collectAsState()

    var t1Input by remember(t1Weight) { mutableStateOf(t1Weight.toString()) }
    var t2Input by remember(t2Weight) { mutableStateOf(t2Weight.toString()) }

    var errorMessage by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Report Settings", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Text(
                text = "Academic Weightage Configuration",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.primary
            )

            Text(
                text = "Define how Term 1 and Term 2 marks contribute to the final Annual result. The sum must be exactly 100%.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    OutlinedTextField(
                        value = t1Input,
                        onValueChange = { 
                            if (it.all { char -> char.isDigit() }) t1Input = it
                        },
                        label = { Text("Term 1 Weightage (%)") },
                        modifier = Modifier.fillMaxWidth(),
                        suffix = { Text("%") },
                        isError = errorMessage != null
                    )

                    OutlinedTextField(
                        value = t2Input,
                        onValueChange = { 
                            if (it.all { char -> char.isDigit() }) t2Input = it
                        },
                        label = { Text("Term 2 Weightage (%)") },
                        modifier = Modifier.fillMaxWidth(),
                        suffix = { Text("%") },
                        isError = errorMessage != null
                    )

                    if (errorMessage != null) {
                        Text(
                            text = errorMessage!!,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                }
            }

            Button(
                onClick = {
                    val t1 = t1Input.toIntOrNull() ?: 0
                    val t2 = t2Input.toIntOrNull() ?: 0
                    
                    if (t1 + t2 == 100) {
                        viewModel.updateTermWeights(t1, t2)
                        errorMessage = null
                        onBack()
                    } else {
                        errorMessage = "Total weightage must equal 100% (Current: ${t1 + t2}%)"
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium
            ) {
                Icon(Icons.Default.Save, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Save Settings")
            }

            Spacer(modifier = Modifier.height(40.dp))
            
            // Visual Indicator
            val t1 = t1Input.toIntOrNull() ?: 0
            val t2 = t2Input.toIntOrNull() ?: 0
            
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Text("Current Split: $t1% | $t2%", fontWeight = FontWeight.Bold)
                LinearProgressIndicator(
                    progress = { (t1.toFloat() / 100f).coerceIn(0f, 1f) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(12.dp)
                        .padding(vertical = 8.dp),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.secondaryContainer
                )
            }
        }
    }
}