package com.hkapps.messagepro.extensions

import android.Manifest
import android.annotation.TargetApi
import android.app.Activity
import android.app.NotificationManager
import android.app.role.RoleManager
import android.content.*
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.database.Cursor
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Point
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.BaseColumns
import android.provider.BlockedNumberContract.BlockedNumbers
import android.provider.MediaStore.Audio
import android.provider.MediaStore.Images
import android.provider.MediaStore.MediaColumns
import android.provider.OpenableColumns
import android.telecom.TelecomManager
import android.telephony.PhoneNumberUtils
import android.util.Log
import android.view.WindowManager
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.loader.content.CursorLoader
import com.hkapps.messagepro.R
import com.hkapps.messagepro.model.BlockedNumberModel
import com.hkapps.messagepro.model.ThemeModel
import com.hkapps.messagepro.utils.*
import com.hkapps.messagepro.utils.MyContentProviderUtils.Companion.COL_ACCENT_COLOR
import com.hkapps.messagepro.utils.MyContentProviderUtils.Companion.COL_APP_ICON_COLOR
import com.hkapps.messagepro.utils.MyContentProviderUtils.Companion.COL_BACKGROUND_COLOR
import com.hkapps.messagepro.utils.MyContentProviderUtils.Companion.COL_LAST_UPDATED_TS
import com.hkapps.messagepro.utils.MyContentProviderUtils.Companion.COL_NAVIGATION_BAR_COLOR
import com.hkapps.messagepro.utils.MyContentProviderUtils.Companion.COL_PRIMARY_COLOR
import com.hkapps.messagepro.utils.MyContentProviderUtils.Companion.COL_TEXT_COLOR
import java.text.SimpleDateFormat
import java.util.*

fun Context.getSharedPrefs() = getSharedPreferences(PREFS_KEY, Context.MODE_PRIVATE)
inline fun <T> List<T>.indexOfFirstOrNull(predicate: (T) -> Boolean): Int? {
    var index = 0
    for (item in this) {
        if (predicate(item))
            return index
        index++
    }
    return null
}

fun Bitmap.CompressFormat.extension() = when (this) {
    Bitmap.CompressFormat.PNG -> "png"
    Bitmap.CompressFormat.WEBP -> "webp"
    else -> "jpg"
}


//fun Context.updateTextColors(viewGroup: ViewGroup, tmpTextColor: Int = 0, tmpAccentColor: Int = 0) {
//    val textColor = if (tmpTextColor == 0) mPref.textColor else tmpTextColor
//    val backgroundColor = mPref.backgroundColor
//    val accentColor = if (tmpAccentColor == 0) {
//        when {
//            isWhiteTheme() || isBlackAndWhiteTheme() -> mPref.accentColor
//            else -> mPref.primaryColor
//        }
//    } else {
//        tmpAccentColor
//    }
//
//    val cnt = viewGroup.childCount
//    (0 until cnt).map { viewGroup.getChildAt(it) }.forEach {
//        when (it) {
////            is TextView -> it.setColors(textColor, accentColor, backgroundColor)
////            is MyAppCompatSpinnerView -> it.setColors(textColor, accentColor, backgroundColor)
////            is MySwitchCompat -> it.setColors(textColor, accentColor, backgroundColor)
////            is MyCompatRadioButtonView -> it.setColors(textColor, accentColor, backgroundColor)
////            is MyAppCompatCheckboxView -> it.setColors(textColor, accentColor, backgroundColor)
////            is EditText -> it.setColors(textColor, accentColor, backgroundColor)
//            is MyAutoCompleteTextView -> it.setColors(textColor, accentColor, backgroundColor)
////            is MyFloatingActionButtonView -> it.setColors(textColor, accentColor, backgroundColor)
////            is MySeekBarView -> it.setColors(textColor, accentColor, backgroundColor)
////            is MyButtonView -> it.setColors(textColor, accentColor, backgroundColor)
//            is MyTextInputLayout -> it.setColors(textColor, accentColor, backgroundColor)
////            is ViewGroup -> updateTextColors(it, textColor, accentColor)
//        }
//    }
//}


fun Context.isBlackAndWhiteTheme() = mPref.textColor == Color.WHITE && mPref.primaryColor == Color.BLACK && mPref.backgroundColor == Color.BLACK

fun Context.isWhiteTheme() = mPref.textColor == DARK_GREY && mPref.primaryColor == Color.WHITE && mPref.backgroundColor == Color.WHITE

fun Context.isUsingSystemDarkTheme() = resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_YES != 0

fun Context.getAdjustedPrimaryColor() = when {
    isWhiteTheme() || isBlackAndWhiteTheme() -> mPref.accentColor
    else -> mPref.primaryColor
}

fun Context.toast(id: Int, length: Int = Toast.LENGTH_SHORT) {
    toast(getString(id), length)
}

fun Context.toast(msg: String, length: Int = Toast.LENGTH_SHORT) {
    try {
        if (isOnMainThread()) {
            doToast(this, msg, length)
        } else {
            Handler(Looper.getMainLooper()).post {
                doToast(this, msg, length)
            }
        }
    } catch (e: Exception) {
        Log.i("Exception",e.toString())
    }
}

private fun doToast(context: Context, message: String, length: Int) {
    if (context is Activity) {
        if (!context.isFinishing && !context.isDestroyed) {
            Toast.makeText(context, message, length).show()
        }
    } else {
        Toast.makeText(context, message, length).show()
    }
}

fun Context.showErrorToast(msg: String, length: Int = Toast.LENGTH_LONG) {
    toast(String.format(getString(R.string.an_error_occurred), msg), length)
}

fun Context.showErrorToast(exception: Exception, length: Int = Toast.LENGTH_LONG) {
    showErrorToast(exception.toString(), length)
}

val Context.mPref: BasePref get() = BasePref.newInstance(this)
val Context.sdCardPath: String get() = mPref.sdCardPath
val Context.internalStoragePath: String get() = mPref.internalStoragePath
val Context.otgPath: String get() = mPref.OTGPath

val Context.targetSdkVersion: Int get() = applicationInfo.targetSdkVersion

fun Context.isTargetSdkVersion30Plus(): Boolean = targetSdkVersion >= Build.VERSION_CODES.R



fun Context.hasPermission(permId: Int) = ContextCompat.checkSelfPermission(this, getPermissionString(permId)) == PackageManager.PERMISSION_GRANTED

fun Context.getPermissionString(id: Int) = when (id) {
    PERMISSION_READ_STORAGE -> Manifest.permission.READ_EXTERNAL_STORAGE
    PERMISSION_WRITE_STORAGE -> Manifest.permission.WRITE_EXTERNAL_STORAGE
    PERMISSION_CAMERA -> Manifest.permission.CAMERA
    PERMISSION_RECORD_AUDIO -> Manifest.permission.RECORD_AUDIO
    PERMISSION_READ_CONTACTS -> Manifest.permission.READ_CONTACTS
    PERMISSION_WRITE_CONTACTS -> Manifest.permission.WRITE_CONTACTS
    PERMISSION_READ_CALENDAR -> Manifest.permission.READ_CALENDAR
    PERMISSION_WRITE_CALENDAR -> Manifest.permission.WRITE_CALENDAR
    PERMISSION_CALL_PHONE -> Manifest.permission.CALL_PHONE
    PERMISSION_READ_CALL_LOG -> Manifest.permission.READ_CALL_LOG
    PERMISSION_WRITE_CALL_LOG -> Manifest.permission.WRITE_CALL_LOG
    PERMISSION_GET_ACCOUNTS -> Manifest.permission.GET_ACCOUNTS
    PERMISSION_READ_SMS -> Manifest.permission.READ_SMS
    PERMISSION_SEND_SMS -> Manifest.permission.SEND_SMS
    PERMISSION_READ_PHONE_STATE -> Manifest.permission.READ_PHONE_STATE
    else -> ""
}

fun Context.launchActivityIntent(intent: Intent) {
    try {
        startActivity(intent)
    } catch (e: ActivityNotFoundException) {
        toast(R.string.no_app_found)
    } catch (e: Exception) {
        showErrorToast(e)
    }
}

fun Context.getMediaContent(path: String, uri: Uri): Uri? {
    val projection = arrayOf(Images.Media._ID)
    val selection = Images.Media.DATA + "= ?"
    val selectionArgs = arrayOf(path)
    try {
        val cursor = contentResolver.query(uri, projection, selection, selectionArgs, null)
        cursor?.use {
            if (cursor.moveToFirst()) {
                val id = cursor.getIntValue(Images.Media._ID).toString()
                return Uri.withAppendedPath(uri, id)
            }
        }
    } catch (e: Exception) {
    }
    return null
}

fun Context.queryCursor(
    uri: Uri,
    projection: Array<String>,
    selection: String? = null,
    selectionArgs: Array<String>? = null,
    sortOrder: String? = null,
    showErrors: Boolean = false,
    callback: (cursor: Cursor) -> Unit
) {
    try {
        val cursor = contentResolver.query(uri, projection, selection, selectionArgs, sortOrder)
        cursor?.use {
            if (cursor.moveToFirst()) {
                do {
                    callback(cursor)
                } while (cursor.moveToNext())
            }
        }
    } catch (e: Exception) {
        /*if (showErrors) {
            if(e.message.toString().contentEquals("Permission Denial: reading com.android.providers.telephony.SmsProvider uri content://sms/draft from pid=29861, uid=10580 requires android.permission.READ_SMS, or grantUriPermission()")){

            }else{
                showErrorToast(e)
            }
        }*/
    }
}

fun Context.getSizeFromContentUri(uri: Uri): Long {
    val projection = arrayOf(OpenableColumns.SIZE)
    try {
        val cursor = contentResolver.query(uri, projection, null, null, null)
        cursor?.use {
            if (cursor.moveToFirst()) {
                return cursor.getLongValue(OpenableColumns.SIZE)
            }
        }
    } catch (e: Exception) {
    }
    return 0L
}

fun Context.getSharedTheme(callback: (sharedTheme: ThemeModel?) -> Unit) {
    if (!isThankYouInstalled()) {
        callback(null)
    } else {
        val cursorLoader = getMyContentProviderCursorLoader()
        ensureBackgroundThread {
            callback(getSharedThemeSync(cursorLoader))
        }
    }
}

fun Context.getSharedThemeSync(cursorLoader: CursorLoader): ThemeModel? {
    val cursor = cursorLoader.loadInBackground()
    cursor?.use {
        if (cursor.moveToFirst()) {
            try {
                val textColor = cursor.getIntValue(COL_TEXT_COLOR)
                val backgroundColor = cursor.getIntValue(COL_BACKGROUND_COLOR)
                val primaryColor = cursor.getIntValue(COL_PRIMARY_COLOR)
                val accentColor = cursor.getIntValue(COL_ACCENT_COLOR)
                val appIconColor = cursor.getIntValue(COL_APP_ICON_COLOR)
                val navigationBarColor = cursor.getIntValueOrNull(COL_NAVIGATION_BAR_COLOR) ?: INVALID_NAVIGATION_BAR_COLOR
                val lastUpdatedTS = cursor.getIntValue(COL_LAST_UPDATED_TS)
                return ThemeModel(textColor, backgroundColor, primaryColor, appIconColor, navigationBarColor, lastUpdatedTS, accentColor)
            } catch (e: Exception) {
            }
        }
    }
    return null
}

fun Context.getMyContentProviderCursorLoader() = CursorLoader(this, MyContentProviderUtils.MY_CONTENT_URI, null, null, null, null)

fun Context.getMyContactsCursor(favoritesOnly: Boolean, withPhoneNumbersOnly: Boolean) = try {
    val getFavoritesOnly = if (favoritesOnly) "1" else "0"
    val getWithPhoneNumbersOnly = if (withPhoneNumbersOnly) "1" else "0"
    val args = arrayOf(getFavoritesOnly, getWithPhoneNumbersOnly)
    CursorLoader(this, MyContactsContentProviderUtils.CONTACTS_CONTENT_URI, null, null, args, null)
} catch (e: Exception) {
    null
}

fun Context.getCurrentFormattedDateTime(): String {
    val simpleDateFormat = SimpleDateFormat("yyyy_MM_dd_HH_mm_ss", Locale.getDefault())
    return simpleDateFormat.format(Date(System.currentTimeMillis()))
}

fun Context.updateSDCardPath() {
    ensureBackgroundThread {
        val oldPath = mPref.sdCardPath
        mPref.sdCardPath = getSDCardPath()
        if (oldPath != mPref.sdCardPath) {
            mPref.sdTreeUri = ""
        }
    }
}


fun Context.isThankYouInstalled() = isPackageInstalled("")


fun Context.isPackageInstalled(pkgName: String): Boolean {
    /* return try {
         packageManager.getPackageInfo(pkgName, 0)
         true
     } catch (e: Exception) {
         false
     }*/
    return true
}

fun Context.checkAppIconColor() {
    val appId = mPref.appId
    if (appId.isNotEmpty() && mPref.lastIconColor != mPref.appIconColor) {
        /*  getAppIconColors().forEachIndexed { index, color ->
              toggleAppIconColor(appId, index, color, false)
          }

          getAppIconColors().forEachIndexed { index, color ->
              if (mPref.appIconColor == color) {
                  toggleAppIconColor(appId, index, color, true)
              }
          }*/
    }
}

fun Context.getStoreUrl() = "https://play.google.com/store/apps/details?id=${packageName.removeSuffix(".debug")}"

fun Context.getTimeFormat() = if (mPref.use24HourFormat) TIME_FORMAT_24 else TIME_FORMAT_12

fun Context.getResolution(path: String): Point? {
    return if (path.isImageFast() || path.isImageSlow()) {
        getImageResolution(path)
    } else if (path.isVideoFast() || path.isVideoSlow()) {
        getVideoResolution(path)
    } else {
        null
    }
}

fun Context.getImageResolution(path: String): Point? {
    val options = BitmapFactory.Options()
    options.inJustDecodeBounds = true
    if (isRestrictedSAFOnlyRoot(path)) {
        BitmapFactory.decodeStream(contentResolver.openInputStream(getAndroidSAFUri(path)), null, options)
    } else {
        BitmapFactory.decodeFile(path, options)
    }

    val width = options.outWidth
    val height = options.outHeight
    return if (width > 0 && height > 0) {
        Point(options.outWidth, options.outHeight)
    } else {
        null
    }
}

fun Context.getVideoResolution(path: String): Point? {
    var point = try {
        val retriever = MediaMetadataRetriever()
        if (isRestrictedSAFOnlyRoot(path)) {
            retriever.setDataSource(this, getAndroidSAFUri(path))
        } else {
            retriever.setDataSource(path)
        }

        val width = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)!!.toInt()
        val height = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)!!.toInt()
        Point(width, height)
    } catch (ignored: Exception) {
        null
    }

    if (point == null && path.startsWith("content://", true)) {
        try {
            val fd = contentResolver.openFileDescriptor(Uri.parse(path), "r")?.fileDescriptor
            val retriever = MediaMetadataRetriever()
            retriever.setDataSource(fd)
            val width = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)!!.toInt()
            val height = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)!!.toInt()
            point = Point(width, height)
        } catch (ignored: Exception) {
        }
    }

    return point
}

fun Context.getDuration(path: String): Int? {
    val projection = arrayOf(
        MediaColumns.DURATION
    )

    val uri = getFileUri(path)
    val selection = if (path.startsWith("content://")) "${BaseColumns._ID} = ?" else "${MediaColumns.DATA} = ?"
    val selectionArgs = if (path.startsWith("content://")) arrayOf(path.substringAfterLast("/")) else arrayOf(path)

    try {
        val cursor = contentResolver.query(uri, projection, selection, selectionArgs, null)
        cursor?.use {
            if (cursor.moveToFirst()) {
                return Math.round(cursor.getIntValue(MediaColumns.DURATION) / 1000.toDouble()).toInt()
            }
        }
    } catch (ignored: Exception) {
    }

    return try {
        val retriever = MediaMetadataRetriever()
        retriever.setDataSource(path)
        Math.round(retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)!!.toInt() / 1000f)
    } catch (ignored: Exception) {
        null
    }
}

fun Context.getTitle(path: String): String? {
    val projection = arrayOf(
        MediaColumns.TITLE
    )

    val uri = getFileUri(path)
    val selection = if (path.startsWith("content://")) "${BaseColumns._ID} = ?" else "${MediaColumns.DATA} = ?"
    val selectionArgs = if (path.startsWith("content://")) arrayOf(path.substringAfterLast("/")) else arrayOf(path)

    try {
        val cursor = contentResolver.query(uri, projection, selection, selectionArgs, null)
        cursor?.use {
            if (cursor.moveToFirst()) {
                return cursor.getStringValue(MediaColumns.TITLE)
            }
        }
    } catch (ignored: Exception) {
    }

    return try {
        val retriever = MediaMetadataRetriever()
        retriever.setDataSource(path)
        retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)
    } catch (ignored: Exception) {
        null
    }
}

fun Context.getArtist(path: String): String? {
    val projection = arrayOf(
        Audio.Media.ARTIST
    )

    val uri = getFileUri(path)
    val selection = if (path.startsWith("content://")) "${BaseColumns._ID} = ?" else "${MediaColumns.DATA} = ?"
    val selectionArgs = if (path.startsWith("content://")) arrayOf(path.substringAfterLast("/")) else arrayOf(path)

    try {
        val cursor = contentResolver.query(uri, projection, selection, selectionArgs, null)
        cursor?.use {
            if (cursor.moveToFirst()) {
                return cursor.getStringValue(Audio.Media.ARTIST)
            }
        }
    } catch (ignored: Exception) {
    }

    return try {
        val retriever = MediaMetadataRetriever()
        retriever.setDataSource(path)
        retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)
    } catch (ignored: Exception) {
        null
    }
}

fun Context.getAlbum(path: String): String? {
    val projection = arrayOf(
        Audio.Media.ALBUM
    )

    val uri = getFileUri(path)
    val selection = if (path.startsWith("content://")) "${BaseColumns._ID} = ?" else "${MediaColumns.DATA} = ?"
    val selectionArgs = if (path.startsWith("content://")) arrayOf(path.substringAfterLast("/")) else arrayOf(path)

    try {
        val cursor = contentResolver.query(uri, projection, selection, selectionArgs, null)
        cursor?.use {
            if (cursor.moveToFirst()) {
                return cursor.getStringValue(Audio.Media.ALBUM)
            }
        }
    } catch (ignored: Exception) {
    }

    return try {
        val retriever = MediaMetadataRetriever()
        retriever.setDataSource(path)
        retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM)
    } catch (ignored: Exception) {
        null
    }
}

fun Context.getMediaStoreLastModified(path: String): Long {
    val projection = arrayOf(
        MediaColumns.DATE_MODIFIED
    )

    val uri = getFileUri(path)
    val selection = "${BaseColumns._ID} = ?"
    val selectionArgs = arrayOf(path.substringAfterLast("/"))

    try {
        val cursor = contentResolver.query(uri, projection, selection, selectionArgs, null)
        cursor?.use {
            if (cursor.moveToFirst()) {
                return cursor.getLongValue(MediaColumns.DATE_MODIFIED) * 1000
            }
        }
    } catch (ignored: Exception) {
    }
    return 0
}

fun Context.getStringsPackageName() = getString(R.string.package_name)

fun Context.getFontSizeText() = getString(
    when (mPref.fontSize) {
        FONT_SIZE_SMALL -> R.string.small
        FONT_SIZE_MEDIUM -> R.string.medium
        FONT_SIZE_LARGE -> R.string.large
        else -> R.string.extra_large
    }
)

fun Context.getTextSize() = when (mPref.fontSize) {
    FONT_SIZE_SMALL -> resources.getDimension(R.dimen.textsize_12)
    FONT_SIZE_MEDIUM -> resources.getDimension(R.dimen.textsize_16)
    FONT_SIZE_LARGE -> resources.getDimension(R.dimen.textsize_18)
    else -> resources.getDimension(R.dimen.textsize_22)
}

val Context.telecomManager: TelecomManager get() = getSystemService(Context.TELECOM_SERVICE) as TelecomManager
val Context.windowManager: WindowManager get() = getSystemService(Context.WINDOW_SERVICE) as WindowManager
val Context.notificationManager: NotificationManager get() = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
val Context.usableScreenSize: Point
    get() {
        val size = Point()
        windowManager.defaultDisplay.getSize(size)
        return size
    }

val Context.realScreenSize: Point
    get() {
        val size = Point()
        windowManager.defaultDisplay.getRealSize(size)
        return size
    }

// we need the Default Dialer functionality only in Simple Dialer and in Simple Contacts for now
@TargetApi(Build.VERSION_CODES.M)
fun Context.isDefaultDialer(): Boolean {
    return if (!packageName.startsWith("com.myapp.contacts") && !packageName.startsWith("com.myapp.dialer")) {
        true
    } else if ((packageName.startsWith("com.myapp.contacts") || packageName.startsWith("com.myapp.dialer")) && isQPlus()) {
        val roleManager = getSystemService(RoleManager::class.java)
        roleManager!!.isRoleAvailable(RoleManager.ROLE_DIALER) && roleManager.isRoleHeld(RoleManager.ROLE_DIALER)
    } else {
        isMarshmallowPlus() && telecomManager.defaultDialerPackage == packageName
    }
}

@TargetApi(Build.VERSION_CODES.N)
fun Context.getBlockedNumbers(): ArrayList<BlockedNumberModel> {
    val blockedNumbers = ArrayList<BlockedNumberModel>()
    if (!isNougatPlus() || !isDefaultDialer()) {
        return blockedNumbers
    }

    val uri = BlockedNumbers.CONTENT_URI
    val projection = arrayOf(
        BlockedNumbers.COLUMN_ID,
        BlockedNumbers.COLUMN_ORIGINAL_NUMBER,
        BlockedNumbers.COLUMN_E164_NUMBER
    )

    queryCursor(uri, projection) { cursor ->
        val id = cursor.getLongValue(BlockedNumbers.COLUMN_ID)
        val number = cursor.getStringValue(BlockedNumbers.COLUMN_ORIGINAL_NUMBER) ?: ""
        val normalizedNumber = cursor.getStringValue(BlockedNumbers.COLUMN_E164_NUMBER) ?: number
        val comparableNumber = normalizedNumber.trimToComparableNumber()
        val blockedNumber = BlockedNumberModel(id, number, normalizedNumber, comparableNumber)
        blockedNumbers.add(blockedNumber)
    }

    return blockedNumbers
}

@TargetApi(Build.VERSION_CODES.N)
fun Context.addBlockedNumber(number: String) {
    ContentValues().apply {
        put(BlockedNumbers.COLUMN_ORIGINAL_NUMBER, number)
        put(BlockedNumbers.COLUMN_E164_NUMBER, PhoneNumberUtils.normalizeNumber(number))
        try {
            contentResolver.insert(BlockedNumbers.CONTENT_URI, this)
        } catch (e: Exception) {
            showErrorToast(e)
        }
    }
}

@TargetApi(Build.VERSION_CODES.N)
fun Context.deleteBlockedNumber(number: String) {
    val selection = "${BlockedNumbers.COLUMN_ORIGINAL_NUMBER} = ?"
    val selectionArgs = arrayOf(number)
    contentResolver.delete(BlockedNumbers.CONTENT_URI, selection, selectionArgs)
}

fun Context.isNumberBlocked(number: String, blockedNumbers: ArrayList<BlockedNumberModel> = getBlockedNumbers()): Boolean {
    if (!isNougatPlus()) {
        return false
    }

    val numberToCompare = number.trimToComparableNumber()
    return blockedNumbers.map { it.numberToCompare }.contains(numberToCompare) || blockedNumbers.map { it.number }.contains(numberToCompare)
}

fun Context.copyToClipboard(text: String) {
    val clip = ClipData.newPlainText(getString(R.string.simple_commons), text)
    (getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager).setPrimaryClip(clip)
    toast(R.string.copied_to_clipboard)
}
