package com.hkapps.messagepro.activity

import android.os.Bundle
import com.hkapps.messagepro.MainAppClass
import com.hkapps.messagepro.R
import com.hkapps.messagepro.databinding.ActivitySelectTextBinding
import com.hkapps.messagepro.dialogs.AlertDialogCustom
import com.hkapps.messagepro.extensions.copyToClipboard
import com.hkapps.messagepro.extensions.shareTextIntent
import com.hkapps.messagepro.utils.THREAD_TITLE


class SelectTextActivity : BaseHomeActivity() {

    private lateinit var binding: ActivitySelectTextBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySelectTextBinding.inflate(layoutInflater)
        setContentView(binding.root)
        MainAppClass.instance!!.loadBanner(binding.adViewBanner, this)

        val msgBody = intent.getStringExtra(THREAD_TITLE)
        binding.tvSelectMessageBody.text = msgBody
        binding.tvSelectMessageBody.setLinkTextColor(resources.getColor(R.color.text_link))


        val bitmapLocal = bitmapFromResourceApp(
            resources, R.drawable.icon_banner_textop, 500, 500
        )
        binding.ivBanner!!.setImageBitmap(bitmapLocal)



        binding.ivBack.setOnClickListener {
            onBackPressed()
        }

        binding.llCopy.setOnClickListener {
            copyToClipboard(msgBody!!)
        }

        binding.llShare.setOnClickListener {
            shareTextIntent(msgBody!!)
        }

        binding.llDelete.setOnClickListener {
            askConfirmDelete()
        }


    }


    private fun askConfirmDelete() {

        AlertDialogCustom(this) {
            setResult(RESULT_OK)
            finish()
        }
    }
}
