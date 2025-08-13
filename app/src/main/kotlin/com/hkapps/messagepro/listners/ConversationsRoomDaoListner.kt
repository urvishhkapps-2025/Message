package com.hkapps.messagepro.listners

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.hkapps.messagepro.model.ConversationSmsModel

@Dao
interface ConversationsRoomDaoListner {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertOrUpdateMessage(conversation: ConversationSmsModel): Long

    @Query("SELECT * FROM conversations")
    fun getAllList(): List<ConversationSmsModel>

    @Query("SELECT * FROM conversations WHERE read = 0")
    fun getUnreadConversationsMessage(): List<ConversationSmsModel>

    @Query("SELECT * FROM conversations WHERE title LIKE :text")
    fun getConversationsWithTextMessage(text: String): List<ConversationSmsModel>

    @Query("UPDATE conversations SET read = 1 WHERE thread_id = :threadId")
    fun markReadAsMessage(threadId: Long)

    @Query("UPDATE conversations SET read = 0 WHERE thread_id = :threadId")
    fun markUnreadAsMessage(threadId: Long)

    @Query("DELETE FROM conversations WHERE thread_id = :threadId")
    fun deleteThreadIdMessage(threadId: Long)
}
