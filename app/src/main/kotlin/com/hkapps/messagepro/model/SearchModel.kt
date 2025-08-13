package com.hkapps.messagepro.model

class SearchModel {

    var messageId: Long? = null
    var title: String? = ""
    var snippet: String? = ""
    var date: String? = ""
    var threadId: Long? = null
    var photoUri: String? = ""
    var phoneNumber: String? = ""


//    constructor(messageId: Long?, title: String?, snippet: String?, date: String?, threadId: Long?, photoUri: String?) {
//        this.messageId = messageId
//        this.title = title
//        this.snippet = snippet
//        this.date = date
//        this.threadId = threadId
//        this.photoUri = photoUri
//    }

    constructor(messageId: Long?, title: String?, snippet: String?, date: String?, threadId: Long?, photoUri: String?, phoneNumber: String?) {
        this.messageId = messageId
        this.title = title
        this.snippet = snippet
        this.date = date
        this.threadId = threadId
        this.photoUri = photoUri
        this.phoneNumber = phoneNumber
    }


}
