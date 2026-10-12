package com.example

import android.app.Application
import com.example.data.local.AppDatabase

class MayraApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // Pre-warm database
        AppDatabase.getInstance(this)
    }
}
