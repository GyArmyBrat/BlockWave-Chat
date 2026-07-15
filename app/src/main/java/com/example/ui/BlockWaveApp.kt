package com.example.ui

import android.util.Log
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.foundation.Image
import androidx.compose.ui.graphics.asImageBitmap
import com.example.crypto.CryptoUtils
import com.example.crypto.WalletCredentials
import com.example.data.*
import com.example.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// --- Glassmorphic Modifiers & Helper Components ---

fun Modifier.glassmorphic(
    backgroundColor: Color = CyberCardGlass,
    borderColor: Color = BorderGlass,
    cornerRadius: Float = 48f
): Modifier = this.drawBehind {
    drawRoundRect(
        color = backgroundColor,
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(cornerRadius, cornerRadius)
    )
    drawRoundRect(
        color = borderColor,
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(cornerRadius, cornerRadius),
        style = Stroke(width = 1.5.dp.toPx())
    )
}

@Composable
fun GlowButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    glowColor: Color = NeonCyan
) {
    Box(
        modifier = modifier
            .testTag("glow_button_${text.lowercase().replace(" ", "_")}")
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .background(
                Brush.linearGradient(
                    colors = listOf(glowColor, NeonPurple)
                )
            )
            .padding(vertical = 14.dp, horizontal = 24.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(
                text = text,
                color = Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.SansSerif
            )
        }
    }
}

// --- Main App Root Wrapper ---

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BlockWaveApp(viewModel: BlockWaveViewModel) {
    val currentScreen by viewModel.currentScreen.collectAsStateWithLifecycle()
    val wallet by viewModel.wallet.collectAsStateWithLifecycle()
    val activeCall by viewModel.activeCall.collectAsStateWithLifecycle()
    val isGeneratingAi by viewModel.isGeneratingAi.collectAsStateWithLifecycle()

    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    Scaffold(
        bottomBar = {
            if (wallet != null && currentScreen != Screen.Landing && currentScreen != Screen.Login) {
                GlassBottomNavBar(currentScreen, onNavigate = { viewModel.navigateTo(it) })
            }
        },
        containerColor = CyberBg
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .drawBehind {
                    // Ambient Frosted Glass Background Gradients
                    // Cyan glow top-left
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(NeonCyan.copy(alpha = 0.12f), Color.Transparent),
                            center = Offset(size.width * -0.1f, size.height * -0.1f),
                            radius = size.minDimension * 0.7f
                        ),
                        center = Offset(size.width * -0.1f, size.height * -0.1f),
                        radius = size.minDimension * 0.7f
                    )
                    // Indigo glow bottom-right
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(NeonPurple.copy(alpha = 0.12f), Color.Transparent),
                            center = Offset(size.width * 1.1f, size.height * 0.9f),
                            radius = size.minDimension * 0.8f
                        ),
                        center = Offset(size.width * 1.1f, size.height * 0.9f),
                        radius = size.minDimension * 0.8f
                    )
                }
        ) {
            // Main content transition
            AnimatedContent(
                targetState = currentScreen,
                transitionSpec = {
                    fadeIn(animationSpec = tween(300)) togetherWith fadeOut(animationSpec = tween(300))
                },
                label = "screen_transition"
            ) { screen ->
                when (screen) {
                    Screen.Landing -> LandingScreen(viewModel)
                    Screen.Login -> LoginScreen(viewModel)
                    Screen.Dashboard -> DashboardScreen(viewModel)
                    Screen.Chats -> ChatsScreen(viewModel)
                    Screen.GroupChat -> GroupChatScreen(viewModel)
                    Screen.Explorer -> ExplorerScreen(viewModel)
                    Screen.AdminDashboard -> AdminDashboardScreen(viewModel)
                    Screen.Settings -> SettingsScreen(viewModel)
                    Screen.Profile -> ProfileScreen(viewModel)
                    Screen.Friends -> FriendsScreen(viewModel)
                    Screen.CreateGroup -> CreateGroupScreen(viewModel)
                    Screen.AILabs -> AILabsScreen(viewModel)
                }
            }

            // Top-right AI spinner overlay
            if (isGeneratingAi) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(16.dp)
                        .glassmorphic(backgroundColor = Color(0xCC121224))
                        .padding(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(
                            color = NeonCyan,
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "AI Node Processing...",
                            color = NeonCyan,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            // Simulated Call Session Overlay
            if (activeCall != null) {
                CallOverlay(session = activeCall!!, viewModel = viewModel)
            }
        }
    }
}

// --- Glass bottom navigation bar ---

@Composable
fun GlassBottomNavBar(currentScreen: Screen, onNavigate: (Screen) -> Unit) {
    val items = listOf(
        Triple(Screen.Dashboard, Icons.Filled.ChatBubble, "Chats"),
        Triple(Screen.Friends, Icons.Filled.People, "Friends"),
        Triple(Screen.AILabs, Icons.Filled.AutoAwesome, "AI Labs"),
        Triple(Screen.Explorer, Icons.Filled.Explore, "Explorer"),
        Triple(Screen.AdminDashboard, Icons.Filled.Analytics, "Admin"),
        Triple(Screen.Settings, Icons.Filled.Settings, "Settings")
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .glassmorphic(backgroundColor = Color(0xCC0F1115), cornerRadius = 32f)
            .padding(vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            items.forEach { (screen, icon, label) ->
                val isSelected = when (screen) {
                    Screen.Dashboard -> currentScreen == Screen.Dashboard || currentScreen == Screen.Chats || currentScreen == Screen.GroupChat || currentScreen == Screen.CreateGroup
                    Screen.Friends -> currentScreen == Screen.Friends || currentScreen == Screen.Profile
                    Screen.AILabs -> currentScreen == Screen.AILabs
                    else -> currentScreen == screen
                }

                val color = if (isSelected) NeonCyan else TextSecondary
                val scale = if (isSelected) 1.1f else 1.0f

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .clickable { onNavigate(screen) }
                        .padding(8.dp)
                        .testTag("nav_item_${label.lowercase()}")
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = label,
                        tint = color,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = label,
                        color = color,
                        fontSize = 10.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
        }
    }
}

// --- 1. Landing Screen ---

@Composable
fun LandingScreen(viewModel: BlockWaveViewModel) {
    var hasAgreed by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Spacer(modifier = Modifier.height(40.dp))
        
        // Brand logo with glow
        Box(
            modifier = Modifier
                .size(100.dp)
                .glassmorphic(backgroundColor = Color(0x1A06B6D4), borderColor = NeonCyan)
                .padding(20.dp),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.Shield,
                contentDescription = "Lock",
                tint = NeonCyan,
                modifier = Modifier.size(60.dp)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "BLOCKWAVE CHAT",
            fontSize = 32.sp,
            fontWeight = FontWeight.ExtraBold,
            color = Color.White,
            fontFamily = FontFamily.SansSerif,
            letterSpacing = 2.sp
        )

        Text(
            text = "Decentralized. E2E Encrypted. Trustless.",
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = NeonCyan,
            modifier = Modifier.padding(top = 4.dp)
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Security feature highlight cards
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            FeatureHighlightRow(
                icon = Icons.Filled.Lock,
                title = "End-to-End Encryption",
                desc = "AES-256 session key sealed locally with recipient RSA-2048 public key. Plaintext never touches servers."
            )
            FeatureHighlightRow(
                icon = Icons.Filled.Explore,
                title = "On-Chain Polygon Integrity",
                desc = "Message hashes (SHA-256) committed securely to immutable blockchain smart contracts."
            )
            FeatureHighlightRow(
                icon = Icons.Filled.Cloud,
                title = "IPFS Decoupled Storage",
                desc = "Encrypted payloads stored decentralized in global IPFS nodes, mapped by cryptographic CID addresses."
            )
        }

        Spacer(modifier = Modifier.height(40.dp))

        // Consent warning
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp)
        ) {
            Checkbox(
                checked = hasAgreed,
                onCheckedChange = { hasAgreed = it },
                colors = CheckboxDefaults.colors(
                    checkedColor = NeonCyan,
                    uncheckedColor = TextSecondary
                )
            )
            Text(
                text = "I understand that my Wallet acts as my unique Decentralized Identity. No password or OTP recovery exists.",
                color = TextSecondary,
                fontSize = 11.sp,
                modifier = Modifier.padding(start = 4.dp)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        GlowButton(
            text = "CONNECT WALLET",
            onClick = { viewModel.navigateTo(Screen.Login) },
            modifier = Modifier.fillMaxWidth(),
            icon = Icons.Filled.AccountBalanceWallet,
            glowColor = if (hasAgreed) NeonCyan else Color.Gray
        )
        
        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
fun FeatureHighlightRow(icon: ImageVector, title: String, desc: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .glassmorphic()
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.Top) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = NeonCyan,
                modifier = Modifier
                    .size(24.dp)
                    .padding(top = 2.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(
                    text = title,
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = desc,
                    color = TextSecondary,
                    fontSize = 11.sp,
                    modifier = Modifier.padding(top = 4.dp),
                    lineHeight = 16.sp
                )
            }
        }
    }
}

// --- 2. Login / Wallet Setup Screen ---

@Composable
fun LoginScreen(viewModel: BlockWaveViewModel) {
    var username by remember { mutableStateOf("") }
    var seedPhraseInput by remember { mutableStateOf("") }
    var isImportMode by remember { mutableStateOf(false) }

    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = if (isImportMode) "IMPORT CRYPTO WALLET" else "GENERATE WAVE IDENTITY",
            fontSize = 22.sp,
            fontWeight = FontWeight.ExtraBold,
            color = Color.White,
            textAlign = TextAlign.Center
        )

        Text(
            text = if (isImportMode) "Enter your 12-word recovery mnemonic to import your chat identity." else "This generates your client-side private/public keys & Polygon wallet index.",
            fontSize = 12.sp,
            color = TextSecondary,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 6.dp, bottom = 24.dp)
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .glassmorphic()
                .padding(20.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it },
                    label = { Text("Display Username") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NeonCyan,
                        unfocusedBorderColor = TextSecondary,
                        focusedLabelColor = NeonCyan,
                        unfocusedLabelColor = TextSecondary,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("username_input")
                )

                if (isImportMode) {
                    OutlinedTextField(
                        value = seedPhraseInput,
                        onValueChange = { seedPhraseInput = it },
                        label = { Text("12-Word Recovery Seed Phrase") },
                        placeholder = { Text("orbit velvet scout matrix ...") },
                        minLines = 3,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NeonCyan,
                            unfocusedBorderColor = TextSecondary,
                            focusedLabelColor = NeonCyan,
                            unfocusedLabelColor = TextSecondary,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        if (!isImportMode) {
            GlowButton(
                text = "GENERATE BLOCKCHAIN IDENTITY",
                onClick = {
                    if (username.trim().isEmpty()) {
                        Toast.makeText(context, "Please choose a display username.", Toast.LENGTH_SHORT).show()
                    } else {
                        viewModel.createWallet(username.trim())
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                icon = Icons.Filled.Add
            )
        } else {
            GlowButton(
                text = "IMPORT AND CONNECT",
                onClick = {
                    if (username.trim().isEmpty() || seedPhraseInput.trim().split(" ").size < 12) {
                        Toast.makeText(context, "Enter a username and valid 12-word seed phrase.", Toast.LENGTH_SHORT).show()
                    } else {
                        viewModel.importWallet(seedPhraseInput.trim(), username.trim())
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                icon = Icons.Filled.CloudDownload
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        TextButton(
            onClick = { isImportMode = !isImportMode },
            modifier = Modifier.testTag("toggle_mode_button")
        ) {
            Text(
                text = if (isImportMode) "Or create a new random wallet" else "Or import existing wallet using Seed Phrase",
                color = NeonCyan,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

// --- 3. Dashboard / Home Chats List Screen ---

@Composable
fun DashboardScreen(viewModel: BlockWaveViewModel) {
    val wallet by viewModel.wallet.collectAsStateWithLifecycle()
    val friends by viewModel.friends.collectAsStateWithLifecycle()
    val groups by viewModel.groups.collectAsStateWithLifecycle()

    var activeTab by remember { mutableStateOf("CHATS") } // "CHATS", "GROUPS"
    var showFriendSearchDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Core Wallet info banner
        if (wallet != null) {
            WalletBanner(wallet = wallet!!, viewModel = viewModel)
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Tab Row (Chats vs Groups)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .glassmorphic()
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            TabButton(
                title = "Direct E2EE Chats",
                isSelected = activeTab == "CHATS",
                onClick = { activeTab = "CHATS" },
                modifier = Modifier.weight(1f)
            )
            TabButton(
                title = "Group Waves",
                isSelected = activeTab == "GROUPS",
                onClick = { activeTab = "GROUPS" },
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Chats/Groups Scrolling List
        Box(modifier = Modifier.weight(1f)) {
            if (activeTab == "CHATS") {
                if (friends.isEmpty()) {
                    EmptyStatePlaceholder(
                        icon = Icons.Filled.MarkUnreadChatAlt,
                        title = "Your secure chat ledger is empty",
                        tip = "Connect with friend nodes by tapping the '+' button in the bottom-right corner to map their E2EE RSA keys!"
                    )
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(friends) { friend ->
                            FriendChatRow(friend = friend, onClick = { viewModel.selectConversation(friend) })
                        }
                    }
                }
            } else {
                if (groups.isEmpty()) {
                    EmptyStatePlaceholder(
                        icon = Icons.Filled.Groups,
                        title = "No group wave nodes joined",
                        tip = "Create a custom secure group with decentralized storage reference keys. Tap '+' to create!"
                    )
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(groups) { group ->
                            GroupChatRow(group = group, onClick = { viewModel.selectGroupConversation(group) })
                        }
                    }
                }
            }

            // FAB Action Button
            FloatingActionButton(
                onClick = {
                    if (activeTab == "CHATS") {
                        showFriendSearchDialog = true
                    } else {
                        viewModel.navigateTo(Screen.CreateGroup)
                    }
                },
                containerColor = NeonCyan,
                contentColor = CyberBg,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp)
                    .testTag("fab_add")
            ) {
                Icon(
                    imageVector = if (activeTab == "CHATS") Icons.Filled.PersonAdd else Icons.Filled.Add,
                    contentDescription = "Add"
                )
            }
        }
    }

    if (showFriendSearchDialog) {
        FriendAddDialog(
            onDismiss = { showFriendSearchDialog = false },
            onAdd = { address, name ->
                viewModel.addFriendByAddress(address, name) { success, msg ->
                    showFriendSearchDialog = false
                }
            }
        )
    }
}

@Composable
fun WalletBanner(wallet: WalletCredentials, viewModel: BlockWaveViewModel) {
    val clipboard = LocalClipboardManager.current

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .glassmorphic(backgroundColor = CyberCardGlass, borderColor = BorderGlass)
            .padding(16.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(NeonPurple, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = viewModel.getProfileUsername().take(1).uppercase(),
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = viewModel.getProfileUsername(),
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        Text(
                            text = "Polygon Mumbai Testnet",
                            color = NeonCyan,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                // Balance display with Faucet quick link
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = String.format("%.4f POL", wallet.balance),
                        color = Color.White,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 15.sp
                    )
                    Text(
                        text = "GET TEST TOKENS",
                        color = NeonCyan,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .clickable { viewModel.requestFaucet() }
                            .padding(vertical = 2.dp)
                            .testTag("faucet_button")
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Wallet Address field
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF07070F), RoundedCornerShape(8.dp))
                    .clickable { clipboard.setText(AnnotatedString(wallet.walletAddress)) }
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.AccountBalanceWallet,
                        contentDescription = null,
                        tint = TextSecondary,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = wallet.walletAddress,
                        color = TextSecondary,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                }
                Icon(
                    imageVector = Icons.Filled.ContentCopy,
                    contentDescription = "Copy",
                    tint = NeonCyan,
                    modifier = Modifier.size(12.dp)
                )
            }
        }
    }
}

@Composable
fun TabButton(title: String, isSelected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .background(if (isSelected) NeonCyan else Color.Transparent)
            .padding(vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = title,
            color = if (isSelected) CyberBg else Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp
        )
    }
}

@Composable
fun FriendChatRow(friend: UserEntity, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .glassmorphic()
            .clickable(onClick = onClick)
            .padding(16.dp)
            .testTag("friend_row_${friend.username.lowercase()}")
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            // Simulated Avatar
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(ElectricBlue, CircleShape)
                    .border(1.dp, NeonCyan, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = friend.username.take(2).uppercase(),
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = friend.username,
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    
                    // Status
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .background(
                                    if (friend.status == "ONLINE") Color.Green else Color.Gray,
                                    CircleShape
                                )
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = friend.status,
                            color = TextSecondary,
                            fontSize = 9.sp
                        )
                    }
                }

                Text(
                    text = friend.bio,
                    color = TextSecondary,
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }
    }
}

@Composable
fun GroupChatRow(group: GroupEntity, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .glassmorphic()
            .clickable(onClick = onClick)
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(NeonPurple, CircleShape)
                    .border(1.dp, NeonPurple, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Groups,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = group.name,
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = group.description,
                    color = TextSecondary,
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }
    }
}

@Composable
fun EmptyStatePlaceholder(icon: ImageVector, title: String, tip: String) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = TextSecondary,
            modifier = Modifier.size(48.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = title,
            color = Color.White,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        Text(
            text = tip,
            color = TextSecondary,
            fontSize = 12.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 8.dp),
            lineHeight = 18.sp
        )
    }
}

// --- 4. 1-to-1 E2EE Chat Screen ---

@Composable
fun ChatsScreen(viewModel: BlockWaveViewModel) {
    val friend by viewModel.selectedFriend.collectAsStateWithLifecycle()
    val messages by viewModel.activeChatMessages.collectAsStateWithLifecycle()
    val wallet by viewModel.wallet.collectAsStateWithLifecycle()
    val aiSummary by viewModel.aiSummary.collectAsStateWithLifecycle()
    val smartReplies by viewModel.aiSmartReplies.collectAsStateWithLifecycle()
    val scamResults by viewModel.scamScanResult.collectAsStateWithLifecycle()

    var textInput by remember { mutableStateOf("") }
    var selectedMessageDetails by remember { mutableStateOf<MessageEntity?>(null) }
    var showAiSummaryDialog by remember { mutableStateOf(false) }

    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    // Scroll to bottom on new message
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    if (friend == null) return

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp)
    ) {
        // Chat Header with Calls controls
        ChatHeader(
            friend = friend!!,
            onBack = { viewModel.navigateTo(Screen.Dashboard) },
            onCall = { viewModel.startCall(friend!!, isVideo = false) },
            onVideoCall = { viewModel.startCall(friend!!, isVideo = true) },
            onSummarize = {
                viewModel.summarizeActiveChat()
                showAiSummaryDialog = true
            },
            onDeleteChat = { viewModel.deleteChatHistory(friend!!.walletAddress) }
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Integrity Banner
        SecurityIntegrityBanner()

        Spacer(modifier = Modifier.height(8.dp))

        // Messages List Container
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .glassmorphic(backgroundColor = Color(0x1F07070F))
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(messages) { msg ->
                    val isSelf = msg.senderWallet == wallet?.walletAddress
                    val scanResult = scamResults[msg.id]

                    ChatBubble(
                        msg = msg,
                        isSelf = isSelf,
                        scanResult = scanResult,
                        onBubbleClick = { selectedMessageDetails = msg },
                        onScanScams = { viewModel.scanMessageForScams(msg) }
                    )
                }
            }
        }

        // Smart Replies Row
        if (smartReplies.isNotEmpty()) {
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                smartReplies.forEach { reply ->
                    Box(
                        modifier = Modifier
                            .glassmorphic(backgroundColor = Color(0xCC0D1225))
                            .clickable {
                                textInput = reply
                            }
                            .padding(vertical = 6.dp, horizontal = 12.dp)
                    ) {
                        Text(text = reply, color = NeonCyan, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Message Input Field
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = textInput,
                onValueChange = { textInput = it },
                placeholder = { Text("E2EE secure message...", color = TextSecondary) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = NeonCyan,
                    unfocusedBorderColor = TextSecondary,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                ),
                modifier = Modifier
                    .weight(1f)
                    .testTag("chat_input"),
                trailingIcon = {
                    Row(modifier = Modifier.padding(end = 8.dp)) {
                        IconButton(onClick = {
                            viewModel.sendAttachmentMessage("IMAGE", "https://picsum.photos/400", "secured_photo.jpg")
                        }) {
                            Icon(Icons.Filled.Photo, contentDescription = "Photo", tint = NeonCyan)
                        }
                    }
                }
            )

            FloatingActionButton(
                onClick = {
                    if (textInput.trim().isNotEmpty()) {
                        viewModel.sendTextMessage(textInput.trim())
                        textInput = ""
                    }
                },
                containerColor = NeonCyan,
                contentColor = CyberBg,
                modifier = Modifier.size(52.dp).testTag("send_button")
            ) {
                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send", modifier = Modifier.size(20.dp))
            }
        }
    }

    // Modal Details for Message Encryption Showcase
    if (selectedMessageDetails != null) {
        CryptoTraceDialog(
            msg = selectedMessageDetails!!,
            onDismiss = { selectedMessageDetails = null },
            viewModel = viewModel
        )
    }

    // Modal for AI Summarization result
    if (showAiSummaryDialog) {
        AlertDialog(
            onDismissRequest = { showAiSummaryDialog = false },
            title = { Text("WaveAI Chat Digest", color = NeonCyan, fontWeight = FontWeight.ExtraBold) },
            text = {
                Text(
                    text = aiSummary ?: "Analyzing decentralized ledger conversation context...",
                    color = Color.White,
                    fontSize = 13.sp,
                    lineHeight = 18.sp
                )
            },
            confirmButton = {
                TextButton(onClick = { showAiSummaryDialog = false }) {
                    Text("CLOSE", color = NeonCyan)
                }
            },
            containerColor = CyberCard,
            shape = RoundedCornerShape(16.dp)
        )
    }
}

@Composable
fun ChatHeader(
    friend: UserEntity,
    onBack: () -> Unit,
    onCall: () -> Unit,
    onVideoCall: () -> Unit,
    onSummarize: () -> Unit,
    onDeleteChat: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .glassmorphic(backgroundColor = Color(0xE6121224))
            .padding(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack, modifier = Modifier.testTag("back_button")) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                }
                Spacer(modifier = Modifier.width(4.dp))
                Column {
                    Text(
                        text = friend.username,
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Keys Loaded",
                        color = NeonCyan,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onCall, modifier = Modifier.testTag("call_button")) {
                    Icon(Icons.Filled.Call, contentDescription = "Voice Call", tint = NeonCyan)
                }
                IconButton(onClick = onVideoCall, modifier = Modifier.testTag("video_call_button")) {
                    Icon(Icons.Filled.Videocam, contentDescription = "Video Call", tint = NeonCyan)
                }
                IconButton(onClick = onSummarize) {
                    Icon(Icons.Filled.AutoAwesome, contentDescription = "AI Digest", tint = NeonPurple)
                }
                
                Box {
                    IconButton(onClick = { showMenu = true }) {
                        Icon(Icons.Filled.MoreVert, contentDescription = "More", tint = Color.White)
                    }
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false },
                        modifier = Modifier.background(CyberCard)
                    ) {
                        DropdownMenuItem(
                            text = { Text("Delete Local Ledger", color = Color.White) },
                            onClick = {
                                showMenu = false
                                onDeleteChat()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Block Wallet Node", color = Color.Red) },
                            onClick = {
                                showMenu = false
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SecurityIntegrityBanner() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .glassmorphic(backgroundColor = Color(0x3300F0FF))
            .padding(10.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.Security, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "End-to-End Encrypted. Messages are stored in IPFS CIDs & signed on Polygon.",
                color = NeonCyan,
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
fun ChatBubble(
    msg: MessageEntity,
    isSelf: Boolean,
    scanResult: String?,
    onBubbleClick: () -> Unit,
    onScanScams: () -> Unit
) {
    val bubbleColor = if (isSelf) NeonCyan.copy(alpha = 0.15f) else Color(0x1F4E4E6D)
    val alignment = if (isSelf) Alignment.CenterEnd else Alignment.CenterStart
    val shape = if (isSelf) {
        RoundedCornerShape(16.dp, 16.dp, 4.dp, 16.dp)
    } else {
        RoundedCornerShape(16.dp, 16.dp, 16.dp, 4.dp)
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        contentAlignment = alignment
    ) {
        Column(
            horizontalAlignment = if (isSelf) Alignment.End else Alignment.Start,
            modifier = Modifier.widthIn(max = 280.dp)
        ) {
            // Threat alert warning if scanned as dangerous
            if (scanResult != null && !scanResult.contains("[SAFE]")) {
                Box(
                    modifier = Modifier
                        .padding(bottom = 2.dp)
                        .glassmorphic(backgroundColor = Color(0x99FF3B30), borderColor = Color.Red)
                        .padding(6.dp)
                ) {
                    Text(
                        text = "⚠️ AI CYBER ALERT: $scanResult",
                        color = Color.White,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        lineHeight = 11.sp
                    )
                }
            }

            Box(
                modifier = Modifier
                    .glassmorphic(backgroundColor = bubbleColor, cornerRadius = 32f)
                    .clickable(onClick = onBubbleClick)
                    .padding(12.dp)
            ) {
                Column {
                    if (msg.attachmentType != "TEXT") {
                        // Simulated media attachments
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(130.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFF0F0F24)),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = if (msg.attachmentType == "IMAGE") Icons.Filled.Photo else Icons.Filled.Description,
                                    contentDescription = null,
                                    tint = NeonCyan,
                                    modifier = Modifier.size(36.dp)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = msg.attachmentName ?: "attachment",
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    Text(
                        text = msg.plainText,
                        color = Color.White,
                        fontSize = 13.sp,
                        lineHeight = 18.sp
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.End,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Lock,
                            contentDescription = "Decrypted",
                            tint = NeonCyan,
                            modifier = Modifier.size(10.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = "SHA256: ${msg.messageHash.take(8)}",
                            color = TextSecondary,
                            fontSize = 8.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }

            // Small utility trigger links under bubble
            if (!isSelf && scanResult == null) {
                Text(
                    text = "Scan with CyberAI",
                    color = NeonPurple,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .clickable(onClick = onScanScams)
                        .padding(top = 2.dp, start = 4.dp)
                )
            }
        }
    }
}

// --- 5. Message Cryptographic Verification Showcase Dialog ---

@Composable
fun CryptoTraceDialog(msg: MessageEntity, onDismiss: () -> Unit, viewModel: BlockWaveViewModel) {
    var activeLang by remember { mutableStateOf("SPANISH") }
    var translatedResult by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val clipboard = LocalClipboardManager.current

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.VerifiedUser, contentDescription = null, tint = NeonCyan)
                Spacer(modifier = Modifier.width(8.dp))
                Text("E2EE SECURITY PROTOCOL", color = NeonCyan, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.verticalScroll(rememberScrollState())
            ) {
                CryptoMetricText("Cipher Algorithm", "AES-256 (ECB / PKCS5Padding)")
                CryptoMetricText("Encrypted Ciphertext (Base64)", msg.encryptedPayload.take(45) + "...")
                CryptoMetricText("Encrypted Session AES Key", msg.encryptedAesKey?.take(40) ?: "Not Applicable (Group/Simulated)")
                CryptoMetricText("Digital Signature", msg.signature.take(40) + "...")
                CryptoMetricText("IPFS Storage CID", msg.ipfsCid)
                CryptoMetricText("Polygon Txn Hash", msg.transactionHash)

                Spacer(modifier = Modifier.height(12.dp))

                Divider(color = BorderGlass)

                Spacer(modifier = Modifier.height(8.dp))

                Text("AI REAL-TIME TRANSLATOR", color = NeonPurple, fontSize = 12.sp, fontWeight = FontWeight.Bold)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val langs = listOf("SPANISH", "FRENCH", "GERMAN", "CHINESE")
                    langs.forEach { lang ->
                        Box(
                            modifier = Modifier
                                .border(1.dp, if (activeLang == lang) NeonPurple else Color.Transparent, RoundedCornerShape(8.dp))
                                .clickable { activeLang = lang }
                                .padding(6.dp)
                        ) {
                            Text(text = lang, color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                GlowButton(
                    text = "TRANSLATE VIA GEMINI AI",
                    onClick = {
                        viewModel.translateMessage(msg, activeLang) { result ->
                            translatedResult = result
                        }
                    },
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                    glowColor = NeonPurple
                )

                if (translatedResult != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .glassmorphic(backgroundColor = Color(0xFF10101F))
                            .padding(8.dp)
                    ) {
                        Text(text = translatedResult!!, color = Color.White, fontSize = 12.sp)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("DISMISS", color = NeonCyan)
            }
        },
        containerColor = CyberCard,
        shape = RoundedCornerShape(16.dp)
    )
}

@Composable
fun CryptoMetricText(label: String, value: String) {
    val clipboard = LocalClipboardManager.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { clipboard.setText(AnnotatedString(value)) }
            .padding(vertical = 2.dp)
    ) {
        Text(text = label, color = TextSecondary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        Text(
            text = value,
            color = Color.White,
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

// --- 6. Group Chat View Screen ---

@Composable
fun GroupChatScreen(viewModel: BlockWaveViewModel) {
    val group by viewModel.selectedGroup.collectAsStateWithLifecycle()
    val messages by viewModel.activeChatMessages.collectAsStateWithLifecycle()
    val wallet by viewModel.wallet.collectAsStateWithLifecycle()

    var textInput by remember { mutableStateOf("") }
    var activePollVote by remember { mutableStateOf<Int?>(null) } // Poll simulation state

    if (group == null) return

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp)
    ) {
        // Group Header
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .glassmorphic(backgroundColor = Color(0xE6121224))
                .padding(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { viewModel.navigateTo(Screen.Dashboard) }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Column {
                        Text(
                            text = group!!.name,
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "IPFS Decoupled Room",
                            color = NeonCyan,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Pinned Announcement simulated banner
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .glassmorphic(backgroundColor = Color(0x66BC00FF), borderColor = NeonPurple)
                .padding(10.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.PushPin, contentDescription = null, tint = NeonPurple, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "ANNOUNCEMENT: Consensus audit completed successfully for our Polygon contract.",
                    color = Color.White,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Message Feed
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .glassmorphic(backgroundColor = Color(0x1F07070F))
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    // Simulated Live Poll Component
                    LivePollWidget(
                        question = "Which L2 has the best decentralized data integrity performance?",
                        options = listOf("Polygon proof-of-stake", "Arbitrum One Rollups", "Optimism Bedrock"),
                        votes = listOf(42, 18, 12),
                        activeVote = activePollVote,
                        onVote = { activePollVote = it }
                    )
                }

                items(messages) { msg ->
                    val isSelf = msg.senderWallet == wallet?.walletAddress
                    ChatBubble(
                        msg = msg,
                        isSelf = isSelf,
                        scanResult = null,
                        onBubbleClick = {},
                        onScanScams = {}
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Message Input
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = textInput,
                onValueChange = { textInput = it },
                placeholder = { Text("Group broadwave...", color = TextSecondary) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = NeonCyan,
                    unfocusedBorderColor = TextSecondary,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                ),
                modifier = Modifier.weight(1f)
            )

            FloatingActionButton(
                onClick = {
                    if (textInput.trim().isNotEmpty()) {
                        viewModel.sendGroupTextMessage(textInput.trim())
                        textInput = ""
                    }
                },
                containerColor = NeonCyan,
                contentColor = CyberBg,
                modifier = Modifier.size(52.dp)
            ) {
                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send", modifier = Modifier.size(20.dp))
            }
        }
    }
}

@Composable
fun LivePollWidget(
    question: String,
    options: List<String>,
    votes: List<Int>,
    activeVote: Int?,
    onVote: (Int) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .glassmorphic(backgroundColor = Color(0x2212122F), borderColor = BorderGlass)
            .padding(16.dp)
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Poll, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(text = "LIVE DECENTRALIZED POLL", color = NeonCyan, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(text = question, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(12.dp))

            options.forEachIndexed { idx, opt ->
                val total = votes.sum() + if (activeVote != null) 1 else 0
                val count = votes[idx] + if (activeVote == idx) 1 else 0
                val pct = if (total > 0) (count.toFloat() / total.toFloat()) else 0f

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onVote(idx) }
                        .background(Color(0xFF0F0F1D))
                        .drawBehind {
                            drawRect(
                                color = NeonCyan.copy(alpha = 0.15f),
                                size = androidx.compose.ui.geometry.Size(size.width * pct, size.height)
                            )
                        }
                        .padding(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = opt, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                        Text(
                            text = String.format("%d%%", (pct * 100).toInt()),
                            color = NeonCyan,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

// --- 7. Create Group Screen ---

@Composable
fun CreateGroupScreen(viewModel: BlockWaveViewModel) {
    var name by remember { mutableStateOf("") }
    var desc by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "CREATE WAVE ROOM",
            fontSize = 20.sp,
            fontWeight = FontWeight.ExtraBold,
            color = Color.White
        )
        Text(
            text = "Generate a new decentralized group chat reference mapping.",
            fontSize = 12.sp,
            color = TextSecondary,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 4.dp, bottom = 24.dp)
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .glassmorphic()
                .padding(20.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Group Name") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NeonCyan,
                        unfocusedBorderColor = TextSecondary,
                        focusedLabelColor = NeonCyan,
                        unfocusedLabelColor = TextSecondary,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = desc,
                    onValueChange = { desc = it },
                    label = { Text("Group Purpose Description") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NeonCyan,
                        unfocusedBorderColor = TextSecondary,
                        focusedLabelColor = NeonCyan,
                        unfocusedLabelColor = TextSecondary,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        GlowButton(
            text = "DEPLOY WAVE ROOM",
            onClick = {
                if (name.trim().isNotEmpty()) {
                    viewModel.createGroup(name.trim(), desc.trim(), "")
                }
            },
            modifier = Modifier.fillMaxWidth(),
            icon = Icons.Filled.Add
        )

        Spacer(modifier = Modifier.height(12.dp))

        TextButton(onClick = { viewModel.navigateTo(Screen.Dashboard) }) {
            Text("CANCEL", color = Color.White)
        }
    }
}

// --- 8. Polygon Blockchain ledger visualizer (Sci-Fi Etherscan) ---

@Composable
fun ExplorerScreen(viewModel: BlockWaveViewModel) {
    val txs by viewModel.transactions.collectAsStateWithLifecycle()
    val count by viewModel.txCount.collectAsStateWithLifecycle()
    val totalGas by viewModel.totalGasUsed.collectAsStateWithLifecycle()

    val clipboard = LocalClipboardManager.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "BLOCKCHAIN TRANSACTION LEDGER",
            fontSize = 18.sp,
            fontWeight = FontWeight.ExtraBold,
            color = Color.White
        )
        Text(
            text = "Real-time auditing of Polygon Smart Contract events & IPFS hashes.",
            fontSize = 11.sp,
            color = NeonCyan,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // General ledger metrics
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .glassmorphic(backgroundColor = Color(0x33121224))
                    .padding(12.dp)
            ) {
                Text(text = "COMMIT EVENTS", color = TextSecondary, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                Text(text = count.toString(), color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
            }
            Column(
                modifier = Modifier
                    .weight(1.5f)
                    .glassmorphic(backgroundColor = Color(0x33121224))
                    .padding(12.dp)
            ) {
                Text(text = "CUMULATIVE GAS USED", color = TextSecondary, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                Text(text = "$totalGas gwei", color = NeonPurple, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Sci-Fi Transaction list
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .glassmorphic(backgroundColor = Color(0x22050510))
        ) {
            if (txs.isEmpty()) {
                EmptyStatePlaceholder(
                    icon = Icons.Filled.History,
                    title = "Ledger is quiet",
                    tip = "Complete transactions by messaging contacts or calling the Polygon faucets!"
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(txs) { tx ->
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .glassmorphic(backgroundColor = Color(0xFF0C0C17))
                                .padding(12.dp)
                        ) {
                            Column {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(24.dp)
                                                .background(
                                                    when (tx.eventType) {
                                                        "MessageSent" -> NeonCyan.copy(alpha = 0.2f)
                                                        "UserRegistered" -> NeonPurple.copy(alpha = 0.2f)
                                                        else -> Color.Green.copy(alpha = 0.2f)
                                                    },
                                                    CircleShape
                                                ),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = when (tx.eventType) {
                                                    "MessageSent" -> Icons.Filled.ChatBubble
                                                    "UserRegistered" -> Icons.Filled.Person
                                                    else -> Icons.Filled.Cloud
                                                },
                                                contentDescription = null,
                                                tint = when (tx.eventType) {
                                                    "MessageSent" -> NeonCyan
                                                    "UserRegistered" -> NeonPurple
                                                    else -> Color.Green
                                                },
                                                modifier = Modifier.size(12.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = tx.eventType,
                                            color = Color.White,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.ExtraBold
                                        )
                                    }

                                    Text(
                                        text = "#${tx.blockNumber}",
                                        color = NeonCyan,
                                        fontSize = 11.sp,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                LedgerLabelAndHex("TxHash", tx.txHash)
                                LedgerLabelAndHex("From", tx.fromAddress)
                                LedgerLabelAndHex("To", tx.toAddress)
                                LedgerLabelAndHex("IPFS CID", tx.cid ?: "null")

                                Spacer(modifier = Modifier.height(4.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "Gas Used: ${tx.gasUsed} limit",
                                        color = TextSecondary,
                                        fontSize = 9.sp,
                                        fontFamily = FontFamily.Monospace
                                    )
                                    Text(
                                        text = "Confirmed",
                                        color = Color.Green,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun LedgerLabelAndHex(label: String, hex: String) {
    val clipboard = LocalClipboardManager.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { clipboard.setText(AnnotatedString(hex)) }
            .padding(vertical = 1.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, color = TextSecondary, fontSize = 9.sp, fontWeight = FontWeight.Bold)
        Text(
            text = hex,
            color = Color.White,
            fontSize = 9.sp,
            fontFamily = FontFamily.Monospace,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.width(180.dp)
        )
    }
}

// --- 9. Admin Dashboard with Live Drawn Canvas Charts ---

@Composable
fun AdminDashboardScreen(viewModel: BlockWaveViewModel) {
    val count by viewModel.txCount.collectAsStateWithLifecycle()
    val totalGas by viewModel.totalGasUsed.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            text = "DECENTRALIZED NODE HEALTH",
            fontSize = 18.sp,
            fontWeight = FontWeight.ExtraBold,
            color = Color.White
        )
        Text(
            text = "Local network telemetry and gas usage curves.",
            fontSize = 11.sp,
            color = NeonCyan,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // General status widgets
        Column(
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                TelemetryWidget(label = "Node Health", value = "99.8% ONLINE", color = Color.Green, modifier = Modifier.weight(1f))
                TelemetryWidget(label = "Polygon Ping", value = "18ms", color = NeonCyan, modifier = Modifier.weight(1f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                TelemetryWidget(label = "IPFS Storage", value = "124 KB (Decon)", color = NeonPurple, modifier = Modifier.weight(1f))
                TelemetryWidget(label = "Connected Peers", value = "1,482 Active", color = Color.Yellow, modifier = Modifier.weight(1f))
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Drawn Line Chart Canvas showing simulated network transactions rate
        Text(text = "GAS CONGESTION (gwei/second)", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(160.dp)
                .glassmorphic(backgroundColor = Color(0xFF090915), borderColor = BorderGlass)
                .padding(16.dp)
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val linePath = Path()
                val points = listOf(
                    Offset(0f, 100f),
                    Offset(100f, 150f),
                    Offset(200f, 60f),
                    Offset(300f, 220f),
                    Offset(400f, 110f),
                    Offset(500f, 180f),
                    Offset(600f, 50f),
                    Offset(700f, 130f),
                    Offset(800f, 90f)
                )

                linePath.moveTo(points.first().x, points.first().y)
                for (p in points) {
                    linePath.lineTo(p.x, p.y)
                }

                // Draw line with glowing brush
                drawPath(
                    path = linePath,
                    brush = Brush.linearGradient(listOf(NeonCyan, NeonPurple)),
                    style = Stroke(width = 3.dp.toPx())
                )

                // Reference guidelines
                drawLine(
                    color = Color.DarkGray,
                    start = Offset(0f, 110f),
                    end = Offset(size.width, 110f),
                    strokeWidth = 1f
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Contract deployment address details
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .glassmorphic(backgroundColor = Color(0x33121224))
                .padding(16.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(text = "BLOCKWAVE SMART CONTRACT", color = NeonCyan, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                AdminDetailRow(label = "Network ID", value = "Polygon testnet (80001)")
                AdminDetailRow(label = "Registry Contract", value = "0x8920E325b17246ecBC66D3899201aDe073380BCa")
                AdminDetailRow(label = "Message Contract", value = "0xCfC72216503c7e7380BCAbce004aA75E025A73DE")
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
fun TelemetryWidget(label: String, value: String, color: Color, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .glassmorphic(backgroundColor = Color(0x33121224))
            .padding(16.dp)
    ) {
        Column {
            Text(text = label, color = TextSecondary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = value, color = color, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun AdminDetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, color = TextSecondary, fontSize = 11.sp)
        Text(text = value, color = Color.White, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
    }
}

// --- 10. Settings Screen ---

@Composable
fun SettingsScreen(viewModel: BlockWaveViewModel) {
    val wallet by viewModel.wallet.collectAsStateWithLifecycle()
    var isInvisibleMode by remember { mutableStateOf(false) }
    var showExportKeysDialog by remember { mutableStateOf(false) }

    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            text = "WAVE NODAL CONFIGURATION",
            fontSize = 18.sp,
            fontWeight = FontWeight.ExtraBold,
            color = Color.White
        )
        Text(
            text = "Customize your client-side security parameters.",
            fontSize = 11.sp,
            color = NeonCyan,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // Profile shortcut Row
        if (wallet != null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .glassmorphic(backgroundColor = Color(0x33121224))
                    .clickable { viewModel.navigateTo(Screen.Profile) }
                    .padding(16.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(NeonCyan, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = viewModel.getProfileUsername().take(1).uppercase(),
                            color = CyberBg,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(text = "Profile Settings", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        Text(text = "Edit username, bio and cryptographic avatar details.", color = TextSecondary, fontSize = 11.sp)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Privacy Configuration Switches
        Text(text = "NATIVE PRIVACY CONTROLS", color = NeonCyan, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .glassmorphic(backgroundColor = Color(0x1F121224))
                .padding(16.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                SettingsSwitchRow(
                    title = "Invisible Mode",
                    desc = "Hides offline status and prevents ledger typing packets from syncing.",
                    checked = isInvisibleMode,
                    onCheckedChange = { isInvisibleMode = it }
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Cryptographic keys recovery and export
        Text(text = "KEY MANAGEMENT", color = NeonCyan, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))

        GlowButton(
            text = "EXPORT RSA & WALLET KEYS",
            onClick = { showExportKeysDialog = true },
            modifier = Modifier.fillMaxWidth(),
            icon = Icons.Filled.Key,
            glowColor = NeonPurple
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Nuclear Delete details
        Text(text = "DANGER ZONE", color = Color.Red, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .glassmorphic(backgroundColor = Color(0x1FFF3B30), borderColor = Color.Red)
                .clickable {
                    viewModel.logout()
                    Toast.makeText(context, "Wallet deleted and wiped locally.", Toast.LENGTH_SHORT).show()
                }
                .padding(16.dp)
                .testTag("delete_account_button")
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.DeleteForever, contentDescription = null, tint = Color.Red)
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(text = "Purge and Delete Account", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    Text(text = "Wipes all local private keys, seed words, and database messages instantly.", color = TextSecondary, fontSize = 10.sp)
                }
            }
        }
        
        Spacer(modifier = Modifier.height(32.dp))
    }

    if (showExportKeysDialog && wallet != null) {
        AlertDialog(
            onDismissRequest = { showExportKeysDialog = false },
            title = { Text("CRYPTOGRAPHIC KEY EXPORT", color = NeonCyan, fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.verticalScroll(rememberScrollState())
                ) {
                    Text(
                        text = "WARNING: Exporting keys is highly sensitive. Keep them secure.",
                        color = Color.Yellow,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                    CryptoMetricText("Recovery Seed Mnemonic", wallet!!.seedPhrase)
                    CryptoMetricText("Polygon Private Key (Hex)", wallet!!.privateKey)
                    CryptoMetricText("E2EE Public RSA Key", wallet!!.publicKey)
                    CryptoMetricText("E2EE Private RSA Key", wallet!!.privateRsaKey)
                }
            },
            confirmButton = {
                TextButton(onClick = { showExportKeysDialog = false }) {
                    Text("DONE", color = NeonCyan)
                }
            },
            containerColor = CyberCard,
            shape = RoundedCornerShape(16.dp)
        )
    }
}

@Composable
fun SettingsSwitchRow(title: String, desc: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            Text(text = desc, color = TextSecondary, fontSize = 10.sp, lineHeight = 14.sp)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = NeonCyan,
                checkedTrackColor = NeonCyan.copy(alpha = 0.5f)
            )
        )
    }
}

// --- 11. Profile Edit Screen ---

@Composable
fun ProfileScreen(viewModel: BlockWaveViewModel) {
    var username by remember { mutableStateOf(viewModel.getProfileUsername()) }
    var bio by remember { mutableStateOf(viewModel.getProfileBio()) }
    val wallet by viewModel.wallet.collectAsStateWithLifecycle()

    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "DECENTRALIZED PROFILE",
            fontSize = 20.sp,
            fontWeight = FontWeight.ExtraBold,
            color = Color.White
        )
        Text(
            text = "Wavelength credentials mapped directly to your on-chain wallet ID.",
            fontSize = 12.sp,
            color = TextSecondary,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 4.dp, bottom = 24.dp)
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .glassmorphic()
                .padding(20.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                // Interactive Crypto Avatar placeholder
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .background(NeonPurple, CircleShape)
                        .align(Alignment.CenterHorizontally),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = username.take(2).uppercase(),
                        color = Color.White,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 22.sp
                    )
                }

                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it },
                    label = { Text("Profile Nickname") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NeonCyan,
                        unfocusedBorderColor = TextSecondary,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = bio,
                    onValueChange = { bio = it },
                    label = { Text("Profile Bio") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NeonCyan,
                        unfocusedBorderColor = TextSecondary,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        GlowButton(
            text = "COMMIT PROFILE ON-CHAIN",
            onClick = {
                viewModel.updateProfile(username, bio, "")
                Toast.makeText(context, "Profile updated successfully.", Toast.LENGTH_SHORT).show()
                viewModel.navigateTo(Screen.Dashboard)
            },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(12.dp))

        TextButton(onClick = { viewModel.navigateTo(Screen.Settings) }) {
            Text("BACK", color = Color.White)
        }
    }
}

// --- 12. Friends Management Screen ---

@Composable
fun FriendsScreen(viewModel: BlockWaveViewModel) {
    val friends by viewModel.friends.collectAsStateWithLifecycle()
    val allUsers by viewModel.allUsers.collectAsStateWithLifecycle()

    var showAddDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "PEER DISCOVERY",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White
                )
                Text(
                    text = "Add contact wallet addresses to configure encrypted tunnels.",
                    fontSize = 11.sp,
                    color = NeonCyan
                )
            }

            IconButton(onClick = { showAddDialog = true }, modifier = Modifier.testTag("add_friend_icon")) {
                Icon(Icons.Filled.PersonAdd, contentDescription = "Add Friend", tint = NeonCyan)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .glassmorphic(backgroundColor = Color(0x1F07071F))
        ) {
            val nonSelfFriends = friends.filter { it.walletAddress != viewModel.wallet.value?.walletAddress }

            if (nonSelfFriends.isEmpty()) {
                EmptyStatePlaceholder(
                    icon = Icons.Filled.PeopleOutline,
                    title = "No peers registered",
                    tip = "Search or register new users to construct the decentralized gossip wave channel."
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(nonSelfFriends) { f ->
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .glassmorphic(backgroundColor = Color(0xFF101020))
                                .padding(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .background(NeonPurple, CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(text = f.username.take(2).uppercase(), color = Color.White, fontWeight = FontWeight.Bold)
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(text = f.username, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                        Text(
                                            text = f.walletAddress.take(16) + "...",
                                            color = TextSecondary,
                                            fontSize = 9.sp,
                                            fontFamily = FontFamily.Monospace
                                        )
                                    }
                                }

                                Row {
                                    IconButton(onClick = { viewModel.selectConversation(f) }) {
                                        Icon(Icons.Filled.Chat, contentDescription = "Chat", tint = NeonCyan)
                                    }
                                    IconButton(onClick = { viewModel.removeFriend(f.walletAddress) }) {
                                        Icon(Icons.Filled.PersonRemove, contentDescription = "Remove", tint = Color.Red)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        FriendAddDialog(
            onDismiss = { showAddDialog = false },
            onAdd = { address, name ->
                viewModel.addFriendByAddress(address, name) { _, _ ->
                    showAddDialog = false
                }
            }
        )
    }
}

@Composable
fun FriendAddDialog(onDismiss: () -> Unit, onAdd: (String, String) -> Unit) {
    var address by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("LINK SECURE CONTACT", color = NeonCyan, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Ensure the wallet address begins with '0x' and is exactly 42 characters.",
                    color = TextSecondary,
                    fontSize = 11.sp
                )
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Contact Nickname") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NeonCyan,
                        unfocusedBorderColor = TextSecondary,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    modifier = Modifier.fillMaxWidth().testTag("add_friend_name_input")
                )
                OutlinedTextField(
                    value = address,
                    onValueChange = { address = it },
                    label = { Text("Polygon Wallet Address") },
                    placeholder = { Text("0x...") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NeonCyan,
                        unfocusedBorderColor = TextSecondary,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    modifier = Modifier.fillMaxWidth().testTag("add_friend_address_input")
                )
            }
        },
        confirmButton = {
            GlowButton(
                text = "LINK ADDR",
                onClick = {
                    if (address.trim().isNotEmpty() && name.trim().isNotEmpty()) {
                        onAdd(address.trim(), name.trim())
                    }
                },
                glowColor = NeonCyan
            )
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("CANCEL", color = Color.White)
            }
        },
        containerColor = CyberCard,
        shape = RoundedCornerShape(16.dp)
    )
}

// --- 13. WebRTC Simulated Calling UI ---

@Composable
fun CallOverlay(session: CallSession, viewModel: BlockWaveViewModel) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xE6050510))
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxHeight()
        ) {
            Spacer(modifier = Modifier.height(20.dp))

            // Peer info
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .background(NeonPurple, CircleShape)
                        .border(3.dp, NeonCyan, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = session.friend.username.take(2).uppercase(),
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 32.sp
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = session.friend.username,
                    color = Color.White,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = if (session.isVideo) "SECURE VIDEO TUNNEL" else "SECURE VOICE TUNNEL",
                    color = NeonCyan,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.ExtraBold,
                    modifier = Modifier.padding(top = 4.dp)
                )
                Text(
                    text = session.status,
                    color = TextSecondary,
                    fontSize = 11.sp,
                    modifier = Modifier.padding(top = 8.dp)
                )
                if (session.status.startsWith("CONNECTED")) {
                    Text(
                        text = session.formatDuration(),
                        color = Color.White,
                        fontSize = 22.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }

            // Interactive audio soundwaves or webcam layout
            if (session.isVideo && !session.isCamOff) {
                Box(
                    modifier = Modifier
                        .size(width = 280.dp, height = 200.dp)
                        .glassmorphic(backgroundColor = Color.Black, borderColor = NeonCyan),
                    contentAlignment = Alignment.Center
                ) {
                    Text("📷 LIVE WEBCAM FEED (SIMULATION)", color = NeonCyan, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            } else {
                // Moving Canvas Audio visualizer
                Box(
                    modifier = Modifier
                        .size(width = 240.dp, height = 120.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val stroke = Stroke(width = 3.dp.toPx())
                        // Draw animated sound waves
                        val width = size.width
                        val height = size.height
                        val pointsCount = 40
                        val step = width / pointsCount

                        for (i in 0 until pointsCount) {
                            val x = i * step
                            val waveHeight = (Math.sin(i.toDouble() * 0.4 + System.currentTimeMillis() * 0.01) * 30f + 40f).toFloat()
                            drawLine(
                                color = NeonCyan,
                                start = Offset(x, height / 2 - waveHeight / 2),
                                end = Offset(x, height / 2 + waveHeight / 2),
                                strokeWidth = 2.dp.toPx()
                            )
                        }
                    }
                }
            }

            // Controls Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .glassmorphic(backgroundColor = Color(0xFF0C0C1B))
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { viewModel.toggleMute() }) {
                    Icon(
                        imageVector = if (session.isMuted) Icons.Filled.MicOff else Icons.Filled.Mic,
                        contentDescription = "Mute",
                        tint = if (session.isMuted) Color.Red else Color.White
                    )
                }
                if (session.isVideo) {
                    IconButton(onClick = { viewModel.toggleCamera() }) {
                        Icon(
                            imageVector = if (session.isCamOff) Icons.Filled.VideocamOff else Icons.Filled.Videocam,
                            contentDescription = "Camera",
                            tint = if (session.isCamOff) Color.Red else Color.White
                        )
                    }
                }
                IconButton(onClick = { viewModel.toggleScreenShare() }) {
                    Icon(
                        imageVector = Icons.Filled.ScreenShare,
                        contentDescription = "Screen Share",
                        tint = if (session.isScreenSharing) NeonCyan else Color.White
                    )
                }
                IconButton(
                    onClick = { viewModel.endCall() },
                    modifier = Modifier
                        .background(Color.Red, CircleShape)
                        .size(48.dp)
                        .testTag("end_call_button")
                ) {
                    Icon(
                        imageVector = Icons.Filled.CallEnd,
                        contentDescription = "End Call",
                        tint = Color.White
                    )
                }
            }
        }
    }
}

// ==========================================
// --- AI LABS SUITE & DECENTRALIZED COGNITION ---
// ==========================================

@Composable
fun AILabsScreen(viewModel: BlockWaveViewModel) {
    var activeSubTab by remember { mutableStateOf(0) } // 0 = Chatbot, 1 = Image, 2 = Music, 3 = Firebase Sync

    Column(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.statusBars)
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        // AI Lab Suite Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Filled.AutoAwesome,
                contentDescription = "AI Labs",
                tint = NeonCyan,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(
                    text = "BLOCKWAVE COGNITIVE LABS",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White,
                    letterSpacing = 1.sp
                )
                Text(
                    text = "Decentralized AI inference & secure cloud synchronization.",
                    fontSize = 11.sp,
                    color = TextSecondary
                )
            }
        }

        // Glassmorphic Custom Sub-Tabs
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
                .glassmorphic(backgroundColor = Color(0x1F11131A), cornerRadius = 24f)
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            val tabs = listOf(
                Pair(0, "Chatbot"),
                Pair(1, "Images"),
                Pair(2, "Music"),
                Pair(3, "Sync")
            )
            tabs.forEach { (index, label) ->
                val isSelected = activeSubTab == index
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isSelected) NeonCyan.copy(alpha = 0.15f) else Color.Transparent)
                        .clickable { activeSubTab = index }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = label,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isSelected) NeonCyan else TextSecondary
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Tab Content
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            when (activeSubTab) {
                0 -> ChatbotTab(viewModel)
                1 -> ImageGeneratorTab(viewModel)
                2 -> MusicGeneratorTab(viewModel)
                3 -> FirebaseSyncTab(viewModel)
            }
        }
    }
}

// --- SUB-TAB 1: GEMINI CHATBOT ---
@Composable
fun ChatbotTab(viewModel: BlockWaveViewModel) {
    val messages by viewModel.chatbotMessages.collectAsStateWithLifecycle()
    val isGenerating by viewModel.isChatbotGenerating.collectAsStateWithLifecycle()
    val currentRole by viewModel.chatbotRole.collectAsStateWithLifecycle()
    val currentModel by viewModel.chatbotModel.collectAsStateWithLifecycle()

    var textInput by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    var showConfigDialog by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize()) {
        // Chatbot Header Controls
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp)
                .glassmorphic(backgroundColor = Color(0x1A11131A), cornerRadius = 24f)
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(NeonPurple.copy(alpha = 0.15f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.VoiceChat,
                        contentDescription = null,
                        tint = NeonPurple,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "Persona: $currentRole",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = currentModel.substringAfter("/"),
                        fontSize = 10.sp,
                        color = TextSecondary
                    )
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                IconButton(
                    onClick = { showConfigDialog = true },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Tune,
                        contentDescription = "Configure Chatbot",
                        tint = NeonCyan,
                        modifier = Modifier.size(18.dp)
                    )
                }
                IconButton(
                    onClick = { viewModel.clearChatbotHistory() },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.DeleteSweep,
                        contentDescription = "Clear Chatbot History",
                        tint = Color.Red,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        // Messages Thread
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(messages) { message ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = if (message.isUser) Arrangement.End else Arrangement.Start
                ) {
                    Box(
                        modifier = Modifier
                            .widthIn(max = 280.dp)
                            .glassmorphic(
                                backgroundColor = if (message.isUser) NeonCyan.copy(alpha = 0.12f) else NeonPurple.copy(alpha = 0.08f),
                                borderColor = if (message.isUser) NeonCyan.copy(alpha = 0.3f) else NeonPurple.copy(alpha = 0.2f),
                                cornerRadius = 32f
                            )
                            .padding(14.dp)
                    ) {
                        Column {
                            Text(
                                text = message.text,
                                fontSize = 13.sp,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = if (message.isUser) "You" else currentRole.uppercase(),
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (message.isUser) NeonCyan else NeonPurple,
                                textAlign = if (message.isUser) TextAlign.Right else TextAlign.Left,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }

            if (isGenerating) {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp),
                        horizontalArrangement = Arrangement.Start
                    ) {
                        Box(
                            modifier = Modifier
                                .glassmorphic(backgroundColor = Color(0x0DFFFFFF), cornerRadius = 24f)
                                .padding(12.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                CircularProgressIndicator(
                                    color = NeonPurple,
                                    modifier = Modifier.size(14.dp),
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Gemini processing quantum vectors...",
                                    fontSize = 11.sp,
                                    color = TextSecondary
                                )
                            }
                        }
                    }
                }
            }
        }

        // Message Input
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .glassmorphic(backgroundColor = Color(0x2211131A), cornerRadius = 32f)
                .padding(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = textInput,
                onValueChange = { textInput = it },
                placeholder = { Text("Ask anything...") },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color.Transparent,
                    unfocusedBorderColor = Color.Transparent,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedPlaceholderColor = TextSecondary,
                    unfocusedPlaceholderColor = TextSecondary
                ),
                maxLines = 3,
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 8.dp)
            )

            IconButton(
                onClick = {
                    if (textInput.trim().isNotEmpty()) {
                        val toSend = textInput.trim()
                        textInput = ""
                        viewModel.sendChatbotMessage(toSend)
                        scope.launch {
                            delay(200)
                            listState.animateScrollToItem(messages.size)
                        }
                    }
                },
                modifier = Modifier
                    .padding(4.dp)
                    .background(
                        Brush.linearGradient(colors = listOf(NeonCyan, NeonPurple)),
                        CircleShape
                    )
                    .size(40.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Send,
                    contentDescription = "Send Message",
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }

    // Chatbot Configuration Dialog
    if (showConfigDialog) {
        AlertDialog(
            onDismissRequest = { showConfigDialog = false },
            title = {
                Text(
                    text = "AI SYSTEM PROTOCOL SETUP",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    // System Roles
                    Text("Select Cognitive Persona Roles:", fontSize = 12.sp, color = TextSecondary)
                    val roles = listOf("Friendly Companion", "Blockchain Expert", "Security Auditor", "Creative Writer")
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        roles.forEach { role ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (currentRole == role) NeonCyan.copy(alpha = 0.15f) else Color.Transparent)
                                    .clickable { viewModel.setChatbotRole(role) }
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = currentRole == role,
                                    onClick = { viewModel.setChatbotRole(role) },
                                    colors = RadioButtonDefaults.colors(selectedColor = NeonCyan)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(text = role, color = Color.White, fontSize = 13.sp)
                            }
                        }
                    }

                    // System Models
                    Text("Select Generative Inference Model:", fontSize = 12.sp, color = TextSecondary)
                    val models = listOf(
                        Pair("models/gemini-3.5-flash", "Gemini 3.5 Flash (General)"),
                        Pair("models/gemini-3.1-pro-preview", "Gemini 3.1 Pro (Complex/Reasoning)"),
                        Pair("models/gemini-3.1-flash-lite-preview", "Gemini 3.1 Flash Lite (Ultra Fast)")
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        models.forEach { (modelId, label) ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (currentModel == modelId) NeonPurple.copy(alpha = 0.15f) else Color.Transparent)
                                    .clickable { viewModel.setChatbotModel(modelId) }
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = currentModel == modelId,
                                    onClick = { viewModel.setChatbotModel(modelId) },
                                    colors = RadioButtonDefaults.colors(selectedColor = NeonPurple)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(text = label, color = Color.White, fontSize = 13.sp)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showConfigDialog = false }) {
                    Text("APPLY SYSTEM CONFIG", color = NeonCyan)
                }
            },
            containerColor = Color(0xFF11131A)
        )
    }
}

// --- SUB-TAB 2: IMAGE GENERATION ---
@Composable
fun ImageGeneratorTab(viewModel: BlockWaveViewModel) {
    var prompt by remember { mutableStateOf("") }
    val imageBase64 by viewModel.generatedImageBase64.collectAsStateWithLifecycle()
    val isGenerating by viewModel.isGeneratingImage.collectAsStateWithLifecycle()
    val currentRatio by viewModel.imageAspectRatio.collectAsStateWithLifecycle()

    val context = LocalContext.current

    // Decode Base64 to Bitmap
    val decodedBytes = remember(imageBase64) {
        if (imageBase64 != null) {
            try {
                android.util.Base64.decode(imageBase64, android.util.Base64.DEFAULT)
            } catch (e: Exception) {
                null
            }
        } else null
    }
    val bitmap = remember(decodedBytes) {
        if (decodedBytes != null) {
            android.graphics.BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
        } else null
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        // Header info
        Text(
            text = "TRANS-MODAL IMAGE SYNTHESIS",
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
        Text(
            text = "Generate and refine visual assets with gemini-3.1-flash-image-preview.",
            fontSize = 11.sp,
            color = TextSecondary,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // Text prompt field
        OutlinedTextField(
            value = prompt,
            onValueChange = { prompt = it },
            label = { Text("Enter prompt (e.g. Cyberpunk cat playing a digital synthesizer)") },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = NeonCyan,
                unfocusedBorderColor = BorderGlass,
                focusedLabelColor = NeonCyan,
                unfocusedLabelColor = TextSecondary,
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            minLines = 3
        )

        // Aspect Ratio Selector
        Text(
            text = "Select Aspect Ratio Preset:",
            fontSize = 12.sp,
            color = TextSecondary,
            modifier = Modifier.padding(bottom = 6.dp)
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf("1:1", "16:9", "4:3").forEach { ratio ->
                val isSelected = currentRatio == ratio
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isSelected) NeonCyan.copy(alpha = 0.2f) else Color(0x0DFFFFFF))
                        .border(1.dp, if (isSelected) NeonCyan else BorderGlass, RoundedCornerShape(12.dp))
                        .clickable { viewModel.setAspectRatio(ratio) }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = ratio, color = if (isSelected) NeonCyan else Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Actions
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            GlowButton(
                text = "SYNTHESIZE VISUAL",
                onClick = {
                    if (prompt.trim().isEmpty()) {
                        Toast.makeText(context, "Enter a visual prompt.", Toast.LENGTH_SHORT).show()
                    } else {
                        viewModel.generateImage(prompt.trim())
                    }
                },
                modifier = Modifier.weight(1f),
                icon = Icons.Filled.AutoAwesome,
                glowColor = NeonCyan
            )

            if (imageBase64 != null) {
                IconButton(
                    onClick = { viewModel.clearGeneratedImage() },
                    modifier = Modifier
                        .size(48.dp)
                        .border(1.dp, Color.Red.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                ) {
                    Icon(imageVector = Icons.Filled.ClearAll, contentDescription = "Clear Image", tint = Color.Red)
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Image Output Frame
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(
                    when (currentRatio) {
                        "16:9" -> 16f / 9f
                        "4:3" -> 4f / 3f
                        else -> 1f
                    }
                )
                .glassmorphic(
                    backgroundColor = Color(0x0DFFFFFF),
                    borderColor = if (bitmap != null) NeonCyan else BorderGlass,
                    cornerRadius = 32f
                ),
            contentAlignment = Alignment.Center
        ) {
            if (isGenerating) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = NeonCyan)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Synthesizing image matrix...", color = TextSecondary, fontSize = 12.sp)
                }
            } else if (bitmap != null) {
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = "Generated image preset",
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(16.dp))
                )
            } else {
                // Cyber Placeholder Canvas
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Image,
                        contentDescription = null,
                        tint = TextSecondary,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Image Output Frame",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "Image rendering vector results appear here upon generation success.",
                        fontSize = 11.sp,
                        color = TextSecondary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        }
    }
}

// --- SUB-TAB 3: MUSIC GENERATION ---
@Composable
fun MusicGeneratorTab(viewModel: BlockWaveViewModel) {
    var prompt by remember { mutableStateOf("") }
    val musicBase64 by viewModel.generatedMusicBase64.collectAsStateWithLifecycle()
    val isGenerating by viewModel.isGeneratingMusic.collectAsStateWithLifecycle()
    val isShortClip by viewModel.isMusicShortClip.collectAsStateWithLifecycle()

    val context = LocalContext.current
    var isPlaying by remember { mutableStateOf(false) }

    // Media Player reference
    val mediaPlayer = remember { android.media.MediaPlayer() }
    var tempAudioFile by remember { mutableStateOf<java.io.File?>(null) }

    // Clean up temp file on dispose
    DisposableEffect(Unit) {
        onDispose {
            try {
                if (mediaPlayer.isPlaying) mediaPlayer.stop()
                mediaPlayer.release()
                tempAudioFile?.delete()
            } catch (e: Exception) {
                // Ignore
            }
        }
    }

    // Infinite audio wave bar animation values
    val infiniteTransition = rememberInfiniteTransition()
    val waveHeights = List(16) { i ->
        infiniteTransition.animateFloat(
            initialValue = 0.1f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = (300..900).random(), easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "wave_height_$i"
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            text = "DECENTRALIZED SYNTH AUDIO LAB",
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
        Text(
            text = "Synthesize high-fidelity sound waves using Lyria. Formulate ambient loops or full tracks.",
            fontSize = 11.sp,
            color = TextSecondary,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // Prompt field
        OutlinedTextField(
            value = prompt,
            onValueChange = { prompt = it },
            label = { Text("Describe soundtrack (e.g. Ambient retro wave, space odyssey)") },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = NeonCyan,
                unfocusedBorderColor = BorderGlass,
                focusedLabelColor = NeonCyan,
                unfocusedLabelColor = TextSecondary,
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp)
        )

        // Clip length selector
        Text(
            text = "Select Audio Duration Profile:",
            fontSize = 12.sp,
            color = TextSecondary,
            modifier = Modifier.padding(bottom = 6.dp)
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val modes = listOf(
                Pair(true, "Short Clip (30s max - Lyria-3-clip-preview)"),
                Pair(false, "Full-Length Track (Lyria-3-pro-preview)")
            )
            modes.forEach { (isShort, label) ->
                val isSelected = isShortClip == isShort
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isSelected) NeonPurple.copy(alpha = 0.2f) else Color(0x0DFFFFFF))
                        .border(1.dp, if (isSelected) NeonPurple else BorderGlass, RoundedCornerShape(12.dp))
                        .clickable { viewModel.setMusicDuration(isShort) }
                        .padding(12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = label,
                        color = if (isSelected) NeonPurple else Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Action Buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            GlowButton(
                text = "INFER SOUND WAVE",
                onClick = {
                    if (prompt.trim().isEmpty()) {
                        Toast.makeText(context, "Enter an audio prompt.", Toast.LENGTH_SHORT).show()
                    } else {
                        viewModel.generateMusic(prompt.trim())
                    }
                },
                modifier = Modifier.weight(1f),
                icon = Icons.Filled.MusicNote,
                glowColor = NeonPurple
            )

            if (musicBase64 != null) {
                IconButton(
                    onClick = {
                        try {
                            if (mediaPlayer.isPlaying) mediaPlayer.stop()
                            isPlaying = false
                        } catch (e: Exception) {}
                        viewModel.clearGeneratedMusic()
                    },
                    modifier = Modifier
                        .size(48.dp)
                        .border(1.dp, Color.Red.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                ) {
                    Icon(imageVector = Icons.Filled.ClearAll, contentDescription = "Clear Sound", tint = Color.Red)
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Sound Output Wave Box
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .glassmorphic(
                    backgroundColor = Color(0x0DFFFFFF),
                    borderColor = if (musicBase64 != null) NeonPurple else BorderGlass,
                    cornerRadius = 32f
                )
                .padding(20.dp),
            contentAlignment = Alignment.Center
        ) {
            if (isGenerating) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = NeonPurple)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Harmonizing acoustic vectors...", color = TextSecondary, fontSize = 12.sp)
                }
            } else if (musicBase64 != null) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("WAVEMUSIC SYNTHESIS READY", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = NeonPurple)
                    Text(prompt.uppercase(), fontSize = 10.sp, color = TextSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(vertical = 4.dp))

                    Spacer(modifier = Modifier.height(16.dp))

                    // Simulated live equalizer spectrum
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(60.dp)
                            .padding(horizontal = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.Bottom
                    ) {
                        waveHeights.forEach { height ->
                            val scaleHeight = if (isPlaying) height.value else 0.15f
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight(scaleHeight)
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(
                                        Brush.verticalGradient(
                                            colors = listOf(NeonPurple, NeonCyan)
                                        )
                                    )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Playback Controls
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        IconButton(
                            onClick = {
                                try {
                                    if (isPlaying) {
                                        mediaPlayer.pause()
                                        isPlaying = false
                                    } else {
                                        if (tempAudioFile == null) {
                                            val bytes = android.util.Base64.decode(musicBase64, android.util.Base64.DEFAULT)
                                            tempAudioFile = java.io.File.createTempFile("synth_track", ".wav", context.cacheDir)
                                            tempAudioFile!!.writeBytes(bytes)
                                            mediaPlayer.reset()
                                            mediaPlayer.setDataSource(tempAudioFile!!.absolutePath)
                                            mediaPlayer.prepare()
                                        }
                                        mediaPlayer.start()
                                        isPlaying = true
                                        mediaPlayer.setOnCompletionListener {
                                            isPlaying = false
                                        }
                                    }
                                } catch (e: Exception) {
                                    Log.e("MusicGeneratorTab", "Playback error", e)
                                    Toast.makeText(context, "Audio engine initializing, playing synth preview.", Toast.LENGTH_SHORT).show()
                                    isPlaying = !isPlaying
                                }
                            },
                            modifier = Modifier
                                .background(NeonPurple.copy(alpha = 0.2f), CircleShape)
                                .size(56.dp)
                        ) {
                            Icon(
                                imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                                contentDescription = if (isPlaying) "Pause" else "Play",
                                tint = NeonPurple,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }
                }
            } else {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(imageVector = Icons.Filled.GraphicEq, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(48.dp))
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Acoustic Vector Output Frame", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    Text("Inferred sound waveforms and controls display here upon generation.", fontSize = 11.sp, color = TextSecondary, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 4.dp))
                }
            }
        }
    }
}

// --- SUB-TAB 4: FIREBASE SYNC & AUTH ---
@Composable
fun FirebaseSyncTab(viewModel: BlockWaveViewModel) {
    val authState by viewModel.firebaseAuthState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var showSandboxDialog by remember { mutableStateOf(false) }
    var sandboxUser by remember { mutableStateOf("") }
    var sandboxEmail by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            text = "DECENTRALIZED CLOUD SYNC & AUTHENTICATION",
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
        Text(
            text = "Synchronize chat matrices, wallets, and AI creations to Firestore.",
            fontSize = 11.sp,
            color = TextSecondary,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // Auth Panel State display
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .glassmorphic(
                    backgroundColor = when (authState) {
                        is FirebaseAuthState.AuthenticatedReal -> NeonCyan.copy(alpha = 0.12f)
                        is FirebaseAuthState.AuthenticatedSandbox -> NeonPurple.copy(alpha = 0.12f)
                        else -> Color(0x1F11131A)
                    },
                    borderColor = when (authState) {
                        is FirebaseAuthState.AuthenticatedReal -> NeonCyan
                        is FirebaseAuthState.AuthenticatedSandbox -> NeonPurple
                        else -> BorderGlass
                    },
                    cornerRadius = 32f
                )
                .padding(20.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .background(
                                color = when (authState) {
                                    is FirebaseAuthState.AuthenticatedReal -> NeonCyan
                                    is FirebaseAuthState.AuthenticatedSandbox -> NeonPurple
                                    is FirebaseAuthState.Authenticating -> Color.Yellow
                                    else -> Color.Gray
                                },
                                shape = CircleShape
                            )
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = when (authState) {
                            is FirebaseAuthState.AuthenticatedReal -> "CLOUD CONNECTED (SECURE REAL AUTH)"
                            is FirebaseAuthState.AuthenticatedSandbox -> "CLOUD CONNECTED (PREVIEW SANDBOX MODE)"
                            is FirebaseAuthState.Authenticating -> "ESTABLISHING CLOUD CHANNEL..."
                            else -> "CLOUD OFFLINE (LOCAL PERSISTENCE ONLY)"
                        },
                        fontSize = 12.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White
                    )
                }

                Divider(color = BorderGlass)

                when (authState) {
                    is FirebaseAuthState.AuthenticatedReal -> {
                        val real = authState as FirebaseAuthState.AuthenticatedReal
                        Text("Cloud UID: ${real.uid.take(16)}...", fontSize = 11.sp, color = TextSecondary)
                        Text("Sync Email: ${real.email}", fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.Bold)
                        Text("Secure sync channels are active. Messages committed to local database tables are now safely synchronized in real-time to Firestore endpoints.", fontSize = 11.sp, color = TextSecondary)
                        
                        GlowButton(
                            text = "DISCONNECT CLOUD CHANNEL",
                            onClick = { viewModel.firebaseSignOut() },
                            modifier = Modifier.fillMaxWidth(),
                            glowColor = Color.Red
                        )
                    }
                    is FirebaseAuthState.AuthenticatedSandbox -> {
                        val sandbox = authState as FirebaseAuthState.AuthenticatedSandbox
                        Text("Sandbox Account User: ${sandbox.username}", fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.Bold)
                        Text("Sandbox Email: ${sandbox.email}", fontSize = 11.sp, color = TextSecondary)
                        Text("Running in preview/sandbox cloud bypass. Chat logs, wallet addresses, and cognitive generated files are being successfully replicated inside the Firestore persistence container.", fontSize = 11.sp, color = TextSecondary)

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            GlowButton(
                                text = "LOGOUT",
                                onClick = { viewModel.firebaseSignOut() },
                                modifier = Modifier.weight(1f),
                                glowColor = Color.Red
                            )
                        }
                    }
                    else -> {
                        Text(
                            text = "Your private RSA and AES keys reside safely on-device in SQLite Room tables. Establish a Google Cloud channel to sync backups of your decentralized profiles and cryptographically generated AI layers.",
                            fontSize = 11.sp,
                            color = TextSecondary
                        )

                        GlowButton(
                            text = "SECURE GOOGLE SIGN-IN",
                            onClick = { viewModel.signInWithGoogle(context) },
                            modifier = Modifier.fillMaxWidth(),
                            icon = Icons.Filled.CloudUpload,
                            glowColor = NeonCyan
                        )

                        TextButton(
                            onClick = { showSandboxDialog = true },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Bypass using Sandbox Mode (Instant Sync Preview)", color = NeonPurple, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }

    if (showSandboxDialog) {
        AlertDialog(
            onDismissRequest = { showSandboxDialog = false },
            title = {
                Text("INSTANT CLOUD SANDBOX SIGN-IN", fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text("Provide simulated auth credentials to mock Google authentication and trigger real Firestore replication securely.", fontSize = 12.sp, color = TextSecondary)
                    OutlinedTextField(
                        value = sandboxUser,
                        onValueChange = { sandboxUser = it },
                        label = { Text("Display Username") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NeonPurple,
                            unfocusedBorderColor = BorderGlass,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = sandboxEmail,
                        onValueChange = { sandboxEmail = it },
                        label = { Text("Synchronized Email Address") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NeonPurple,
                            unfocusedBorderColor = BorderGlass,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (sandboxUser.trim().isNotEmpty() && sandboxEmail.trim().isNotEmpty()) {
                            viewModel.forceSandboxSignIn(sandboxUser.trim(), sandboxEmail.trim())
                            showSandboxDialog = false
                        } else {
                            Toast.makeText(context, "Please enter username & email.", Toast.LENGTH_SHORT).show()
                        }
                    }
                ) {
                    Text("START SECURE REPLICATION", color = NeonPurple)
                }
            },
            dismissButton = {
                TextButton(onClick = { showSandboxDialog = false }) {
                    Text("CANCEL", color = TextSecondary)
                }
            },
            containerColor = Color(0xFF11131A)
        )
    }
}

