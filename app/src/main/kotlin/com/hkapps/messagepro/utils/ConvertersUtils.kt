package com.hkapps.messagepro.utils

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.hkapps.messagepro.model.ContactsModel
import com.hkapps.messagepro.model.AttachmentModel
import com.hkapps.messagepro.model.AttachmentSMSModel

class ConvertersUtils {
    private val gson = Gson()
    private val attachmentType = object : TypeToken<List<AttachmentModel>>() {}.type
    private val simpleContactType = object : TypeToken<List<ContactsModel>>() {}.type
    private val messageAttachmentType = object : TypeToken<AttachmentSMSModel?>() {}.type

    @TypeConverter
    fun jsonToAttachmentList(value: String) = gson.fromJson<ArrayList<AttachmentModel>>(value, attachmentType)

    @TypeConverter
    fun attachmentListToJson(list: ArrayList<AttachmentModel>) = gson.toJson(list)

    @TypeConverter
    fun jsonToSimpleContactList(value: String) = gson.fromJson<ArrayList<ContactsModel>>(value, simpleContactType)

    @TypeConverter
    fun simpleContactListToJson(list: ArrayList<ContactsModel>) = gson.toJson(list)

    @TypeConverter
    fun jsonToMessageAttachment(value: String) = gson.fromJson<AttachmentSMSModel>(value, messageAttachmentType)

    @TypeConverter
    fun messageAttachmentToJson(messageAttachment: AttachmentSMSModel?) = gson.toJson(messageAttachment)
}
