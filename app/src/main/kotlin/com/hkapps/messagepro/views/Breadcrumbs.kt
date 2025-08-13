package com.hkapps.messagepro.views

import android.content.Context
import android.util.AttributeSet
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import com.hkapps.messagepro.R
import com.hkapps.messagepro.extensions.getBasePath
import com.hkapps.messagepro.extensions.humanizePath
import com.hkapps.messagepro.extensions.onGlobalLayout
import com.hkapps.messagepro.model.FileDIRModel

class Breadcrumbs(context: Context, attrs: AttributeSet) : HorizontalScrollView(context, attrs) {
    private val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
    private val itemsLayout: LinearLayout
    private var fontSize = resources.getDimension(R.dimen.textsize_16)
    private var lastPath = ""
    private var isLayoutDirty = true
    private var isScrollToSelectedItemPending = false
    private var isFirstScroll = true
    private var stickyRootInitialLeft = 0
    private var rootStartPadding = 0


    var listener: BreadcrumbsListener? = null
    val itemsCount: Int
        get() = itemsLayout.childCount

    init {
        isHorizontalScrollBarEnabled = false
        itemsLayout = LinearLayout(context)
        itemsLayout.orientation = LinearLayout.HORIZONTAL
        rootStartPadding = paddingStart
        itemsLayout.setPaddingRelative(0, paddingTop, paddingEnd, paddingBottom)
        setPaddingRelative(0, 0, 0, 0)
        addView(itemsLayout, LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.MATCH_PARENT))
        onGlobalLayout {
            stickyRootInitialLeft = if (itemsLayout.childCount > 0) {
                itemsLayout.getChildAt(0).left
            } else {
                0
            }
        }
    }

    private fun recomputeStickyRootLocation(left: Int) {
        stickyRootInitialLeft = left
        handleRootStickiness(scrollX)
    }

    private fun handleRootStickiness(scrollX: Int) {
        if (scrollX > stickyRootInitialLeft) {
            stickRoot(scrollX - stickyRootInitialLeft)
        } else {
            freeRoot()
        }
    }

    private fun freeRoot() {
        if (itemsLayout.childCount > 0) {
            itemsLayout.getChildAt(0).translationX = 0f
        }
    }

    private fun stickRoot(translationX: Int) {
        if (itemsLayout.childCount > 0) {
            val root = itemsLayout.getChildAt(0)
            root.translationX = translationX.toFloat()
            ViewCompat.setTranslationZ(root, translationZ)
        }
    }

    override fun onScrollChanged(scrollX: Int, scrollY: Int, oldScrollX: Int, oldScrollY: Int) {
        super.onScrollChanged(scrollX, scrollY, oldScrollX, oldScrollY)
        handleRootStickiness(scrollX)
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)

        isLayoutDirty = false
        if (isScrollToSelectedItemPending) {
            scrollToSelectedItem()
            isScrollToSelectedItemPending = false
        }

        recomputeStickyRootLocation(left)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val heightMode = MeasureSpec.getMode(heightMeasureSpec)
        var heightMeasureSpec = heightMeasureSpec
        if (heightMode == MeasureSpec.UNSPECIFIED || heightMode == MeasureSpec.AT_MOST) {
            var height = context.resources.getDimensionPixelSize(R.dimen.margin_48)
            if (heightMode == MeasureSpec.AT_MOST) {
                height = height.coerceAtMost(MeasureSpec.getSize(heightMeasureSpec))
            }
            heightMeasureSpec = MeasureSpec.makeMeasureSpec(height, MeasureSpec.EXACTLY)
        }
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
    }

    private fun scrollToSelectedItem() {
        if (isLayoutDirty) {
            isScrollToSelectedItemPending = true
            return
        }

        var selectedIndex = itemsLayout.childCount - 1
        val cnt = itemsLayout.childCount
        for (i in 0 until cnt) {
            val child = itemsLayout.getChildAt(i)
            if ((child.tag as? FileDIRModel)?.path?.trimEnd('/') == lastPath.trimEnd('/')) {
                selectedIndex = i
                break
            }
        }

        val selectedItemView = itemsLayout.getChildAt(selectedIndex)
        val scrollX = if (layoutDirection == View.LAYOUT_DIRECTION_LTR) {
            selectedItemView.left - itemsLayout.paddingStart
        } else {
            selectedItemView.right - width + itemsLayout.paddingStart
        }

        if (!isFirstScroll && isShown) {
            smoothScrollTo(scrollX, 0)
        } else {
            scrollTo(scrollX, 0)
        }

        isFirstScroll = false
    }

    override fun requestLayout() {
        isLayoutDirty = true
        super.requestLayout()
    }

    fun setBreadcrumb(fullPath: String) {
        lastPath = fullPath
        val basePath = fullPath.getBasePath(context)
        var currPath = basePath
        val tempPath = context.humanizePath(fullPath)

        itemsLayout.removeAllViews()
        val dirs = tempPath.split("/").dropLastWhile(String::isEmpty)
        for (i in dirs.indices) {
            val dir = dirs[i]
            if (i > 0) {
                currPath += dir + "/"
            }

            if (dir.isEmpty()) {
                continue
            }

            currPath = "${currPath.trimEnd('/')}/"
            val item = FileDIRModel(currPath, dir, true, 0, 0, 0)
            addBreadcrumb(item, i, i > 0)
            scrollToSelectedItem()
        }
    }

    private fun addBreadcrumb(item: FileDIRModel, index: Int, addPrefix: Boolean) {
        if (itemsLayout.childCount == 0) {
            inflater.inflate(R.layout.row_breadcrumb_first, itemsLayout, false).apply {
                resources.apply {
                    findViewById<TextView>(R.id.breadcrumb_text).background = ContextCompat.getDrawable(context, R.drawable.apk_background_button_9)
//                    breadcrumb_text.background.applyColorFilter(textColor)
                    elevation = 1f
//                    background = ColorDrawable(resources.getColor(R.color.dialog_background))
                    val medium = getDimension(R.dimen.margin_8).toInt()
                    findViewById<TextView>(R.id.breadcrumb_text).setPadding(medium, medium, medium, medium)
                    setPadding(rootStartPadding, 0, 0, 0)
                }

                isActivated = item.path.trimEnd('/') == lastPath.trimEnd('/')
                findViewById<TextView>(R.id.breadcrumb_text).text = item.name
//                breadcrumb_text.setTextColor(textColorStateList)
                findViewById<TextView>(R.id.breadcrumb_text).setTextSize(TypedValue.COMPLEX_UNIT_PX, fontSize)

                itemsLayout.addView(this)

                findViewById<TextView>(R.id.breadcrumb_text).setOnClickListener {
                    if (itemsLayout.getChildAt(index) != null) {
                        listener?.breadcrumbClicked(index)
                    }
                }

                tag = item
            }
        } else {
            inflater.inflate(R.layout.row_breadcrumb, itemsLayout, false).apply {
                var textToAdd = item.name
                if (addPrefix) {
                    textToAdd = "> $textToAdd"
                }

                isActivated = item.path.trimEnd('/') == lastPath.trimEnd('/')

                findViewById<TextView>(R.id.breadcrumb_text).text = textToAdd
//                breadcrumb_text.setTextColor(textColorStateList)
                findViewById<TextView>(R.id.breadcrumb_text).setTextSize(TypedValue.COMPLEX_UNIT_PX, fontSize)

                itemsLayout.addView(this)

                setOnClickListener { v ->
                    if (itemsLayout.getChildAt(index) != null && itemsLayout.getChildAt(index) == v) {
                        if ((v.tag as? FileDIRModel)?.path?.trimEnd('/') == lastPath.trimEnd('/')) {
                            scrollToSelectedItem()
                        } else {
                            listener?.breadcrumbClicked(index)
                        }
                    }
                }

                tag = item
            }
        }
    }


    fun updateFontSize(size: Float) {
        fontSize = size
        setBreadcrumb(lastPath)
    }

    fun removeBreadcrumb() {
        itemsLayout.removeView(itemsLayout.getChildAt(itemsLayout.childCount - 1))
    }

    fun getItem(index: Int) = itemsLayout.getChildAt(index).tag as FileDIRModel

    fun getLastItem() = itemsLayout.getChildAt(itemsLayout.childCount - 1).tag as FileDIRModel

    interface BreadcrumbsListener {
        fun breadcrumbClicked(id: Int)
    }
}
