package com.hkapps.messagepro.fragment

import android.content.Intent
import android.os.Bundle
import android.text.Html
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentStatePagerAdapter
import androidx.viewpager.widget.ViewPager
import com.hkapps.messagepro.R
import com.hkapps.messagepro.activity.ActivityPermissions
import com.hkapps.messagepro.utils.Utility


class IntroductionFragment : BaseFragment(), ViewPager.OnPageChangeListener {

    var currentFragment: Fragment? = null
    private var viewPager: ViewPager? = null
    private var dotsLayout: LinearLayout? = null
    private var btnNext: TextView? = null
    private var mCurrentItem = 0
    var fragments: MutableList<Fragment> = ArrayList()
    private var screenSlidePagerRepeater: ScreenSlidePagerRepeater? = null


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        return inflater.inflate(R.layout.fragment_introduction, container, false)
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewPager = view.findViewById(R.id.view_pager)
        viewPager?.offscreenPageLimit = 3
        dotsLayout = view.findViewById(R.id.layoutDots)
        btnNext = view.findViewById(R.id.btn_next)
        btnNext?.setOnClickListener { v: View? ->
            if (btnNext?.text.toString()
                    .equals(getString(R.string.txt_start), ignoreCase = true)
            ) {
                Utility.setIsIntroShow(true)
                startActivity(Intent(mActivity, ActivityPermissions::class.java))
                mActivity?.finish()
            } else {
                if (mCurrentItem < screenSlidePagerRepeater!!.count - 1) {
                    ++mCurrentItem
                    viewPager?.currentItem = mCurrentItem
                }
            }
        }
        fragments.add(Introduction1())
        fragments.add(Introduction2())
        fragments.add(Introduction3())
        setupViewPager()
        addDotsIndicator(0)


    }

    private fun setupViewPager() {
        screenSlidePagerRepeater = ScreenSlidePagerRepeater(fragments, childFragmentManager)
        viewPager!!.adapter = screenSlidePagerRepeater
        viewPager!!.addOnPageChangeListener(this)
    }

    fun addDotsIndicator(position: Int) {
        val mDots = arrayOfNulls<TextView>(screenSlidePagerRepeater!!.count)
        dotsLayout!!.removeAllViews()
        for (i in mDots.indices) {
            mDots[i] = TextView(mActivity)
            mDots[i]!!.text = Html.fromHtml("•")
            mDots[i]!!.textSize = 35f
            mDots[i]!!.setTextColor(
                ContextCompat.getColor(
                    mActivity!!,
                    R.color.switch_background
                )
            )
            dotsLayout!!.addView(mDots[i])
        }
        if (mDots.size > 0) {
            mDots[position]!!.setTextColor(
                ContextCompat.getColor(
                    mActivity!!,
                    R.color.only_blue
                )
            )
        }
    }

    override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {}
    override fun onPageSelected(position: Int) {
        mCurrentItem = position
        addDotsIndicator(position)
        currentFragment = screenSlidePagerRepeater!!.getItem(position)
        if (currentFragment is Introduction1) {
            if (mCurrentItem == 0) (currentFragment as Introduction1).doAnimTitleSubTitle()
        } else if (currentFragment is Introduction2) {
            if (mCurrentItem == 1) (currentFragment as Introduction2).doAnimTitleSubTitle()
        } else if (currentFragment is Introduction3) {
            if (mCurrentItem == 2) (currentFragment as Introduction3).doAnimTitleSubTitle()
        }
        if (position == screenSlidePagerRepeater!!.count - 1) {
            btnNext!!.text = getString(R.string.txt_start)
        } else {
            btnNext!!.text = getString(R.string.txt_next)
        }
    }

    override fun onPageScrollStateChanged(state: Int) {}

    private inner class ScreenSlidePagerRepeater(
        var mFragments: List<Fragment>,
        fragmentManager: FragmentManager?
    ) : FragmentStatePagerAdapter(
        fragmentManager!!
    ) {
        override fun getCount(): Int {
            return mFragments.size
        }

        override fun getItem(i: Int): Fragment {
            return mFragments[i]
        }
    }

}
