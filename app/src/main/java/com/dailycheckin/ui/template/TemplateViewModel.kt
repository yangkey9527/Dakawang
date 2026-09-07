package com.dailycheckin.ui.template

import android.app.Application
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.dailycheckin.data.db.CheckinTask
import com.dailycheckin.data.model.PresetTemplate
import com.dailycheckin.data.model.PresetTemplates
import com.dailycheckin.data.repo.CheckinRepository
import com.dailycheckin.reminder.CheckinScheduler
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class TemplateViewModel(private val app: Application) : AndroidViewModel(app) {

    private val repo = CheckinRepository(app)

    val templates: List<PresetTemplate> = PresetTemplates.all

    /** 模板名 → 已存在任务（用于标记"已添加"） */
    val addedNames: StateFlow<Set<String>> = repo.observeTasks()
        .map { tasks -> tasks.map { it.name }.toSet() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptySet())

    fun addFromTemplate(template: PresetTemplate, onDone: (Long) -> Unit) {
        viewModelScope.launch {
            val task = CheckinTask(
                name = template.name,
                packageName = template.packageName,
                deepLink = template.deepLink,
                remindTime = template.defaultTime,
                enabled = true,
                checkKeywords = template.checkKeywords.joinToString(","),
                targetHint = template.targetHint,
                platform = template.platform,
                colorIndex = template.colorIndex
            )
            val id = repo.saveTask(task)
            CheckinScheduler.scheduleOne(app, id)
            Toast.makeText(app, "已添加「${template.name}」，可点进去修正应用", Toast.LENGTH_SHORT).show()
            onDone(id)
        }
    }
}
