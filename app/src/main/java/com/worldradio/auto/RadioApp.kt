package com.worldradio.auto

import android.app.Application
import android.content.pm.ApplicationInfo
import com.worldradio.auto.data.local.RadioDatabase
import com.worldradio.auto.data.remote.RadioBrowserClient
import com.worldradio.auto.data.repository.RadioRepository
import com.worldradio.auto.util.DiagnosticLogger

class RadioApp : Application() {

    lateinit var repository: RadioRepository
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this

        DiagnosticLogger.init(this)
        val installer = DiagnosticLogger.getInstallerPackage(this)
        val batteryIgnored = DiagnosticLogger.isIgnoringBatteryOptimizations(this)
        val isDebuggable = (applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
        DiagnosticLogger.log(
            "RadioApp",
            "App started. installerPackage='$installer', batteryUnrestricted=$batteryIgnored, debuggable=$isDebuggable"
        )

        val database = RadioDatabase.getInstance(this)
        repository = RadioRepository(
            api = RadioBrowserClient.api,
            dao = database.stationDao()
        )
    }

    companion object {
        lateinit var instance: RadioApp
            private set
    }
}
