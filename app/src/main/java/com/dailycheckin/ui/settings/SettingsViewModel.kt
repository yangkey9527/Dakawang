package com.dailycheckin.ui.settings

import android.accessibilityservice.AccessibilityServiceInfo
import android.app.Application
import android.content.Context
import android.view.accessibility.AccessibilityManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.dailycheckin.data.repo.SettingsRepository
import com.dailycheckin.reminder.CheckinScheduler
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(private val app: Application) : AndroidViewModel(app) {

    private val settings = SettingsRepository(app)

    val remindersEnabled: StateFlow<Boolean> = settings.remindersEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), true)

    val detectEnabled: StateFlow<Boolean> = settings.detectEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    fun setRemindersEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settings.setRemindersEnabled(enabled)
            if (enabled) {
                CheckinScheduler.scheduleAll(app)
                CheckinScheduler.scheduleReview(app)
            } else {
                CheckinScheduler.cancelAll(app)
                CheckinScheduler.cancelReview(app)
            }
        }
    }

    fun setDetectEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settings.setDetectEnabled(enabled)
        }
    }

    /** 无障碍服务是否正在运行 */
    fun accessibilityServiceRunning(): Boolean {
        val am = app.getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager
        return am.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK)
            .any { it.resolveInfo.serviceInfo.packageName == app.packageName }
    }
}
