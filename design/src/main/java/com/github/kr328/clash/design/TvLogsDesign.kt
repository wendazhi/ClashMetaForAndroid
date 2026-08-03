package com.github.kr328.clash.design

import android.content.Context
import android.graphics.Typeface
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.github.kr328.clash.core.model.LogMessage
import com.github.kr328.clash.design.adapter.LogFileAdapter
import com.github.kr328.clash.design.adapter.LogMessageAdapter
import com.github.kr328.clash.design.model.LogFile
import com.github.kr328.clash.design.view.AppRecyclerView
import com.github.kr328.clash.design.util.patchDataSet

class TvLogsDesign(context: Context) : Design<TvLogsDesign.Request>(context) {
    sealed class Request {
        object Start : Request()
        object Stop : Request()
        object DeleteAll : Request()
        data class Open(val file: LogFile) : Request()
    }

    private val filesAdapter = LogFileAdapter(context) { requests.trySend(Request.Open(it)) }
    private val messagesAdapter = LogMessageAdapter(context) {}
    private val fileList = RecyclerView(context)
    private val messageList = AppRecyclerView(context)
    private val action = Button(context)
    private val clear = Button(context)
    private val title = TextView(context)
    private var streamingVisible = false

    override val root: View = LinearLayout(context).apply {
        orientation = LinearLayout.HORIZONTAL
        val gap = (16 * resources.displayMetrics.density).toInt()
        setPadding(gap, gap, gap, gap)

        addView(LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            addView(LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
                addView(action.apply {
                    text = context.getString(R.string.clash_logcat)
                    isAllCaps = false
                    setOnClickListener { requests.trySend(Request.Start) }
                }, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
                addView(clear.apply {
                    text = context.getString(R.string.delete_all_logs)
                    isAllCaps = false
                    setOnClickListener { requests.trySend(Request.DeleteAll) }
                }, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply {
                    marginStart = gap
                })
            }, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            addView(TextView(context).apply {
                text = context.getString(R.string.history)
                setPadding(gap, gap, gap, gap / 2)
            })
            addView(fileList.apply {
                layoutManager = LinearLayoutManager(context)
                adapter = filesAdapter
                clipToPadding = false
            }, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f))
        }, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 0.36f))

        addView(LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(gap, 0, 0, 0)
            addView(title.apply {
                text = context.getString(R.string.logcat)
                textSize = 18f
                setTypeface(typeface, Typeface.BOLD)
                setPadding(gap, gap / 2, gap, gap)
            })
            addView(messageList.apply {
                layoutManager = LinearLayoutManager(context)
                adapter = messagesAdapter
                enableTvDocumentScrolling()
                contentDescription = context.getString(R.string.logcat)
            }, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f))
        }, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 0.64f))
    }

    suspend fun patchLogs(logs: List<LogFile>) {
        filesAdapter.patchDataSet(filesAdapter::logs, logs, false, LogFile::fileName)
    }

    fun showMessages(label: CharSequence, messages: List<LogMessage>, streaming: Boolean) {
        val handOffFocus = !streaming || !streamingVisible
        streamingVisible = streaming
        title.text = label
        messagesAdapter.messages = messages
        messagesAdapter.notifyDataSetChanged()
        action.text = context.getString(if (streaming) R.string.close else R.string.clash_logcat)
        action.setOnClickListener {
            requests.trySend(if (streaming) Request.Stop else Request.Start)
        }
        messageList.post {
            if (streaming && messages.isNotEmpty()) messageList.scrollToPosition(messages.lastIndex)
            if (handOffFocus) messageList.requestFocus()
        }
    }

    fun focusFiles() {
        action.requestFocus()
    }
}
