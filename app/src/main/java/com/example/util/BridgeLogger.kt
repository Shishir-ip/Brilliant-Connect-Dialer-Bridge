package com.example.util

import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class LogEntry(
    val timestamp: Long = System.currentTimeMillis(),
    val level: String,
    val tag: String,
    val message: String
) {
    fun formatDisplay(): String {
        val sdf = SimpleDateFormat("HH:mm:ss.SSS", Locale.US)
        val time = sdf.format(Date(timestamp))
        return "[$time] $level/$tag: $message"
    }
}

object BridgeLogger {

    private const val MAX_LOGS = 200
    private val lock = Any()
    private val buffer = ArrayDeque<LogEntry>(MAX_LOGS)

    private val _logsFlow = MutableStateFlow<List<LogEntry>>(emptyList())
    val logsFlow: StateFlow<List<LogEntry>> = _logsFlow.asStateFlow()

    private fun addEntry(level: String, tag: String, message: String) {
        val safeMessage = NumberMasker.maskInText(message)
        val entry = LogEntry(level = level, tag = tag, message = safeMessage)
        synchronized(lock) {
            if (buffer.size >= MAX_LOGS) {
                buffer.removeFirst()
            }
            buffer.addLast(entry)
            _logsFlow.value = buffer.toList()
        }
    }

    fun d(tag: String, msg: String) {
        Log.d(tag, NumberMasker.maskInText(msg))
        addEntry("D", tag, msg)
    }

    fun i(tag: String, msg: String) {
        Log.i(tag, NumberMasker.maskInText(msg))
        addEntry("I", tag, msg)
    }

    fun w(tag: String, msg: String) {
        Log.w(tag, NumberMasker.maskInText(msg))
        addEntry("W", tag, msg)
    }

    fun e(tag: String, msg: String, tr: Throwable? = null) {
        val fullMsg = if (tr != null) "$msg: ${tr.message}" else msg
        Log.e(tag, NumberMasker.maskInText(fullMsg), tr)
        addEntry("E", tag, fullMsg)
    }

    fun getAllLogsFormatted(): String {
        synchronized(lock) {
            return buffer.joinToString(separator = "\n") { it.formatDisplay() }
        }
    }

    fun clear() {
        synchronized(lock) {
            buffer.clear()
            _logsFlow.value = emptyList()
        }
    }
}
