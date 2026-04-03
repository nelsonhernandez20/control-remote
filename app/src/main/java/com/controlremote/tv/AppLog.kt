package com.controlremote.tv

import android.util.Log

/** Logs visibles con: `adb logcat -s ControlRemote` */
internal object AppLog {
    const val TAG = "ControlRemote"

    fun d(message: String) {
        Log.d(TAG, message)
    }

    fun e(message: String, t: Throwable? = null) {
        if (t != null) Log.e(TAG, message, t) else Log.e(TAG, message)
    }
}
