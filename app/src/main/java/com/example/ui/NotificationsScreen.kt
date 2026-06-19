package com.example.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.NotificationItem
import com.example.viewmodel.AppViewModel
import com.example.viewmodel.AuthState
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SendNotificationScreen(
    viewModel: AppViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var message by remember { mutableStateOf("") }
    var selectedFileUri by remember { mutableStateOf<Uri?>(null) }
    var selectedFileName by remember { mutableStateOf<String?>(null) }
    var selectedMimeType by remember { mutableStateOf<String?>(null) }
    var isSending by remember { mutableStateOf(false) }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            val fileInfo = getFileInfoFromUri(context, uri)
            selectedFileUri = uri
            selectedFileName = fileInfo.first ?: "attached_file"
            selectedMimeType = fileInfo.second ?: "application/octet-stream"
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.send_notification_title), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("send_notif_back_button")) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(20.dp),
            contentAlignment = Alignment.TopStart
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedTextField(
                    value = message,
                    onValueChange = { message = it },
                    label = { Text(stringResource(R.string.message_body_label)) },
                    placeholder = { Text(stringResource(R.string.message_placeholder)) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .testTag("send_notif_message_input"),
                    shape = RoundedCornerShape(12.dp),
                    maxLines = 10,
                    enabled = !isSending
                )

                // Attachment area
                if (selectedFileName != null) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f)
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AttachFile,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = selectedFileName!!,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            }
                            IconButton(
                                onClick = {
                                    selectedFileUri = null
                                    selectedFileName = null
                                    selectedMimeType = null
                                },
                                enabled = !isSending
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = stringResource(R.string.remove_attachment),
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                } else {
                    OutlinedButton(
                        onClick = { filePickerLauncher.launch("*/*") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        enabled = !isSending
                    ) {
                        Icon(Icons.Default.AttachFile, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.attach_file_btn))
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = {
                        if (message.trim().isEmpty()) {
                            Toast.makeText(context, context.getString(R.string.notification_message_empty_error), Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        isSending = true
                        viewModel.sendNotification(
                            message = message.trim(),
                            fileUri = selectedFileUri,
                            fileName = selectedFileName,
                            mimeType = selectedMimeType,
                            onSuccess = {
                                isSending = false
                                Toast.makeText(context, context.getString(R.string.notification_sent_success), Toast.LENGTH_SHORT).show()
                                onBack()
                            },
                            onFailure = { error ->
                                isSending = false
                                Toast.makeText(context, "${context.getString(R.string.notification_send_failed)}: $error", Toast.LENGTH_LONG).show()
                            }
                        )
                    },
                    enabled = message.trim().isNotEmpty() && !isSending,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .testTag("send_notif_submit_button"),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    if (isSending) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                    } else {
                        Icon(Icons.Default.Send, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.send_btn))
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ViewNotificationsScreen(
    viewModel: AppViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val notifications by viewModel.notifications.collectAsState()
    val authState by viewModel.authState.collectAsState()
    val coordId by viewModel.coordinatorId.collectAsState()
    
    val googleId = (authState as? AuthState.Authenticated)?.googleId ?: ""
    val sharedPrefs = remember(context) { context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE) }
    val lastReadPrefsKey = "last_read_notif_${googleId}_${coordId}"
    
    val lastReadTimestamp = remember { sharedPrefs.getLong(lastReadPrefsKey, 0L) }

    // Update the last viewed timestamp to mark all current notifications as read
    LaunchedEffect(notifications) {
        if (notifications.isNotEmpty()) {
            val newestTime = notifications.maxOfOrNull { notif -> parseIsoTimestamp(notif.createdAt) } ?: 0L
            if (newestTime > lastReadTimestamp) {
                sharedPrefs.edit().putLong(lastReadPrefsKey, newestTime).apply()
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.notifications_title), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        if (notifications.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(
                        imageVector = Icons.Default.NotificationsOff,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.outline
                    )
                    Text(
                        text = stringResource(R.string.no_notifications_yet),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .background(MaterialTheme.colorScheme.background),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(notifications, key = { it.id }) { notif ->
                    val notifTime = parseIsoTimestamp(notif.createdAt)
                    val isUnread = notifTime > lastReadTimestamp
                    
                    NotificationCard(
                        notification = notif,
                        isUnread = isUnread,
                        onOpenAttachment = { url, mime ->
                            openAttachment(context, url, mime)
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun NotificationCard(
    notification: NotificationItem,
    isUnread: Boolean,
    onOpenAttachment: (String, String) -> Unit
) {
    val relativeTime = getRelativeTimeSpanString(notification.createdAt)
    val cardBackground = if (isUnread) {
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f)
    } else {
        MaterialTheme.colorScheme.surface
    }
    
    val borderStroke = if (isUnread) {
        BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
    } else {
        BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = cardBackground),
        border = borderStroke
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (isUnread) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary)
                        )
                    }
                    Text(
                        text = stringResource(R.string.notification_sender_format).format(notification.senderName),
                        fontWeight = if (isUnread) FontWeight.Bold else FontWeight.SemiBold,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Text(
                    text = relativeTime,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.outline
                )
            }

            Text(
                text = notification.message,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = if (isUnread) FontWeight.Medium else FontWeight.Normal
            )

            if (notification.attachmentUrl != null) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        .clickable {
                            onOpenAttachment(
                                notification.attachmentUrl,
                                notification.attachmentMimeType ?: "*/*"
                            )
                        }
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.AttachFile,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = notification.attachmentName ?: "View Attachment",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Text(
                        text = stringResource(R.string.view_attachment_btn),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

private fun getFileInfoFromUri(context: Context, uri: Uri): Pair<String?, String?> {
    val mimeType = context.contentResolver.getType(uri)
    var fileName: String? = null
    if (uri.scheme == "content") {
        val cursor = context.contentResolver.query(uri, null, null, null, null)
        cursor?.use {
            if (it.moveToFirst()) {
                val nameIndex = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (nameIndex != -1) {
                    fileName = it.getString(nameIndex)
                }
            }
        }
    }
    if (fileName == null) {
        fileName = uri.path?.substringAfterLast('/')
    }
    return Pair(fileName, mimeType)
}

private fun parseIsoTimestamp(isoString: String): Long {
    val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }
    return try {
        val cleanDate = isoString.substringBefore(".")
        sdf.parse(cleanDate)?.time ?: 0L
    } catch (e: Exception) {
        0L
    }
}

private fun getRelativeTimeSpanString(createdAtIso: String): String {
    val dateMs = parseIsoTimestamp(createdAtIso)
    if (dateMs == 0L) return createdAtIso
    val now = System.currentTimeMillis()
    val diff = now - dateMs
    return when {
        diff < 60000 -> "Just now"
        diff < 3600000 -> "${diff / 60000}m ago"
        diff < 86400000 -> "${diff / 3600000}h ago"
        diff < 172800000 -> "Yesterday"
        else -> SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(dateMs))
    }
}

private fun openAttachment(context: Context, url: String, mimeType: String) {
    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
        setDataAndType(Uri.parse(url), mimeType)
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
    }
    try {
        context.startActivity(intent)
    } catch (e: Exception) {
        // Fallback to simple browser intent if no default viewer
        try {
            val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(browserIntent)
        } catch (ex: Exception) {
            Toast.makeText(context, "No app available to open this link.", Toast.LENGTH_SHORT).show()
        }
    }
}
