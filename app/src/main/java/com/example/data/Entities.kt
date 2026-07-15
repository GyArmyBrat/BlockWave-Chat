package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey val walletAddress: String,
    val username: String,
    val avatarUrl: String,
    val publicKey: String, // Base64 RSA Public Key for E2EE
    val status: String, // "ONLINE", "OFFLINE", "BLOCKED"
    val isFriend: Boolean,
    val isIncomingRequest: Boolean = false,
    val isOutgoingRequest: Boolean = false,
    val lastSeen: Long = System.currentTimeMillis(),
    val bio: String = ""
)

@Entity(tableName = "messages")
data class MessageEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val senderWallet: String,
    val receiverWallet: String?, // Null if group chat
    val groupId: String?, // Null if 1-to-1 chat
    val encryptedPayload: String, // Base64 AES encrypted message
    val encryptedAesKey: String?, // Base64 encrypted AES key (only for 1-to-1)
    val plainText: String, // Decrypted preview (or text for self-sent, decrypted locally)
    val timestamp: Long = System.currentTimeMillis(),
    val messageHash: String, // SHA-256 message hash
    val ipfsCid: String, // Simulated IPFS CID
    val transactionHash: String, // Simulated Polygon transaction hash
    val isRead: Boolean = false,
    val deliveryStatus: String = "SENT", // "SENT", "DELIVERED", "READ"
    val attachmentType: String = "TEXT", // "TEXT", "IMAGE", "VIDEO", "FILE", "VOICE", "LOCATION"
    val attachmentUrl: String? = null,
    val attachmentName: String? = null,
    val signature: String = "" // Sender's digital signature of the hash
)

@Entity(tableName = "groups")
data class GroupEntity(
    @PrimaryKey val id: String,
    val name: String,
    val description: String,
    val imageUrl: String,
    val adminAddresses: String, // Comma-separated list
    val memberAddresses: String, // Comma-separated list
    val isPinned: Boolean = false,
    val inviteLink: String = "",
    val pinMessageId: Long? = null
)

@Entity(tableName = "blockchain_tx")
data class BlockchainTxEntity(
    @PrimaryKey val txHash: String,
    val blockNumber: Long,
    val fromAddress: String,
    val toAddress: String,
    val gasUsed: Long,
    val eventType: String, // "MessageSent", "UserRegistered", "GroupCreated", "CIDStored"
    val cid: String?,
    val messageHash: String?,
    val timestamp: Long = System.currentTimeMillis()
)
