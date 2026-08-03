package com.github.kr328.clash.design.view

import android.content.Context
import android.graphics.Rect
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import androidx.recyclerview.widget.RecyclerView
import com.github.kr328.clash.design.R
import com.github.kr328.clash.design.databinding.ComponentTvActivityShellBinding
import com.github.kr328.clash.design.util.layoutInflater

class TvActivityShell(context: Context) : LinearLayout(context) {
    private val binding = ComponentTvActivityShellBinding
        .inflate(context.layoutInflater, this, true)

    val navigationBar: TvNavigationBar
        get() = binding.navigationBar

    private val focusSafeMargin = resources.getDimensionPixelSize(R.dimen.tv_focus_safe_margin)
    private val focusChangedListener = ViewTreeObserver.OnGlobalFocusChangeListener { _, focused ->
        if (focused != null && focused.isDescendantOf(binding.contentContainer)) {
            focused.post { keepFocusVisible(focused) }
        }
    }

    init {
        orientation = VERTICAL
        clipChildren = false
    }

    fun setContent(view: View) {
        binding.contentContainer.removeAllViews()
        binding.contentContainer.addView(
            view,
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT,
            ),
        )
    }

    fun focusFirstContent() {
        binding.contentContainer.post {
            val contentFocus = binding.contentContainer.firstFocusableDescendant(skipActivityBar = true)
            val toolbarFocus = binding.contentContainer.firstFocusableDescendant(skipActivityBar = false)
            (contentFocus ?: toolbarFocus)?.requestFocus()
        }
    }

    fun bindNavigationDown(tab: TvNavigationBar.Tab) {
        val contentFocus = binding.contentContainer.firstFocusableDescendant(skipActivityBar = true)
            ?: return
        if (contentFocus.id == View.NO_ID) contentFocus.id = View.generateViewId()
        navigationBar.setNextFocusDown(tab, contentFocus.id)
    }

    fun isFirstContentFocus(focused: View): Boolean {
        val recycler = focused.recyclerAncestor()
        if (recycler != null && focused !== recycler) {
            return recycler.findContainingViewHolder(focused)?.bindingAdapterPosition == 0
        }
        if (focused is RecyclerView) return !focused.canScrollVertically(-1)

        return binding.contentContainer.firstFocusableDescendant(skipActivityBar = true) === focused
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        viewTreeObserver.addOnGlobalFocusChangeListener(focusChangedListener)
    }

    override fun onDetachedFromWindow() {
        if (viewTreeObserver.isAlive) {
            viewTreeObserver.removeOnGlobalFocusChangeListener(focusChangedListener)
        }
        super.onDetachedFromWindow()
    }

    private fun keepFocusVisible(focused: View) {
        val scroller = focused.scrollableAncestor() ?: return
        val activityBar = binding.contentContainer.findViewById<ViewGroup>(R.id.activity_bar_layout)
        if (activityBar != null && focused.isDescendantOf(activityBar)) return

        val containerLocation = IntArray(2)
        binding.contentContainer.getLocationOnScreen(containerLocation)
        var safeTop = containerLocation[1] + focusSafeMargin
        if (activityBar?.isShown == true) {
            val toolbarLocation = IntArray(2)
            activityBar.getLocationOnScreen(toolbarLocation)
            safeTop = maxOf(safeTop, toolbarLocation[1] + activityBar.height + focusSafeMargin)
        }
        val safeBottom = containerLocation[1] + binding.contentContainer.height - focusSafeMargin

        val focusedLocation = IntArray(2)
        focused.getLocationOnScreen(focusedLocation)
        val scaleOutset = maxOf(
            ((focused.width * focused.scaleX - focused.width) / 2f).toInt(),
            ((focused.height * focused.scaleY - focused.height) / 2f).toInt(),
        )
        val bounds = Rect(
            focusedLocation[0] - scaleOutset,
            focusedLocation[1] - scaleOutset,
            focusedLocation[0] + focused.width + scaleOutset,
            focusedLocation[1] + focused.height + scaleOutset,
        )
        val focusedCenter = bounds.centerY()
        val contentCenter = (safeTop + safeBottom) / 2
        val delta = when {
            scroller is ScrollView && focusedCenter < contentCenter && scroller.scrollY > 0 ->
                (focusedCenter - contentCenter).coerceAtLeast(-scroller.scrollY)
            bounds.top < safeTop -> bounds.top - safeTop
            bounds.bottom > safeBottom -> bounds.bottom - safeBottom
            else -> 0
        }
        if (delta == 0) return

        when (scroller) {
            is ScrollView -> scroller.smoothScrollBy(0, delta)
            is RecyclerView -> scroller.scrollBy(0, delta)
        }
    }

    private fun View.scrollableAncestor(): View? {
        var current = parent as? View
        while (current != null && current !== binding.contentContainer) {
            if (current is ScrollView || current is RecyclerView) return current
            current = current.parent as? View
        }
        return null
    }

    private fun View.recyclerAncestor(): RecyclerView? {
        var current = parent as? View
        while (current != null && current !== binding.contentContainer) {
            if (current is RecyclerView) return current
            current = current.parent as? View
        }
        return null
    }

    private fun View.isDescendantOf(parent: ViewGroup): Boolean {
        var current: View? = this
        while (current != null) {
            if (current === parent) return true
            current = current.parent as? View
        }
        return false
    }

    private fun View.firstFocusableDescendant(skipActivityBar: Boolean): View? {
        if (visibility != View.VISIBLE || !isEnabled) return null
        if (skipActivityBar && this is ActivityBarLayout) return null
        if (this is ScrollView) {
            for (index in 0 until childCount) {
                getChildAt(index).firstFocusableDescendant(skipActivityBar)?.let { return it }
            }
        }
        if (isFocusable) return this
        if (this !is ViewGroup) return null

        for (index in 0 until childCount) {
            getChildAt(index).firstFocusableDescendant(skipActivityBar)?.let { return it }
        }
        return null
    }
}
