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
import java.util.regex.Matcher
import java.util.regex.Pattern


class OffersFragment : BaseFragment() {

    fun newInstance(): OffersFragment {
        val fragment = OffersFragment()
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


        if (isAdded && !mActivity!!.isFinishing) {
            ensureBackgroundThread {
                mActivity!!.runOnUiThread {
                    onOptionType()
                }
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

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun refreshMessages(event: RefreshEventsModel.RefreshMessages) {
        Log.e("Event: ", "Offer RefreshMessages")
        onOptionType()
    }

    val searchKey = "Link"
    override fun onDestroy() {
        super.onDestroy()
        bus?.unregister(this)
    }

    private var bus: EventBus? = null
    fun onOptionType() {
        bus = EventBus.getDefault()
        try {
            bus!!.register(this)
        } catch (e: Exception) {
        }
        if (isAdded && !mActivity!!.isFinishing) {
            ensureBackgroundThread {

                val searchQuery = "%$searchKey%"

                val messages = mActivity!!.messagesDB.getMessagesWithText(searchQuery)
                Log.e("Event: ", "messagesAll size offer: " + messages.size)


                val offerMessage = checkLinkIsValid(messages)


                val sortedConversationsmessages = offerMessage.sortedWith(
                    compareByDescending<MessagesModel> { mActivity!!.config.pinnedConversations.contains(it.threadId.toString()) }
                        .thenByDescending { it.date }
                ).toMutableList() as ArrayList<MessagesModel>


                mActivity!!.runOnUiThread {
                    showSearchResultsNew(sortedConversationsmessages)
                }
            }
        }
    }

    fun checkLinkIsValid(messages: List<MessagesModel>): List<MessagesModel> {

        val localResult = ArrayList<MessagesModel>()
        messages.forEach { message ->
            val text = message.body
            val listUrl = extractUrls(text)
            if (listUrl.size > 0) {
                localResult.add(message)
            }
        }

        return localResult

    }

    fun extractUrls(text: String): List<String> {
        val containedUrls: MutableList<String> = ArrayList()
        val urlRegex = "((https?|ftp|gopher|telnet|file):((//)|(\\\\))+[\\w\\d:#@%/;$()~_?\\+-=\\\\\\.&]*)"
        val pattern: Pattern = Pattern.compile(urlRegex, Pattern.CASE_INSENSITIVE)
        val urlMatcher: Matcher = pattern.matcher(text)
        while (urlMatcher.find()) {
            containedUrls.add(
                text.substring(
                    urlMatcher.start(0),
                    urlMatcher.end(0)
                )
            )
        }
        return containedUrls
    }

    val searchResultsAds = ArrayList<SearchModel>()
    private fun showSearchResultsNew(messages: ArrayList<MessagesModel>) {
        val searchResults = ArrayList<SearchModel>()
        if (messages != null && messages.size > 0) {
            messages.forEach { message ->
                if (message != null) {
                    var recipient = message.senderName
                    val phoneNumber = message.participants[0].phoneNumbers[0]
                    if (recipient.isEmpty() && message.participants.isNotEmpty()) {
                        val participantNames = message.participants.map { it.name }
//                        val number = message.participants.map { it.phoneNumbers[0] }
                        recipient = TextUtils.join(", ", participantNames)
                    }

                    val date = message.date.formatDateOrTime(mActivity!!, true, true)
                    val searchResult = SearchModel(message.id, recipient, message.body, date, message.threadId, message.senderPhotoUri, phoneNumber)
                    searchResults.add(searchResult)
                }
            }
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

        Log.e("Event: ", "result size offer: " + searchResultsAds.size)

        if (mAdapter == null) {

            /*if (searchResultsAds.size == 0) {
                noDataView()
            } else {
                visibleDataView()
            }*/

        } else {
//            mAdapter.updateConversations(searchResultsAds)
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
            binding.ivThumbNodata.setImageResource(R.drawable.icon_banner_offer)
            binding.tvNoData1.text = resources.getString(R.string.no_offer_messages_found)
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
