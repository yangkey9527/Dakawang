package com.dailycheckin.data.repo

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "settings")

class SettingsRepository(private val context: Context) {

    private object Keys {
        val remindersEnabled = booleanPreferencesKey("reminders_enabled")
        val detectEnabled = booleanPreferencesKey("detect_enabled")
    }

    val remindersEnabled: Flow<Boolean> = context.dataStore.data.map { it[Keys.remindersEnabled] ?: true }
    val detectEnabled: Flow<Boolean> = context.dataStore.data.map { it[Keys.detectEnabled] ?: false }

    suspend fun setRemindersEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.remindersEnabled] = enabled }
    }

    suspend fun setDetectEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.detectEnabled] = enabled }
    }
}
