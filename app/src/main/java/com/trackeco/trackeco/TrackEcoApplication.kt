package com.trackeco.trackeco

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class TrackEcoApplication : Application() {
    override fun onCreate() {
        super.onCreate()
    }
}