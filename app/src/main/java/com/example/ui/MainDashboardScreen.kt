package com.example.ui

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
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
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import java.io.ByteArrayOutputStream
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import com.example.R
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
    onNavigateToAttendance: () -> Unit,
    onNavigateToExamSettings: () -> Unit,
    onLogout: () -> Unit
) {
    val schoolSetting by viewModel.schoolSetting.collectAsState()
    val authState by viewModel.authState.collectAsState()
    val activeRole by viewModel.activeRole.collectAsState()
    var showEditDialog by remember { mutableStateOf(false) }

    val guestAccountText = stringResource(R.string.guest_account)
    val userEmail = when (val auth = authState) {
        is AuthState.Authenticated -> auth.email
        else -> guestAccountText
    }

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    
    // Observe configuration changes to trigger recomposition when locale changes
    val configuration = androidx.compose.ui.platform.LocalConfiguration.current
    val currentLocale = remember(configuration) {
        AppCompatDelegate.getApplicationLocales().toLanguageTags().ifEmpty { "en" }
    }
    
    val languages = listOf(
        "en" to "English",
        "hi" to "हिन्दी (Hindi)",
        "es" to "Español"
    )
    
    var expanded by remember { mutableStateOf(false) }
    val currentLangName = remember(currentLocale) {
        languages.find { it.first == currentLocale }?.second ?: "English"
    }

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
                        text = schoolSetting.schoolName,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = stringResource(R.string.signed_in_context),
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.outline
                    )
                    Text(
                        text = userEmail,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                    
                    // ROLE SIMULATOR ROW SELECTORS
                    Text(
                        text = stringResource(R.string.simulate_role_context),
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
                                    text = if (r == "Principal/Coordinator") stringResource(R.string.school_coordinator) else {
                                        when(r) {
                                            "Admin" -> stringResource(R.string.role_admin)
                                            "Teacher" -> stringResource(R.string.role_teacher)
                                            else -> stringResource(R.string.role_principal_coordinator)
                                        }
                                    },
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                }
                
                HorizontalDivider()
                Spacer(modifier = Modifier.height(12.dp))

                // DRAWER ITEMS - ONLY ONE MAIN FUNCTION BUTTON: Exam setting
                NavigationDrawerItem(
                    icon = { Icon(Icons.Default.Settings, contentDescription = "") },
                    label = { Text(stringResource(R.string.exam_setting)) },
                    selected = false,
                    onClick = {
                        scope.launch {
                            drawerState.close()
                            onNavigateToExamSettings()
                        }
                    },
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp).testTag("drawer_exam_setting_button")
                )

                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
                
                // LANGUAGE SWITCHER SECTION
                Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.Language, contentDescription = null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.outline)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.language_switcher_label),
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))

                    Box {
                        OutlinedButton(
                            onClick = { expanded = true },
                            modifier = Modifier.fillMaxWidth().wrapContentHeight().padding(vertical = 4.dp),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(currentLangName, fontSize = 12.sp)
                                Icon(Icons.Default.ArrowDropDown, contentDescription = null, modifier = Modifier.size(16.dp))
                            }
                        }
                        
                        DropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false },
                            modifier = Modifier.fillMaxWidth(0.6f)
                        ) {
                            languages.forEach { (tag, name) ->
                                DropdownMenuItem(
                                    text = { Text(name, fontSize = 12.sp) },
                                    onClick = {
                                        AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(tag))
                                        expanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                NavigationDrawerItem(
                    icon = { Icon(Icons.Default.Logout, contentDescription = "", tint = MaterialTheme.colorScheme.error) },
                    label = { Text(stringResource(R.string.sign_out_session), color = MaterialTheme.colorScheme.error) },
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
                            text = stringResource(R.string.dashboard_title),
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                    },
                    navigationIcon = {
                        IconButton(
                            onClick = { scope.launch { drawerState.open() } },
                            modifier = Modifier.testTag("drawer_hamburger_button")
                        ) {
                            Icon(imageVector = Icons.Default.Menu, contentDescription = stringResource(R.string.menu_drawer))
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
                                text = if (activeRole == "Principal/Coordinator") stringResource(R.string.coordinator) else {
                                    when(activeRole) {
                                        "Admin" -> stringResource(R.string.role_admin)
                                        "Teacher" -> stringResource(R.string.role_teacher)
                                        else -> activeRole
                                    }
                                },
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
                                contentDescription = stringResource(R.string.sign_out),
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
                LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .background(MaterialTheme.colorScheme.background),
                contentPadding = PaddingValues(20.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                // HERO CARD: SCHOOL DETAILS (Full width)
                item(span = { GridItemSpan(2) }) {
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
                                    contentDescription = stringResource(R.string.edit_details),
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
                                    if (schoolSetting.schoolLogoBase64.isNotEmpty()) {
                                        val logoBitmap = remember(schoolSetting.schoolLogoBase64) {
                                            try {
                                                val bytes = Base64.decode(schoolSetting.schoolLogoBase64, Base64.DEFAULT)
                                                BitmapFactory.decodeByteArray(bytes, 0, bytes.size).asImageBitmap()
                                            } catch (e: Exception) {
                                                null
                                            }
                                        }
                                        if (logoBitmap != null) {
                                            Image(
                                                bitmap = logoBitmap,
                                                contentDescription = stringResource(R.string.school_logo),
                                                modifier = Modifier.fillMaxSize().padding(4.dp),
                                                contentScale = ContentScale.Fit
                                            )
                                        } else {
                                            Text(
                                                text = schoolSetting.logoEmoji,
                                                fontSize = 38.sp,
                                                textAlign = TextAlign.Center
                                            )
                                        }
                                    } else {
                                        Text(
                                            text = schoolSetting.logoEmoji,
                                            fontSize = 38.sp,
                                            textAlign = TextAlign.Center
                                        )
                                    }
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
                                            contentDescription = stringResource(R.string.session_label),
                                            tint = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f),
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Text(
                                            text = stringResource(R.string.session_label) + " " + schoolSetting.session,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
                                        )
                                    }

                                    if (schoolSetting.location.isNotEmpty()) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Place,
                                                contentDescription = stringResource(R.string.location_label),
                                                tint = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f),
                                                modifier = Modifier.size(14.dp)
                                            )
                                            Text(
                                                text = schoolSetting.location,
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Normal,
                                                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Coordinator details (Full width)
                item(span = { GridItemSpan(2) }) {
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
                            contentDescription = stringResource(R.string.active_user),
                            tint = MaterialTheme.colorScheme.outline,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = stringResource(R.string.signed_in_format).format(userEmail),
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                item(span = { GridItemSpan(2) }) {
                    Text(
                        text = stringResource(R.string.academic_modules),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }

                // MODULE CARDS
                item {
                    DashboardModuleCard(
                        title = stringResource(R.string.module_students),
                        subtitle = stringResource(R.string.module_students_desc),
                        icon = Icons.Default.Group,
                        colorAccent = MaterialTheme.colorScheme.primary,
                        onClick = onNavigateToStudents,
                        testTag = "dashboard_students_button"
                    )
                }

                item {
                    DashboardModuleCard(
                        title = stringResource(R.string.module_marks),
                        subtitle = stringResource(R.string.module_marks_desc),
                        icon = Icons.Default.EditNote,
                        colorAccent = Color(0xFF0F9D58), // Google Green style
                        onClick = onNavigateToMarksEntry,
                        testTag = "dashboard_marks_button"
                    )
                }

                item {
                    DashboardModuleCard(
                        title = stringResource(R.string.module_results),
                        subtitle = stringResource(R.string.module_results_desc),
                        icon = Icons.Default.ReceiptLong,
                        colorAccent = Color(0xFFDB4437), // Google Red style
                        onClick = onNavigateToResultGenerator,
                        testTag = "dashboard_results_button"
                    )
                }

                item {
                    DashboardModuleCard(
                        title = stringResource(R.string.module_analysis),
                        subtitle = stringResource(R.string.module_analysis_desc),
                        icon = Icons.Default.Assessment,
                        colorAccent = Color(0xFFF4B400), // Google Yellow style
                        onClick = onNavigateToAnalysis,
                        testTag = "dashboard_analysis_button"
                    )
                }

                item {
                    DashboardModuleCard(
                        title = stringResource(R.string.module_attendance),
                        subtitle = stringResource(R.string.module_attendance_desc),
                        icon = Icons.Default.HowToReg,
                        colorAccent = Color(0xFF8E24AA),
                        onClick = onNavigateToAttendance,
                        testTag = "dashboard_attendance_button"
                    )
                }

                }
        // EDIT DETAILS DIALOG
        if (showEditDialog) {
            EditSchoolDetailsDialog(
                currentSetting = schoolSetting,
                onDismiss = { showEditDialog = false },
                onSave = { name, session, loc, emoji, colorHex, affiliation, logo ->
                    viewModel.updateSchoolDetails(name, session, loc, emoji, colorHex, affiliation, logo)
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
            .aspectRatio(1.1f)
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
            verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically)
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
    onSave: (name: String, session: String, location: String, emoji: String, colorHex: String, affiliationNumber: String, logoBase64: String) -> Unit
) {
    val context = LocalContext.current
    var name by remember { mutableStateOf(currentSetting.schoolName) }
    var session by remember { mutableStateOf(currentSetting.session) }
    var location by remember { mutableStateOf(currentSetting.location) }
    var affiliationNumber by remember { mutableStateOf(currentSetting.affiliationNumber) }
    var selectedEmoji by remember { mutableStateOf(currentSetting.logoEmoji) }
    var selectedColorHex by remember { mutableStateOf(currentSetting.logoColorHex) }
    var schoolLogoBase64 by remember { mutableStateOf(currentSetting.schoolLogoBase64) }

    val logoLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            val b64 = uriToBase64(context, uri)
            if (b64 != null) {
                schoolLogoBase64 = b64
            }
        }
    }

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
                text = stringResource(R.string.edit_school_profile),
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(R.string.school_name_label)) },
                    modifier = Modifier.fillMaxWidth().testTag("edit_school_name_field"),
                    singleLine = true
                )

                OutlinedTextField(
                    value = session,
                    onValueChange = { session = it },
                    label = { Text(stringResource(R.string.academic_session_label)) },
                    placeholder = { Text(stringResource(R.string.session_placeholder)) },
                    modifier = Modifier.fillMaxWidth().testTag("edit_school_session_field"),
                    singleLine = true
                )

                OutlinedTextField(
                    value = location,
                    onValueChange = { location = it },
                    label = { Text(stringResource(R.string.school_location_label)) },
                    placeholder = { Text(stringResource(R.string.location_placeholder)) },
                    modifier = Modifier.fillMaxWidth().testTag("edit_school_location_field"),
                    singleLine = true
                )

                OutlinedTextField(
                    value = affiliationNumber,
                    onValueChange = { newValue ->
                        // Allow only digits
                        val filtered = newValue.filter { it.isDigit() }
                        if (filtered.length <= 10) {
                            affiliationNumber = filtered
                        }
                    },
                    label = { Text(stringResource(R.string.affiliation_number_label)) },
                    placeholder = { Text(stringResource(R.string.affiliation_placeholder)) },
                    modifier = Modifier.fillMaxWidth().testTag("edit_school_affiliation_field"),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )

                Text(
                    text = stringResource(R.string.school_branding_logo),
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(top = 4.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(60.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        if (schoolLogoBase64.isNotEmpty()) {
                            val bitmap = remember(schoolLogoBase64) {
                                try {
                                    val bytes = Base64.decode(schoolLogoBase64, Base64.DEFAULT)
                                    BitmapFactory.decodeByteArray(bytes, 0, bytes.size).asImageBitmap()
                                } catch (e: Exception) { null }
                            }
                            if (bitmap != null) {
                                Image(
                                    bitmap = bitmap,
                                    contentDescription = stringResource(R.string.logo_preview),
                                    modifier = Modifier.fillMaxSize().padding(4.dp),
                                    contentScale = ContentScale.Fit
                                )
                            } else {
                                Text(selectedEmoji, fontSize = 24.sp)
                            }
                        } else {
                            Text(selectedEmoji, fontSize = 24.sp)
                        }
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Button(
                            onClick = { logoLauncher.launch("image/*") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.Upload, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(stringResource(R.string.upload_custom_logo), fontSize = 12.sp)
                        }
                        if (schoolLogoBase64.isNotEmpty()) {
                            TextButton(
                                onClick = { schoolLogoBase64 = "" },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.error)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(stringResource(R.string.reset_to_emoji), color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                            }
                        }
                    }
                }

                Text(
                    text = stringResource(R.string.select_logo_emoji),
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
                    text = stringResource(R.string.select_brand_color),
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
                        onSave(name, session, location, selectedEmoji, selectedColorHex, affiliationNumber, schoolLogoBase64)
                    }
                },
                modifier = Modifier.testTag("save_school_settings_button")
            ) {
                Text(stringResource(R.string.apply_modifications))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}
