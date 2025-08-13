package com.hkapps.messagepro.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "message_attachments")
data class AttachmentSMSModel(
    @PrimaryKey val id: Long,
    @ColumnInfo(name = "text") var text: String,
    @ColumnInfo(name = "attachments") var attachments: ArrayList<AttachmentModel>)
