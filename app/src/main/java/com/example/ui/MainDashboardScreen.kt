package com.example.ui

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.SchoolSetting
import com.example.viewmodel.AppViewModel
import com.example.viewmodel.AuthState
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainDashboardScreen(
    viewModel: AppViewModel,
    onNavigateToStudents: () -> Unit,
    onNavigateToMarksEntry: () -> Unit,
    onNavigateToResultGenerator: () -> Unit,
    onNavigateToAnalysis: () -> Unit,
    onNavigateToExamSettings: () -> Unit,
    onLogout: () -> Unit
) {
    val schoolSetting by viewModel.schoolSetting.collectAsState()
    val authState by viewModel.authState.collectAsState()
    val activeRole by viewModel.activeRole.collectAsState()
    var showEditDialog by remember { mutableStateOf(false) }

    val userEmail = when (val auth = authState) {
        is AuthState.Authenticated -> auth.email
        else -> "Guest Account"
    }

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                Spacer(modifier = Modifier.height(12.dp))
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Global Academy",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Signed in context:",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.outline
                    )
                    Text(
                        text = userEmail,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    Divider(modifier = Modifier.padding(vertical = 4.dp))
                    
                    // ROLE SIMULATOR ROW SELECTORS
                    Text(
                        text = "Simulate Role Context:",
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.outline
                    )
                    
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        listOf("Admin", "Teacher", "Principal/Coordinator").forEach { r ->
                            val isSelected = activeRole == r
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(
                                        if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f) else Color.Transparent
                                    )
                                    .clickable {
                                        viewModel.updateActiveRole(r)
                                    }
                                    .padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                RadioButton(
                                    selected = isSelected,
                                    onClick = { viewModel.updateActiveRole(r) },
                                    colors = RadioButtonDefaults.colors(selectedColor = MaterialTheme.colorScheme.primary)
                                )
                                Text(
                                    text = if (r == "Principal/Coordinator") "School Coordinator" else r,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                }
                
                Divider()
                Spacer(modifier = Modifier.height(12.dp))

                // DRAWER ITEMS
                NavigationDrawerItem(
                    icon = { Icon(Icons.Default.School, contentDescription = "") },
                    label = { Text("Portal Info & Dashboard") },
                    selected = true,
                    onClick = { scope.launch { drawerState.close() } },
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp)
                )

                NavigationDrawerItem(
                    icon = { Icon(Icons.Default.Settings, contentDescription = "") },
                    label = { Text("Exam Settings Module") },
                    selected = false,
                    onClick = {
                        scope.launch {
                            drawerState.close()
                            onNavigateToExamSettings()
                        }
                    },
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp).testTag("drawer_exam_setting_button")
                )

                NavigationDrawerItem(
                    icon = { Icon(Icons.Default.Group, contentDescription = "") },
                    label = { Text("Students & Classes") },
                    selected = false,
                    onClick = {
                        scope.launch {
                            drawerState.close()
                            onNavigateToStudents()
                        }
                    },
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp)
                )

                NavigationDrawerItem(
                    icon = { Icon(Icons.Default.EditNote, contentDescription = "") },
                    label = { Text("Academic Marks Entry") },
                    selected = false,
                    onClick = {
                        scope.launch {
                            drawerState.close()
                            onNavigateToMarksEntry()
                        }
                    },
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp)
                )

                NavigationDrawerItem(
                    icon = { Icon(Icons.Default.ReceiptLong, contentDescription = "") },
                    label = { Text("Report Card Generator") },
                    selected = false,
                    onClick = {
                        scope.launch {
                            drawerState.close()
                            onNavigateToResultGenerator()
                        }
                    },
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp)
                )

                NavigationDrawerItem(
                    icon = { Icon(Icons.Default.Assessment, contentDescription = "") },
                    label = { Text("Performance Analytics") },
                    selected = false,
                    onClick = {
                        scope.launch {
                            drawerState.close()
                            onNavigateToAnalysis()
                        }
                    },
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp)
                )

                Spacer(modifier = Modifier.weight(1f))

                NavigationDrawerItem(
                    icon = { Icon(Icons.Default.Logout, contentDescription = "", tint = MaterialTheme.colorScheme.error) },
                    label = { Text("Sign Out Session", color = MaterialTheme.colorScheme.error) },
                    selected = false,
                    onClick = {
                        scope.launch {
                            drawerState.close()
                            viewModel.logout(onLogout)
                        }
                    },
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp)
                )
                Spacer(modifier = Modifier.height(12.dp))
            }
        }
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = "School Workspace Portal",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                    },
                    navigationIcon = {
                        IconButton(
                            onClick = { scope.launch { drawerState.open() } },
                            modifier = Modifier.testTag("drawer_hamburger_button")
                        ) {
                            Icon(imageVector = Icons.Default.Menu, contentDescription = "Menu Drawer")
                        }
                    },
                    actions = {
                        // Display role banner directly
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = when (activeRole) {
                                "Admin" -> MaterialTheme.colorScheme.primaryContainer
                                "Teacher" -> MaterialTheme.colorScheme.secondaryContainer
                                else -> MaterialTheme.colorScheme.tertiaryContainer
                            },
                            modifier = Modifier.padding(end = 4.dp)
                        ) {
                            Text(
                                text = if (activeRole == "Principal/Coordinator") "Coordinator" else activeRole,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }

                        IconButton(
                            onClick = { viewModel.logout(onLogout) },
                            modifier = Modifier.testTag("signout_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Logout,
                                contentDescription = "Sign Out",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                )
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .background(MaterialTheme.colorScheme.background)
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
            // HERO CARD: SCHOOL DETAILS (DYNAMIC & EDITABLE LOGO, NAME, SESSION)
            ElevatedCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.outlineVariant,
                        shape = RoundedCornerShape(24.dp)
                    )
                    .testTag("school_info_card"),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.elevatedCardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.25f)
                )
            ) {
                Box(modifier = Modifier.fillMaxWidth()) {
                    // Edit trigger button (floating inside hero card top-right)
                    IconButton(
                        onClick = { showEditDialog = true },
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(12.dp)
                            .testTag("edit_school_settings")
                            .background(
                                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f),
                                shape = CircleShape
                            )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit Details",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(18.dp)
                    ) {
                        // School Logo (Dynamic / Editable color & emoji)
                        val brandColor = try {
                            Color(android.graphics.Color.parseColor(schoolSetting.logoColorHex))
                        } catch (e: Exception) {
                            MaterialTheme.colorScheme.primary
                        }

                        Box(
                            modifier = Modifier
                                .size(76.dp)
                                .background(
                                    brush = Brush.radialGradient(
                                        colors = listOf(brandColor.copy(alpha = 0.9f), brandColor)
                                    ),
                                    shape = RoundedCornerShape(20.dp)
                                )
                                .border(
                                    width = 2.dp,
                                    color = MaterialTheme.colorScheme.surface,
                                    shape = RoundedCornerShape(20.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = schoolSetting.logoEmoji,
                                fontSize = 38.sp,
                                textAlign = TextAlign.Center
                            )
                        }

                        Column(
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = schoolSetting.schoolName,
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                            
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CalendarToday,
                                    contentDescription = "Session",
                                    tint = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f),
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = "Session " + schoolSetting.session,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
                                )
                            }
                        }
                    }
                }
            }

            // Coordinator details
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = "Active user",
                    tint = MaterialTheme.colorScheme.outline,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = "Signed in: $userEmail",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "Academic Modules",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )

            // THE FOUR KEY BUTTONS IN A VISUALLY POLISHED GRID
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                item {
                    DashboardModuleCard(
                        title = "Students & Classes",
                        subtitle = "Classes, Sections & Rosters",
                        icon = Icons.Default.Group,
                        colorAccent = MaterialTheme.colorScheme.primary,
                        onClick = onNavigateToStudents,
                        testTag = "dashboard_students_button"
                    )
                }

                item {
                    DashboardModuleCard(
                        title = "Marks Entry",
                        subtitle = "Input Grades & Subjects",
                        icon = Icons.Default.EditNote,
                        colorAccent = Color(0xFF0F9D58), // Google Green style
                        onClick = onNavigateToMarksEntry,
                        testTag = "dashboard_marks_button"
                    )
                }

                item {
                    DashboardModuleCard(
                        title = "Result Generator",
                        subtitle = "Generate Report PDF",
                        icon = Icons.Default.ReceiptLong,
                        colorAccent = Color(0xFFDB4437), // Google Red style
                        onClick = onNavigateToResultGenerator,
                        testTag = "dashboard_results_button"
                    )
                }

                item {
                    DashboardModuleCard(
                        title = "Result Analysis",
                        subtitle = "Performance Analytics",
                        icon = Icons.Default.Assessment,
                        colorAccent = Color(0xFFF4B400), // Google Yellow style
                        onClick = onNavigateToAnalysis,
                        testTag = "dashboard_analysis_button"
                    )
                }
            }
        }

        // EDIT DETAILS DIALOG
        if (showEditDialog) {
            EditSchoolDetailsDialog(
                currentSetting = schoolSetting,
                onDismiss = { showEditDialog = false },
                onSave = { name, session, emoji, colorHex ->
                    viewModel.updateSchoolDetails(name, session, emoji, colorHex)
                    showEditDialog = false
                }
            )
        }
    }
}
}

@Composable
fun DashboardModuleCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    colorAccent: Color,
    onClick: () -> Unit,
    testTag: String
) {
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .height(150.dp)
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                shape = RoundedCornerShape(20.dp)
            )
            .testTag(testTag)
            .clickable { onClick() },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(
                        color = colorAccent.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(12.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = colorAccent,
                    modifier = Modifier.size(24.dp)
                )
            }

            Column(
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = subtitle,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun EditSchoolDetailsDialog(
    currentSetting: SchoolSetting,
    onDismiss: () -> Unit,
    onSave: (name: String, session: String, emoji: String, colorHex: String) -> Unit
) {
    var name by remember { mutableStateOf(currentSetting.schoolName) }
    var session by remember { mutableStateOf(currentSetting.session) }
    var selectedEmoji by remember { mutableStateOf(currentSetting.logoEmoji) }
    var selectedColorHex by remember { mutableStateOf(currentSetting.logoColorHex) }

    val logoPredefinedEmojis = listOf("🏫", "🎓", "📚", "🖊️", "🔬", "🌍", "🏆", "🎨")
    val brandPredefinedColors = listOf(
        Pair("Indigo", "#4F46E5"),
        Pair("Teal", "#0D9488"),
        Pair("Rose", "#E11D48"),
        Pair("Amber", "#D97706"),
        Pair("Slate", "#475569"),
        Pair("Forest", "#166534")
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Edit School Profile",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("School Name") },
                    modifier = Modifier.fillMaxWidth().testTag("edit_school_name_field"),
                    singleLine = true
                )

                OutlinedTextField(
                    value = session,
                    onValueChange = { session = it },
                    label = { Text("Academic Session") },
                    placeholder = { Text("e.g. 2025 - 2026") },
                    modifier = Modifier.fillMaxWidth().testTag("edit_school_session_field"),
                    singleLine = true
                )

                Text(
                    text = "Select Logo Emoji",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(top = 4.dp)
                )

                // Emoji row selector
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    logoPredefinedEmojis.forEach { emoji ->
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(
                                    if (selectedEmoji == emoji) {
                                        MaterialTheme.colorScheme.primaryContainer
                                    } else {
                                        Color.Transparent
                                    }
                                )
                                .clickable { selectedEmoji = emoji }
                                .padding(4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = emoji, fontSize = 18.sp)
                        }
                    }
                }

                Text(
                    text = "Select School Brand Color",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(top = 4.dp)
                )

                // Primary brand color circles list
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    brandPredefinedColors.forEach { (cName, hex) ->
                        val color = Color(android.graphics.Color.parseColor(hex))
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(color)
                                .border(
                                    width = if (selectedColorHex == hex) 3.dp else 1.dp,
                                    color = if (selectedColorHex == hex) {
                                        MaterialTheme.colorScheme.onSurface
                                    } else {
                                        Color.Transparent
                                    },
                                    shape = CircleShape
                                )
                                .clickable { selectedColorHex = hex }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotEmpty() && session.isNotEmpty()) {
                        onSave(name, session, selectedEmoji, selectedColorHex)
                    }
                },
                modifier = Modifier.testTag("save_school_settings_button")
            ) {
                Text("Apply Modifications")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
