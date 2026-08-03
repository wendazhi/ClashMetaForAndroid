package com.github.kr328.clash.design.view

import android.content.Context
import android.content.res.Configuration
import android.util.AttributeSet
import android.view.KeyEvent
import android.view.ViewGroup
import androidx.annotation.AttrRes
import androidx.recyclerview.widget.RecyclerView

class AppRecyclerView @JvmOverloads constructor(
    context: Context,
    attributeSet: AttributeSet? = null,
    @AttrRes defStyleAttr: Int = 0
) : RecyclerView(context, attributeSet, defStyleAttr) {
    init {
        isFocusable = false
    }

    fun enableTvDocumentScrolling() {
        val television = resources.configuration.uiMode and Configuration.UI_MODE_TYPE_MASK ==
            Configuration.UI_MODE_TYPE_TELEVISION
        if (!television) return

        isFocusable = true
        descendantFocusability = ViewGroup.FOCUS_BEFORE_DESCENDANTS
        setOnKeyListener { _, keyCode, event ->
            if (event.action != KeyEvent.ACTION_DOWN) return@setOnKeyListener false

            val direction = when (keyCode) {
                KeyEvent.KEYCODE_DPAD_UP -> -1
                KeyEvent.KEYCODE_DPAD_DOWN -> 1
                else -> return@setOnKeyListener false
            }
            if (!canScrollVertically(direction)) return@setOnKeyListener false

            stopScroll()
            scrollBy(0, direction * maxOf(height * 2 / 3, 1))
            true
        }
    }
}
