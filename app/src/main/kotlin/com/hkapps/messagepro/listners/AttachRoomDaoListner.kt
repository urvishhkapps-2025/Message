package com.hkapps.messagepro.listners

import androidx.room.Dao
import androidx.room.Query
import com.hkapps.messagepro.model.AttachmentSMSModel

@Dao
interface AttachRoomDaoListner {
    @Query("SELECT * FROM message_attachments")
    fun getAllList(): List<AttachmentSMSModel>
}
