package com.github.kr328.clash.design.adapter

import android.content.Context
import android.content.res.Configuration
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.github.kr328.clash.core.model.LogMessage
import com.github.kr328.clash.design.databinding.AdapterLogMessageBinding
import com.github.kr328.clash.design.util.layoutInflater

class LogMessageAdapter(
    private val context: Context,
    private val copy: (LogMessage) -> Unit,
) :
    RecyclerView.Adapter<LogMessageAdapter.Holder>() {
    class Holder(val binding: AdapterLogMessageBinding) : RecyclerView.ViewHolder(binding.root)

    var messages: List<LogMessage> = emptyList()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        val holder = Holder(
            AdapterLogMessageBinding
                .inflate(context.layoutInflater, parent, false)
        )
        val television = context.resources.configuration.uiMode and Configuration.UI_MODE_TYPE_MASK ==
            Configuration.UI_MODE_TYPE_TELEVISION
        if (television) holder.binding.root.isFocusable = false
        return holder
    }

    override fun onBindViewHolder(holder: Holder, position: Int) {
        val current = messages[position]

        holder.binding.message = current
        holder.binding.root.setOnLongClickListener {
            copy(current)

            true
        }
    }

    override fun getItemCount(): Int {
        return messages.size
    }
}
