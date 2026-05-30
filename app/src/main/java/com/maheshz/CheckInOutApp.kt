package com.maheshz

import android.app.Application
import com.maheshz.di.AppContainer

class CheckInOutApp : Application() {
    lateinit var container: AppContainer
    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
