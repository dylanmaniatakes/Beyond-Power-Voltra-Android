package com.technogizguy.voltra.controller

import android.app.Application

class VoltraControllerApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        AppGraph.init(this)
    }
}
