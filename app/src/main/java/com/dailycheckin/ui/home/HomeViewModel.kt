package com.dailycheckin.ui.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.dailycheckin.data.db.CheckinRecord
import com.dailycheckin.data.repo.CheckinRepository
import com.dailycheckin.data.repo.HomeTaskItem
import com.dailycheckin.reminder.CheckinScheduler
import com.dailycheckin.util.AppLauncher
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class HomeViewModel(private val app: Application) : AndroidViewModel(app) {

    private val repo = CheckinRepository(app)

    val items: StateFlow<List<HomeTaskItem>> = repo.observeToday()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val stats: StateFlow<Pair<Int, Int>> = repo.observeTodayStats()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0 to 0)

    init {
        viewModelScope.launch {
            repo.ensureTodayRecords()
            CheckinScheduler.scheduleAll(app)
            CheckinScheduler.scheduleReview(app)
        }
    }

    fun markDone(item: HomeTaskItem) {
        viewModelScope.launch {
            repo.markDone(item.task.id)
            CheckinScheduler.cancelOne(app, item.task.id)
        }
    }

    fun markPending(item: HomeTaskItem) {
        viewModelScope.launch {
            repo.markPending(item.task.id)
            CheckinScheduler.scheduleOne(app, item.task.id)
        }
    }

    fun markSkipped(item: HomeTaskItem) {
        viewModelScope.launch {
            repo.markSkipped(item.task.id)
            CheckinScheduler.cancelOne(app, item.task.id)
        }
    }

    fun launch(item: HomeTaskItem): Boolean = AppLauncher.launchTask(app, item.task)

    fun isReminderOverdue(item: HomeTaskItem): Boolean {
        if (item.status != CheckinRecord.STATUS_PENDING) return false
        return com.dailycheckin.util.DateUtils.parseTime(item.task.remindTime)
            .isBefore(java.time.LocalTime.now())
    }
}
