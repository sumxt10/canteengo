package com.example.canteengo.utils

import android.content.Context

/** Stores one-time onboarding flags. */
object OnboardingPrefs {
    private const val PREFS = "canteengo_prefs"
    private const val KEY_GET_STARTED_SEEN = "get_started_seen"

    fun hasSeenGetStarted(context: Context): Boolean {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getBoolean(KEY_GET_STARTED_SEEN, false)
    }

    fun setGetStartedSeen(context: Context, seen: Boolean) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_GET_STARTED_SEEN, seen)
            .apply()
    }
}

