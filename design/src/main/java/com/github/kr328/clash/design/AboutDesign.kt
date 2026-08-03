package com.github.kr328.clash.design

import android.content.Context
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import com.github.kr328.clash.design.databinding.DesignAboutBinding
import com.github.kr328.clash.design.util.layoutInflater

class AboutDesign(context: Context, versionName: String) : Design<Unit>(context) {
    override val root: View = LinearLayout(context).apply {
        orientation = LinearLayout.VERTICAL
        gravity = Gravity.CENTER
        addView(DesignAboutBinding.inflate(context.layoutInflater).apply {
            this.versionName = versionName
        }.root)
    }
}
