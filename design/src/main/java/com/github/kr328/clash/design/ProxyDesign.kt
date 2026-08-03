package com.github.kr328.clash.design

import android.content.Context
import android.content.res.Configuration
import android.content.res.ColorStateList
import android.view.View
import android.widget.Toast
import android.widget.TextView
import android.view.ViewGroup
import androidx.viewpager2.widget.ViewPager2
import com.github.kr328.clash.core.model.Proxy
import com.github.kr328.clash.core.model.TunnelState
import com.github.kr328.clash.design.adapter.ProxyAdapter
import com.github.kr328.clash.design.adapter.ProxyPageAdapter
import com.github.kr328.clash.design.component.ProxyMenu
import com.github.kr328.clash.design.component.ProxyViewConfig
import com.github.kr328.clash.design.databinding.DesignProxyBinding
import com.github.kr328.clash.design.model.ProxyState
import com.github.kr328.clash.design.store.UiStore
import com.github.kr328.clash.design.util.applyFrom
import com.github.kr328.clash.design.util.layoutInflater
import com.github.kr328.clash.design.util.resolveThemedColor
import com.github.kr328.clash.design.util.root
import com.google.android.material.tabs.TabLayoutMediator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ProxyDesign(
    context: Context,
    overrideMode: TunnelState.Mode?,
    groupNames: List<String>,
    uiStore: UiStore,
) : Design<ProxyDesign.Request>(context) {
    sealed class Request {
        object ReloadAll : Request()
        object ReLaunch : Request()

        data class PatchMode(val mode: TunnelState.Mode?) : Request()
        data class Reload(val index: Int) : Request()
        data class Select(val index: Int, val name: String) : Request()
        data class UrlTest(val index: Int) : Request()
    }

    private val binding = DesignProxyBinding
        .inflate(context.layoutInflater, context.root, false)

    private val television = context.resources.configuration.uiMode and Configuration.UI_MODE_TYPE_MASK ==
        Configuration.UI_MODE_TYPE_TELEVISION

    private var config = ProxyViewConfig(context, if (television) 3 else uiStore.proxyLine)

    private val menu: ProxyMenu by lazy {
        ProxyMenu(context, binding.menuView, overrideMode, uiStore, requests) {
            config.proxyLine = uiStore.proxyLine
        }
    }

    private val adapter: ProxyPageAdapter
        get() = binding.pagesView.adapter!! as ProxyPageAdapter

    private var horizontalScrolling = false
    private var restoreUrlTestFocus = false
    private val verticalBottomScrolled: Boolean
        get() = adapter.states[binding.pagesView.currentItem].bottom
    private var urlTesting: Boolean
        get() = adapter.states[binding.pagesView.currentItem].urlTesting
        set(value) {
            adapter.states[binding.pagesView.currentItem].urlTesting = value
        }

    override val root: View = binding.root

    suspend fun updateGroup(
        position: Int,
        proxies: List<Proxy>,
        selectable: Boolean,
        parent: ProxyState,
        links: Map<String, ProxyState>
    ) {
        adapter.updateAdapter(position, proxies, selectable, parent, links)

        adapter.states[position].urlTesting = false

        updateUrlTestButtonStatus()
    }

    suspend fun requestRedrawVisible() {
        withContext(Dispatchers.Main) {
            adapter.requestRedrawVisible()
        }
    }

    suspend fun showModeSwitchTips() {
        withContext(Dispatchers.Main) {
            Toast.makeText(context, R.string.mode_switch_tips, Toast.LENGTH_LONG).show()
        }
    }

    init {
        binding.self = this

        binding.activityBarLayout.applyFrom(context)

        if (television) {
            binding.root.post {
                val horizontalMargin = (12 * context.resources.displayMetrics.density).toInt()
                binding.root.setPaddingRelative(horizontalMargin, 0, horizontalMargin, 0)
            }
        }

        binding.menuView.setOnClickListener {
            menu.show()
        }

        if (groupNames.isEmpty()) {
            binding.emptyView.visibility = View.VISIBLE

            binding.urlTestView.visibility = View.GONE
            binding.tabLayoutView.visibility = View.GONE
            binding.elevationView.visibility = View.GONE
            binding.pagesView.visibility = View.GONE
            binding.urlTestFloatView.visibility = View.GONE
        } else {
            if (television) {
                binding.urlTestFloatView.visibility = View.GONE
            } else {
                binding.urlTestFloatView.supportImageTintList = ColorStateList.valueOf(
                    context.resolveThemedColor(com.google.android.material.R.attr.colorOnPrimary)
                )
            }

            binding.pagesView.apply {
                adapter = ProxyPageAdapter(
                    surface,
                    config,
                    List(groupNames.size) { index ->
                        ProxyAdapter(config) { name ->
                            requests.trySend(Request.Select(index, name))
                        }
                    }
                ) {
                    if (it == currentItem)
                        updateUrlTestButtonStatus()
                }

                registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
                    override fun onPageScrollStateChanged(state: Int) {
                        horizontalScrolling = state != ViewPager2.SCROLL_STATE_IDLE

                        updateUrlTestButtonStatus()
                    }

                    override fun onPageSelected(position: Int) {
                        uiStore.proxyLastGroup = groupNames[position]
                    }
                })
            }

            TabLayoutMediator(binding.tabLayoutView, binding.pagesView) { tab, index ->
                tab.text = groupNames[index]
            }.attach()

            if (television) {
                binding.tabLayoutView.setPaddingRelative(
                    0,
                    0,
                    (220 * context.resources.displayMetrics.density).toInt(),
                    0,
                )
                binding.tabLayoutView.tabMode = com.google.android.material.tabs.TabLayout.MODE_SCROLLABLE
                binding.tabLayoutView.tabGravity = com.google.android.material.tabs.TabLayout.GRAVITY_START
                binding.tabLayoutView.setSelectedTabIndicatorColor(android.graphics.Color.TRANSPARENT)
                binding.tabLayoutView.post {
                    val strip = binding.tabLayoutView.getChildAt(0) as? ViewGroup ?: return@post
                    for (index in 0 until strip.childCount) {
                        val tabView = strip.getChildAt(index) as? ViewGroup ?: continue
                        val spacing = (6 * context.resources.displayMetrics.density).toInt()
                        (tabView.layoutParams as? ViewGroup.MarginLayoutParams)?.apply {
                            marginStart = spacing
                            marginEnd = spacing
                            tabView.layoutParams = this
                        }
                        tabView.setPadding(spacing * 2, 0, spacing * 2, 0)
                        tabView.minimumWidth = 0
                        tabView.background = context.getDrawable(R.drawable.tv_proxy_tab_background)
                        val label = tabView.findTextView()
                        label?.apply {
                            maxLines = 1
                            isSingleLine = true
                        }
                    }
                }
            }

            val initialPosition = groupNames.indexOf(uiStore.proxyLastGroup)

            binding.pagesView.post {
                if (initialPosition > 0)
                    binding.pagesView.setCurrentItem(initialPosition, false)
            }
        }
    }

    private fun View.findTextView(): TextView? {
        if (this is TextView) return this
        if (this !is ViewGroup) return null
        for (index in 0 until childCount) {
            getChildAt(index).findTextView()?.let { return it }
        }
        return null
    }

    fun requestUrlTesting() {
        if (urlTesting) return

        restoreUrlTestFocus = television && binding.urlTestView.hasFocus()
        urlTesting = true

        requests.trySend(Request.UrlTest(binding.pagesView.currentItem))

        updateUrlTestButtonStatus()
    }

    private fun updateUrlTestButtonStatus() {
        if (television || verticalBottomScrolled || horizontalScrolling || urlTesting) {
            binding.urlTestFloatView.hide()
        } else {
            binding.urlTestFloatView.show()
        }

        if (television) {
            binding.urlTestView.visibility = View.VISIBLE
            binding.urlTestProgressView.visibility = if (urlTesting) View.VISIBLE else View.GONE
            if (!urlTesting && restoreUrlTestFocus) {
                restoreUrlTestFocus = false
                binding.urlTestView.post { binding.urlTestView.requestFocus() }
            }
        } else if (urlTesting) {
            binding.urlTestView.visibility = View.GONE
            binding.urlTestProgressView.visibility = View.VISIBLE
        } else {
            binding.urlTestView.visibility = View.VISIBLE
            binding.urlTestProgressView.visibility = View.GONE
        }
    }
}
