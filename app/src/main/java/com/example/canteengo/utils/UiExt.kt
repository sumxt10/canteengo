package com.example.canteengo.utils

import android.app.Activity
import android.view.inputmethod.InputMethodManager
import android.widget.Toast

fun Activity.toast(message: String) {
    Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
}

fun Activity.hideKeyboard() {
    val imm = getSystemService(InputMethodManager::class.java) ?: return
    val view = currentFocus ?: return
    imm.hideSoftInputFromWindow(view.windowToken, 0)
}

