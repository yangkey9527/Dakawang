package com.dailycheckin.data.repo

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "settings")

class SettingsRepository(private val context: Context) {

    private object Keys {
        val remindersEnabled = booleanPreferencesKey("reminders_enabled")
        val detectEnabled = booleanPreferencesKey("detect_enabled")
        /** 每日未完成汇总提醒时间 HH:mm，默认 23:00 */
        val reviewTime = stringPreferencesKey("review_time")
        /** 到点未完成后的重复提醒间隔（分钟），0 = 不重复 */
        val repeatIntervalMin = intPreferencesKey("repeat_interval_min")
    }

    val remindersEnabled: Flow<Boolean> = context.dataStore.data.map { it[Keys.remindersEnabled] ?: true }
    val detectEnabled: Flow<Boolean> = context.dataStore.data.map { it[Keys.detectEnabled] ?: false }
    val reviewTime: Flow<String> = context.dataStore.data.map { it[Keys.reviewTime] ?: DEFAULT_REVIEW_TIME }
    val repeatIntervalMin: Flow<Int> = context.dataStore.data.map { it[Keys.repeatIntervalMin] ?: DEFAULT_REPEAT_INTERVAL_MIN }

    suspend fun setRemindersEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.remindersEnabled] = enabled }
    }

    suspend fun setDetectEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.detectEnabled] = enabled }
    }

    suspend fun setReviewTime(time: String) {
        context.dataStore.edit { it[Keys.reviewTime] = time }
    }

    suspend fun setRepeatIntervalMin(minutes: Int) {
        context.dataStore.edit { it[Keys.repeatIntervalMin] = minutes.coerceIn(0, 120) }
    }

    companion object {
        const val DEFAULT_REVIEW_TIME = "23:00"
        const val DEFAULT_REPEAT_INTERVAL_MIN = 0
    }
}
