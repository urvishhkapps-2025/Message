package com.hkapps.messagepro.utils

import android.content.Context
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.Looper
import android.util.Log
import androidx.annotation.ChecksSdkIntAtLeast
import com.hkapps.messagepro.R
import com.hkapps.messagepro.model.RefreshEventsModel
import org.greenrobot.eventbus.EventBus
import java.util.*
import kotlin.collections.HashMap
import kotlin.collections.arrayListOf
import kotlin.collections.component1
import kotlin.collections.component2
import kotlin.collections.forEach
import kotlin.collections.hashMapOf
import kotlin.collections.set

const val isNotification = "isNotification"
const val THREAD_ID = "thread_id"
const val OTP = "OTP"
const val THREAD_TITLE = "thread_title"
const val THREAD_TEXT = "thread_text"
const val THREAD_NUMBER = "thread_number"
const val THREAD_ATTACHMENT_URI = "thread_attachment_uri"
const val THREAD_ATTACHMENT_URIS = "thread_attachment_uris"
const val SEARCHED_MESSAGE_ID = "searched_message_id"
const val USE_SIM_ID_PREFIX = "use_sim_id_"
const val NOTIFICATION_CHANNEL = "simple_sms_messenger"
const val SHOW_CHARACTER_COUNTER = "show_character_counter"
const val USE_SIMPLE_CHARACTERS = "use_simple_characters"
const val LOCK_SCREEN_VISIBILITY = "lock_screen_visibility"
const val ENABLE_DELIVERY_REPORTS = "enable_delivery_reports"
const val MMS_FILE_SIZE_LIMIT = "mms_file_size_limit"
const val PINNED_CONVERSATIONS = "pinned_conversations"
const val LAST_EXPORT_DATE = "last_export_date"
const val EXPORT_SMS = "export_sms"
const val EXPORT_MMS = "export_mms"
const val EXPORT_MIME_TYPE = "application/json"
const val EXPORT_FILE_EXT = ".json"
const val IMPORT_SMS = "import_sms"
const val IMPORT_MMS = "import_mms"

private const val PATH = "com.hkapps.messagepro.action."
const val MARK_AS_READ = PATH + "mark_as_read"
const val COPY_OTP = PATH + "copy_otp"
const val REPLY = PATH + "reply"

// view types for the thread list view
const val THREAD_DATE_TIME = 1
const val THREAD_RECEIVED_MESSAGE = 2
const val THREAD_SENT_MESSAGE = 3
const val THREAD_SENT_MESSAGE_ERROR = 4
const val THREAD_SENT_MESSAGE_SENT = 5
const val THREAD_SENT_MESSAGE_SENDING = 6

// lock screen visibility constants
const val LOCK_SCREEN_SENDER_MESSAGE = 1
const val LOCK_SCREEN_SENDER = 2
const val LOCK_SCREEN_NOTHING = 3

const val FILE_SIZE_NONE = -1L
const val FILE_SIZE_100_KB = 102_400L
const val FILE_SIZE_200_KB = 204_800L
const val FILE_SIZE_300_KB = 307_200L
const val FILE_SIZE_600_KB = 614_400L
const val FILE_SIZE_1_MB = 1_048_576L
const val FILE_SIZE_2_MB = 2_097_152L

fun refreshMessages() {
    Log.e("Event: ", "refreshMessages()")
    EventBus.getDefault().post(RefreshEventsModel.RefreshMessages())
}

const val EXTERNAL_STORAGE_PROVIDER_AUTHORITY = "com.android.externalstorage.documents"


const val APP_ICON_IDS = "app_icon_ids"
const val APP_ID = "app_id"
const val APP_LAUNCHER_NAME = "app_launcher_name"
const val BLOCKED_NUMBERS_EXPORT_DELIMITER = ","
const val BLOCKED_NUMBERS_EXPORT_EXTENSION = ".txt"
const val NOMEDIA = ".nomedia"
const val INVALID_NAVIGATION_BAR_COLOR = -1
const val SD_OTG_PATTERN = "^/storage/[A-Za-z0-9]{4}-[A-Za-z0-9]{4}$"
const val SD_OTG_SHORT = "^[A-Za-z0-9]{4}-[A-Za-z0-9]{4}$"
const val KEY_PHONE = "phone"
val DARK_GREY = 0xFF333333.toInt()
const val MEDIUM_ALPHA = 0.5f
const val HIGHER_ALPHA = 0.75f

const val HOUR_MINUTES = 60
const val DAY_MINUTES = 24 * HOUR_MINUTES
const val WEEK_MINUTES = DAY_MINUTES * 7
const val MONTH_MINUTES = DAY_MINUTES * 30
const val YEAR_MINUTES = DAY_MINUTES * 365

const val MINUTE_SECONDS = 60
const val HOUR_SECONDS = HOUR_MINUTES * 60
const val DAY_SECONDS = DAY_MINUTES * 60

// shared preferences
const val PREFS_KEY = "Prefs"
const val APP_RUN_COUNT = "app_run_count"
const val SD_TREE_URI = "tree_uri_2"
const val PRIMARY_ANDROID_DATA_TREE_URI = "primary_android_data_tree_uri_2"
const val OTG_ANDROID_DATA_TREE_URI = "otg_android_data_tree__uri_2"
const val SD_ANDROID_DATA_TREE_URI = "sd_android_data_tree_uri_2"
const val PRIMARY_ANDROID_OBB_TREE_URI = "primary_android_obb_tree_uri_2"
const val OTG_ANDROID_OBB_TREE_URI = "otg_android_obb_tree_uri_2"
const val SD_ANDROID_OBB_TREE_URI = "sd_android_obb_tree_uri_2"
const val OTG_TREE_URI = "otg_tree_uri_2"
const val SD_CARD_PATH = "sd_card_path_2"
const val OTG_REAL_PATH = "otg_real_path_2"
const val INTERNAL_STORAGE_PATH = "internal_storage_path"
const val TEXT_COLOR = "text_color"
const val BACKGROUND_COLOR = "background_color"
const val PRIMARY_COLOR = "primary_color_2"
const val ACCENT_COLOR = "accent_color"
const val APP_ICON_COLOR = "app_icon_color"
const val NAVIGATION_BAR_COLOR = "navigation_bar_color"
const val DEFAULT_NAVIGATION_BAR_COLOR = "default_navigation_bar_color"
const val CUREENT_THEME = "current_theme"
const val LAST_HANDLED_SHORTCUT_COLOR = "last_handled_shortcut_color"
const val LAST_ICON_COLOR = "last_icon_color"
const val PASSWORD_PROTECTION = "password_protection"
const val PASSWORD_HASH = "password_hash"
const val PROTECTION_TYPE = "protection_type"
const val PROTECTED_FOLDER_HASH = "protected_folder_hash_"
const val PROTECTED_FOLDER_TYPE = "protected_folder_type_"
const val KEEP_LAST_MODIFIED = "keep_last_modified"
const val USE_ENGLISH = "use_english"
const val WAS_USE_ENGLISH_TOGGLED = "was_use_english_toggled"
const val WAS_SHARED_THEME_EVER_ACTIVATED = "was_shared_theme_ever_activated"
const val IS_USING_SHARED_THEME = "is_using_shared_theme"
const val IS_USING_AUTO_THEME = "is_using_auto_theme"
const val WAS_SHARED_THEME_FORCED = "was_shared_theme_forced"
const val LAST_CONFLICT_RESOLUTION = "last_conflict_resolution"
const val LAST_CONFLICT_APPLY_TO_ALL = "last_conflict_apply_to_all"
const val ENABLE_PULL_TO_REFRESH = "enable_pull_to_refresh"
const val USE_24_HOUR_FORMAT = "use_24_hour_format"
const val OTG_PARTITION = "otg_partition_2"
const val IS_USING_MODIFIED_APP_ICON = "is_using_modified_app_icon"
const val WAS_ORANGE_ICON_CHECKED = "was_orange_icon_checked"
const val WAS_APP_ON_SD_SHOWN = "was_app_on_sd_shown"
const val APP_SIDELOADING_STATUS = "app_sideloading_status"
const val DATE_FORMAT = "date_format"
const val WAS_APP_RATED = "was_app_rated"
const val LAST_RENAME_PATTERN_USED = "last_rename_pattern_used"
const val LAST_EXPORTED_SETTINGS_FOLDER = "last_exported_settings_folder"
const val LAST_EXPORTED_SETTINGS_FILE = "last_exported_settings_file"
const val LAST_BLOCKED_NUMBERS_EXPORT_PATH = "last_blocked_numbers_export_path"
const val FONT_SIZE = "font_size"
const val THEME = "theme"
const val START_NAME_WITH_SURNAME = "start_name_with_surname"
const val FAVORITES = "favorites"


// global intents
const val OPEN_DOCUMENT_TREE_FOR_ANDROID_DATA_OR_OBB = 1000
const val OPEN_DOCUMENT_TREE_OTG = 1001
const val OPEN_DOCUMENT_TREE_SD = 1002
const val SELECT_EXPORT_SETTINGS_FILE_INTENT = 1004
const val REQUEST_CODE_SET_DEFAULT_DIALER = 1005

// sorting
const val SORT_BY_NAME = 1
const val SORT_BY_DATE_MODIFIED = 2
const val SORT_BY_SIZE = 4
const val SORT_BY_EXTENSION = 16
const val SORT_DESCENDING = 1024
const val SORT_USE_NUMERIC_VALUE = 32768

const val PROTECTION_NONE = -1
const val PROTECTION_PATTERN = 0
const val PROTECTION_PIN = 1
const val PROTECTION_FINGERPRINT = 2

const val SHOW_ALL_TABS = -1

// permissions
const val PERMISSION_READ_STORAGE = 1
const val PERMISSION_WRITE_STORAGE = 2
const val PERMISSION_CAMERA = 3
const val PERMISSION_RECORD_AUDIO = 4
const val PERMISSION_READ_CONTACTS = 5
const val PERMISSION_WRITE_CONTACTS = 6
const val PERMISSION_READ_CALENDAR = 7
const val PERMISSION_WRITE_CALENDAR = 8
const val PERMISSION_CALL_PHONE = 9
const val PERMISSION_READ_CALL_LOG = 10
const val PERMISSION_WRITE_CALL_LOG = 11
const val PERMISSION_GET_ACCOUNTS = 12
const val PERMISSION_READ_SMS = 13
const val PERMISSION_SEND_SMS = 14
const val PERMISSION_READ_PHONE_STATE = 15

// conflict resolving
const val CONFLICT_SKIP = 1
const val CONFLICT_OVERWRITE = 2
const val CONFLICT_MERGE = 3
const val CONFLICT_KEEP_BOTH = 4

// font sizes
const val FONT_SIZE_SMALL = 0
const val FONT_SIZE_MEDIUM = 1
const val FONT_SIZE_LARGE = 2
const val FONT_SIZE_EXTRA_LARGE = 3

const val SIDELOADING_UNCHECKED = 0
const val SIDELOADING_TRUE = 1
const val SIDELOADING_FALSE = 2

val photoExtensions: Array<String> get() = arrayOf(".jpg", ".png", ".jpeg", ".bmp", ".webp", ".heic", ".heif", ".apng")
val videoExtensions: Array<String> get() = arrayOf(".mp4", ".mkv", ".webm", ".avi", ".3gp", ".mov", ".m4v", ".3gpp")
val audioExtensions: Array<String> get() = arrayOf(".mp3", ".wav", ".wma", ".ogg", ".m4a", ".opus", ".flac", ".aac")
val rawExtensions: Array<String> get() = arrayOf(".dng", ".orf", ".nef", ".arw", ".rw2", ".cr2", ".cr3")

const val DATE_FORMAT_ONE = "dd.MM.yyyy"
const val DATE_FORMAT_TWO = "dd/MM/yyyy"
const val DATE_FORMAT_THREE = "MM/dd/yyyy"
const val DATE_FORMAT_FOUR = "yyyy-MM-dd"
const val DATE_FORMAT_FIVE = "d MMMM yyyy"
const val DATE_FORMAT_SIX = "MMMM d yyyy"
const val DATE_FORMAT_SEVEN = "MM-dd-yyyy"
const val DATE_FORMAT_EIGHT = "dd-MM-yyyy"
const val TIME_FORMAT_12 = "hh:mm a"
const val TIME_FORMAT_24 = "HH:mm"

// most app icon colors from md_app_icon_colors with reduced alpha
// used at showing contact placeholders without image
val letterBackgroundColors = arrayListOf(
    0xCCD32F2F,
    0xCCC2185B,
    0xCC1976D2,
    0xCC0288D1,
    0xCC0097A7,
    0xCC00796B,
    0xCC388E3C,
    0xCC689F38,
    0xCCF57C00,
    0xCCE64A19
)


fun isOnMainThread() = Looper.myLooper() == Looper.getMainLooper()

fun ensureBackgroundThread(callback: () -> Unit) {
    if (isOnMainThread()) {
        Thread {
            callback()
        }.start()
    } else {
        callback()
    }
}

@ChecksSdkIntAtLeast(api = Build.VERSION_CODES.M)
fun isMarshmallowPlus() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.M

@ChecksSdkIntAtLeast(api = Build.VERSION_CODES.N)
fun isNougatPlus() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.N

@ChecksSdkIntAtLeast(api = Build.VERSION_CODES.N_MR1)
fun isNougatMR1Plus() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1

@ChecksSdkIntAtLeast(api = Build.VERSION_CODES.O)
fun isOreoPlus() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O

@ChecksSdkIntAtLeast(api = Build.VERSION_CODES.Q)
fun isQPlus() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q

@ChecksSdkIntAtLeast(api = Build.VERSION_CODES.R)
fun isRPlus() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.R

val normalizeRegex = "\\p{InCombiningDiacriticalMarks}+".toRegex()

fun getConflictResolution(resolutions: LinkedHashMap<String, Int>, path: String): Int {
    return if (resolutions.size == 1 && resolutions.containsKey("")) {
        resolutions[""]!!
    } else if (resolutions.containsKey(path)) {
        resolutions[path]!!
    } else {
        CONFLICT_SKIP
    }
}

operator fun String.times(x: Int): String {
    val stringBuilder = StringBuilder()
    for (i in 1..x) {
        stringBuilder.append(this)
    }
    return stringBuilder.toString()
}

fun getQuestionMarks(size: Int) = ("?," * size).trimEnd(',')

fun getFilePlaceholderDrawables(context: Context): java.util.HashMap<String, Drawable> {
    val fileDrawables = HashMap<String, Drawable>()
    hashMapOf<String, Int>().apply {
        put("aep", R.drawable.icon_f_aep)
        put("ai", R.drawable.icon_f_ai)
        put("avi", R.drawable.icon_f_avi)
        put("css", R.drawable.icon_f_css)
        put("csv", R.drawable.icon_f_csv)
        put("dbf", R.drawable.icon_f_dbf)
        put("doc", R.drawable.icon_f_doc)
        put("docx", R.drawable.icon_f_doc)
        put("dwg", R.drawable.icon_f_dwg)
        put("exe", R.drawable.icon_f_exe)
        put("fla", R.drawable.icon_f_fla)
        put("flv", R.drawable.icon_f_flv)
        put("htm", R.drawable.icon_f_html)
        put("html", R.drawable.icon_f_html)
        put("ics", R.drawable.icon_f_ics)
        put("indd", R.drawable.icon_f_indd)
        put("iso", R.drawable.icon_f_iso)
        put("jpg", R.drawable.icon_f_jpg)
        put("jpeg", R.drawable.icon_f_jpg)
        put("js", R.drawable.icon_f_js)
        put("json", R.drawable.icon_f_json)
        put("m4a", R.drawable.icon_f_m4a)
        put("mp3", R.drawable.icon_f_mp3)
        put("mp4", R.drawable.icon_f_mp4)
        put("ogg", R.drawable.icon_f_ogg)
        put("pdf", R.drawable.icon_f_pdf)
        put("plproj", R.drawable.icon_f_plproj)
        put("prproj", R.drawable.icon_f_prproj)
        put("psd", R.drawable.icon_f_psd)
        put("rtf", R.drawable.icon_f_rtf)
        put("sesx", R.drawable.icon_f_sesx)
        put("sql", R.drawable.icon_f_sql)
        put("svg", R.drawable.icon_f_svg)
        put("txt", R.drawable.icon_f_txt)
        put("vcf", R.drawable.icon_f_vcf)
        put("wav", R.drawable.icon_f_wav)
        put("wmv", R.drawable.icon_f_wmv)
        put("xls", R.drawable.icon_f_xls)
        put("xml", R.drawable.icon_f_xml)
        put("zip", R.drawable.icon_f_zip)
    }.forEach { (key, value) ->
        fileDrawables[key] = context.resources.getDrawable(value)
    }
    return fileDrawables
}
