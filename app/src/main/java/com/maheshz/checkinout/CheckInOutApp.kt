package com.maheshz.checkinout

import android.app.Application
import com.maheshz.checkinout.di.AppContainer

class CheckInOutApp : Application() {
    lateinit var container: AppContainer
    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}