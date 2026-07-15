package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {
    @Query("SELECT * FROM users WHERE isFriend = 1")
    fun getFriends(): Flow<List<UserEntity>>

    @Query("SELECT * FROM users WHERE isIncomingRequest = 1")
    fun getIncomingRequests(): Flow<List<UserEntity>>

    @Query("SELECT * FROM users WHERE isOutgoingRequest = 1")
    fun getOutgoingRequests(): Flow<List<UserEntity>>

    @Query("SELECT * FROM users WHERE status = 'BLOCKED'")
    fun getBlockedUsers(): Flow<List<UserEntity>>

    @Query("SELECT * FROM users WHERE walletAddress = :address LIMIT 1")
    suspend fun getUserByAddress(address: String): UserEntity?

    @Query("SELECT * FROM users WHERE walletAddress = :address LIMIT 1")
    fun getUserByAddressFlow(address: String): Flow<UserEntity?>

    @Query("SELECT * FROM users")
    fun getAllUsersFlow(): Flow<List<UserEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateUser(user: UserEntity)

    @Delete
    suspend fun deleteUser(user: UserEntity)

    @Query("UPDATE users SET status = :status WHERE walletAddress = :address")
    suspend fun updateUserStatus(address: String, status: String)

    @Query("UPDATE users SET isFriend = :isFriend, isIncomingRequest = :isIncoming, isOutgoingRequest = :isOutgoing WHERE walletAddress = :address")
    suspend fun updateFriendStatus(address: String, isFriend: Boolean, isIncoming: Boolean, isOutgoing: Boolean)
}

@Dao
interface MessageDao {
    @Query("SELECT * FROM messages ORDER BY timestamp ASC")
    fun getAllMessagesFlow(): Flow<List<MessageEntity>>

    @Query("SELECT * FROM messages WHERE (senderWallet = :wallet1 AND receiverWallet = :wallet2) OR (senderWallet = :wallet2 AND receiverWallet = :wallet1) ORDER BY timestamp ASC")
    fun getChatMessagesFlow(wallet1: String, wallet2: String): Flow<List<MessageEntity>>

    @Query("SELECT * FROM messages WHERE groupId = :groupId ORDER BY timestamp ASC")
    fun getGroupMessagesFlow(groupId: String): Flow<List<MessageEntity>>

    @Query("SELECT * FROM messages WHERE id = :id LIMIT 1")
    suspend fun getMessageById(id: Long): MessageEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: MessageEntity): Long

    @Query("DELETE FROM messages WHERE id = :id")
    suspend fun deleteMessageById(id: Long)

    @Query("DELETE FROM messages WHERE senderWallet = :wallet1 AND receiverWallet = :wallet2")
    suspend fun clearChatMessages(wallet1: String, wallet2: String)

    @Query("UPDATE messages SET deliveryStatus = :status WHERE id = :id")
    suspend fun updateMessageStatus(id: Long, status: String)
}

@Dao
interface GroupDao {
    @Query("SELECT * FROM groups")
    fun getAllGroupsFlow(): Flow<List<GroupEntity>>

    @Query("SELECT * FROM groups WHERE id = :groupId LIMIT 1")
    suspend fun getGroupById(groupId: String): GroupEntity?

    @Query("SELECT * FROM groups WHERE id = :groupId LIMIT 1")
    fun getGroupByIdFlow(groupId: String): Flow<GroupEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGroup(group: GroupEntity)

    @Query("DELETE FROM groups WHERE id = :groupId")
    suspend fun deleteGroupById(groupId: String)
}

@Dao
interface BlockchainTxDao {
    @Query("SELECT * FROM blockchain_tx ORDER BY timestamp DESC")
    fun getAllTransactionsFlow(): Flow<List<BlockchainTxEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(tx: BlockchainTxEntity)

    @Query("SELECT COUNT(*) FROM blockchain_tx")
    fun getTxCountFlow(): Flow<Int>

    @Query("SELECT SUM(gasUsed) FROM blockchain_tx")
    fun getTotalGasUsedFlow(): Flow<Long?>
}
