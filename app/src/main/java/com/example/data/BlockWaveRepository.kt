package com.example.data

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.example.crypto.CryptoUtils
import com.example.crypto.WalletCredentials
import kotlinx.coroutines.flow.Flow
import java.util.UUID

class BlockWaveRepository(
    private val context: Context,
    private val userDao: UserDao,
    private val messageDao: MessageDao,
    private val groupDao: GroupDao,
    private val blockchainTxDao: BlockchainTxDao
) {
    private val prefs: SharedPreferences = context.getSharedPreferences("blockwave_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_WALLET_ADDRESS = "wallet_address"
        private const val KEY_PRIVATE_KEY = "private_key"
        private const val KEY_PUBLIC_KEY = "public_key"
        private const val KEY_PRIVATE_RSA_KEY = "private_rsa_key"
        private const val KEY_BALANCE = "balance"
        private const val KEY_SEED_PHRASE = "seed_phrase"
        private const val KEY_USERNAME = "username"
        private const val KEY_BIO = "bio"
        private const val KEY_AVATAR_URL = "avatar_url"
    }

    // --- Active Wallet Configuration ---

    fun hasWallet(): Boolean {
        return prefs.contains(KEY_WALLET_ADDRESS)
    }

    fun getWalletCredentials(): WalletCredentials? {
        val address = prefs.getString(KEY_WALLET_ADDRESS, null) ?: return null
        val privKey = prefs.getString(KEY_PRIVATE_KEY, "") ?: ""
        val pubKey = prefs.getString(KEY_PUBLIC_KEY, "") ?: ""
        val privRsa = prefs.getString(KEY_PRIVATE_RSA_KEY, "") ?: ""
        val balance = prefs.getFloat(KEY_BALANCE, 0f).toDouble()
        val seed = prefs.getString(KEY_SEED_PHRASE, "") ?: ""
        return WalletCredentials(address, privKey, pubKey, privRsa, balance, seed)
    }

    fun getProfileUsername(): String {
        return prefs.getString(KEY_USERNAME, "wave_user") ?: "wave_user"
    }

    fun getProfileBio(): String {
        return prefs.getString(KEY_BIO, "Decentralized. Secure. BlockWave.") ?: "Decentralized. Secure. BlockWave."
    }

    fun getProfileAvatar(): String {
        return prefs.getString(KEY_AVATAR_URL, "https://api.dicebear.com/7.x/identicon/svg?seed=wave") ?: "https://api.dicebear.com/7.x/identicon/svg?seed=wave"
    }

    suspend fun saveProfile(username: String, bio: String, avatarUrl: String) {
        prefs.edit().apply {
            putString(KEY_USERNAME, username)
            putString(KEY_BIO, bio)
            putString(KEY_AVATAR_URL, avatarUrl)
            apply()
        }
        val credentials = getWalletCredentials()
        if (credentials != null) {
            // Update our own representation in the database (or notify friends in simulated flow)
            val selfUser = UserEntity(
                walletAddress = credentials.walletAddress,
                username = username,
                avatarUrl = avatarUrl,
                publicKey = credentials.publicKey,
                status = "ONLINE",
                isFriend = false,
                bio = bio
            )
            userDao.insertOrUpdateUser(selfUser)
        }
    }

    suspend fun createNewWallet(username: String): WalletCredentials {
        val credentials = CryptoUtils.generateSimulatedWallet()
        saveWalletToPrefs(credentials, username)
        
        // Also add our first block in the blockchain ledger
        addSimulatedTransaction(
            from = "0x0000000000000000000000000000000000000000",
            to = credentials.walletAddress,
            eventType = "UserRegistered",
            cid = "QmWaveWelcomeInitSignatureHash",
            messageHash = CryptoUtils.sha256("Registered username: $username")
        )

        return credentials
    }

    suspend fun importWallet(seedPhrase: String, username: String): WalletCredentials {
        // Recover/Import wallet
        val credentials = CryptoUtils.generateSimulatedWallet() // In simulation we derive a valid structure
        val recoveredCredentials = credentials.copy(seedPhrase = seedPhrase)
        saveWalletToPrefs(recoveredCredentials, username)

        addSimulatedTransaction(
            from = "0x0000000000000000000000000000000000000000",
            to = recoveredCredentials.walletAddress,
            eventType = "UserRegistered",
            cid = "QmWaveRestoreInitSignatureHash",
            messageHash = CryptoUtils.sha256("Recovered seed phrase: $username")
        )

        return recoveredCredentials
    }

    private fun saveWalletToPrefs(credentials: WalletCredentials, username: String) {
        prefs.edit().apply {
            putString(KEY_WALLET_ADDRESS, credentials.walletAddress)
            putString(KEY_PRIVATE_KEY, credentials.privateKey)
            putString(KEY_PUBLIC_KEY, credentials.publicKey)
            putString(KEY_PRIVATE_RSA_KEY, credentials.privateRsaKey)
            putFloat(KEY_BALANCE, credentials.balance.toFloat())
            putString(KEY_SEED_PHRASE, credentials.seedPhrase)
            putString(KEY_USERNAME, username)
            apply()
        }
    }

    fun deleteWallet() {
        prefs.edit().clear().apply()
    }

    // --- Simulated Transaction Ledger Engine ---

    val transactions: Flow<List<BlockchainTxEntity>> = blockchainTxDao.getAllTransactionsFlow()
    val txCount: Flow<Int> = blockchainTxDao.getTxCountFlow()
    val totalGasUsed: Flow<Long?> = blockchainTxDao.getTotalGasUsedFlow()

    suspend fun addSimulatedTransaction(
        from: String,
        to: String,
        eventType: String,
        cid: String?,
        messageHash: String?
    ): BlockchainTxEntity {
        // Generate a random standard tx hash
        val randomTxHash = "0x" + UUID.randomUUID().toString().replace("-", "").take(64)
        val latestBlockNumber = System.currentTimeMillis() / 10000 // Simple increasing block index
        val gasUsed = (21000..65000).random().toLong()

        val tx = BlockchainTxEntity(
            txHash = randomTxHash,
            blockNumber = latestBlockNumber,
            fromAddress = from,
            toAddress = to,
            gasUsed = gasUsed,
            eventType = eventType,
            cid = cid,
            messageHash = messageHash,
            timestamp = System.currentTimeMillis()
        )
        blockchainTxDao.insertTransaction(tx)

        // Adjust gas/balance cost in prefs
        val credentials = getWalletCredentials()
        if (credentials != null && from == credentials.walletAddress) {
            val gasCost = gasUsed * 0.000005 // simulated POL price
            val newBalance = maxOf(0.0, credentials.balance - gasCost)
            prefs.edit().putFloat(KEY_BALANCE, newBalance.toFloat()).apply()
        }

        return tx
    }

    suspend fun triggerFaucet() {
        val credentials = getWalletCredentials() ?: return
        val newBalance = credentials.balance + 10.0 // Add 10 MATIC/POL
        prefs.edit().putFloat(KEY_BALANCE, newBalance.toFloat()).apply()

        addSimulatedTransaction(
            from = "0xFaucetContractPolygonTestnetWaveEngine",
            to = credentials.walletAddress,
            eventType = "CIDStored",
            cid = "QmFaucetRequestSuccess",
            messageHash = CryptoUtils.sha256("Faucet +10 POL")
        )
    }

    // --- Message Exchange with E2EE Cryptography ---

    val allMessages: Flow<List<MessageEntity>> = messageDao.getAllMessagesFlow()

    fun getChatMessages(friendAddress: String): Flow<List<MessageEntity>> {
        val myAddress = getWalletCredentials()?.walletAddress ?: ""
        return messageDao.getChatMessagesFlow(myAddress, friendAddress)
    }

    fun getGroupMessages(groupId: String): Flow<List<MessageEntity>> {
        return messageDao.getGroupMessagesFlow(groupId)
    }

    suspend fun sendOneToOneMessage(
        recipientAddress: String,
        plainText: String,
        attachmentType: String = "TEXT",
        attachmentUrl: String? = null,
        attachmentName: String? = null
    ): Long {
        val myWallet = getWalletCredentials() ?: throw IllegalStateException("Wallet not initialized")
        val recipient = userDao.getUserByAddress(recipientAddress) ?: throw IllegalArgumentException("Recipient not found")

        // 1. Compute SHA-256 hash of plaintext
        val messageHash = CryptoUtils.sha256(plainText)

        // 2. Encrypt plaintext using AES-256 session key
        val aesKey = CryptoUtils.generateAesKey()
        val encryptedPayload = CryptoUtils.encryptAes(plainText, aesKey)

        // 3. Encrypt AES key bytes using recipient's RSA Public Key
        val encryptedAesKey = CryptoUtils.encryptRsa(aesKey.encoded, recipient.publicKey)

        // 4. Generate digital signature using our RSA Private Key
        val signature = CryptoUtils.signData(messageHash, myWallet.privateRsaKey)

        // 5. Generate simulated IPFS CID
        val ipfsCid = "Qm" + UUID.randomUUID().toString().replace("-", "").take(44)

        // 6. Generate simulated Polygon transaction hash via blockchain record
        val tx = addSimulatedTransaction(
            from = myWallet.walletAddress,
            to = recipientAddress,
            eventType = "MessageSent",
            cid = ipfsCid,
            messageHash = messageHash
        )

        // 7. Save message to local database (we also store plainText for our own view)
        val message = MessageEntity(
            senderWallet = myWallet.walletAddress,
            receiverWallet = recipientAddress,
            groupId = null,
            encryptedPayload = encryptedPayload,
            encryptedAesKey = encryptedAesKey,
            plainText = plainText, // decrypted copy for our history UI
            messageHash = messageHash,
            ipfsCid = ipfsCid,
            transactionHash = tx.txHash,
            isRead = true,
            deliveryStatus = "SENT",
            attachmentType = attachmentType,
            attachmentUrl = attachmentUrl,
            attachmentName = attachmentName,
            signature = signature
        )

        val id = messageDao.insertMessage(message)

        // Simulated Auto-Reply / Chat Partner network event
        simulateIncomingResponse(recipient, plainText)

        return id
    }

    private suspend fun simulateIncomingResponse(friend: UserEntity, originalMsg: String) {
        // If friend is not BLOCKED, let's trigger a realistic reply after a delay
        if (friend.status == "BLOCKED" || friend.walletAddress == "0xGeminiAIAssistantAddress") return

        // We can simulate an automated decentralized state response
        // Let's create a list of contextual auto-replies or call Gemini if it's the AI friend!
        val responseText = if (friend.walletAddress == "0xAiAssistantAddress") {
            // This is handled by a direct VM AI assistant stream, but let's allow general replies
            "I am ready to assist you. Use the AI Smart Reply tool to compose custom blockchain logic!"
        } else {
            val dynamicReplies = listOf(
                "Got your message! Verified signature and IPFS hash successfully on-chain.",
                "Yes, E2EE check passed. RSA-2048 private key locally decrypted session AES.",
                "Let's schedule a WebRTC call. Is your connection gas-ready?",
                "Awesome, block confirmation looks good. Talk soon!",
                "Confirmed. This message is secure."
            )
            dynamicReplies.random()
        }

        // Simulate delivery delay
        kotlinx.coroutines.delay(2000)

        val myWallet = getWalletCredentials() ?: return
        val responseHash = CryptoUtils.sha256(responseText)

        // Since it's incoming, the sender is the friend, recipient is us
        val friendAesKey = CryptoUtils.generateAesKey()
        val encryptedPayload = CryptoUtils.encryptAes(responseText, friendAesKey)
        val encryptedAesKey = CryptoUtils.encryptRsa(friendAesKey.encoded, myWallet.publicKey)
        val signature = CryptoUtils.signData(responseHash, friend.publicKey) // Simulated key sign

        val ipfsCid = "Qm" + UUID.randomUUID().toString().replace("-", "").take(44)
        val tx = addSimulatedTransaction(
            from = friend.walletAddress,
            to = myWallet.walletAddress,
            eventType = "MessageSent",
            cid = ipfsCid,
            messageHash = responseHash
        )

        val message = MessageEntity(
            senderWallet = friend.walletAddress,
            receiverWallet = myWallet.walletAddress,
            groupId = null,
            encryptedPayload = encryptedPayload,
            encryptedAesKey = encryptedAesKey,
            plainText = responseText, // Decrypted locally
            messageHash = responseHash,
            ipfsCid = ipfsCid,
            transactionHash = tx.txHash,
            isRead = false,
            deliveryStatus = "READ",
            attachmentType = "TEXT",
            signature = signature
        )

        messageDao.insertMessage(message)
    }

    suspend fun sendGroupMessage(
        groupId: String,
        plainText: String,
        attachmentType: String = "TEXT",
        attachmentUrl: String? = null,
        attachmentName: String? = null
    ): Long {
        val myWallet = getWalletCredentials() ?: throw IllegalStateException("Wallet not initialized")
        val group = groupDao.getGroupById(groupId) ?: throw IllegalArgumentException("Group not found")

        val messageHash = CryptoUtils.sha256(plainText)
        val aesKey = CryptoUtils.generateAesKey()
        val encryptedPayload = CryptoUtils.encryptAes(plainText, aesKey)

        val ipfsCid = "Qm" + UUID.randomUUID().toString().replace("-", "").take(44)
        val tx = addSimulatedTransaction(
            from = myWallet.walletAddress,
            to = group.id,
            eventType = "CIDStored",
            cid = ipfsCid,
            messageHash = messageHash
        )

        val signature = CryptoUtils.signData(messageHash, myWallet.privateRsaKey)

        val message = MessageEntity(
            senderWallet = myWallet.walletAddress,
            receiverWallet = null,
            groupId = groupId,
            encryptedPayload = encryptedPayload,
            encryptedAesKey = null, // In multi-user chat, key is managed differently
            plainText = plainText,
            messageHash = messageHash,
            ipfsCid = ipfsCid,
            transactionHash = tx.txHash,
            isRead = true,
            deliveryStatus = "SENT",
            attachmentType = attachmentType,
            attachmentUrl = attachmentUrl,
            attachmentName = attachmentName,
            signature = signature
        )

        return messageDao.insertMessage(message)
    }

    // --- Friends and Contacts Logic ---

    val friends: Flow<List<UserEntity>> = userDao.getFriends()
    val incomingRequests: Flow<List<UserEntity>> = userDao.getIncomingRequests()
    val outgoingRequests: Flow<List<UserEntity>> = userDao.getOutgoingRequests()
    val blockedUsers: Flow<List<UserEntity>> = userDao.getBlockedUsers()
    val allUsers: Flow<List<UserEntity>> = userDao.getAllUsersFlow()

    suspend fun searchAndAddFriendByWallet(address: String, username: String) {
        // Validate address syntax
        if (!address.startsWith("0x") || address.length != 42) {
            throw IllegalArgumentException("Invalid Ethereum / Polygon wallet address format.")
        }

        // Generate matching cryptographic keys for E2EE
        val (_, mockPubKey) = CryptoUtils.generateRsaKeyPair()

        val newUser = UserEntity(
            walletAddress = address,
            username = username,
            avatarUrl = "https://api.dicebear.com/7.x/identicon/svg?seed=$username",
            publicKey = mockPubKey,
            status = "ONLINE",
            isFriend = true, // Auto-friend for seamless prototyping
            bio = "Decentralized Wave Node | Web3 Developer"
        )
        userDao.insertOrUpdateUser(newUser)

        addSimulatedTransaction(
            from = getWalletCredentials()?.walletAddress ?: "0x",
            to = address,
            eventType = "UserRegistered",
            cid = "QmAddFriendSuccess",
            messageHash = CryptoUtils.sha256("Connected with friend $username")
        )
    }

    suspend fun updateFriendship(address: String, status: String) {
        userDao.updateUserStatus(address, status)
    }

    suspend fun removeFriend(address: String) {
        val user = userDao.getUserByAddress(address)
        if (user != null) {
            userDao.insertOrUpdateUser(user.copy(isFriend = false))
        }
    }

    suspend fun blockUser(address: String) {
        userDao.updateUserStatus(address, "BLOCKED")
    }

    suspend fun clearChatHistory(address: String) {
        val myAddress = getWalletCredentials()?.walletAddress ?: ""
        messageDao.clearChatMessages(myAddress, address)
    }

    // --- Group Management ---

    val groups: Flow<List<GroupEntity>> = groupDao.getAllGroupsFlow()

    suspend fun createGroup(name: String, description: String, imageUrl: String, inviteLink: String = ""): String {
        val myWallet = getWalletCredentials() ?: throw IllegalStateException("Wallet not initialized")
        val groupId = "group_" + UUID.randomUUID().toString().take(8)

        val newGroup = GroupEntity(
            id = groupId,
            name = name,
            description = description,
            imageUrl = imageUrl.ifEmpty { "https://api.dicebear.com/7.x/initials/svg?seed=$name" },
            adminAddresses = myWallet.walletAddress,
            memberAddresses = myWallet.walletAddress,
            inviteLink = inviteLink.ifEmpty { "https://blockwave.chat/invite/$groupId" }
        )

        groupDao.insertGroup(newGroup)

        addSimulatedTransaction(
            from = myWallet.walletAddress,
            to = groupId,
            eventType = "GroupCreated",
            cid = "QmGroupCreated" + groupId,
            messageHash = CryptoUtils.sha256("Group: $name")
        )

        return groupId
    }

    // --- Prepopulate mock network users & assistants ---

    suspend fun prepopulateDatabase() {
        // Create standard AI and Developer companion nodes to give a highly active chat experience on first load!
        val myWallet = getWalletCredentials() ?: return

        val companions = listOf(
            UserEntity(
                walletAddress = "0xAiAssistantAddress",
                username = "WaveAI Companion",
                avatarUrl = "https://api.dicebear.com/7.x/bottts/svg?seed=waveai",
                publicKey = CryptoUtils.generateRsaKeyPair().second,
                status = "ONLINE",
                isFriend = true,
                bio = "Wave Network native AI node. Powered by Gemini. E2E Encryption capable."
            ),
            UserEntity(
                walletAddress = "0xPolygonCreatorVitalikNode",
                username = "Vitalik Wave Node",
                avatarUrl = "https://api.dicebear.com/7.x/identicon/svg?seed=vitalik",
                publicKey = CryptoUtils.generateRsaKeyPair().second,
                status = "ONLINE",
                isFriend = true,
                bio = "Ethereum co-founder simulation node. Checking decentralized messaging consensus."
            ),
            UserEntity(
                walletAddress = "0xSatoshiNakamotoDecentralNode",
                username = "Satoshi Nakamoto",
                avatarUrl = "https://api.dicebear.com/7.x/pixel-art/svg?seed=satoshi",
                publicKey = CryptoUtils.generateRsaKeyPair().second,
                status = "OFFLINE",
                isFriend = true,
                bio = "Vires in Numeris. The ultimate genesis node."
            )
        )

        for (c in companions) {
            if (userDao.getUserByAddress(c.walletAddress) == null) {
                userDao.insertOrUpdateUser(c)
            }
        }

        // Add an initial welcome transaction
        val welcomeCount = userDao.getFriends()
        Log.d("BlockWaveRepo", "Prepopulated nodes successfully.")
    }
}
