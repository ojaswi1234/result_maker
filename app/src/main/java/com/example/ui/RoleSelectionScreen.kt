package com.example.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.viewmodel.AppViewModel
import java.util.*

@Composable
fun RoleSelectionScreen(
    viewModel: AppViewModel,
    onNavigateToDashboard: () -> Unit
) {
    val currentUserRole by viewModel.currentUserRole.collectAsState()
    val isWaitingForApproval by viewModel.isWaitingForApproval.collectAsState()
    
    LaunchedEffect(currentUserRole, isWaitingForApproval) {
        if (currentUserRole != null && !isWaitingForApproval) {
            onNavigateToDashboard()
        }
    }
    
    var showTeacherForm by remember { mutableStateOf(false) }
    var inputCoordId by remember { mutableStateOf("") }
    var inputMobile by remember { mutableStateOf("") }

    if (isWaitingForApproval) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                CircularProgressIndicator()
                Spacer(modifier = Modifier.height(16.dp))
                Text("Waiting for Coordinator Approval...", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            }
        }
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Select Your Role", fontSize = 28.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 32.dp))

        if (!showTeacherForm) {
            ElevatedCard(
                onClick = {
                    val newId = "COORD-" + UUID.randomUUID().toString().substring(0, 4).uppercase()
                    viewModel.updateCoordinatorId(newId)
                    viewModel.updateRole("Admin")
                    onNavigateToDashboard()
                },
                modifier = Modifier.fillMaxWidth().height(120.dp).padding(bottom = 16.dp)
            ) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("School Coordinator / Admin", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
            }

            ElevatedCard(
                onClick = { showTeacherForm = true },
                modifier = Modifier.fillMaxWidth().height(120.dp)
            ) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Teacher", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
            }
        } else {
            OutlinedTextField(
                value = inputCoordId,
                onValueChange = { inputCoordId = it },
                label = { Text("Enter Coordinator ID") },
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
            )
            
            OutlinedTextField(
                value = inputMobile,
                onValueChange = { inputMobile = it },
                label = { Text("Enter Your Mobile Number") },
                modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp)
            )

            Button(
                onClick = {
                    if (inputCoordId.isNotEmpty() && inputMobile.isNotEmpty()) {
                        viewModel.requestAccess(inputCoordId, inputMobile)
                        viewModel.setWaitingForApproval(true)
                    }
                },
                modifier = Modifier.fillMaxWidth().height(56.dp)
            ) {
                Text("Submit Request")
            }

            TextButton(onClick = { showTeacherForm = false }) {
                Text("Back")
            }
        }
    }
}
