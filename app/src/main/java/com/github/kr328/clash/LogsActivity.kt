package com.github.kr328.clash

import android.content.ComponentName
import android.content.Context
import android.content.ServiceConnection
import android.content.res.Configuration
import android.os.IBinder
import com.github.kr328.clash.common.compat.startForegroundServiceCompat
import com.github.kr328.clash.common.util.intent
import com.github.kr328.clash.common.util.setFileName
import com.github.kr328.clash.core.model.LogMessage
import com.github.kr328.clash.design.*
import com.github.kr328.clash.design.model.LogFile
import com.github.kr328.clash.log.LogcatReader
import com.github.kr328.clash.util.logsDir
import kotlinx.coroutines.*
import kotlinx.coroutines.selects.select
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class LogsActivity : BaseActivity<Design<*>>() {
    private var conn: ServiceConnection? = null
    private var streamJob: Job? = null

    override suspend fun main() {
        if (resources.configuration.uiMode and Configuration.UI_MODE_TYPE_MASK == Configuration.UI_MODE_TYPE_TELEVISION) {
            return mainTelevision()
        }
        mainPhone()
    }

    private suspend fun mainPhone() {
        val design = LogsDesign(this)

        setContentDesign(design)

        while (isActive) {
            select<Unit> {
                events.onReceive {
                    when (it) {
                        Event.ActivityStart -> {
                            val files = withContext(Dispatchers.IO) {
                                loadFiles()
                            }

                            design.patchLogs(files)
                        }
                        else -> Unit
                    }
                }
                design.requests.onReceive {
                    when (it) {
                        LogsDesign.Request.StartLogcat -> {
                            startActivity(LogcatActivity::class.intent)
                            finish()
                        }
                        LogsDesign.Request.DeleteAll -> {
                            if (design.requestDeleteAll()) {
                                withContext(Dispatchers.IO) {
                                    deleteAllLogs()
                                }

                                events.trySend(Event.ActivityStart)
                            }
                        }
                        is LogsDesign.Request.OpenFile -> {
                            startActivity(LogcatActivity::class.intent.setFileName(it.file.fileName))
                        }
                    }
                }
            }
        }
    }

    private suspend fun mainTelevision() {
        val design = TvLogsDesign(this)
        setContentDesign(design)
        design.patchLogs(withContext(Dispatchers.IO) { loadFiles() })

        while (isActive) {
            select<Unit> {
                events.onReceive { event ->
                    if (event == Event.ActivityStart) design.patchLogs(withContext(Dispatchers.IO) { loadFiles() })
                }
                design.requests.onReceive { request ->
                    when (request) {
                        TvLogsDesign.Request.Start -> startStreaming(design)
                        TvLogsDesign.Request.Stop -> stopStreaming(design)
                        TvLogsDesign.Request.DeleteAll -> {
                            val confirmed = com.google.android.material.dialog.MaterialAlertDialogBuilder(this@LogsActivity)
                                .setTitle(com.github.kr328.clash.design.R.string.delete_all_logs)
                                .setMessage(com.github.kr328.clash.design.R.string.delete_all_logs_warn)
                                .setNegativeButton(com.github.kr328.clash.design.R.string.cancel, null)
                                .setPositiveButton(com.github.kr328.clash.design.R.string.ok, null)
                                .create()
                            val remove = suspendCancellableCoroutine<Boolean> { continuation ->
                                confirmed.setOnShowListener {
                                    confirmed.getButton(android.app.AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                                        continuation.resume(true) {}
                                        confirmed.dismiss()
                                    }
                                }
                                confirmed.setOnDismissListener {
                                    if (continuation.isActive) continuation.resume(false) {}
                                }
                                confirmed.show()
                            }
                            if (remove) {
                                withContext(Dispatchers.IO) { deleteAllLogs() }
                                design.patchLogs(emptyList())
                                design.showMessages(
                                    getString(com.github.kr328.clash.design.R.string.logcat),
                                    emptyList(),
                                    false,
                                )
                                design.focusFiles()
                            }
                        }
                        is TvLogsDesign.Request.Open -> {
                            streamJob?.cancel()
                            val messages = withContext(Dispatchers.IO) {
                                LogcatReader(this@LogsActivity, request.file).readAll()
                            }
                            design.showMessages(request.file.fileName, messages, false)
                        }
                    }
                }
            }
        }
    }

    private suspend fun startStreaming(design: TvLogsDesign) {
        if (streamJob?.isActive == true) return
        startForegroundServiceCompat(LogcatService::class.intent)
        val logcat = bindLogcatService()
        streamJob = launch {
            var initial = true
            while (isActive) {
                val snapshot = logcat.snapshot(initial)
                if (snapshot != null) {
                    design.showMessages(
                        getString(com.github.kr328.clash.design.R.string.clash_logcat),
                        snapshot.messages,
                        true,
                    )
                    initial = false
                }
                delay(500)
            }
        }
    }

    private fun stopStreaming(design: TvLogsDesign) {
        streamJob?.cancel()
        streamJob = null
        stopService(LogcatService::class.intent)
        conn?.let(::unbindService)
        conn = null
        design.showMessages(getString(com.github.kr328.clash.design.R.string.logcat), emptyList<LogMessage>(), false)
        design.focusFiles()
    }

    private suspend fun bindLogcatService(): LogcatService = suspendCoroutine { continuation ->
        bindService(LogcatService::class.intent, object : ServiceConnection {
            override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
                conn = this
                continuation.resume(service!!.queryLocalInterface("") as LogcatService)
            }

            override fun onServiceDisconnected(name: ComponentName?) {
                conn = null
            }
        }, Context.BIND_AUTO_CREATE)
    }

    private fun loadFiles(): List<LogFile> {
        val list = cacheDir.resolve("logs").listFiles()?.toList() ?: emptyList()

        return list.mapNotNull { LogFile.parseFromFileName(it.name) }
    }

    private fun deleteAllLogs() {
        logsDir.deleteRecursively()
    }

    override fun onDestroy() {
        streamJob?.cancel()
        conn?.let(::unbindService)
        conn = null
        super.onDestroy()
    }
}
