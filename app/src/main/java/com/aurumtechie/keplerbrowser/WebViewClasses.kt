package com.aurumtechie.keplerbrowser

import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import android.os.Message
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.ProgressBar
import androidx.core.view.NestedScrollingChild2
import androidx.core.view.NestedScrollingChildHelper
import androidx.core.view.ViewCompat

class NestedScrollWebView(context: Context, attrs: AttributeSet) : WebView(context, attrs),
    NestedScrollingChild2 {

    private var lastMotionY = 0
    private var nestedYOffset = 0
    private val scrollOffset = IntArray(2)
    private val scrollConsumed = IntArray(2)
    private val childHelper = NestedScrollingChildHelper(this)

    init {
        isNestedScrollingEnabled = true
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent?): Boolean {
        if (event == null) return false

        val motionEvent = MotionEvent.obtain(event)
        val currentY = event.y.toInt()

        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                nestedYOffset = 0
                lastMotionY = currentY
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    startNestedScroll(View.SCROLL_AXIS_VERTICAL)
                } else startNestedScroll(ViewCompat.SCROLL_AXIS_VERTICAL)
            }

            MotionEvent.ACTION_MOVE -> {
                var deltaY = lastMotionY - currentY

                if (dispatchNestedPreScroll(0, deltaY, scrollConsumed, scrollOffset)) {
                    deltaY -= scrollConsumed[1]
                    motionEvent.offsetLocation(0f, scrollOffset[1].toFloat())
                    nestedYOffset += scrollOffset[1]
                }

                lastMotionY = currentY - scrollOffset[1]

                val oldY = scrollY
                val newScrollY = 0.coerceAtLeast(oldY + deltaY)
                val dyConsumed = newScrollY - oldY
                val dyUnconsumed = deltaY - dyConsumed

                if (dispatchNestedScroll(0, dyConsumed, 0, dyUnconsumed, scrollOffset)) {
                    lastMotionY -= scrollOffset[1]
                    motionEvent.offsetLocation(0f, scrollOffset[1].toFloat())
                    nestedYOffset += scrollOffset[1]
                }

                motionEvent.recycle()
            }

            MotionEvent.ACTION_POINTER_DOWN,
            MotionEvent.ACTION_UP,
            MotionEvent.ACTION_CANCEL -> stopNestedScroll()

            else -> {
            }
        }

        return super.onTouchEvent(event)
    }

    override fun startNestedScroll(axes: Int, type: Int) = childHelper.startNestedScroll(axes, type)

    override fun stopNestedScroll(type: Int) = childHelper.stopNestedScroll(type)

    override fun hasNestedScrollingParent(type: Int) = childHelper.hasNestedScrollingParent(type)

    override fun dispatchNestedPreScroll(
        dx: Int, dy: Int,
        consumed: IntArray?,
        offsetInWindow: IntArray?,
        type: Int
    ) = childHelper.dispatchNestedPreScroll(dx, dy, consumed, offsetInWindow, type)

    override fun dispatchNestedScroll(
        dxConsumed: Int, dyConsumed: Int,
        dxUnconsumed: Int, dyUnconsumed: Int,
        offsetInWindow: IntArray?,
        type: Int
    ) = childHelper.dispatchNestedScroll(
        dxConsumed, dyConsumed,
        dxUnconsumed, dyUnconsumed,
        offsetInWindow,
        type
    )
}

object KeplerWebViewClient : WebViewClient() {
    override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
        view?.loadUrl(url)
        CookieManager.getInstance().setAcceptCookie(true)
        return true
    }
}

class KeplerWebChromeClient(private val progressBar: ProgressBar) : WebChromeClient() {
    override fun onProgressChanged(view: WebView?, newProgress: Int) {
        super.onProgressChanged(view, newProgress)

        progressBar.progress = newProgress
        if (newProgress < 100 && progressBar.visibility == ProgressBar.GONE)
            progressBar.visibility = ProgressBar.VISIBLE

        if (newProgress == 100)
            progressBar.visibility = ProgressBar.GONE
        else
            progressBar.visibility = ProgressBar.VISIBLE
    }

    //  TODO: Create a new window and open the link there
    override fun onCreateWindow(
        view: WebView?,
        isDialog: Boolean,
        isUserGesture: Boolean,
        resultMsg: Message?
    ): Boolean {
        if (!isUserGesture) return false
        return super.onCreateWindow(view, isDialog, isUserGesture, resultMsg)
    }
}