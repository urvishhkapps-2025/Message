package com.hkapps.messagepro.listners

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.hkapps.messagepro.model.MessagesModel

@Dao
interface DaoListner {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertOrUpdateMessage(message: MessagesModel)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insertOrIgnoreMessage(message: MessagesModel): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertAddMessages(vararg message: MessagesModel)

    @Query("SELECT * FROM messages")
    fun getAllList(): List<MessagesModel>

    @Query("SELECT * FROM messages WHERE thread_id = :threadId")
    fun getThreadMessages(threadId: Long): List<MessagesModel>

    @Query("SELECT * FROM messages WHERE body LIKE :text")
    fun getMessagesWithText(text: String): List<MessagesModel>

    @Query("UPDATE messages SET read = 1 WHERE id = :id")
    fun markReadAsMessage(id: Long)

    @Query("UPDATE messages SET read = 1 WHERE thread_id = :threadId")
    fun markThreadRead(threadId: Long)

    @Query("UPDATE messages SET type = :type WHERE id = :id")
    fun updateType(id: Long, type: Int): Int

    @Query("UPDATE messages SET status = :status WHERE id = :id")
    fun updateStatusMessages(id: Long, status: Int): Int

    @Query("DELETE FROM messages WHERE id = :id")
    fun delete(id: Long)

    @Query("DELETE FROM messages WHERE thread_id = :threadId")
    fun deleteThreadMessagesApp(threadId: Long)
}
