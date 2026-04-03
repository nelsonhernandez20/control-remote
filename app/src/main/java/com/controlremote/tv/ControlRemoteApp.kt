package com.controlremote.tv

import android.app.Application
import com.google.android.gms.ads.MobileAds

class ControlRemoteApp : Application() {
    override fun onCreate() {
        super.onCreate()
        MobileAds.initialize(this) {}
    }
}
