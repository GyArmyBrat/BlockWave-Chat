package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import android.content.Context
import com.example.api.GeminiClient
import com.example.crypto.CryptoUtils
import com.example.crypto.WalletCredentials
import com.example.data.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class BlockWaveViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    private val repository = BlockWaveRepository(
        application,
        db.userDao(),
        db.messageDao(),
        db.groupDao(),
        db.blockchainTxDao()
    )

    // --- State Holders ---
    private val _currentScreen = MutableStateFlow<Screen>(Screen.Landing)
    val currentScreen: StateFlow<Screen> = _currentScreen.asStateFlow()

    private val _wallet = MutableStateFlow<WalletCredentials?>(null)
    val wallet: StateFlow<WalletCredentials?> = _wallet.asStateFlow()

    private val _selectedFriend = MutableStateFlow<UserEntity?>(null)
    val selectedFriend: StateFlow<UserEntity?> = _selectedFriend.asStateFlow()

    private val _selectedGroup = MutableStateFlow<GroupEntity?>(null)
    val selectedGroup: StateFlow<GroupEntity?> = _selectedGroup.asStateFlow()

    // --- Flows from Repo ---
    val friends: StateFlow<List<UserEntity>> = repository.friends
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val incomingRequests: StateFlow<List<UserEntity>> = repository.incomingRequests
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val outgoingRequests: StateFlow<List<UserEntity>> = repository.outgoingRequests
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val blockedUsers: StateFlow<List<UserEntity>> = repository.blockedUsers
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allUsers: StateFlow<List<UserEntity>> = repository.allUsers
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val groups: StateFlow<List<GroupEntity>> = repository.groups
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val transactions: StateFlow<List<BlockchainTxEntity>> = repository.transactions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val txCount: StateFlow<Int> = repository.txCount
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val totalGasUsed: StateFlow<Long> = repository.totalGasUsed
        .map { it ?: 0L }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0L)

    // Active Chat Message Flow
    private val _activeChatMessages = MutableStateFlow<List<MessageEntity>>(emptyList())
    val activeChatMessages: StateFlow<List<MessageEntity>> = _activeChatMessages.asStateFlow()

    // --- AI Feature States ---
    private val _aiSummary = MutableStateFlow<String?>(null)
    val aiSummary: StateFlow<String?> = _aiSummary.asStateFlow()

    private val _aiSmartReplies = MutableStateFlow<List<String>>(emptyList())
    val aiSmartReplies: StateFlow<List<String>> = _aiSmartReplies.asStateFlow()

    private val _isGeneratingAi = MutableStateFlow(false)
    val isGeneratingAi: StateFlow<Boolean> = _isGeneratingAi.asStateFlow()

    private val _scamScanResult = MutableStateFlow<Map<Long, String>>(emptyMap()) // messageId -> Scanned Status
    val scamScanResult: StateFlow<Map<Long, String>> = _scamScanResult.asStateFlow()

    // --- Voice/Video Call States (Simulated) ---
    private val _activeCall = MutableStateFlow<CallSession?>(null)
    val activeCall: StateFlow<CallSession?> = _activeCall.asStateFlow()

    init {
        viewModelScope.launch {
            if (repository.hasWallet()) {
                val creds = repository.getWalletCredentials()
                _wallet.value = creds
                repository.prepopulateDatabase()
                _currentScreen.value = Screen.Dashboard
            } else {
                _currentScreen.value = Screen.Landing
            }
        }
    }

    fun navigateTo(screen: Screen) {
        _currentScreen.value = screen
        // Clear screen-specific states
        if (screen != Screen.Chats) {
            _selectedFriend.value = null
            _aiSummary.value = null
            _aiSmartReplies.value = emptyList()
        }
        if (screen != Screen.GroupChat) {
            _selectedGroup.value = null
        }
    }

    // --- Wallet Management ---

    fun createWallet(username: String) {
        viewModelScope.launch {
            _isGeneratingAi.value = true
            val creds = repository.createNewWallet(username)
            _wallet.value = creds
            repository.prepopulateDatabase()
            _isGeneratingAi.value = false
            navigateTo(Screen.Dashboard)
        }
    }

    fun importWallet(seedPhrase: String, username: String) {
        viewModelScope.launch {
            _isGeneratingAi.value = true
            val creds = repository.importWallet(seedPhrase, username)
            _wallet.value = creds
            repository.prepopulateDatabase()
            _isGeneratingAi.value = false
            navigateTo(Screen.Dashboard)
        }
    }

    fun logout() {
        viewModelScope.launch {
            repository.deleteWallet()
            _wallet.value = null
            navigateTo(Screen.Landing)
        }
    }

    fun requestFaucet() {
        viewModelScope.launch {
            repository.triggerFaucet()
            // Reload wallet state to refresh balance
            _wallet.value = repository.getWalletCredentials()
        }
    }

    fun getProfileUsername(): String = repository.getProfileUsername()
    fun getProfileBio(): String = repository.getProfileBio()
    fun getProfileAvatar(): String = repository.getProfileAvatar()

    fun updateProfile(username: String, bio: String, avatarUrl: String) {
        viewModelScope.launch {
            repository.saveProfile(username, bio, avatarUrl)
            _wallet.value = repository.getWalletCredentials()
        }
    }

    // --- Contacts & Friends System ---

    fun addFriendByAddress(address: String, name: String, onComplete: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            try {
                repository.searchAndAddFriendByWallet(address, name)
                onComplete(true, "Friend linked successfully on-chain.")
            } catch (e: Exception) {
                onComplete(false, e.localizedMessage ?: "Error processing friend request.")
            }
        }
    }

    fun removeFriend(address: String) {
        viewModelScope.launch {
            repository.removeFriend(address)
        }
    }

    fun blockUser(address: String) {
        viewModelScope.launch {
            repository.blockUser(address)
        }
    }

    // --- Messaging and Chat Engine ---

    fun selectConversation(friend: UserEntity) {
        _selectedFriend.value = friend
        navigateTo(Screen.Chats)
        
        // Connect flow of messages
        viewModelScope.launch {
            repository.getChatMessages(friend.walletAddress).collect { msgs ->
                _activeChatMessages.value = msgs
                
                // Clear AI state when loading new conversation
                _aiSummary.value = null
                _aiSmartReplies.value = emptyList()

                // Generate smart replies if there are messages
                if (msgs.isNotEmpty() && msgs.last().senderWallet != wallet.value?.walletAddress) {
                    generateSmartReplies(msgs.last().plainText)
                }
            }
        }
    }

    fun selectGroupConversation(group: GroupEntity) {
        _selectedGroup.value = group
        navigateTo(Screen.GroupChat)

        viewModelScope.launch {
            repository.getGroupMessages(group.id).collect { msgs ->
                _activeChatMessages.value = msgs
            }
        }
    }

    fun sendTextMessage(plainText: String) {
        val recipient = _selectedFriend.value ?: return
        viewModelScope.launch {
            repository.sendOneToOneMessage(recipient.walletAddress, plainText, "TEXT")
            // Refresh wallet balance due to gas deduction
            _wallet.value = repository.getWalletCredentials()
            
            // Intercept for WaveAI Companion Direct response
            if (recipient.walletAddress == "0xAiAssistantAddress") {
                triggerGeminiCompanionResponse(plainText)
            }
        }
    }

    fun sendAttachmentMessage(type: String, url: String, name: String) {
        val recipient = _selectedFriend.value ?: return
        viewModelScope.launch {
            repository.sendOneToOneMessage(
                recipientAddress = recipient.walletAddress,
                plainText = "[Shared $type: $name]",
                attachmentType = type,
                attachmentUrl = url,
                attachmentName = name
            )
            _wallet.value = repository.getWalletCredentials()
        }
    }

    fun sendGroupTextMessage(plainText: String) {
        val group = _selectedGroup.value ?: return
        viewModelScope.launch {
            repository.sendGroupMessage(group.id, plainText, "TEXT")
            _wallet.value = repository.getWalletCredentials()
        }
    }

    fun sendGroupAttachmentMessage(type: String, url: String, name: String) {
        val group = _selectedGroup.value ?: return
        viewModelScope.launch {
            repository.sendGroupMessage(
                groupId = group.id,
                plainText = "[Shared $type: $name]",
                attachmentType = type,
                attachmentUrl = url,
                attachmentName = name
            )
            _wallet.value = repository.getWalletCredentials()
        }
    }

    fun createGroup(name: String, description: String, imageUrl: String) {
        viewModelScope.launch {
            repository.createGroup(name, description, imageUrl)
            navigateTo(Screen.Dashboard)
        }
    }

    fun deleteChatHistory(friendAddress: String) {
        viewModelScope.launch {
            repository.clearChatHistory(friendAddress)
            _activeChatMessages.value = emptyList()
        }
    }

    // --- Web3 / Gemini AI Assistant capabilities ---

    private suspend fun triggerGeminiCompanionResponse(userMessage: String) {
        _isGeneratingAi.value = true
        
        val systemInstruction = """
            You are "WaveAI Companion", a secure, helpful native AI node inside the BlockWave decentralized chat app.
            The user is speaking to you directly.
            - Answer questions concisely (under 3 sentences).
            - Keep your tone futuristic, clean, technical, yet friendly.
            - Focus on security, blockchain tech, cryptography, and Web3 capabilities.
            - Mention that your chat with them is fully simulated as E2E Encrypted (AES-256 + RSA-2048) locally on their Android node.
        """.trimIndent()

        // Call real Gemini API
        val reply = GeminiClient.generateResponse(userMessage, systemInstruction)
        
        _isGeneratingAi.value = false

        // Save reply into the DB as from WaveAI
        val aiNodeAddress = "0xAiAssistantAddress"
        val aiNode = db.userDao().getUserByAddress(aiNodeAddress) ?: return
        
        val responseHash = CryptoUtils.sha256(reply)
        val aesKey = CryptoUtils.generateAesKey()
        val encryptedPayload = CryptoUtils.encryptAes(reply, aesKey)
        val encryptedAesKey = CryptoUtils.encryptRsa(aesKey.encoded, wallet.value?.publicKey ?: "")
        val signature = CryptoUtils.signData(responseHash, aiNode.publicKey)

        val ipfsCid = "QmWaveAIResponse" + System.currentTimeMillis().toString().takeLast(6)
        val tx = repository.addSimulatedTransaction(
            from = aiNodeAddress,
            to = wallet.value?.walletAddress ?: "0x",
            eventType = "MessageSent",
            cid = ipfsCid,
            messageHash = responseHash
        )

        val message = MessageEntity(
            senderWallet = aiNodeAddress,
            receiverWallet = wallet.value?.walletAddress ?: "0x",
            groupId = null,
            encryptedPayload = encryptedPayload,
            encryptedAesKey = encryptedAesKey,
            plainText = reply,
            messageHash = responseHash,
            ipfsCid = ipfsCid,
            transactionHash = tx.txHash,
            isRead = false,
            deliveryStatus = "READ",
            attachmentType = "TEXT",
            signature = signature
        )
        db.messageDao().insertMessage(message)
    }

    fun summarizeActiveChat() {
        val msgs = _activeChatMessages.value
        if (msgs.isEmpty()) {
            _aiSummary.value = "No messages in this conversation to summarize."
            return
        }

        viewModelScope.launch {
            _isGeneratingAi.value = true
            val chatHistoryText = msgs.joinToString("\n") { 
                "${if (it.senderWallet == wallet.value?.walletAddress) "Me" else "Friend"}: ${it.plainText}" 
            }

            val prompt = """
                Please summarize the following encrypted chat conversation history into a concise, professional 2-3 bullet point bulleted summary.
                Focus on key topics discussed and any actions.
                
                Chat history:
                $chatHistoryText
            """.trimIndent()

            val summary = GeminiClient.generateResponse(prompt, "You are a secure, offline-safe chat summaries generator.")
            _aiSummary.value = summary
            _isGeneratingAi.value = false
        }
    }

    fun translateMessage(msg: MessageEntity, targetLanguage: String, onResult: (String) -> Unit) {
        viewModelScope.launch {
            _isGeneratingAi.value = true
            val prompt = "Translate this chat message strictly to $targetLanguage. Output ONLY the translated text, with no extra commentary:\n\"${msg.plainText}\""
            val result = GeminiClient.generateResponse(prompt, "You are an expert native real-time chat translator.")
            onResult(result)
            _isGeneratingAi.value = false
        }
    }

    fun scanMessageForScams(msg: MessageEntity) {
        viewModelScope.launch {
            val prompt = """
                Check if the following message is a phishing attempt, cryptomarketing spam, a suspicious smart contract scam, or standard clean chat text.
                Answer strictly with one of these prefix tokens: [SAFE], [SPAM], [PHISHING], [SCAM] followed by a 1-sentence warning of why if not safe.
                
                Message to scan:
                "${msg.plainText}"
            """.trimIndent()

            val result = GeminiClient.generateResponse(prompt, "You are a decentralized cybersecurity analysis engine. Always keep users safe.")
            val scanMap = _scamScanResult.value.toMutableMap()
            scanMap[msg.id] = result
            _scamScanResult.value = scanMap
        }
    }

    private fun generateSmartReplies(lastMessageText: String) {
        viewModelScope.launch {
            val prompt = """
                Generate exactly 3 short, contextual smart replies (each under 4 words) that a user can click to respond to this message:
                "$lastMessageText"
                
                Output your response strictly as a JSON string array of 3 strings. Example: ["Awesome!", "Where is it?", "I agree."]. Do not include markdown code block formats.
            """.trimIndent()

            val response = GeminiClient.generateResponse(prompt, "You are a smart reply suggestion service. Return only valid string arrays.")
            
            // Extract strings manually to avoid JSON parsing exceptions
            try {
                val replies = response.replace("[", "")
                    .replace("]", "")
                    .replace("\"", "")
                    .split(",")
                    .map { it.trim() }
                    .filter { it.isNotEmpty() }
                    .take(3)
                
                if (replies.size == 3) {
                    _aiSmartReplies.value = replies
                } else {
                    _aiSmartReplies.value = listOf("Understood.", "Sounds good!", "Talk later.")
                }
            } catch (e: Exception) {
                _aiSmartReplies.value = listOf("Understood.", "Sounds good!", "Talk later.")
            }
        }
    }

    // --- Gemini Chatbot ---
    private val _chatbotMessages = MutableStateFlow<List<ChatBotMessage>>(listOf(
        ChatBotMessage("Hello, I am your decentralized AI assistant. How can I help you navigate the BlockWave network today?", false)
    ))
    val chatbotMessages: StateFlow<List<ChatBotMessage>> = _chatbotMessages.asStateFlow()

    private val _chatbotRole = MutableStateFlow("Friendly Companion")
    val chatbotRole: StateFlow<String> = _chatbotRole.asStateFlow()

    private val _chatbotModel = MutableStateFlow("gemini-3.5-flash")
    val chatbotModel: StateFlow<String> = _chatbotModel.asStateFlow()

    private val _isChatbotGenerating = MutableStateFlow(false)
    val isChatbotGenerating: StateFlow<Boolean> = _isChatbotGenerating.asStateFlow()

    fun setChatbotRole(role: String) {
        _chatbotRole.value = role
        _chatbotMessages.value = listOf(
            ChatBotMessage("Understood! System persona reconfigured to: **$role**. What would you like to discuss?", false)
        )
    }

    fun setChatbotModel(model: String) {
        _chatbotModel.value = model
    }

    fun sendChatbotMessage(text: String) {
        val userMsg = ChatBotMessage(text, true)
        _chatbotMessages.value = _chatbotMessages.value + userMsg

        viewModelScope.launch {
            _isChatbotGenerating.value = true
            val currentModel = _chatbotModel.value
            val currentRole = _chatbotRole.value
            val systemInstruction = getSystemInstructionForRole(currentRole)
            
            val historyPairs = _chatbotMessages.value.dropLast(1).map { it.text to it.isUser }
            val response = GeminiClient.generateChatResponse(text, currentModel, systemInstruction, historyPairs)
            
            _chatbotMessages.value = _chatbotMessages.value + ChatBotMessage(response, false)
            _isChatbotGenerating.value = false

            // Sync conversation to Firestore if authenticated
            FirebaseSyncManager.syncAICreationToFirestore("chatbot_history", text, response)
        }
    }

    fun clearChatbotHistory() {
        _chatbotMessages.value = listOf(
            ChatBotMessage("Chat history cleared. I am ready to start fresh as a ${_chatbotRole.value}.", false)
        )
    }

    private fun getSystemInstructionForRole(role: String): String {
        return when (role) {
            "Blockchain Expert" -> "You are an expert blockchain auditor and Web3 cryptographer. Answer questions about smart contracts, consensus mechanisms, encryption, and decentralized networks in an informative, authoritative manner."
            "Security Auditor" -> "You are a cyber security auditor, expert in E2EE cryptography (RSA, AES), IPFS architecture, and vulnerability analysis. Your answers must be sharp, highly technical, security-focused, and direct."
            "Creative Writer" -> "You are a creative cyberpunk writer and lore crafter. Express yourself in vivid, futuristic, high-tech/low-life metaphors. Be expressive and imaginative."
            else -> "You are a friendly, intelligent decentralized chat companion built for BlockWave. Help users understand end-to-end encryption, wallets, and IPFS, and chat about general topics."
        }
    }

    // --- Image Generator ---
    private val _generatedImageBase64 = MutableStateFlow<String?>(null)
    val generatedImageBase64: StateFlow<String?> = _generatedImageBase64.asStateFlow()

    private val _isGeneratingImage = MutableStateFlow(false)
    val isGeneratingImage: StateFlow<Boolean> = _isGeneratingImage.asStateFlow()

    private val _imageAspectRatio = MutableStateFlow("1:1")
    val imageAspectRatio: StateFlow<String> = _imageAspectRatio.asStateFlow()

    fun setAspectRatio(ratio: String) {
        _imageAspectRatio.value = ratio
    }

    fun generateImage(prompt: String) {
        viewModelScope.launch {
            _isGeneratingImage.value = true
            _generatedImageBase64.value = null
            
            val ratio = _imageAspectRatio.value
            val b64 = GeminiClient.generateImage(prompt, ratio)
            _generatedImageBase64.value = b64
            _isGeneratingImage.value = false

            if (b64 != null) {
                FirebaseSyncManager.syncAICreationToFirestore("generated_image", prompt, b64)
            }
        }
    }

    fun clearGeneratedImage() {
        _generatedImageBase64.value = null
    }

    // --- Music Generator ---
    private val _generatedMusicBase64 = MutableStateFlow<String?>(null)
    val generatedMusicBase64: StateFlow<String?> = _generatedMusicBase64.asStateFlow()

    private val _isGeneratingMusic = MutableStateFlow(false)
    val isGeneratingMusic: StateFlow<Boolean> = _isGeneratingMusic.asStateFlow()

    private val _isMusicShortClip = MutableStateFlow(true) // true = up to 30s lyria-3-clip-preview, false = lyria-3-pro-preview
    val isMusicShortClip: StateFlow<Boolean> = _isMusicShortClip.asStateFlow()

    fun setMusicDuration(isShort: Boolean) {
        _isMusicShortClip.value = isShort
    }

    fun generateMusic(prompt: String) {
        viewModelScope.launch {
            _isGeneratingMusic.value = true
            _generatedMusicBase64.value = null
            
            val isShort = _isMusicShortClip.value
            val b64 = GeminiClient.generateMusic(prompt, isShort)
            _generatedMusicBase64.value = b64
            _isGeneratingMusic.value = false

            if (b64 != null) {
                FirebaseSyncManager.syncAICreationToFirestore("generated_music", prompt, b64)
            }
        }
    }

    fun clearGeneratedMusic() {
        _generatedMusicBase64.value = null
    }

    // --- Firebase Sync & Auth ---
    val firebaseAuthState: StateFlow<FirebaseAuthState> = FirebaseSyncManager.authState

    fun signInWithGoogle(context: Context) {
        viewModelScope.launch {
            FirebaseSyncManager.signInWithGoogleCredential(context)
            // After auth, trigger Firestore profile sync if we have a wallet
            val credentials = _wallet.value
            if (credentials != null) {
                FirebaseSyncManager.syncProfileToFirestore(
                    walletAddress = credentials.walletAddress,
                    username = getProfileUsername(),
                    bio = getProfileBio(),
                    avatarUrl = getProfileAvatar()
                )
            }
        }
    }

    fun forceSandboxSignIn(username: String, email: String) {
        FirebaseSyncManager.forceSandboxSignIn(username, email)
        viewModelScope.launch {
            val credentials = _wallet.value
            if (credentials != null) {
                FirebaseSyncManager.syncProfileToFirestore(
                    walletAddress = credentials.walletAddress,
                    username = getProfileUsername(),
                    bio = getProfileBio(),
                    avatarUrl = getProfileAvatar()
                )
            }
        }
    }

    fun firebaseSignOut() {
        FirebaseSyncManager.signOut()
    }

    // --- WebRTC Simulated Voice & Video Calls ---

    fun startCall(friend: UserEntity, isVideo: Boolean) {
        _activeCall.value = CallSession(
            friend = friend,
            isVideo = isVideo,
            isMuted = false,
            isCamOff = false,
            isScreenSharing = false,
            isNoiseCancelled = true,
            status = "CONNECTING...",
            durationSecs = 0
        )
        // Simulate peer accept after 1.5 seconds
        viewModelScope.launch {
            kotlinx.coroutines.delay(1500)
            val session = _activeCall.value
            if (session != null) {
                _activeCall.value = session.copy(status = "CONNECTED (E2EE SECURE)")
                // Start timer
                while (_activeCall.value != null && _activeCall.value?.status?.startsWith("CONNECTED") == true) {
                    kotlinx.coroutines.delay(1000)
                    val s = _activeCall.value ?: break
                    _activeCall.value = s.copy(durationSecs = s.durationSecs + 1)
                }
            }
        }
    }

    fun toggleMute() {
        val s = _activeCall.value ?: return
        _activeCall.value = s.copy(isMuted = !s.isMuted)
    }

    fun toggleCamera() {
        val s = _activeCall.value ?: return
        _activeCall.value = s.copy(isCamOff = !s.isCamOff)
    }

    fun toggleScreenShare() {
        val s = _activeCall.value ?: return
        _activeCall.value = s.copy(isScreenSharing = !s.isScreenSharing)
    }

    fun toggleNoiseCancellation() {
        val s = _activeCall.value ?: return
        _activeCall.value = s.copy(isNoiseCancelled = !s.isNoiseCancelled)
    }

    fun endCall() {
        _activeCall.value = null
    }
}

// --- Screen Router ---
sealed class Screen {
    object Landing : Screen()
    object Login : Screen()
    object Dashboard : Screen()
    object Chats : Screen()
    object GroupChat : Screen()
    object CreateGroup : Screen()
    object Explorer : Screen()
    object AdminDashboard : Screen()
    object Settings : Screen()
    object Profile : Screen()
    object Friends : Screen()
    object AILabs : Screen() // Combined AI Features Hub
}

// --- AI Chatbot Data Classes ---
data class ChatBotMessage(
    val text: String,
    val isUser: Boolean,
    val timestamp: Long = System.currentTimeMillis()
)

// --- Call Session ---
data class CallSession(
    val friend: UserEntity,
    val isVideo: Boolean,
    val isMuted: Boolean,
    val isCamOff: Boolean,
    val isScreenSharing: Boolean,
    val isNoiseCancelled: Boolean,
    val status: String,
    val durationSecs: Int
) {
    fun formatDuration(): String {
        val mins = durationSecs / 60
        val secs = durationSecs % 60
        return String.format("%02d:%02d", mins, secs)
    }
}
