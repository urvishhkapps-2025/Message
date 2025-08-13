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
import com.hkapps.messagepro.model.MessagesModel
import com.hkapps.messagepro.model.RefreshEventsModel
import com.hkapps.messagepro.model.SearchModel
import com.hkapps.messagepro.utils.*
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode


class OTPsFragment : BaseFragment() {

    fun newInstance(): OTPsFragment {
        val fragment = OTPsFragment()
        return fragment
    }

    var mAdapter: ChatHistoryBySearchAdapter? = null
    fun newInstance2() {
        if (isAdded && !mActivity!!.isFinishing) {
            ensureBackgroundThread {
                mActivity!!.runOnUiThread {
                    onOptionType()
                }
            }
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
            mActivity!!.runOnUiThread {
                onOptionType()
            }
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

    val searchKey = "OTP"
    override fun onDestroy() {
        super.onDestroy()
        bus?.unregister(this)
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun refreshMessages(event: RefreshEventsModel.RefreshMessages) {
        Log.e("Event: ", "OTP RefreshMessages")
        onOptionType()
    }

    private var bus: EventBus? = null
    fun onOptionType() {
        bus = EventBus.getDefault()
        try {
            bus!!.register(this)
        } catch (e: Exception) {
        }
        if (isAdded && mActivity!! != null && !mActivity!!.isFinishing) {
            ensureBackgroundThread {

                val searchQuery = "%$searchKey%"

                val messages = mActivity!!.messagesDB.getMessagesWithText(searchQuery)
                Log.e("Event: ", "messagesAll size OTP: " + messages.size)

                val sortedConversationsmessages = messages.sortedWith(
                    compareByDescending<MessagesModel> { mActivity!!.config.pinnedConversations.contains(it.threadId.toString()) }
                        .thenByDescending { it.date }
                ).toMutableList() as ArrayList<MessagesModel>


                mActivity!!.runOnUiThread {
                    showSearchResultsNew(sortedConversationsmessages, searchKey)
                }
            }
        }
    }

    val searchResultsAds = ArrayList<SearchModel>()
    private fun showSearchResultsNew(messages: ArrayList<MessagesModel>, searchedText: String) {

        try {

            val searchResults = ArrayList<SearchModel>()

            searchResultsAds.clear()

            if (messages != null && messages.size > 0) {

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

                for (i in searchResults.indices) {
//            if (SMS_Ads_Flag_Constats.showNative == 1 && i == 0) {
//                searchResultsAds.add(SMS_Mdl_Searched(0, "", "", "", 0, ""))
//            }
                    searchResultsAds.add(searchResults[i])
                }
            }

            Log.e("Event: ", "result size otp: " + searchResultsAds.size)
            if (mAdapter == null) {

                /*mAdapter = SMS_Adpt_Home_Chat_History_By_Search(mActivity!! as SMS_Act_Home_Base, searchResultsAds, recyclerViewChatHistory, searchedText) {
                    val intent = Intent(mActivity!!, SMS_Act_Message::class.java)
                    intent.putExtra(THREAD_ID, (it as SMS_Mdl_Searched).threadId)
                    intent.putExtra(THREAD_TITLE, it.title)
                    intent.putExtra(SEARCHED_MESSAGE_ID, it.messageId)
                    MainApp.instance?.displayInterstitialAds(mActivity!!, intent, false)
                }
                recyclerViewChatHistory.adapter = mAdapter*/
                /* if (searchResultsAds.size == 0) {
                     noDataView()
                 } else {
                     visibleDataView()
                 }*/
            } else {
//            mAdapter!!.updateConversations(searchResultsAds)

                Log.e("Event: ", "otp list not null")
                Log.e("Event: ", "searchResultsAds: " + searchResultsAds.size)
                mAdapter!!.notifyDataSetChanged()

                if (mAdapter!!.mSearchResults.isEmpty()) {
                    Log.e("Event: ", "otp noDataView")
                    noDataView()
                } else {
                    Log.e("Event: ", "otp visibleDataView")
                    visibleDataView()
                }
            }
        } catch (e: Exception) {
            Log.e("CRASH", e.message!!)
        }
    }

    fun noDataView() {
        if (binding.conversationsFastscroller != null && binding.tvNoData1 != null && binding.progressBar != null && binding.ivThumbNodata != null) {
            binding.conversationsFastscroller.beGone()
            binding.tvNoData1.beVisible()
            binding.progressBar.beGone()
            binding.ivThumbNodata.beVisible()
            binding.ivThumbNodata.setImageResource(R.drawable.icon_banner_otp)
            binding.tvNoData1.text = resources.getString(R.string.no_otp_messages_found)
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
