package com.dailycheckin.reminder

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.dailycheckin.data.repo.SettingsRepository
import kotlinx.coroutines.flow.first

/**
 * 到点未完成后的重复提醒：按全局设置的间隔再次提醒，
 * 直到任务完成或当天 23:50 截止。结束时补排明天的到点提醒。
 */
class CheckinRetryWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val taskId = inputData.getLong(CheckinReminderWorker.KEY_TASK_ID, -1L)
        if (taskId <= 0) return Result.success()

        val appContext = applicationContext
        if (!SettingsRepository(appContext).remindersEnabled.first()) {
            CheckinScheduler.scheduleOne(appContext, taskId)
            return Result.success()
        }

        val interval = SettingsRepository(appContext).repeatIntervalMin.first()
        val hasRetry = CheckinScheduler.notifyIfPending(appContext, taskId, interval)
        // 今天没有后续重复了 → 补排明天的主提醒
        if (!hasRetry) {
            CheckinScheduler.scheduleOne(appContext, taskId)
        }
        return Result.success()
    }
}
