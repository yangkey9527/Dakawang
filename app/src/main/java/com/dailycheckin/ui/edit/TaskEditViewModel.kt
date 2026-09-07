package com.dailycheckin.ui.edit

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.dailycheckin.data.db.CheckinTask
import com.dailycheckin.data.repo.CheckinRepository
import com.dailycheckin.reminder.CheckinScheduler
import kotlinx.coroutines.launch

class TaskEditViewModel(
    private val app: Application,
    private val taskId: Long
) : AndroidViewModel(app) {

    private val repo = CheckinRepository(app)

    var name by mutableStateOf("")
    var packageName by mutableStateOf("")
    var deepLink by mutableStateOf("")
    var remindTime by mutableStateOf("09:00")
    var targetHint by mutableStateOf("")
    var checkKeywords by mutableStateOf("已签到,今日已签到,已打卡")
    var platform by mutableStateOf(CheckinTask.PLATFORM_MOBILE)
    var enabled by mutableStateOf(true)
    var colorIndex by mutableStateOf(0)
    var error by mutableStateOf<String?>(null)

    val isNew: Boolean get() = taskId <= 0

    init {
        if (taskId > 0) {
            viewModelScope.launch {
                repo.getTask(taskId)?.let { fill(it) }
            }
        }
    }

    private fun fill(task: CheckinTask) {
        name = task.name
        packageName = task.packageName
        deepLink = task.deepLink ?: ""
        remindTime = task.remindTime
        targetHint = task.targetHint
        checkKeywords = task.checkKeywords
        enabled = task.enabled
        colorIndex = task.colorIndex
    }

    fun save(onSaved: () -> Unit) {
        val trimmedName = name.trim()
        if (trimmedName.isEmpty()) {
            error = "请填写任务名称"
            return
        }
        error = null
        viewModelScope.launch {
            val task = CheckinTask(
                id = taskId,
                name = trimmedName,
                packageName = packageName.trim(),
                deepLink = deepLink.trim().ifBlank { null },
                remindTime = remindTime,
                enabled = enabled,
                checkKeywords = checkKeywords.trim().ifBlank { "已签到,今日已签到,已打卡" },
                targetHint = targetHint.trim(),
                platform = platform,
                colorIndex = colorIndex
            )
            val id = repo.saveTask(task)
            CheckinScheduler.scheduleOne(app, id)
            onSaved()
        }
    }

    fun delete(onDeleted: () -> Unit) {
        viewModelScope.launch {
            if (taskId > 0) {
                repo.getTask(taskId)?.let { repo.deleteTask(it) }
                CheckinScheduler.cancelOne(app, taskId)
            }
            onDeleted()
        }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY]!!
                val taskId = createSavedStateHandle().get<Long>("taskId") ?: -1L
                TaskEditViewModel(app, taskId)
            }
        }
    }
}
