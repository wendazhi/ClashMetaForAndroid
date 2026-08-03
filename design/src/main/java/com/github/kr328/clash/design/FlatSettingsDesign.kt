package com.github.kr328.clash.design

import android.content.Context
import android.graphics.Typeface
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import com.github.kr328.clash.design.view.ObservableScrollView
import com.github.kr328.clash.design.util.resolveThemedColor

/** TV-only host that presents the existing preference screens as one document. */
class FlatSettingsDesign(
    context: Context,
    sections: List<Pair<Int, Design<*>>>,
) : Design<Unit>(context) {
    private val container = LinearLayout(context).apply {
        orientation = LinearLayout.VERTICAL
        val horizontal = context.resources.getDimensionPixelSize(R.dimen.item_tailing_margin)
        val vertical = context.resources.getDimensionPixelSize(R.dimen.item_padding_vertical)
        setPadding(horizontal, vertical, horizontal, vertical * 3)
        descendantFocusability = ViewGroup.FOCUS_AFTER_DESCENDANTS
    }

    override val root: View = FrameLayout(context).apply {
        addView(ObservableScrollView(context).apply {
            id = R.id.scroll_root
            isFillViewport = true
            clipToPadding = false
            overScrollMode = View.OVER_SCROLL_NEVER
            addView(container, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        }, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
    }

    init {
        sections.forEachIndexed { index, (title, design) ->
            container.addView(TextView(context).apply {
                text = context.getText(title)
                setTextColor(context.resolveThemedColor(com.google.android.material.R.attr.colorPrimary))
                textSize = 18f
                setTypeface(typeface, Typeface.BOLD)
                val top = if (index == 0) 12 else 32
                setPadding(16, top, 16, 12)
                isFocusable = false
            })

            val content = design.root.findViewById<ViewGroup>(R.id.content)
            val section = content?.getChildAt(0)
                ?: error("Flat settings section has no preference content")
            content.removeView(section)
            container.addView(section, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        }
    }
}
