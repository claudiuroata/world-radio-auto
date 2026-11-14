package com.worldradio.auto.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.worldradio.auto.util.DiagnosticLogger

/**
 * BroadcastReceiver triggered on phone reboot or app update.
 * Awakens the package from Android's stopped/dormant state so that
 * Android Auto's package manager queries find the MediaBrowserService
 * reliably upon vehicle startup.
 */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        DiagnosticLogger.log("BootReceiver", "onReceive() action='${intent.action}'")
    }
}
