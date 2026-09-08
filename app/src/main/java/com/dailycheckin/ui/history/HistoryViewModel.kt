package com.dailycheckin.ui.history

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.dailycheckin.data.db.AppDatabase
import com.dailycheckin.data.db.CheckinRecord
import com.dailycheckin.data.db.CheckinTask
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

class HistoryViewModel(
    app: Application,
    private val taskId: Long
) : AndroidViewModel(app) {

    private val db = AppDatabase.get(app)

    val task: StateFlow<CheckinTask?> = db.taskDao().observeById(taskId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val records: StateFlow<List<CheckinRecord>> = db.recordDao().observeByTask(taskId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY]!!
                val taskId = createSavedStateHandle().get<Long>("taskId") ?: -1L
                HistoryViewModel(app, taskId)
            }
        }
    }
}
