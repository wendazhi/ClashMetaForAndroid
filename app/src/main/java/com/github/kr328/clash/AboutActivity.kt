package com.github.kr328.clash

import com.github.kr328.clash.design.AboutDesign
import kotlinx.coroutines.awaitCancellation

class AboutActivity : BaseActivity<AboutDesign>() {
    override suspend fun main() {
        setContentDesign(AboutDesign(this, BuildConfig.VERSION_NAME))
        awaitCancellation()
    }
}
