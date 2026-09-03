package com.dualdex

import android.app.Application
import android.util.Log

class DualDexApp : Application() {
    override fun onCreate() {
        super.onCreate()
        Log.i("DualDex", "DualDex Application initialized")
    }
}
