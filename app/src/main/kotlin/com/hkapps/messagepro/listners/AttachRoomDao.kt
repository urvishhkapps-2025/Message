package com.hkapps.messagepro.listners

import androidx.room.Dao
import androidx.room.Query
import com.hkapps.messagepro.model.AttachmentModel

@Dao
interface AttachRoomDao {
    @Query("SELECT * FROM attachments")
    fun getAllList(): List<AttachmentModel>
}
