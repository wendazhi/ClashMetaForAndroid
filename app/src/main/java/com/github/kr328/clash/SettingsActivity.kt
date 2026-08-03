package com.github.kr328.clash

import android.content.pm.PackageManager
import android.content.res.Configuration
import com.github.kr328.clash.common.util.componentName
import com.github.kr328.clash.core.Clash
import com.github.kr328.clash.design.*
import com.github.kr328.clash.design.model.Behavior
import com.github.kr328.clash.design.store.UiStore.Companion.mainActivityAlias
import com.github.kr328.clash.service.store.ServiceStore
import com.github.kr328.clash.util.ApplicationObserver
import com.github.kr328.clash.util.withClash
import kotlinx.coroutines.isActive
import kotlinx.coroutines.selects.select

class SettingsActivity : BaseActivity<Design<*>>(), Behavior {
    override suspend fun main() {
        if (resources.configuration.uiMode and Configuration.UI_MODE_TYPE_MASK == Configuration.UI_MODE_TYPE_TELEVISION) {
            return mainTelevision()
        }

        val design = SettingsDesign(this)

        setContentDesign(design)

        while (isActive) {
            select<Unit> {
                events.onReceive {

                }
                design.requests.onReceive {
                    when (it) {
                        SettingsDesign.Request.StartApp -> startActivity(android.content.Intent(this@SettingsActivity, AppSettingsActivity::class.java))
                        SettingsDesign.Request.StartNetwork -> startActivity(android.content.Intent(this@SettingsActivity, NetworkSettingsActivity::class.java))
                        SettingsDesign.Request.StartOverride -> startActivity(android.content.Intent(this@SettingsActivity, OverrideSettingsActivity::class.java))
                        SettingsDesign.Request.StartMetaFeature -> startActivity(android.content.Intent(this@SettingsActivity, MetaFeatureSettingsActivity::class.java))
                    }
                }
            }
        }
    }

    private suspend fun mainTelevision() {
        val service = ServiceStore(this)
        val configuration = withClash { queryOverride(Clash.OverrideSlot.Persist) }
        defer { withClash { patchOverride(Clash.OverrideSlot.Persist, configuration) } }

        val app = AppSettingsDesign(this, uiStore, service, this, clashRunning, ::onHideIconChange)
        val network = NetworkSettingsDesign(this, uiStore, service, clashRunning)
        val override = OverrideSettingsDesign(this, configuration)
        val meta = MetaFeatureSettingsDesign(this, configuration)
        val flat = FlatSettingsDesign(
            this,
            listOf(
                com.github.kr328.clash.design.R.string.app to app,
                com.github.kr328.clash.design.R.string.network to network,
                com.github.kr328.clash.design.R.string.override to override,
                com.github.kr328.clash.design.R.string.meta_features to meta,
            ),
        )
        setContentDesign(flat)

        while (isActive) {
            select<Unit> {
                events.onReceive {
                    when (it) {
                        Event.ClashStart, Event.ClashStop, Event.ServiceRecreated -> recreate()
                        else -> Unit
                    }
                }
                app.requests.onReceive {
                    ApplicationObserver.createdActivities.forEach { activity -> activity.recreate() }
                }
                network.requests.onReceive { /* Access-control mode remains inline; no child Activity on TV. */ }
                override.requests.onReceive { /* Reset is intentionally not a navigation action on TV. */ }
                meta.requests.onReceive { /* File pickers are handled by the legacy phone screen only. */ }
            }
        }
    }

    override var autoRestart: Boolean
        get() = packageManager.getComponentEnabledSetting(RestartReceiver::class.componentName) ==
            PackageManager.COMPONENT_ENABLED_STATE_ENABLED
        set(value) {
            packageManager.setComponentEnabledSetting(
                RestartReceiver::class.componentName,
                if (value) PackageManager.COMPONENT_ENABLED_STATE_ENABLED else PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                PackageManager.DONT_KILL_APP,
            )
        }

    private fun onHideIconChange(hide: Boolean) {
        packageManager.setComponentEnabledSetting(
            mainActivityAlias,
            if (hide) PackageManager.COMPONENT_ENABLED_STATE_DISABLED else PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
            PackageManager.DONT_KILL_APP,
        )
    }
}
