package com.hkapps.messagepro.fragment

import android.app.Activity
import android.app.Dialog
import android.app.role.RoleManager
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.os.Bundle
import android.provider.Telephony
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.TextView
import com.hkapps.messagepro.R
import com.hkapps.messagepro.activity.ActivityNotificationPermissions
import com.hkapps.messagepro.activity.HomeActivity
import com.hkapps.messagepro.utils.*


class PermissionFragment : BaseFragment() {

    private val MAKE_DEFAULT_APP_REQUEST = 1
    var textView: TextView? = null
    var textView1: TextView? = null
    var tvSetAsDefault: TextView? = null
    var anim_out: Animation? = null
    var anim_in: Animation? = null


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        return inflater.inflate(R.layout.fragment_permission, container, false)
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)


        tvSetAsDefault = view.findViewById(R.id.tvSetAsDefault)
        textView = view.findViewById(R.id.tvHead)
        textView1 = view.findViewById(R.id.tvStr)
        textView?.text = getString(R.string.text_header_default_app)
        textView1?.text = getString(R.string.text_intro_sub_title3)

        tvSetAsDefault?.setOnClickListener {
            getPermission()
        }

        doAnimTitleSubTitle()
    }

    fun doAnimTitleSubTitle() {
        textView!!.clearAnimation()
        textView1!!.clearAnimation()
        tvSetAsDefault!!.clearAnimation()

        anim_out = AnimationUtils.loadAnimation(requireContext(), android.R.anim.fade_in)
        anim_out?.repeatCount = Animation.ABSOLUTE
        anim_out?.duration = 1500

        textView!!.animation = anim_out
        textView1!!.animation = anim_out
        tvSetAsDefault!!.animation = anim_out

        anim_in = AnimationUtils.loadAnimation(requireContext(), android.R.anim.fade_out)
        anim_in?.repeatCount = Animation.ABSOLUTE
        anim_in?.duration = 1500
        anim_in?.setAnimationListener(object : Animation.AnimationListener {
            override fun onAnimationStart(animation: Animation) {}
            override fun onAnimationEnd(animation: Animation) {
                textView!!.startAnimation(anim_out)
                textView1!!.startAnimation(anim_out)
                tvSetAsDefault!!.startAnimation(anim_out)

            }

            override fun onAnimationRepeat(animation: Animation) {}
        })
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
                mActivity?.finish()
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


    // while SEND_SMS and READ_SMS permissions are mandatory, READ_CONTACTS is optional. If we don't have it, we just won't be able to show the contact name in some cases
    private fun askPermissions() {
        handlePermission(PERMISSION_READ_SMS) {
            if (it) {
                handlePermission(PERMISSION_SEND_SMS) {
                    if (it) {
                        handlePermission(PERMISSION_READ_CONTACTS) {
                            checkNotificationPermission()
                        }
                    } else {
                        showPermissionDialog()
                    }
                }
            } else {
                showPermissionDialog()
            }
        }
    }

    private fun checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            gotoNotificationScreen()
        } else {
            gotoHome()
        }
    }

    private fun gotoNotificationScreen() {
        startActivity(Intent(mActivity, ActivityNotificationPermissions::class.java))
        mActivity?.finish()
    }

    private fun gotoHome() {
        startActivity(Intent(mActivity, HomeActivity::class.java))
        mActivity?.finish()
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, resultData: Intent?) {
        super.onActivityResult(requestCode, resultCode, resultData)
        if (requestCode == MAKE_DEFAULT_APP_REQUEST) {
            if (resultCode == Activity.RESULT_OK) {
                Log.e("Event: ", "askPermissions RESULT_OK")
                askPermissions()
            } else {
                showPermissionDialog()
            }
        }
    }

    private fun showPermissionDialog() {
        val mDialog = Dialog(mActivity!!)
        mDialog.setContentView(R.layout.dialog_permission)
        mDialog.setCanceledOnTouchOutside(false)
        mDialog.setCancelable(false)
        mDialog.window!!.setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.WRAP_CONTENT)
        mDialog.window!!.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        mDialog.window!!.attributes.windowAnimations = android.R.style.Animation_Dialog
        mDialog.show()

        (mDialog.findViewById<View>(R.id.tvSetAsDefault) as TextView).setOnClickListener {
            if (!mActivity?.isFinishing!! && mDialog.isShowing) {
                mDialog.dismiss()
            }
            getPermission()
        }
    }


}
