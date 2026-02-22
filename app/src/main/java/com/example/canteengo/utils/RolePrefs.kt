package com.example.canteengo.utils

import android.content.Context
import com.example.canteengo.models.UserRole

/**
 * Remembers which role was picked before signing up.
 * Firestore is the source of truth after the first successful login.
 */
object RolePrefs {
    private const val PREFS = "canteengo_prefs"
    private const val KEY_SELECTED_ROLE = "selected_role"

    fun setSelectedRole(context: Context, role: UserRole) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_SELECTED_ROLE, role.name)
            .apply()
    }

    fun getSelectedRole(context: Context): UserRole? {
        val value = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY_SELECTED_ROLE, null)
        return UserRole.from(value)
    }

    fun clear(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .remove(KEY_SELECTED_ROLE)
            .apply()
    }
}

