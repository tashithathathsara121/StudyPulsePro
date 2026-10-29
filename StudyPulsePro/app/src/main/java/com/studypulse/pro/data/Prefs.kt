package com.studypulse.pro.data

import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore

val Context.ds by preferencesDataStore("sp_prefs")

object K {
    val kiosk = booleanPreferencesKey("kiosk")
    val pin = booleanPreferencesKey("pin")
    val onboarded = booleanPreferencesKey("onboarded")
    val emergency = longPreferencesKey("emergency_until")
    val sites = stringSetPreferencesKey("sites")
    val lastKey = stringPreferencesKey("last_key")
    val savedValid = booleanPreferencesKey("saved_valid")
    val savedFilter = intPreferencesKey("saved_filter")
    val savedCats = intPreferencesKey("saved_cats")
    val savedCall = intPreferencesKey("saved_call")
    val savedMsg = intPreferencesKey("saved_msg")
}

val DEFAULT_SITES = setOf(
    "wikipedia.org", "khanacademy.org", "coursera.org", "edx.org", "youtube.com",
    "docs.google.com", "developer.android.com", "kotlinlang.org", "w3schools.com",
    "geeksforgeeks.org", "ocw.mit.edu", "github.com", "stackoverflow.com"
)

data class Cfg(
    val kiosk: Boolean = false,
    val pin: Boolean = false,
    val onboarded: Boolean = false,
    val emergencyUntil: Long = 0L,
    val sites: Set<String> = DEFAULT_SITES
)
