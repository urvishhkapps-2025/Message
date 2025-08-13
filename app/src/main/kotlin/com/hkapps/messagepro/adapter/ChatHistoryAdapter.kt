package com.hkapps.messagepro.adapter

import android.app.Dialog
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Handler
import android.os.Looper
import android.text.TextUtils
import android.util.Log
import android.util.TypedValue
import android.view.*
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.res.ResourcesCompat
import androidx.recyclerview.widget.RecyclerView
import com.hkapps.messagepro.MainAppClass
import com.hkapps.messagepro.R
import com.hkapps.messagepro.activity.BaseHomeActivity
import com.hkapps.messagepro.dialogs.AlertDialogCustom
import com.hkapps.messagepro.extensions.*
import com.hkapps.messagepro.model.ConversationSmsModel
import com.hkapps.messagepro.utils.*
import com.qtalk.recyclerviewfastscroller.RecyclerViewFastScroller


class ChatHistoryAdapter(
    mActivity: BaseHomeActivity,
    var mConversations: ArrayList<ConversationSmsModel>,
    recyclerView: com.hkapps.messagepro.views.CustomRecyclerView,
    itemClick: (Any) -> Unit,
) : BaseAdapter(mActivity, recyclerView, null, itemClick), RecyclerViewFastScroller.OnPopupTextUpdate {
    private var fontSize = mActivity.getTextSize()
    private var drafts = HashMap<Long, String?>()

    init {
        setupDragListener(true)
        fetchDrafts(drafts)
    }

    override fun getActionMenuId() = R.menu.app_menu_cab_conversations

    override fun prepareActionMode(menu: Menu) {
        val selectedItems = getSelectedItems()

        menu.apply {
            findItem(R.id.menu_cab_block_number).isVisible = isNougatPlus()
            findItem(R.id.menu_cab_add_number_to_contact).isVisible = isOneItemSelected() && selectedItems.firstOrNull()?.isGroupConversation == false
            findItem(R.id.menu_cab_dial_number).isVisible = isOneItemSelected() && selectedItems.firstOrNull()?.isGroupConversation == false
            findItem(R.id.menu_menu_cab_copy_number).isVisible = isOneItemSelected() && selectedItems.firstOrNull()?.isGroupConversation == false
            findItem(R.id.menu_cab_mark_as_read).isVisible = selectedItems.any { !it.read }
            findItem(R.id.menu_cab_mark_as_unread).isVisible = selectedItems.any { it.read }
            checkPinBtnVisibility(this)
        }
    }

    override fun actionItemPressed(id: Int) {
        if (selectedKeys.isEmpty()) {
            return
        }

        when (id) {
            R.id.menu_cab_add_number_to_contact -> addNumberToContact()
            R.id.menu_cab_block_number -> askConfirmBlock()
            R.id.menu_cab_dial_number -> dialNumber()
            R.id.menu_menu_cab_copy_number -> copyNumberToClipboard()
            R.id.menu_cab_delete -> askConfirmDelete()
            R.id.menu_cab_mark_as_read -> markAsRead()
            R.id.menu_cab_mark_as_unread -> markAsUnread()
            R.id.menu_cab_pin_conversation -> pinConversation(true)
            R.id.menu_cab_unpin_conversation -> pinConversation(false)
            R.id.menu_cab_select_all -> selectAll()
        }
    }

    override fun getSelectableItemCount() = mConversations.size

    override fun getIsItemSelectable(position: Int) = true

    override fun getItemSelectionKey(position: Int) = mConversations.getOrNull(position)?.hashCode()

    override fun getItemKeyPosition(key: Int) = mConversations.indexOfFirst { it.hashCode() == key }

    override fun onActionModeCreated() {}

    override fun onActionModeDestroyed() {}


    override fun onCreateViewHolder(viewGroup: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val view1 = LayoutInflater.from(viewGroup.context).inflate(R.layout.list_raw_chat_history, viewGroup, false)
        return ViewHolder(view1)
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val holderMessage = holder as ViewHolder
        val conversation = mConversations[position]
        holderMessage.bindView(conversation, true, true) { itemView, layoutPosition ->
            setupView(itemView, conversation)
        }
        holderMessage.itemView.tag = holder
    }


    override fun getItemCount() = mConversations.size

    private fun askConfirmBlock() {
        val numbers = getSelectedItems().distinctBy { it.phoneNumber }.map { it.phoneNumber }
        val numbersString = TextUtils.join(", ", numbers)
        val question = String.format(resources.getString(R.string.block_confirmation), numbersString)

        AlertDialogCustom(mActivity, question) {
            blockNumbers()
        }
    }

    private fun blockNumbers() {
        if (selectedKeys.isEmpty()) {
            return
        }

        val numbersToBlock = getSelectedItems()
        val positions = getSelectedItemPositions()
        mConversations.removeAll(numbersToBlock)

        ensureBackgroundThread {
            numbersToBlock.map { it.phoneNumber }.forEach { number ->
                mActivity.addBlockedNumber(number)
            }

            mActivity.runOnUiThread {
                removeSelectedItems(positions)
                finishActMode()
            }
        }
    }

    private fun dialNumber() {
        val conversation = getSelectedItems().firstOrNull() ?: return
        mActivity.dialNumber(conversation.phoneNumber) {
            finishActMode()
        }
    }

    private fun copyNumberToClipboard() {
        val conversation = getSelectedItems().firstOrNull() ?: return
        mActivity.copyToClipboard(conversation.phoneNumber)
        finishActMode()
    }

    private fun askConfirmDelete() {
        val itemsCnt = selectedKeys.size
        val items = resources.getQuantityString(R.plurals.delete_conversations, itemsCnt, itemsCnt)

        val baseString = R.string.deletion_confirmation
        val question = String.format(resources.getString(baseString), items)

        AlertDialogCustom(mActivity, question) {
            ensureBackgroundThread {
                deleteConversations()
            }
        }
    }

    fun loadDialog() {
        MainAppClass.loadAdsDialog = Dialog(mActivity)
        MainAppClass.loadAdsDialog!!.setContentView(R.layout.app_layout_loading)
        MainAppClass.loadAdsDialog!!.setCanceledOnTouchOutside(false)
        MainAppClass.loadAdsDialog!!.setCancelable(false)
        MainAppClass.loadAdsDialog!!.window!!
            .setLayout(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.WRAP_CONTENT
            )
        MainAppClass.loadAdsDialog!!.window!!.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        MainAppClass.loadAdsDialog!!.window!!.attributes.windowAnimations = android.R.style.Animation_Dialog
        MainAppClass.loadAdsDialog!!.show()
        (MainAppClass.loadAdsDialog!!.findViewById<View>(R.id.title) as TextView).text = "Deleting Messages..."
    }

    fun hideDialog() {
        if (MainAppClass.loadAdsDialog != null && MainAppClass.loadAdsDialog!!.isShowing) {
            MainAppClass.loadAdsDialog!!.dismiss()
        }
    }

    private fun deleteConversations() {
        if (selectedKeys.isEmpty()) {
            return
        }
        Handler(Looper.getMainLooper()).post {
            loadDialog()
        }


        val conversationsToRemove = mConversations.filter { selectedKeys.contains(it.hashCode()) } as ArrayList<ConversationSmsModel>
        val positions = getSelectedItemPositions()
        conversationsToRemove.forEach {
            mActivity.deleteConversation(it.threadId)
            mActivity.notificationManager.cancel(it.hashCode())
        }

        try {
            mConversations.removeAll(conversationsToRemove)
        } catch (ignored: Exception) {
        }

        mActivity.runOnUiThread {
            Log.e("Event: ", "conversationsToRemove size: " + conversationsToRemove.size)
            if (conversationsToRemove.isEmpty()) {
                Handler(Looper.getMainLooper()).post {
                    hideDialog()
                }
                Log.e("Event: ", "deleteConversations")
                refreshMessages()
                finishActMode()
            } else {
                Handler(Looper.getMainLooper()).post {
                    hideDialog()
                }
                removeSelectedItems(positions)
                if (mConversations.isEmpty()) {
                    Log.e("Event: ", "deleteConversations is Empty")
                    mActivity.config.appRunCount = 1
                    refreshMessages()
                }
            }
        }
    }

    private fun markAsRead() {
        if (selectedKeys.isEmpty()) {
            return
        }

        val conversationsMarkedAsRead = mConversations.filter { selectedKeys.contains(it.hashCode()) } as ArrayList<ConversationSmsModel>
        ensureBackgroundThread {
            conversationsMarkedAsRead.filter { conversation -> !conversation.read }.forEach {
                mActivity.markThreadMessagesReadNew(it.threadId)
            }

            mActivity.runOnUiThread {
                Log.e("Event: ", "markAsRead")
                refreshMessages()
                finishActMode()
            }
        }
    }

    private fun markAsUnread() {
        if (selectedKeys.isEmpty()) {
            return
        }

        val conversationsMarkedAsUnread = mConversations.filter { selectedKeys.contains(it.hashCode()) } as ArrayList<ConversationSmsModel>
        ensureBackgroundThread {
            conversationsMarkedAsUnread.filter { conversation -> conversation.read }.forEach {
                mActivity.markThreadMessagesUnreadNew(it.threadId)
            }

            mActivity.runOnUiThread {
                Log.e("Event: ", "markAsUnread 2")
                refreshMessages()
                finishActMode()
            }
        }
    }

    private fun addNumberToContact() {
        val conversation = getSelectedItems().firstOrNull() ?: return
        Intent().apply {
            action = Intent.ACTION_INSERT_OR_EDIT
            type = "vnd.android.cursor.item/contact"
            putExtra(KEY_PHONE, conversation.phoneNumber)
            mActivity.launchActivityIntent(this)
        }
    }

    private fun getSelectedItems() = mConversations.filter { selectedKeys.contains(it.hashCode()) } as ArrayList<ConversationSmsModel>

    private fun pinConversation(pin: Boolean) {
        val mConversations = getSelectedItems()
        if (mConversations.isEmpty()) {
            return
        }

        if (pin) {
            mActivity.config.addPinnedConversations(mConversations)
        } else {
            mActivity.config.removePinnedConversations(mConversations)
        }

        mActivity.runOnUiThread {
            Log.e("Event: ", "pinConversation")
            refreshMessages()
            finishActMode()
        }
    }

    private fun checkPinBtnVisibility(menu: Menu) {
        val pinnedConversations = mActivity.config.pinnedConversations
        val selectedConversations = getSelectedItems()
        menu.findItem(R.id.menu_cab_pin_conversation).isVisible = selectedConversations.any { !pinnedConversations.contains(it.threadId.toString()) }
        menu.findItem(R.id.menu_cab_unpin_conversation).isVisible = selectedConversations.any { pinnedConversations.contains(it.threadId.toString()) }
    }

    private fun fetchDrafts(drafts: HashMap<Long, String?>) {
        drafts.clear()
        for ((threadId, draft) in mActivity.getAllDrafts()) {
            drafts[threadId] = draft
        }
    }

    fun updateFontSize() {
        fontSize = mActivity.getTextSize()
        notifyDataSetChanged()
    }

    fun updateDrafts() {
        val newDrafts = HashMap<Long, String?>()
        fetchDrafts(newDrafts)
        if (drafts.hashCode() != newDrafts.hashCode()) {
            drafts = newDrafts
            notifyDataSetChanged()
        }
    }

    private fun setupView(view: View, conversation: ConversationSmsModel) {
        view.apply {
            val smsDraft = drafts[conversation.threadId]
            findViewById<TextView>(R.id.tvDraftIndicator).beVisibleIf(smsDraft != null)

            findViewById<ImageView>(R.id.ivPinThumb).beVisibleIf(mActivity.config.pinnedConversations.contains(conversation.threadId.toString()))


            if (selectedKeys.contains(conversation.hashCode())) {
                findViewById<LinearLayout>(R.id.flParent).setBackgroundColor(mActivity.resources.getColor(R.color.activated_item_foreground))
            } else {
                findViewById<LinearLayout>(R.id.flParent).setBackgroundColor(mActivity.resources.getColor(R.color.transparent))
            }

            findViewById<TextView>(R.id.tvConversationTitle).apply {
                text = conversation.title
                setTextSize(TypedValue.COMPLEX_UNIT_PX, fontSize * 1.2f)
            }

            findViewById<TextView>(R.id.tvConversationDesc).apply {
                text = smsDraft ?: conversation.snippet
                setTextSize(TypedValue.COMPLEX_UNIT_PX, fontSize * 0.9f)
            }

            findViewById<TextView>(R.id.tvDate).apply {
                text = conversation.date.formatDateOrTime(context, true, false)
                setTextSize(TypedValue.COMPLEX_UNIT_PX, fontSize * 0.8f)
            }

            if (conversation.read) {
                val typeface = ResourcesCompat.getFont(context, R.font.sans_regilar)
                findViewById<TextView>(R.id.tvConversationTitle).typeface = typeface
                findViewById<TextView>(R.id.tvConversationDesc).typeface = typeface
            } else {
                val typeface = ResourcesCompat.getFont(context, R.font.sons_bold)
                findViewById<TextView>(R.id.tvConversationTitle).typeface = typeface
                findViewById<TextView>(R.id.tvConversationDesc).typeface = typeface
            }

            val placeholder = if (conversation.isGroupConversation) {
                SimpleContactsHelperUtils(context).getColoredGroupIcon(mActivity, conversation.title)
            } else {
                null
            }

            SimpleContactsHelperUtils(context).loadContactImage(conversation.photoUri, findViewById<ImageView>(R.id.ivThumb), conversation.title, placeholder)

        }
    }

    override fun onChange(position: Int) = mConversations.getOrNull(position)?.title ?: ""
}
