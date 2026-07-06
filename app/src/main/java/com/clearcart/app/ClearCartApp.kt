package com.clearcart.app

import android.app.Application
import com.clearcart.app.data.repository.AppContainer

class ClearCartApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
