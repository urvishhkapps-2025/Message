package com.hkapps.messagepro.fragment

import android.app.Activity
import android.app.role.RoleManager
import android.content.Intent
import android.os.Bundle
import android.provider.Telephony
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.hkapps.messagepro.MainAppClass
import com.hkapps.messagepro.R
import com.hkapps.messagepro.activity.BaseHomeActivity
import com.hkapps.messagepro.activity.ContactsActivity
import com.hkapps.messagepro.activity.HomeActivity
import com.hkapps.messagepro.activity.MessagesActivity
import com.hkapps.messagepro.adapter.ChatHistoryAdapter
import com.hkapps.messagepro.databinding.FragmentSmsBinding
import com.hkapps.messagepro.extensions.*
import com.hkapps.messagepro.model.ConversationSmsModel
import com.hkapps.messagepro.model.RefreshEventsModel
import com.hkapps.messagepro.utils.*
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode


class AllSMSFragment : BaseFragment() {
    private var bus: EventBus? = null
    var mAdapter: ChatHistoryAdapter? = null
    private var storedFontSize = 0
    var sortedConversations: ArrayList<ConversationSmsModel> = ArrayList()
    private val MAKE_DEFAULT_APP_REQUEST = 1

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

    override fun onDestroy() {
        super.onDestroy()
        bus?.unregister(this)
    }

    override fun onPause() {
        super.onPause()
        storeStateVariables()
    }


    override fun onResume() {
        super.onResume()
        if (isAdded && !mActivity!!.isFinishing) {
            if (storedFontSize != mActivity!!.config.fontSize) {
                if (mAdapter != null) {
                    mAdapter!!.updateFontSize()
                }
            }
            if (mAdapter != null) {
                mAdapter!!.updateDrafts()
            }
            binding.tvNoData2.underlineText()
            val adjustedPrimaryColor = requireContext().getAdjustedPrimaryColor()
            binding.conversationsFastscroller.updateColors(adjustedPrimaryColor)
        }
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.e("Event: ", "onViewCreated")


        mAdapter = ChatHistoryAdapter(mActivity!! as BaseHomeActivity, sortedConversations, binding.recyclerViewChatHistory) {
            val intent = Intent(mActivity!!, MessagesActivity::class.java)
            intent.putExtra(THREAD_ID, (it as ConversationSmsModel).threadId)
            intent.putExtra(THREAD_TITLE, it.title)
            intent.putExtra(THREAD_NUMBER, it.phoneNumber)
            MainAppClass.instance?.displayInterstitialAds(mActivity!!, intent, false)
        }
        binding.recyclerViewChatHistory.adapter = mAdapter

        binding.tvNoData2.setOnClickListener {
            Intent(mActivity!!, ContactsActivity::class.java).apply {
                startActivity(this)
            }
        }


        getPermission()

        /*  ensureBackgroundThread {
              mActivity!!.runOnUiThread {
  //                getCachedConversationsFirst()
                  initMessenger()
              }
          }*/
    }

    private fun getPermission() {
        if (isQPlus()) {
            val roleManager = requireContext().getSystemService(RoleManager::class.java)
            if (roleManager!!.isRoleAvailable(RoleManager.ROLE_SMS)) {
                if (roleManager.isRoleHeld(RoleManager.ROLE_SMS)) {
                    Log.e("Event: ", "askPermissions isQPlus")
                    askPermissions()
                } else {
                    val intent = roleManager.createRequestRoleIntent(RoleManager.ROLE_SMS)
                    startActivityForResult(intent, MAKE_DEFAULT_APP_REQUEST)
                }
            } else {
                toast(requireContext().getString(R.string.unknown_error_occurred))
                (mActivity!! as HomeActivity).finish()
            }
        } else {
            if (Telephony.Sms.getDefaultSmsPackage(requireContext()) == mActivity!!.packageName) {
                Log.e("Event: ", "askPermissions isQPlus else")
                askPermissions()
            } else {
                val intent = Intent(Telephony.Sms.Intents.ACTION_CHANGE_DEFAULT)
                intent.putExtra(Telephony.Sms.Intents.EXTRA_PACKAGE_NAME, mActivity!!.packageName)
                startActivityForResult(intent, MAKE_DEFAULT_APP_REQUEST)
            }
        }
    }


    fun newInstance2() {
        if (isAdded && !mActivity!!.isFinishing) {
            ensureBackgroundThread {
                mActivity!!.runOnUiThread {
//                getCachedConversationsFirst()
                    initMessenger()
                }
            }
        }
    }

    fun newInstance(): AllSMSFragment {
        val fragment = AllSMSFragment()
        return fragment
    }


    @Subscribe(threadMode = ThreadMode.MAIN)
    fun refreshMessages(event: RefreshEventsModel.RefreshMessages) {
        Log.e("Event: ", "All Message refreshMessages")
        Log.e("Event: ", "initMessenger 1")
        initMessenger()
    }

    // while SEND_SMS and READ_SMS permissions are mandatory, READ_CONTACTS is optional. If we don't have it, we just won't be able to show the contact name in some cases
    private fun askPermissions() {
        handlePermission(PERMISSION_READ_SMS) {
            if (it) {
                handlePermission(PERMISSION_SEND_SMS) {
                    if (it) {
                        handlePermission(PERMISSION_READ_CONTACTS) {
                            Log.e("Event: ", "initMessenger 2")
                            initMessenger()
                            bus = EventBus.getDefault()
                            try {
                                bus!!.register(this)
                            } catch (e: Exception) {
                            }
                        }
                    } else {
                        toast(requireContext().getString(R.string.unknown_error_occurred))
                        (mActivity!! as HomeActivity).finish()
                    }
                }
            } else {
                toast(requireContext().getString(R.string.unknown_error_occurred))
                (mActivity!! as HomeActivity).finish()
            }
        }
    }

    private fun initMessenger() {
        bus = EventBus.getDefault()
        try {
            bus!!.register(this)
        } catch (e: Exception) {
        }
        storeStateVariables()
        Log.e("Event: ", "getCachedConversations 2")
        getCachedConversations()


    }

    private fun storeStateVariables() {
        storedFontSize = mActivity!!.config.fontSize
    }


    private fun getCachedConversations() {
        if (isAdded && mActivity!! != null && !mActivity!!.isFinishing) {
            ensureBackgroundThread {
                val conversations = try {
                    mActivity!!.conversationsDB.getAllList().toMutableList() as ArrayList<ConversationSmsModel>
                } catch (e: Exception) {
                    ArrayList()
                }

                mActivity!!.updateUnreadCountBadge(conversations)
                mActivity!!.runOnUiThread {
                    Log.e("Event: ", "setupConversations 1")
                    setupConversations(conversations)
                    getNewConversations(conversations)
                }
            }
        }

    }

    private fun getCachedConversationsFirst() {
        if (isAdded && mActivity!! != null && !mActivity!!.isFinishing) {
            ensureBackgroundThread {
                val conversations = try {
                    mActivity!!.conversationsDB.getAllList().toMutableList() as ArrayList<ConversationSmsModel>
                } catch (e: Exception) {
                    ArrayList()
                }

                mActivity!!.updateUnreadCountBadge(conversations)
                mActivity!!.runOnUiThread {
                    Log.e("Event: ", "getCachedConversationsFirst")
                    setupConversations(conversations)
                }
            }
        }

    }


    private fun setupConversations(conversations: ArrayList<ConversationSmsModel>) {
        if (isAdded && !mActivity!!.isFinishing) {
            val hasConversations = conversations.isNotEmpty()
            val localAll = conversations.sortedWith(
                compareByDescending<ConversationSmsModel> { mActivity!!.config.pinnedConversations.contains(it.threadId.toString()) }
                    .thenByDescending { it.date }
            ).toMutableList() as ArrayList<ConversationSmsModel>

            if (isAdded && !mActivity!!.isFinishing) {
                binding.conversationsFastscroller.beVisibleIf(hasConversations)
                binding.tvNoData1.beGoneIf(hasConversations)
                binding.progressBar.beVisibleIf(hasConversations)
                binding.tvNoData2.beGoneIf(hasConversations)
                binding.ivThumbNodata.beGoneIf(hasConversations)


                setAdapterNew(localAll, hasConversations)
            }
        }
    }

    private fun getNewConversations(cachedConversations: ArrayList<ConversationSmsModel>) {
        val privateCursor = mActivity!!.getMyContactsCursor(false, true)?.loadInBackground()
        ensureBackgroundThread {
            val privateContacts = MyContactsContentProviderUtils.getSimpleContacts(mActivity!!, privateCursor)
            val conversations = mActivity!!.getConversations(privateContacts = privateContacts)

            if (isAdded && !mActivity!!.isFinishing) {
                mActivity!!.runOnUiThread {
                    Log.e("Event: ", "setupConversations 2")
                    setupConversations(conversations)
                }

                conversations.forEach { clonedConversation ->
                    if (!cachedConversations.map { it.threadId }.contains(clonedConversation.threadId)) {
                        mActivity!!.conversationsDB.insertOrUpdateMessage(clonedConversation)
                        cachedConversations.add(clonedConversation)
                    }
                }

                cachedConversations.forEach { cachedConversation ->
                    if (!conversations.map { it.threadId }.contains(cachedConversation.threadId)) {
                        mActivity!!.conversationsDB.deleteThreadIdMessage(cachedConversation.threadId)
                    }
                }

                cachedConversations.forEach { cachedConversation ->
                    val conv = conversations.firstOrNull { it.threadId == cachedConversation.threadId && it.toString() != cachedConversation.toString() }
                    if (conv != null) {
                        mActivity!!.conversationsDB.insertOrUpdateMessage(conv)
                    }
                }

                if (mActivity!!.config.appRunCount == 1) {
                    conversations.map { it.threadId }.forEach { threadId ->
                        val messages = mActivity!!.getMessages(threadId)
                        messages.forEach { currentMessages ->
                            mActivity!!.messagesDB.insertAddMessages(currentMessages)
                        }
                    }

                    mActivity!!.config.appRunCount++
                }
            }
        }
    }

    private fun setAdapterNew(localAll: ArrayList<ConversationSmsModel>, hasConversations: Boolean) {
        sortedConversations.clear()
        for (i in localAll.indices) {
//            if (SMS_Ads_Flag_Constats.showNative == 1 && i == 0) {
//                sortedConversations.add(SMS_Mdl_Conversation(0, "", 0, false, "", "", false, ""))
//            }
            sortedConversations.add(localAll[i])
        }


        if (mAdapter == null) {
            Log.e("Event: ", "all message currAdapter null")
            /* mAdapter = SMS_Adpt_Home_Chat_History(mActivity!! as SMS_Act_Home_Base, sortedConversations, recyclerViewChatHistory) {
                 val intent = Intent(mActivity!!, SMS_Act_Message::class.java)
                 intent.putExtra(THREAD_ID, (it as SMS_Mdl_Conversation).threadId)
                 intent.putExtra(THREAD_TITLE, it.title)
                 intent.putExtra(THREAD_NUMBER, it.phoneNumber)
                 MainApp.instance?.displayInterstitialAds(mActivity!!, intent, false)
             }
             recyclerViewChatHistory.adapter = mAdapter*/
        } else {
            Log.e("Event: ", "all message currAdapter not null")
            Log.e("Event: ", "sortedConversations: " + sortedConversations.size)

//            mAdapter!!.updateConversations(sortedConversations)
            mAdapter!!.notifyDataSetChanged()
            if (mAdapter!!.mConversations.isEmpty()) {
                binding.conversationsFastscroller.beGone()
                binding.tvNoData1.beVisible()
                binding.progressBar.beGone()
                binding.tvNoData2.beVisible()
                binding.ivThumbNodata.beVisible()

                if (!hasConversations && mActivity!!.config.appRunCount == 1) {
                    binding.tvNoData1.text = getString(R.string.loading_messages)
                    binding.progressBar.beVisible()
                    binding.tvNoData2.beGone()
                    binding.ivThumbNodata.beGone()
                } else if (!hasConversations && mActivity!!.config.appRunCount != 1) {
                    binding.tvNoData1.text = getString(R.string.no_conversations_found)
//                    progressBar.beGone()
//                    tvNoData2.beVisible()
//                    ivThumbNodata.beVisible()
                } else {
                    binding.progressBar.beGone()
                }
            } else {
                binding.progressBar.beGone()
                binding.conversationsFastscroller.beVisible()
                binding.tvNoData1.beGone()
                binding.tvNoData2.beGone()
                binding.ivThumbNodata.beGone()
            }
        }


    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, resultData: Intent?) {
        super.onActivityResult(requestCode, resultCode, resultData)
        if (requestCode == MAKE_DEFAULT_APP_REQUEST) {
            if (resultCode == Activity.RESULT_OK) {
                Log.e("Event: ", "askPermissions RESULT_OK")
                askPermissions()
            } else {
                toast(requireContext().getString(R.string.unknown_error_occurred))
                getPermission()
            }
        }
    }


}
