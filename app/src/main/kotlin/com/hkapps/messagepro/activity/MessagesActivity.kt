package com.hkapps.messagepro.activity

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.database.Cursor
import android.graphics.BitmapFactory
import android.graphics.drawable.Drawable
import android.graphics.drawable.LayerDrawable
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.ContactsContract
import android.provider.Telephony
import android.telephony.SubscriptionManager
import android.text.TextUtils
import android.util.Log
import android.util.TypedValue
import android.view.*
import android.view.inputmethod.EditorInfo
import android.widget.*
import android.widget.LinearLayout.LayoutParams
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.request.target.Target
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.hkapps.messagepro.MainAppClass
import com.hkapps.messagepro.R
import com.hkapps.messagepro.adapter.AutoCompeletAdapter
import com.hkapps.messagepro.adapter.MessagesAdapter
import com.hkapps.messagepro.databinding.ActivityMessagesBinding
import com.hkapps.messagepro.dialogs.AlertDialogCustom
import com.hkapps.messagepro.extensions.*
import com.hkapps.messagepro.klinker.android.send_message.Message
import com.hkapps.messagepro.klinker.android.send_message.Settings
import com.hkapps.messagepro.klinker.android.send_message.Transaction
import com.hkapps.messagepro.listners.OnActivityResultLauncher
import com.hkapps.messagepro.model.*
import com.hkapps.messagepro.services.SMS_Service_Sended_Status_Receiver
import com.hkapps.messagepro.services.SendedSMSStatusService
import com.hkapps.messagepro.utils.*
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode


class MessagesActivity : BaseHomeActivity(), MessagesAdapter.DeleteLuancherListner {
    private val MIN_DATE_TIME_DIFF_SECS = 300
    private var posLauncher = 0
    private var threadId = 0L
    private var isNotification = false
    private var currentSIMCardIndex = 0
    private var isActivityVisible = false
    private var refreshedSinceSent = false
    private var threadItems = ArrayList<ItemModel>()
    private var bus: EventBus? = null
    private var participants = ArrayList<ContactsModel>()
    private var privateContacts = ArrayList<ContactsModel>()
    private var messages = ArrayList<MessagesModel>()
    private val availableSIMCards = ArrayList<SimModel>()
    private var attachmentSelections = mutableMapOf<String, SelectedAttachmentModel>()
    private val imageCompressor by lazy { ImageCompressorUtils(this) }
    var appTopToolbar: Toolbar? = null
    var msgAdapter: MessagesAdapter? = null
    var mTitle: String? = ""
    var mNUmber: String? = ""

    private lateinit var binding: ActivityMessagesBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMessagesBinding.inflate(layoutInflater)
        setContentView(binding.root)
        appTopToolbar = findViewById(R.id.appTopToolbar)
        setSupportActionBar(appTopToolbar)
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)
        supportActionBar!!.setDisplayShowHomeEnabled(true)
        appTopToolbar?.navigationIcon = ContextCompat.getDrawable(this, R.drawable.icon_back)

        mTitle = intent.getStringExtra(THREAD_TITLE)
        mNUmber = intent.getStringExtra(THREAD_NUMBER)

        val extras = intent.extras
        if (extras == null) {
            toast(R.string.unknown_error_occurred)
            hideKeyboard()
            finish()
            return
        }


        threadId = intent.getLongExtra(THREAD_ID, 0L)
        if (intent.hasExtra("isNotification")) {
            isNotification = intent.getBooleanExtra("isNotification", false)
        }
        if (mTitle != null && mNUmber != null) {
            checkAllowReply(mTitle!!, mNUmber!!)
        }

        supportActionBar?.title = mTitle
        if (!mTitle.equals(mNUmber) && mNUmber != null && !mNUmber!!.isEmpty()) {
            supportActionBar?.subtitle = mNUmber
        }

        bus = EventBus.getDefault()
        bus!!.register(this)
        handlePermission(PERMISSION_READ_PHONE_STATE) {
            if (it) {
                setupButtons()
                setupCachedMessages {
                    val searchedMessageId = intent.getLongExtra(SEARCHED_MESSAGE_ID, -1L)
                    intent.removeExtra(SEARCHED_MESSAGE_ID)
                    if (searchedMessageId != -1L) {
                        val index = threadItems.indexOfFirst { (it as? MessagesModel)?.id == searchedMessageId }
                        if (index != -1) {
                            binding.myRecyclerView.smoothScrollToPosition(index)
                        }
                    }

                    setupThread()
                }
            } else {
                hideKeyboard()
                finish()
            }
        }
    }

    private fun checkAllowReply(name: String, number: String) {
        if ((!TextUtils.isEmpty(name) && !TextUtils.isEmpty(number))) {
            if (name.equals(number) && !isNumeric(number)) {
                binding.llReplies.visibility = View.GONE
                binding.llNotReplies.visibility = View.VISIBLE
            } else {
                binding.llReplies.visibility = View.VISIBLE
                binding.llNotReplies.visibility = View.GONE
            }
        } else {
            binding.llReplies.visibility = View.VISIBLE
            binding.llNotReplies.visibility = View.GONE
        }
    }

    fun isNumeric(str: String): Boolean = str
        .removePrefix("-")
        .removePrefix("+")
        .all { it in '0'..'9' }

    override fun onResume() {
        super.onResume()
        hideKeyboard()
        val smsDraft = getSmsDraft(threadId)
        if (smsDraft != null && !isAttach) {
            binding.threadTypeMessage.setText(smsDraft)
        }
        isActivityVisible = true
    }

    override fun onPause() {
        super.onPause()

        if (binding.threadTypeMessage.value != "" && attachmentSelections.isEmpty() && !isAttach) {
            saveSmsDraft(binding.threadTypeMessage.value, threadId)
        } else {
            deleteSmsDraft(threadId)
        }

        bus?.post(RefreshEventsModel.RefreshMessages())

        isActivityVisible = false
    }

    override fun onDestroy() {
        super.onDestroy()
        bus?.unregister(this)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.app_menu_thread, menu)
        menu.apply {
            findItem(R.id.delete).isVisible = threadItems.isNotEmpty()
        }
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (participants.isEmpty()) {
            return true
        }

        when (item.itemId) {
            R.id.delete -> askConfirmDelete()
            R.id.manage_people -> managePeople()
            R.id.itemMore -> showMoreDialog()
            android.R.id.home -> onBackPressed()
            else -> return super.onOptionsItemSelected(item)
        }
        return true
    }

    fun startNewMainActivity(currentActivity: Activity, newTopActivityClass: Class<out Activity?>?) {
        val intent = Intent(currentActivity, newTopActivityClass)
        currentActivity.startActivity(intent)
    }

    override fun onBackPressed() {
        if (isNotification) {
            startNewMainActivity(this, HomeActivity::class.java)
            finish()
        } else {
            super.onBackPressed()
        }
    }

    private fun showMoreDialog() {
        val view: View = layoutInflater.inflate(R.layout.dialog_message_more, null)
        val popupMore = PopupWindow(
            view, WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT, true
        )
        popupMore.animationStyle = android.R.style.Animation_Dialog
        popupMore.showAtLocation(view, Gravity.TOP or Gravity.END, 0, 180)
        dimBackgroundPopWindow(this, popupMore)

        if (isNougatPlus()) {
            view.findViewById<View>(R.id.llBlock).visibility = View.VISIBLE
        }
        if (participants.size == 1) {
            view.findViewById<View>(R.id.llDial).visibility = View.VISIBLE
        }

        view.findViewById<View>(R.id.llDial).setOnClickListener { v: View? ->
            dismissPopup(popupMore)
            dialNumber()
        }
        view.findViewById<View>(R.id.llBlock).setOnClickListener { v: View? ->
            dismissPopup(popupMore)
            blockNumber()
        }
        view.findViewById<View>(R.id.llMarkUnread).setOnClickListener { v: View? ->
            dismissPopup(popupMore)
            markAsUnread()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, resultData: Intent?) {
        super.onActivityResult(requestCode, resultCode, resultData)
        if (requestCode == AttachContactRequestCode && resultCode == Activity.RESULT_OK) {
            binding.threadTypeMessage.setText("")
            addAttachmentContact(resultData)
        }
    }

    private fun setupCachedMessages(callback: () -> Unit) {
        ensureBackgroundThread {
            messages = try {
                messagesDB.getThreadMessages(threadId).toMutableList() as ArrayList<MessagesModel>
            } catch (e: Exception) {
                ArrayList()
            }

            setupParticipants()
            setupAdapter()

            runOnUiThread {
                if (messages.isEmpty()) {
                    window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE)
                    binding.threadTypeMessage.requestFocus()
                }

                setupThreadTitle()
                setupSIMSelector()
                callback()
            }
        }
    }

    private fun setupThread() {
        val privateCursor = getMyContactsCursor(false, true)?.loadInBackground()
        ensureBackgroundThread {
            val cachedMessagesCode = messages.clone().hashCode()
            messages = getMessages(threadId)

            val hasParticipantWithoutName = participants.any {
                it.phoneNumbers.contains(it.name)
            }

            try {
                if (participants.isNotEmpty() && messages.hashCode() == cachedMessagesCode && !hasParticipantWithoutName) {
                    return@ensureBackgroundThread
                }
            } catch (ignored: Exception) {
            }

            setupParticipants()

            // check if no participant came from a privately stored contact in Simple Contacts
            privateContacts = MyContactsContentProviderUtils.getSimpleContacts(this, privateCursor)
            if (privateContacts.isNotEmpty()) {
                val senderNumbersToReplace = HashMap<String, String>()
                participants.filter { it.doesHavePhoneNumber(it.name) }.forEach { participant ->
                    privateContacts.firstOrNull { it.doesHavePhoneNumber(participant.phoneNumbers.first()) }?.apply {
                        senderNumbersToReplace[participant.phoneNumbers.first()] = name
                        participant.name = name
                        participant.photoUri = photoUri
                    }
                }

                messages.forEach { message ->
                    if (senderNumbersToReplace.keys.contains(message.senderName)) {
                        message.senderName = senderNumbersToReplace[message.senderName]!!
                    }
                }
            }

            if (participants.isEmpty()) {
                val name = intent.getStringExtra(THREAD_TITLE) ?: ""
                val number = intent.getStringExtra(THREAD_NUMBER)
                if (number == null) {
                    toast(R.string.unknown_error_occurred)
                    hideKeyboard()
                    finish()
                    return@ensureBackgroundThread
                }

                val contact = ContactsModel(0, 0, name, "", arrayListOf(number), ArrayList(), ArrayList())
                participants.add(contact)
            }

            messages.chunked(30).forEach { currentMessages ->
                messagesDB.insertAddMessages(*currentMessages.toTypedArray())
            }

            setupAttachmentSizes()
            setupAdapter()
            runOnUiThread {
                setupThreadTitle()
                setupSIMSelector()
            }
        }
    }

    private fun setupAdapter() {
        threadItems = getThreadItems()
        invalidateOptionsMenu()

        runOnUiThread {
            val currAdapter = binding.myRecyclerView.adapter
            if (currAdapter == null) {
                msgAdapter = MessagesAdapter(this, threadItems, binding.myRecyclerView, binding.threadMessagesFastscroller) {
                    (it as? ExeptionModel)?.apply {
                        binding.threadTypeMessage.setText(it.messageText)
                    }
                }.apply {
                    binding.myRecyclerView.adapter = this
                }
                msgAdapter!!.deleteLuancherListner(this)
            } else {
                (currAdapter as MessagesAdapter).updateMessages(threadItems)
            }
        }

        SimpleContactsHelperUtils(this).getAvailableContacts(false) { contacts ->
            contacts.addAll(privateContacts)
            runOnUiThread {
                val adapter = AutoCompeletAdapter(this, contacts)
                binding.addContactOrNumber.setAdapter(adapter)
                binding.addContactOrNumber.imeOptions = EditorInfo.IME_ACTION_NEXT
                binding.addContactOrNumber.setOnItemClickListener { _, _, position, _ ->
                    val currContacts = (binding.addContactOrNumber.adapter as AutoCompeletAdapter).mResultList
                    val selectedContact = currContacts[position]
                    addSelectedContact(selectedContact)
                }


            }
        }
    }

    var isAttach = false
    private fun setupButtons() {

        binding.threadCharacterCounter.beVisibleIf(config.showCharacterCounter)
        binding.threadCharacterCounter.setTextSize(TypedValue.COMPLEX_UNIT_PX, getTextSize())

        binding.threadTypeMessage.setTextSize(TypedValue.COMPLEX_UNIT_PX, getTextSize())
        binding.threadSendMessage.setOnClickListener {
            sendMessage()
        }

        binding.threadSendMessage.isClickable = false
        binding.threadTypeMessage.onTextChangeListener {
            checkSendMessageAvailability()
            binding.threadCharacterCounter.text = it.length.toString()
        }

        binding.confirmManageContacts.setOnClickListener {
            hideKeyboard()
            binding.threadAddContacts.beGone()

            val numbers = HashSet<String>()
            participants.forEach {
                it.phoneNumbers.forEach {
                    numbers.add(it)
                }


            }
            val newThreadId = getThreadId(numbers)
            if (threadId != newThreadId) {
                val intentApp = Intent(this, MessagesActivity::class.java)
                intentApp.putExtra(THREAD_ID, newThreadId)
                intentApp.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                MainAppClass.instance?.displayInterstitialAds(this, intentApp, false)
            }
        }

        binding.threadTypeMessage.setText(intent.getStringExtra(THREAD_TEXT))
        binding.threadAddAttachment.setOnClickListener {
            if (binding.relOption.visibility == View.VISIBLE) {
                binding.relOption.visibility = View.GONE
            } else {
                binding.relOption.visibility = View.VISIBLE
            }
        }

        if (intent.extras?.containsKey(THREAD_ATTACHMENT_URI) == true) {
            val uri = Uri.parse(intent.getStringExtra(THREAD_ATTACHMENT_URI))
            addAttachment(uri)
        } else if (intent.extras?.containsKey(THREAD_ATTACHMENT_URIS) == true) {
            (intent.getSerializableExtra(THREAD_ATTACHMENT_URIS) as? ArrayList<*>)?.forEach {
                addAttachment(it as Uri)
            }
        }

        binding.ivContact.setOnClickListener {
            isAttach = true
            val intent = Intent(Intent.ACTION_PICK)
                .setType(ContactsContract.CommonDataKinds.Phone.CONTENT_TYPE)

            startActivityForResult(Intent.createChooser(intent, null), AttachContactRequestCode)
        }

        binding.ivLocation.setOnClickListener {}
        binding.ivPhotoGallery.setOnClickListener {}
    }

    private val AttachContactRequestCode = 100
    private fun setupAttachmentSizes() {
        messages.filter { it.attachment != null }.forEach {
            it.attachment!!.attachments.forEach {
                try {
                    if (it.mimetype.startsWith("image/")) {
                        val fileOptions = BitmapFactory.Options()
                        fileOptions.inJustDecodeBounds = true
                        BitmapFactory.decodeStream(contentResolver.openInputStream(it.getUri()), null, fileOptions)
                        it.width = fileOptions.outWidth
                        it.height = fileOptions.outHeight
                    } else if (it.mimetype.startsWith("video/")) {
                        val metaRetriever = MediaMetadataRetriever()
                        metaRetriever.setDataSource(this, it.getUri())
                        it.width = metaRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)!!.toInt()
                        it.height = metaRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)!!.toInt()
                    }

                    if (it.width < 0) {
                        it.width = 0
                    }

                    if (it.height < 0) {
                        it.height = 0
                    }
                } catch (ignored: Exception) {
                }
            }
        }
    }

    private fun setupParticipants() {
        if (participants.isEmpty()) {
            participants = if (messages.isEmpty()) {
                val intentNumbers = getPhoneNumbersFromIntent()
                val participants = getThreadParticipants(threadId, null)

                fixParticipantNumbers(participants, intentNumbers)
            } else {
                messages.first().participants
            }
        }
    }

    private fun setupThreadTitle() {
        val threadTitle = participants.getThreadTitle()
        if (threadTitle.isNotEmpty()) {
            supportActionBar?.title = participants.getThreadTitle()
            if (!participants.getThreadTitle()
                    .equals(participants.getThreadTitle()) && participants.getThreadTitle() != null && !participants.getThreadTitle()!!.isEmpty()
            ) {
                supportActionBar?.subtitle = participants.getSubTitle()
            }
            checkAllowReply(participants.getThreadTitle()!!, participants.getSubTitle())
        }
    }

    @SuppressLint("MissingPermission")
    private fun setupSIMSelector() {
        val availableSIMs = SubscriptionManager.from(this).activeSubscriptionInfoList ?: return
        if (availableSIMs.size > 1) {
            availableSIMs.forEachIndexed { index, subscriptionInfo ->
                var label = subscriptionInfo.displayName?.toString() ?: ""
                if (subscriptionInfo.number?.isNotEmpty() == true) {
                    label += " (${subscriptionInfo.number})"
                }
                val SIMCard = SimModel(index + 1, subscriptionInfo.subscriptionId, label)
                availableSIMCards.add(SIMCard)
            }

            val numbers = ArrayList<String>()
            participants.forEach {
                it.phoneNumbers.forEach {
                    numbers.add(it)
                }
            }

            if (numbers.isEmpty()) {
                return
            }

            currentSIMCardIndex = availableSIMs.indexOfFirstOrNull { it.subscriptionId == config.getUseSIMIdAtNumber(numbers.first()) } ?: 0

            binding.threadSelectSimIcon.beVisible()
            binding.threadSelectSimNumber.beVisible()

            if (availableSIMCards.isNotEmpty()) {
                binding.threadSelectSimIcon.setOnClickListener {
                    currentSIMCardIndex = (currentSIMCardIndex + 1) % availableSIMCards.size
                    val currentSIMCard = availableSIMCards[currentSIMCardIndex]
                    binding.threadSelectSimNumber.text = currentSIMCard.id.toString()
                    toast(currentSIMCard.label)
                }
            }

            binding.threadSelectSimNumber.text = (availableSIMCards[currentSIMCardIndex].id).toString()
        }
    }

    private fun blockNumber() {
        val numbers = ArrayList<String>()
        participants.forEach {
            it.phoneNumbers.forEach {
                numbers.add(it)
            }
        }

        val numbersString = TextUtils.join(", ", numbers)
        val question = String.format(resources.getString(R.string.block_confirmation), numbersString)

        AlertDialogCustom(this, question) {
            ensureBackgroundThread {
                numbers.forEach {
                    addBlockedNumber(it)
                }
                Log.e("Event: ", "SMS_Dialog_Alert")
                refreshMessages()
                finish()
            }
        }
    }

    private fun askConfirmDelete() {
        AlertDialogCustom(this, getString(R.string.delete_whole_conversation_confirmation)) {
            ensureBackgroundThread {
                deleteConversation(threadId)
                runOnUiThread {
                    Log.e("Event: ", "askConfirmDelete")
                    refreshMessages()
                    finish()
                }
            }
        }
    }

    private fun dialNumber() {
        val phoneNumber = participants.first().phoneNumbers.first()
        dialNumber(phoneNumber)
    }

    private fun managePeople() {
        if (binding.threadAddContacts.isVisible()) {
            hideKeyboard()
            binding.threadAddContacts.beGone()
        } else {
            showSelectedContacts()
            binding.threadAddContacts.beVisible()
            binding.addContactOrNumber.requestFocus()
            showKeyboard(binding.addContactOrNumber)
        }
    }

    private fun showSelectedContacts() {
        val adjustedColor = getAdjustedPrimaryColor()

        val views = ArrayList<View>()
        participants.forEach {
            val contact = it
            layoutInflater.inflate(R.layout.row_selected_contact, null).apply {
                val selectedContactBg = resources.getDrawable(R.drawable.item_selected_contact_background)
                (selectedContactBg as LayerDrawable).findDrawableByLayerId(R.id.selected_contact_bg).applyColorFilter(adjustedColor)
                findViewById<LinearLayout>(R.id.selected_contact_holder).background = selectedContactBg

                findViewById<TextView>(R.id.selected_contact_name).text = contact.name
                findViewById<TextView>(R.id.selected_contact_name).setTextColor(adjustedColor.getContrastColor())
                findViewById<ImageView>(R.id.selected_contact_remove).applyColorFilter(adjustedColor.getContrastColor())

                findViewById<ImageView>(R.id.selected_contact_remove).setOnClickListener {
                    if (contact.rawId != participants.first().rawId) {
                        removeSelectedContact(contact.rawId)
                    }
                }
                views.add(this)
            }
        }
        showSelectedContact(views)
    }

    private fun addSelectedContact(contact: ContactsModel) {
        binding.addContactOrNumber.setText("")
        if (participants.map { it.rawId }.contains(contact.rawId)) {
            return
        }

        participants.add(contact)
        showSelectedContacts()
    }

    private fun markAsUnread() {
        ensureBackgroundThread {
            conversationsDB.markUnreadAsMessage(threadId)
            markThreadMessagesUnreadNew(threadId)
            runOnUiThread {
                hideKeyboard()
                finish()
                bus?.post(RefreshEventsModel.RefreshMessages())
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun getThreadItems(): ArrayList<ItemModel> {
        val items = ArrayList<ItemModel>()
        if (isFinishing) {
            return items
        }

        messages.sortBy { it.date }

        val subscriptionIdToSimId = HashMap<Int, String>()
        subscriptionIdToSimId[-1] = "?"
        SubscriptionManager.from(this).activeSubscriptionInfoList?.forEachIndexed { index, subscriptionInfo ->
            subscriptionIdToSimId[subscriptionInfo.subscriptionId] = "${index + 1}"
        }

        var prevDateTime = 0
        var hadUnreadItems = false
        val cnt = messages.size
        for (i in 0 until cnt) {
            val message = messages.getOrNull(i) ?: continue
            // do not show the date/time above every message, only if the difference between the 2 messages is at least MIN_DATE_TIME_DIFF_SECS
            if (message.date - prevDateTime > MIN_DATE_TIME_DIFF_SECS) {
                val simCardID = subscriptionIdToSimId[message.subscriptionId] ?: "?"
                items.add(DateAndTimeModel(message.date, simCardID))
                prevDateTime = message.date
            }
            items.add(message)

            if (message.type == Telephony.Sms.MESSAGE_TYPE_FAILED) {
                items.add(ExeptionModel(message.id, message.body))
            }

            if (message.type == Telephony.Sms.MESSAGE_TYPE_OUTBOX) {
                items.add(SendSmsModel(message.id))
            }
            if (!message.read) {
                hadUnreadItems = true
                markMessageRead(message.id, message.isMMS)
                conversationsDB.markReadAsMessage(threadId)
            }

            if (i == cnt - 1 && (message.type == Telephony.Sms.MESSAGE_TYPE_SENT)) {
                items.add(SendedSmsModel(message.id, delivered = message.status == Telephony.Sms.STATUS_COMPLETE))
            }
        }

        if (hadUnreadItems) {
            bus?.post(RefreshEventsModel.RefreshMessages())
        }

        return items
    }


    private fun addAttachment(uri: Uri) {
        val originalUriString = uri.toString()
        if (attachmentSelections.containsKey(originalUriString)) {
            return
        }

        attachmentSelections[originalUriString] = SelectedAttachmentModel(uri, false)
        val attachmentView = addAttachmentView(originalUriString, uri)
        val mimeType = contentResolver.getType(uri) ?: return

        if (mimeType.isImageMimeType() && config.mmsFileSizeLimit != FILE_SIZE_NONE) {
            val selection = attachmentSelections[originalUriString]
            attachmentSelections[originalUriString] = selection!!.copy(isPending = true)
            checkSendMessageAvailability()
            attachmentView.findViewById<ProgressBar>(R.id.thread_attachment_progress).beVisible()
            imageCompressor.compressImage(uri, config.mmsFileSizeLimit) { compressedUri ->
                runOnUiThread {
                    if (compressedUri != null) {
                        attachmentSelections[originalUriString] = SelectedAttachmentModel(compressedUri, false)
                        loadAttachmentPreview(attachmentView, compressedUri)
                    }
                    checkSendMessageAvailability()
                    attachmentView.findViewById<ProgressBar>(R.id.thread_attachment_progress).beGone()
                }
            }
        }
    }

    private fun addAttachmentContact(intent: Intent?) {
        var cursor: Cursor? = null
        try {
            var phoneNo: String? = null
            var name: String? = null
            val uri: Uri = intent!!.data!!
            cursor = contentResolver.query(uri, null, null, null, null)
            cursor!!.moveToFirst()
            val phoneIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
            val nameIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
            phoneNo = cursor.getString(phoneIndex)
            name = cursor.getString(nameIndex)
            Log.e("Name and C", "$name,$phoneNo")
            binding.threadTypeMessage.setText("Name: " + name + "\nContact Number: " + phoneNo)

        } catch (e: java.lang.Exception) {
            e.printStackTrace()
        }
    }


    private fun addAttachmentView(originalUri: String, uri: Uri): View {
        binding.threadAttachmentsHolder.beVisible()
        val attachmentView = layoutInflater.inflate(R.layout.layout_attach_photo, null).apply {
            binding.threadAttachmentsWrapper.addView(this)
            findViewById<ImageView>(R.id.thread_remove_attachment).setOnClickListener {
                binding.threadAttachmentsWrapper.removeView(this)
                attachmentSelections.remove(originalUri)
                if (attachmentSelections.isEmpty()) {
                    binding.threadAttachmentsHolder.beGone()
                }
            }
        }

        loadAttachmentPreview(attachmentView, uri)
        return attachmentView
    }

    private fun loadAttachmentPreview(attachmentView: View, uri: Uri) {
        if (isDestroyed || isFinishing) {
            return
        }

        val roundedCornersRadius = resources.getDimension(R.dimen.margin_8).toInt()
        val options = RequestOptions()
            .diskCacheStrategy(DiskCacheStrategy.NONE)
            .transform(CenterCrop(), RoundedCorners(roundedCornersRadius))

        Glide.with(attachmentView.findViewById<ImageView>(R.id.thread_attachment_preview))
            .load(uri)
            .transition(DrawableTransitionOptions.withCrossFade())
            .apply(options)
            .listener(object : RequestListener<Drawable> {
                override fun onLoadFailed(
                    e: GlideException?,
                    model: Any?,
                    target: Target<Drawable?>,
                    isFirstResource: Boolean
                ): Boolean {
                    attachmentView.findViewById<ImageView>(R.id.thread_attachment_preview).beGone()
                    attachmentView.findViewById<ImageView>(R.id.thread_remove_attachment).beGone()
                    return false
                }

                override fun onResourceReady(
                    resource: Drawable,
                    model: Any,
                    target: Target<Drawable?>?,
                    dataSource: DataSource,
                    isFirstResource: Boolean
                ): Boolean {
                    attachmentView.findViewById<ImageView>(R.id.thread_attachment_preview).beVisible()
                    attachmentView.findViewById<ImageView>(R.id.thread_remove_attachment).beVisible()
                    checkSendMessageAvailability()
                    return false
                }

            })
            .into(attachmentView.findViewById<ImageView>(R.id.thread_attachment_preview))
    }

    private fun checkSendMessageAvailability() {
        binding.threadSendMessage.isClickable =
            binding.threadTypeMessage.text.isNotEmpty() || (attachmentSelections.isNotEmpty() && !attachmentSelections.values.any { it.isPending })
    }


    private fun sendMessage() {
        var msg = binding.threadTypeMessage.value
        if (msg.isEmpty() && attachmentSelections.isEmpty()) {
            return
        }

        msg = removeDiacriticsIfNeeded(msg)

        val numbers = ArrayList<String>()

        if (participants.size > 1) {
            participants.forEach {
                it.phoneNumbers.forEach {
                    numbers.add(it)
                }
            }

            val settings = Settings()
            settings.useSystemSending = true
            settings.deliveryReports = config.enableDeliveryReports

            val SIMId = availableSIMCards.getOrNull(currentSIMCardIndex)?.subscriptionId
            if (SIMId != null) {
                settings.subscriptionId = SIMId
                numbers.forEach {
                    config.saveUseSIMIdAtNumber(it, SIMId)
                }
            }

            val transaction = Transaction(this, settings)
            val message = Message(msg, numbers.toTypedArray())

            if (attachmentSelections.isNotEmpty()) {
                for (selection in attachmentSelections.values) {
                    try {
                        val byteArray = contentResolver.openInputStream(selection.uri)?.readBytes() ?: continue
                        val mimeType = contentResolver.getType(selection.uri) ?: continue
                        message.addMedia(byteArray, mimeType)
                    } catch (e: Exception) {
                        showErrorToast(e)
                    } catch (e: Error) {
                        toast(e.localizedMessage ?: getString(R.string.unknown_error_occurred))
                    }
                }
            }

            val smsSentIntent = Intent(this, SMS_Service_Sended_Status_Receiver::class.java)
            val deliveredIntent = Intent(this, SendedSMSStatusService::class.java)

            transaction.setExplicitBroadcastForSentSms(smsSentIntent)
            transaction.setExplicitBroadcastForDeliveredSms(deliveredIntent)

            refreshedSinceSent = false
            transaction.sendNewMessage(message, threadId)
            binding.threadTypeMessage.setText("")
            attachmentSelections.clear()
            binding.threadAttachmentsHolder.beGone()
            binding.threadAttachmentsWrapper.removeAllViews()

            Handler(Looper.getMainLooper()).postDelayed({
                if (!refreshedSinceSent) {
                    Log.e("Event: ", "sendMessage")
                    refreshMessages()
                }
            }, 2000)

        } else {
            sendMessage(msg, participants.get(0).phoneNumbers[0], false)
        }


    }

    fun sendMessage(msg: String, numbers: String, isMultipleUser: Boolean) {
        val settings = Settings()
        settings.useSystemSending = true
        settings.deliveryReports = config.enableDeliveryReports

        val SIMId = availableSIMCards.getOrNull(currentSIMCardIndex)?.subscriptionId
        if (SIMId != null) {
            settings.subscriptionId = SIMId
            numbers.forEach {
                config.saveUseSIMIdAtNumber(numbers, SIMId)
            }
        }

        val transaction = Transaction(this, settings)
        val message = Message(msg, numbers)

        if (attachmentSelections.isNotEmpty()) {
            for (selection in attachmentSelections.values) {
                try {
                    val byteArray = contentResolver.openInputStream(selection.uri)?.readBytes() ?: continue
                    val mimeType = contentResolver.getType(selection.uri) ?: continue
                    message.addMedia(byteArray, mimeType)
                } catch (e: Exception) {
                    showErrorToast(e)
                } catch (e: Error) {
                    toast(e.localizedMessage ?: getString(R.string.unknown_error_occurred))
                }
            }
        }

        val smsSentIntent = Intent(this, SMS_Service_Sended_Status_Receiver::class.java)
        val deliveredIntent = Intent(this, SendedSMSStatusService::class.java)

        transaction.setExplicitBroadcastForSentSms(smsSentIntent)
        transaction.setExplicitBroadcastForDeliveredSms(deliveredIntent)

        refreshedSinceSent = false
        transaction.sendNewMessage(message, threadId)
        binding.threadTypeMessage.setText("")
        attachmentSelections.clear()
        binding.threadAttachmentsHolder.beGone()
        binding.threadAttachmentsWrapper.removeAllViews()

        Handler(Looper.getMainLooper()).postDelayed({
            if (!refreshedSinceSent) {
                Log.e("Event: ", " not refreshedSinceSent")
                refreshMessages()
            }
        }, 2000)

    }

    private fun showSelectedContact(views: ArrayList<View>) {
        binding.selectedContacts.removeAllViews()
        var newLinearLayout = LinearLayout(this)
        newLinearLayout.layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
        newLinearLayout.orientation = LinearLayout.HORIZONTAL

        val sideMargin = (binding.selectedContacts.layoutParams as LayoutParams).leftMargin
        val mediumMargin = resources.getDimension(R.dimen.margin_8).toInt()
        val parentWidth = realScreenSize.x - sideMargin * 2
        val firstRowWidth = parentWidth - resources.getDimension(R.dimen.margin_48).toInt() + sideMargin / 2
        var widthSoFar = 0
        var isFirstRow = true

        for (i in views.indices) {
            val LL = LinearLayout(this)
            LL.orientation = LinearLayout.HORIZONTAL
            LL.gravity = Gravity.CENTER_HORIZONTAL or Gravity.BOTTOM
            LL.layoutParams = LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT)
            views[i].measure(0, 0)

            var params = LayoutParams(views[i].measuredWidth, LayoutParams.WRAP_CONTENT)
            params.setMargins(0, 0, mediumMargin, 0)
            LL.addView(views[i], params)
            LL.measure(0, 0)
            widthSoFar += views[i].measuredWidth + mediumMargin

            val checkWidth = if (isFirstRow) firstRowWidth else parentWidth
            if (widthSoFar >= checkWidth) {
                isFirstRow = false
                binding.selectedContacts.addView(newLinearLayout)
                newLinearLayout = LinearLayout(this)
                newLinearLayout.layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
                newLinearLayout.orientation = LinearLayout.HORIZONTAL
                params = LayoutParams(LL.measuredWidth, LL.measuredHeight)
                params.topMargin = mediumMargin
                newLinearLayout.addView(LL, params)
                widthSoFar = LL.measuredWidth
            } else {
                if (!isFirstRow) {
                    (LL.layoutParams as LayoutParams).topMargin = mediumMargin
                }
                newLinearLayout.addView(LL)
            }
        }
        binding.selectedContacts.addView(newLinearLayout)
    }

    private fun removeSelectedContact(id: Int) {
        participants = participants.filter { it.rawId != id }.toMutableList() as ArrayList<ContactsModel>
        showSelectedContacts()
    }

    private fun getPhoneNumbersFromIntent(): ArrayList<String> {

        val numbers = ArrayList<String>()

        if (mNUmber != null) {
            if (mNUmber!!.startsWith('[') && mNUmber!!.endsWith(']')) {
                val type = object : TypeToken<List<String>>() {}.type
                numbers.addAll(Gson().fromJson(mNUmber!!, type))
            } else {
                numbers.add(mNUmber!!)
            }
        }
        return numbers
    }

    private fun fixParticipantNumbers(participants: ArrayList<ContactsModel>, properNumbers: ArrayList<String>): ArrayList<ContactsModel> {
        for (number in properNumbers) {
            for (participant in participants) {
                participant.phoneNumbers = participant.phoneNumbers.map {
                    val numberWithoutPlus = number.replace("+", "")
                    if (numberWithoutPlus == it.trim()) {
                        if (participant.name == it) {
                            participant.name = number
                        }
                        number
                    } else {
                        it
                    }
                } as ArrayList<String>
            }
        }

        return participants
    }

    @SuppressLint("MissingPermission")
    @Subscribe(threadMode = ThreadMode.ASYNC)
    fun refreshMessages(event: RefreshEventsModel.RefreshMessages) {
        refreshedSinceSent = true
        if (isActivityVisible) {
            notificationManager.cancel(threadId.hashCode())
        }

        val lastMaxId = messages.maxByOrNull { it.id }?.id ?: 0L
        messages = getMessages(threadId)

        messages.filter { !it.isReceivedMessage() && it.id > lastMaxId }.forEach { latestMessage ->
            // subscriptionIds seem to be not filled out at sending with multiple SIM cards, so fill it manually
            if (SubscriptionManager.from(this).activeSubscriptionInfoList?.size ?: 0 > 1) {
                val SIMId = availableSIMCards.getOrNull(currentSIMCardIndex)?.subscriptionId
                if (SIMId != null) {
                    updateMessageSubscriptionId(latestMessage.id, SIMId)
                    latestMessage.subscriptionId = SIMId
                }
            }

            messagesDB.insertOrIgnoreMessage(latestMessage)
        }

        setupAdapter()
    }

    override fun onDeleteLuancherCall(message: String, pos: Int) {
        posLauncher = pos
        val intent = Intent(this, SelectTextActivity::class.java)
        intent.putExtra(THREAD_TITLE, message)
        MainAppClass.instance?.displayInterstitialAdsResultLaunch(this, object : OnActivityResultLauncher {
            override fun onLauncher() {
                resultLauncher.launch(intent)
            }
        })

    }

    var resultLauncher: ActivityResultLauncher<Intent> = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            ensureBackgroundThread {
                msgAdapter!!.deleteMessagesLauncher(posLauncher)
            }
        }
    }
}
