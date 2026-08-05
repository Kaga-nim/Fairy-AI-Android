package com.kaganim.fairyai.presentation.features.chat

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.kaganim.fairyai.domain.model.ChatMessage
import com.kaganim.fairyai.domain.model.Participant
import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import android.app.ActivityManager
import android.net.Uri
import android.provider.Settings as AndroidSettings
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.draw.clip
import coil.compose.AsyncImage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    viewModel: ChatViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val listState = rememberLazyListState()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var showPermissionDialog by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val audioGranted = permissions[Manifest.permission.RECORD_AUDIO] ?: false
        val notificationGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions[Manifest.permission.POST_NOTIFICATIONS] ?: false
        } else true

        if (audioGranted && notificationGranted) {
            Toast.makeText(context, "✅ Izin diberikan! Klik Mic lagi untuk memulai.", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, "❌ Izin ditolak! Suara tidak akan berfungsi.", Toast.LENGTH_LONG).show()
        }
    }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        viewModel.onImageSelected(uri)
    }

    LaunchedEffect(Unit) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            permissionLauncher.launch(arrayOf(Manifest.permission.RECORD_AUDIO, Manifest.permission.POST_NOTIFICATIONS))
        }
    }

    if (showPermissionDialog) {
        AlertDialog(
            onDismissRequest = { showPermissionDialog = false },
            icon = { Icon(Icons.Default.Settings, contentDescription = null) },
            title = { Text("Izin Latar Belakang") },
            text = { Text("Agar Fairy bisa merespon kapan saja, silakan ubah izin Mikrofon menjadi 'Selalu Izinkan' di pengaturan.") },
            confirmButton = {
                TextButton(onClick = {
                    showPermissionDialog = false
                    val intent = Intent(AndroidSettings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = Uri.fromParts("package", context.packageName, null)
                    }
                    context.startActivity(intent)
                }) { Text("Pengaturan") }
            },
            dismissButton = { TextButton(onClick = { showPermissionDialog = false }) { Text("Nanti") } }
        )
    }

    // Auto-scroll logic
    LaunchedEffect(state.messages.size, state.isLoading) {
        if (state.messages.isNotEmpty() || state.isLoading) {
            listState.animateScrollToItem(if (state.isLoading) state.messages.size else state.messages.size - 1)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Column {
                        Text("Fairy AI", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(
                            if (state.isVoiceActive) "Voice Listening Active" else "Voice Mode Off",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (state.isVoiceActive) Color(0xFF00FFCC) else Color.Gray
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            val intent = Intent(context, FairyVoiceService::class.java)
                            if (state.isVoiceActive) {
                                context.stopService(intent)
                            } else {
                                val permissionsNeeded = mutableListOf(Manifest.permission.RECORD_AUDIO)
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                    permissionsNeeded.add(Manifest.permission.POST_NOTIFICATIONS)
                                }

                                val allGranted = permissionsNeeded.all {
                                    ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
                                }

                                if (allGranted) {
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) context.startForegroundService(intent)
                                    else context.startService(intent)
                                } else {
                                    permissionLauncher.launch(permissionsNeeded.toTypedArray())
                                }
                            }
                        }
                    ) {
                        Icon(
                            imageVector = if (state.isVoiceActive) Icons.Default.Mic else Icons.Default.MicOff,
                            contentDescription = "Toggle Voice",
                            tint = if (state.isVoiceActive) Color(0xFF00FFCC) else Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Black, titleContentColor = Color.White)
            )
        },
        containerColor = Color(0xFF0F0F0F)
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            LazyColumn(
                state = listState,
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(state.messages) { message ->
                    ChatBubble(message, onRetry = viewModel::retryLastMessage)
                }
                
                if (state.isLoading) {
                    item { ThinkingIndicator() }
                }
            }

            if (state.selectedImageUri != null) {
                Box(
                    modifier = Modifier
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .size(100.dp)
                ) {
                    AsyncImage(
                        model = state.selectedImageUri,
                        contentDescription = "Selected Image",
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(8.dp))
                    )
                    IconButton(
                        onClick = { viewModel.onImageSelected(null) },
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .size(24.dp)
                            .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Remove Image", tint = Color.White, modifier = Modifier.size(16.dp))
                    }
                }
            }

            ChatInput(
                text = state.inputText,
                onTextChange = viewModel::onInputTextChange,
                onSend = viewModel::sendMessage,
                onPickImage = { imagePickerLauncher.launch("image/*") }
            )
        }
    }
}

@Composable
fun ThinkingIndicator() {
    Row(
        modifier = Modifier.padding(start = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(8.dp).background(Color(0xFF00FFCC), CircleShape)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            "Fairy sedang berpikir...",
            style = MaterialTheme.typography.bodySmall,
            color = Color(0xFF00FFCC).copy(alpha = 0.7f)
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ChatBubble(message: ChatMessage, onRetry: () -> Unit) {
    val isUser = message.participant == Participant.USER
    val isError = message.participant == Participant.ERROR
    val context = LocalContext.current
    
    val copyText = {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("Fairy AI Message", message.text)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(context, "Pesan berhasil disalin!", Toast.LENGTH_SHORT).show()
    }
    
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
    ) {
        Surface(
            modifier = Modifier.combinedClickable(
                onClick = {},
                onLongClick = copyText
            ),
            color = when {
                isUser -> Color(0xFF2C2C2E)
                isError -> Color(0xFF442222)
                else -> Color(0xFF1C1C1E)
            },
            contentColor = when {
                isUser -> Color.White
                isError -> Color(0xFFFF6666)
                else -> Color(0xFF00FFCC)
            },
            shape = RoundedCornerShape(
                topStart = 16.dp, topEnd = 16.dp,
                bottomStart = if (isUser) 16.dp else 4.dp,
                bottomEnd = if (isUser) 4.dp else 16.dp
            ),
            tonalElevation = 2.dp,
            border = if (isError) androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFF6666).copy(alpha = 0.5f)) else null
        ) {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
                Text(
                    text = message.text,
                    fontSize = 15.sp,
                    lineHeight = 20.sp
                )
                
                Row(
                    modifier = Modifier.fillMaxWidth(if (isUser) 0f else 1f),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (!isUser && !isError) {
                        IconButton(
                            onClick = copyText,
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ContentCopy,
                                contentDescription = "Copy Message",
                                modifier = Modifier.size(16.dp),
                                tint = Color(0xFF00FFCC).copy(alpha = 0.6f)
                            )
                        }
                    }

                    if (isError) {
                        TextButton(
                            onClick = onRetry,
                            contentPadding = PaddingValues(0.dp),
                            modifier = Modifier.height(32.dp)
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Coba Lagi", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ChatInput(
    text: String,
    onTextChange: (String) -> Unit,
    onSend: () -> Unit,
    onPickImage: () -> Unit
) {
    Surface(
        color = Color.Black,
        tonalElevation = 8.dp,
        border = androidx.compose.foundation.BorderStroke(0.5.dp, Color.White.copy(alpha = 0.1f))
    ) {
        Row(
            modifier = Modifier.padding(12.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onPickImage) {
                Icon(Icons.Default.Image, contentDescription = "Pick Image", tint = Color.Gray)
            }
            Spacer(modifier = Modifier.width(4.dp))
            TextField(
                value = text,
                onValueChange = onTextChange,
                modifier = Modifier.weight(1f),
                placeholder = { Text("Ketik pesan...", color = Color.Gray, fontSize = 14.sp) },
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color(0xFF1C1C1E),
                    unfocusedContainerColor = Color(0xFF1C1C1E),
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    cursorColor = Color(0xFF00FFCC)
                ),
                shape = RoundedCornerShape(24.dp),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(onSend = { onSend() }),
                maxLines = 4
            )
            Spacer(modifier = Modifier.width(8.dp))
            IconButton(
                onClick = onSend,
                enabled = text.isNotBlank(),
                modifier = Modifier
                    .size(48.dp)
                    .background(
                        if (text.isNotBlank()) Color(0xFF00FFCC) else Color.DarkGray,
                        CircleShape
                    ),
                colors = IconButtonDefaults.iconButtonColors(
                    contentColor = Color.Black,
                    disabledContentColor = Color.Gray
                )
            ) {
                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send", modifier = Modifier.size(20.dp))
            }
        }
    }
}
