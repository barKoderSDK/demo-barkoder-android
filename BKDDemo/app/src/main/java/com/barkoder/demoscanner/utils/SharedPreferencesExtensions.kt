package com.barkoder.demoscanner.utils

import android.content.SharedPreferences
import android.content.res.Resources.NotFoundException

// These methods will be used where we already need to have that key in the shared prefs for sure,
// otherwise something went wrong and its better to crash and find our problem

@Throws(NotFoundException::class)
fun SharedPreferences.getString(key: String): String {
    return if (this.contains(key))
        this.getString(key, "")!!
    else
        throw NotFoundException(key)
}

@Throws(NotFoundException::class)
fun SharedPreferences.getBoolean(key: String): Boolean {
    return if (this.contains(key))
        this.getBoolean(key, false)
    else
        throw NotFoundException(key)
}

@Throws(NotFoundException::class)
fun SharedPreferences.getInt(key: String): Int {
    return if (this.contains(key))
        this.getInt(key, -1)
    else
        throw NotFoundException(key)
}
