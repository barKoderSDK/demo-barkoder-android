package com.barkoder.demoscanner.utils

import android.content.SharedPreferences

fun SharedPreferences.Editor.putBooleanWithOptions(
    sharedPrefs: SharedPreferences,
    key: String,
    value: Boolean,
    onlyIfNotContains: Boolean
) {
    if (!(onlyIfNotContains && sharedPrefs.contains(key)))
        this.putBoolean(key, value)
}

fun SharedPreferences.Editor.putStringWithOptions(
    sharedPrefs: SharedPreferences,
    key: String,
    value: String,
    onlyIfNotContains: Boolean
) {
    if (!(onlyIfNotContains && sharedPrefs.contains(key)))
        this.putString(key, value)
}

fun SharedPreferences.Editor.putIntWithOptions(
    sharedPrefs: SharedPreferences,
    key: String,
    value: Int,
    onlyIfNotContains: Boolean
) {
    if (!(onlyIfNotContains && sharedPrefs.contains(key)))
        this.putInt(key, value)
}
