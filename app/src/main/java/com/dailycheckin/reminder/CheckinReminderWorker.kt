package com.dailycheckin.reminder

import android.content.Context
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import androidx.work.CoroutineWorker
import com.dailycheckin.data.db.CheckinRecord
import com.dailycheckin.data.db.AppDatabase
import com.dailycheckin.data.repo.SettingsRepository
import com.dailycheckin.util.DateUtils
import kotlinx.coroutines.flow.first

/**
 * 单个任务的到点提醒。执行逻辑：
 * 1. 总开关关闭则跳过
 * 2. 当天已打卡则不再提醒
 * 3. 未打卡 → 发提醒通知
 * 4. 无论结果如何，重排明天的提醒
 */
class CheckinReminderWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val taskId = inputData.getLong(KEY_TASK_ID, -1L)
        if (taskId <= 0) return Result.success()

        val appContext = applicationContext
        if (!SettingsRepository(appContext).remindersEnabled.first()) {
            reschedule()
            return Result.success()
        }

        val db = AppDatabase.get(appContext)
        val task = db.taskDao().getById(taskId)
        if (task == null || !task.enabled) return Result.success()

        val today = DateUtils.today()
        val record = db.recordDao().get(taskId, today)
        val done = record?.status == CheckinRecord.STATUS_DONE

        if (!done) {
            NotificationHelper.showReminder(appContext, task)
        }

        reschedule()
        return Result.success()
    }

    private suspend fun reschedule() {
        val taskId = inputData.getLong(KEY_TASK_ID, -1L)
        CheckinScheduler.scheduleOne(applicationContext, taskId)
    }

    companion object {
        const val KEY_TASK_ID = "task_id"

        fun inputData(taskId: Long) = workDataOf(KEY_TASK_ID to taskId)
    }
}
