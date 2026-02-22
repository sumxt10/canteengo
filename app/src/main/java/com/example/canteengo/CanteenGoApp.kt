package com.example.canteengo

import android.app.Application
import com.google.firebase.FirebaseApp

class CanteenGoApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // When google-services.json isn't present (dev builds), Firebase won't auto-init.
        // This makes the app not crash at startup; Firebase calls will still fail gracefully
        // if no Firebase config exists.
        runCatching {
            FirebaseApp.initializeApp(this)
        }
    }
}

