package com.hkapps.messagepro.fragment

import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.hkapps.messagepro.MainAppClass
import com.hkapps.messagepro.R
import com.hkapps.messagepro.activity.BaseHomeActivity
import com.hkapps.messagepro.activity.MessagesActivity
import com.hkapps.messagepro.adapter.ChatHistoryBySearchAdapter
import com.hkapps.messagepro.databinding.FragmentSmsBinding
import com.hkapps.messagepro.extensions.beGone
import com.hkapps.messagepro.extensions.beVisible
import com.hkapps.messagepro.extensions.formatDateOrTime
import com.hkapps.messagepro.extensions.getAdjustedPrimaryColor
import com.hkapps.messagepro.model.ContactsModel
import com.hkapps.messagepro.model.MessagesModel
import com.hkapps.messagepro.model.RefreshEventsModel
import com.hkapps.messagepro.model.SearchModel
import com.hkapps.messagepro.utils.*
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode


class PersonalFragment : BaseFragment() {
    var mAdapter: ChatHistoryBySearchAdapter? = null
    fun newInstance(): PersonalFragment {
        val fragment = PersonalFragment()
        return fragment
    }

    fun newInstance2() {
        if (isAdded && !mActivity!!.isFinishing) {
            onOptionType()
        }
    }

    private var _binding: FragmentSmsBinding? = null

    // Non-nullable getter for use in code
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        _binding = FragmentSmsBinding.inflate(inflater, container, false)
        return binding.root
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        mAdapter = ChatHistoryBySearchAdapter(mActivity!! as BaseHomeActivity, searchResultsAds, binding.recyclerViewChatHistory, searchKey) {
            val intent = Intent(mActivity!!, MessagesActivity::class.java)
            intent.putExtra(THREAD_ID, (it as SearchModel).threadId)
            intent.putExtra(THREAD_TITLE, it.title)
            intent.putExtra(THREAD_NUMBER, it.phoneNumber)
            intent.putExtra(SEARCHED_MESSAGE_ID, it.messageId)
            MainAppClass.instance?.displayInterstitialAds(mActivity!!, intent, false)
        }
        binding.recyclerViewChatHistory.adapter = mAdapter
        ensureBackgroundThread {
            onOptionType()

        }
    }

    override fun onResume() {
        super.onResume()
        if (isAdded && !mActivity!!.isFinishing) {
            if (mActivity!!.config.fontSize != mActivity!!.config.fontSize) {
                if (mAdapter != null) {
                    mAdapter!!.updateFontSize()
                }
            }
            if (mAdapter != null) {
                mAdapter!!.updateFontSize()
            }
            val adjustedPrimaryColor = requireContext().getAdjustedPrimaryColor()
            binding.conversationsFastscroller.updateColors(adjustedPrimaryColor)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        bus?.unregister(this)
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun refreshMessages(event: RefreshEventsModel.RefreshMessages) {
        Log.e("Event: ", "Personal_Message RefreshMessages")
        onOptionType()
    }

    var searchKey = ""


    private var bus: EventBus? = null
    fun onOptionType() {
        bus = EventBus.getDefault()
        try {
            bus!!.register(this)
        } catch (e: Exception) {
        }
        if (isAdded && !mActivity!!.isFinishing) {
            ensureBackgroundThread {


                val messagesAll = mActivity!!.messagesDB.getAllList()
                Log.e("Event: ", "messagesAll size: " + messagesAll.size)

                val searchKeyCredit = "Credit"
                val searchQueryCredit = "%$searchKeyCredit%"
                val messagesCredit = mActivity!!.messagesDB.getMessagesWithText(searchQueryCredit)

                val searchKeyDebit = "Debit"
                val searchQueryDebit = "%$searchKeyDebit%"
                val messagesDebit = mActivity!!.messagesDB.getMessagesWithText(searchQueryDebit)

                val searchKeyOTP = "OTP"
                val searchQueryOTP = "%$searchKeyOTP%"
                val messagesOTP = mActivity!!.messagesDB.getMessagesWithText(searchQueryOTP)

                val searchKeyLink = "Link"
                val searchQueryLink = "%$searchKeyLink%"
                val messagesLink = mActivity!!.messagesDB.getMessagesWithText(searchQueryLink)


                val messagesLocal = messagesAll - messagesCredit - messagesDebit - messagesOTP - messagesLink

                val messages = ArrayList<MessagesModel>()
                var mAllContacts: ArrayList<ContactsModel>
                val mContactsNumber = ArrayList<String>()

                SimpleContactsHelperUtils(mActivity!!).getAvailableContacts(false) {
                    mAllContacts = it

                    for (item in mAllContacts) {
                        for (phoneNumber in item.phoneNumbers) {
                            mContactsNumber.add(phoneNumber.toString().replace("[", "").replace("]", ""))
                        }
//                        mContactsNumber.add(item.phoneNumbers.toString().replace("[", "").replace("]", ""))
                    }

                    for (messageItem in messagesLocal) {
                        for (participants in messageItem.participants) {
                            if (participants.phoneNumbers != null) {
                                for (number in participants.phoneNumbers) {
                                    for (contactNumber in mContactsNumber) {
                                        if (contactNumber.contains(number)) {
                                            messages.add(messageItem)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                val sortedConversationsmessages = messages.sortedWith(
                    compareByDescending<MessagesModel> { mActivity!!.config.pinnedConversations.contains(it.threadId.toString()) }
                        .thenByDescending { it.date }
                ).toMutableList() as ArrayList<MessagesModel>

                Log.e("Event: ", "result Personal: " + sortedConversationsmessages.size)


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
                mActivity!!.runOnUiThread {
                    showSearchResultsNew(messagesNew, searchKey)
                }
            }
        }
    }

    val searchResultsAds = ArrayList<SearchModel>()
    private fun showSearchResultsNew(messages: ArrayList<MessagesModel>, searchedText: String) {
        val searchResults = ArrayList<SearchModel>()

        messages.forEach { message ->
            var recipient = message.senderName
            val phoneNumber = message.participants[0].phoneNumbers[0]
            if (recipient.isEmpty() && message.participants.isNotEmpty()) {
                val participantNames = message.participants.map { it.name }
                recipient = TextUtils.join(", ", participantNames)
            }

            val date = message.date.formatDateOrTime(mActivity!!, true, true)
            val searchResult = SearchModel(message.id, recipient, message.body, date, message.threadId, message.senderPhotoUri, phoneNumber)
            searchResults.add(searchResult)
        }

        searchResultsAds.clear()
        for (i in searchResults.indices) {
//            if (SMS_Ads_Flag_Constats.showNative == 1) {
////                    if (i % 5 == 0 && i != 0) {
//                if (i == 0) {
//                    searchResultsAds.add(SMS_Mdl_Searched(0, "", "", "", 0, ""))
//                }
//            }
            searchResultsAds.add(searchResults[i])
        }

        if (mAdapter == null) {


            /*  if (searchResultsAds.isEmpty()) {
                  noDataView()
              } else {
                  visibleDataView()
              }*/

        } else {
//                mAdapter.updateConversations(searchResultsAds)
            mAdapter!!.notifyDataSetChanged()
            if (mAdapter!!.mSearchResults.isEmpty()) {
                noDataView()
            } else {
                visibleDataView()
            }
        }
    }

    fun noDataView() {
        if (binding.conversationsFastscroller != null && binding.tvNoData1 != null && binding.progressBar != null && binding.ivThumbNodata != null) {
            binding.conversationsFastscroller.beGone()
            binding.tvNoData1.beVisible()
            binding.progressBar.beGone()
            binding.ivThumbNodata.beVisible()
            binding.ivThumbNodata.setImageResource(R.drawable.icon_banner_personal)
            binding.tvNoData1.text = resources.getString(R.string.no_personal_conversations_found)
        }
    }

    fun visibleDataView() {
        if (binding.conversationsFastscroller != null && binding.tvNoData1 != null && binding.progressBar != null && binding.ivThumbNodata != null) {
            binding.conversationsFastscroller.beVisible()
            binding.tvNoData1.beGone()
            binding.progressBar.beGone()
            binding.ivThumbNodata.beGone()
        }
    }

}
