package com.hkapps.messagepro.activity

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextUtils
import android.text.TextWatcher
import com.hkapps.messagepro.MainAppClass
import com.hkapps.messagepro.R
import com.hkapps.messagepro.adapter.SearchAdapter
import com.hkapps.messagepro.adapter.SuggestionAdapter
import com.hkapps.messagepro.ads.AdsHelperClass
import com.hkapps.messagepro.databinding.ActivitySearchMessageBinding
import com.hkapps.messagepro.extensions.*
import com.hkapps.messagepro.model.ContactsModel
import com.hkapps.messagepro.model.ConversationSmsModel
import com.hkapps.messagepro.model.MessagesModel
import com.hkapps.messagepro.model.SearchModel
import com.hkapps.messagepro.utils.*

class SearchMessagesActivity : BaseHomeActivity() {
    private var mLastSearchedText = ""

    private lateinit var binding: ActivitySearchMessageBinding

    @SuppressLint("InlinedApi")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySearchMessageBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupSearch()

        binding.tvCancel.setOnClickListener {
            onBackPressed()
        }

        binding.ivSeachClear.setOnClickListener {
            binding.etSearchText.setText("")
        }

        ensureBackgroundThread {
            val messagesAll = messagesDB.getAllList()

            val searchKeyCredit = "Credit"
            val searchQueryCredit = "%$searchKeyCredit%"
            val messagesCredit = messagesDB.getMessagesWithText(searchQueryCredit)

            val searchKeyDebit = "Debit"
            val searchQueryDebit = "%$searchKeyDebit%"
            val messagesDebit = messagesDB.getMessagesWithText(searchQueryDebit)

            val searchKeyOTP = "OTP"
            val searchQueryOTP = "%$searchKeyOTP%"
            val messagesOTP = messagesDB.getMessagesWithText(searchQueryOTP)

            val searchKeyLink = "Link"
            val searchQueryLink = "%$searchKeyLink%"
            val messagesLink = messagesDB.getMessagesWithText(searchQueryLink)


            val messagesLocal = messagesAll - messagesCredit - messagesDebit - messagesOTP - messagesLink

            val messages = ArrayList<MessagesModel>()
            var mAllContacts: ArrayList<ContactsModel>
            val mContactsNumber = ArrayList<String>()

            SimpleContactsHelperUtils(this).getAvailableContacts(false) {
                mAllContacts = it

                for (item in mAllContacts) {
                    mContactsNumber.add(item.phoneNumbers.toString().replace("[", "").replace("]", ""))
                }

                for (messageItem in messagesLocal) {
                    val messageNum = messageItem.participants.get(0).phoneNumbers
                    for (contactNumber in mContactsNumber) {
                        if (messageNum.contains(contactNumber)) {
                            messages.add(messageItem)
                        }
                    }
                }
            }

            val sortedConversationsmessages = messages.sortedWith(
                compareByDescending<MessagesModel> { config.pinnedConversations.contains(it.threadId.toString()) }
                    .thenByDescending { it.date }
            ).toMutableList() as ArrayList<MessagesModel>

            val messagesNew = ArrayList<MessagesModel>()
            for (i in sortedConversationsmessages.indices) {
                if (i == 0) {
                    messagesNew.add(sortedConversationsmessages.get(i))
                } else {
                    if (!checkIsInList(sortedConversationsmessages.get(i).participants.get(0).phoneNumbers.toString(), messagesNew)) {
                        messagesNew.add(sortedConversationsmessages.get(i))
                    }
                }
            }
            runOnUiThread {
                val searchResults = ArrayList<SearchModel>()
                messagesNew.forEach { message ->
                    var recipient = message.senderName
                    val phoneNumber = message.participants[0].phoneNumbers[0]
                    if (recipient.isEmpty() && message.participants.isNotEmpty()) {
                        val participantNames = message.participants.map { it.name }
                        recipient = TextUtils.join(", ", participantNames)
                    }

                    val date = message.date.formatDateOrTime(this, true, true)
                    val searchResult = SearchModel(message.id, recipient, message.body, date, message.threadId, message.senderPhotoUri, phoneNumber)
                    searchResults.add(searchResult)
                }

                SuggestionAdapter(this, searchResults, binding.recyclerViewsSuggestions) {
                    val intentApp = Intent(this, MessagesActivity::class.java)
                    intentApp.putExtra(THREAD_ID, (it as SearchModel).threadId)
                    intentApp.putExtra(THREAD_TITLE, it.title)
                    intentApp.putExtra(THREAD_NUMBER, it.phoneNumber)
                    intentApp.putExtra(SEARCHED_MESSAGE_ID, it.messageId)
                    MainAppClass.instance?.displayInterstitialAds(this, intentApp, false)

                }.apply {
                    binding.recyclerViewsSuggestions.adapter = this
                }
            }
        }
    }


    fun setupSearch() {
        binding.etSearchText.requestFocus()
        binding.etSearchText.apply {

            addTextChangedListener(object : TextWatcher {
                override fun afterTextChanged(s: Editable?) {
                }

                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                }

                override fun onTextChanged(newText: CharSequence?, start: Int, before: Int, count: Int) {
                    mLastSearchedText = newText.toString()
                    textChanged(newText.toString())
                }
            })
        }
    }

    fun checkIsInList(phoneNumber: String, messagesNew: ArrayList<MessagesModel>): Boolean {
        for (contactNumber in messagesNew) {
            if (phoneNumber.contains(contactNumber.participants[0].phoneNumbers.toString())) {
                return true
            }
        }
        return false
    }

    private fun textChanged(text: String) {
        binding.tvNoData2.beGoneIf(text.length >= 2)
        if (text.length >= 2) {
            binding.ivSeachClear.beVisible()
            ensureBackgroundThread {
                val searchQuery = "%$text%"
                val messages = messagesDB.getMessagesWithText(searchQuery)
                val conversations = conversationsDB.getConversationsWithTextMessage(searchQuery)
                if (text == mLastSearchedText) {
                    showSearchResults(messages, conversations, text)
                }
            }
        } else {
            binding.ivSeachClear.beGone()
            binding.tvNoData1.beVisible()
            binding.ivThumbNodata.beVisible()
            binding.recyclerSearchContact.beGone()
        }
    }

    private fun showSearchResults(messages: List<MessagesModel>, conversations: List<ConversationSmsModel>, searchedText: String) {
        val searchResults = ArrayList<SearchModel>()
        val searchResultsAds = ArrayList<SearchModel>()
        conversations.forEach { conversation ->
            val date = conversation.date.formatDateOrTime(this, true, true)
            val searchResult =
                SearchModel(-1, conversation.title, conversation.phoneNumber, date, conversation.threadId, conversation.photoUri, conversation.phoneNumber)
            searchResults.add(searchResult)
        }

        messages.forEach { message ->
            var recipient = message.senderName
            val phoneNumber = message.participants[0].phoneNumbers[0]
            if (recipient.isEmpty() && message.participants.isNotEmpty()) {
                val participantNames = message.participants.map { it.name }
                recipient = TextUtils.join(", ", participantNames)
            }

            val date = message.date.formatDateOrTime(this, true, true)
            val searchResult = SearchModel(message.id, recipient, message.body, date, message.threadId, message.senderPhotoUri, phoneNumber)
            searchResults.add(searchResult)
        }

        runOnUiThread {
            binding.recyclerSearchContact.beVisibleIf(searchResults.isNotEmpty())
            binding.tvNoData1.beVisibleIf(searchResults.isEmpty())
            binding.ivThumbNodata.beVisibleIf(searchResults.isEmpty())

            val currAdapter = binding.recyclerSearchContact.adapter
            if (currAdapter == null) {
                searchResultsAds.clear()
                for (i in searchResults.indices) {
                    if (AdsHelperClass.getShowNative() == 1) {
//                        if (i % 5 == 0 && i != 0) {
                        if (i == 0) {
                            searchResultsAds.add(SearchModel(0, "", "", "", 0, "", ""))
                        }
                    }
                    searchResultsAds.add(searchResults[i])
                }
                SearchAdapter(this, searchResultsAds, binding.recyclerSearchContact, searchedText) {
                    hideKeyboard()
                    val intentApp = Intent(this, MessagesActivity::class.java)
                    intentApp.putExtra(THREAD_ID, (it as SearchModel).threadId)
                    intentApp.putExtra(THREAD_TITLE, it.title)
                    intentApp.putExtra(THREAD_NUMBER, it.phoneNumber)
                    intentApp.putExtra(SEARCHED_MESSAGE_ID, it.messageId)
                    MainAppClass.instance?.displayInterstitialAds(this, intentApp, false)

                }.apply {
                    binding.recyclerSearchContact.adapter = this
                }
            } else {
                searchResultsAds.clear()
                for (i in searchResults.indices) {
                    if (AdsHelperClass.getShowNative() == 1) {
//                        if (i % 5 == 0 && i != 0) {
                        if (i == 0) {
                            searchResultsAds.add(SearchModel(0, "", "", "", 0, "", ""))
                        }
                    }
                    searchResultsAds.add(searchResults[i])
                }
                (currAdapter as SearchAdapter).updateItems(searchResultsAds, searchedText)
            }
        }
    }
}
