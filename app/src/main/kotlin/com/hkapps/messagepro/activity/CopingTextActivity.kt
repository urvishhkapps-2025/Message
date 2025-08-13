package com.hkapps.messagepro.activity

import android.os.Bundle
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import com.hkapps.messagepro.MainAppClass
import com.hkapps.messagepro.R
import com.hkapps.messagepro.databinding.ActivityCopingTextBinding
import com.hkapps.messagepro.utils.THREAD_TITLE

class CopingTextActivity : BaseHomeActivity() {

    var appTopToolbar: Toolbar? = null

    private lateinit var binding: ActivityCopingTextBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCopingTextBinding.inflate(layoutInflater)
        setContentView(binding.root)

        appTopToolbar = findViewById(R.id.appTopToolbar)
        setSupportActionBar(appTopToolbar)
        getSupportActionBar()!!.setDisplayHomeAsUpEnabled(true)
        getSupportActionBar()!!.setDisplayShowHomeEnabled(true)
        appTopToolbar?.setNavigationIcon(ContextCompat.getDrawable(this, R.drawable.icon_back))

        MainAppClass.instance!!.loadBanner(binding.adViewBanner, this)

        val msgBody = intent.getStringExtra(THREAD_TITLE)
        binding.tvSelectMessageBody.setText(msgBody)
        binding.tvSelectMessageBody.setLinkTextColor(resources.getColor(R.color.text_link))

    }
}
