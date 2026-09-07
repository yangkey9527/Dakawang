package com.dailycheckin.reminder

import android.content.Context
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.dailycheckin.data.db.CheckinTask
import com.dailycheckin.data.db.AppDatabase
import com.dailycheckin.util.DateUtils
import java.util.concurrent.TimeUnit

/**
 * 提醒调度：每个启用任务维护一条唯一的一次性提醒链。
 * 每次提醒触发后由 Worker 自动重排到明天，无需额外的周期任务。
 */
object CheckinScheduler {

    fun reminderWorkName(taskId: Long) = "reminder_$taskId"

    /** 为单个任务调度下一次提醒（Worker 触发后重排也用此方法） */
    suspend fun scheduleOne(context: Context, taskId: Long) {
        val task = AppDatabase.get(context).taskDao().getById(taskId) ?: return
        if (!task.enabled) return
        scheduleTask(context, task)
    }

    suspend fun scheduleAll(context: Context) {
        val tasks = AppDatabase.get(context).taskDao().getEnabled()
        tasks.forEach { scheduleTask(context, it) }
    }

    fun cancelAll(context: Context) {
        WorkManager.getInstance(context).cancelAllWorkByTag(TAG_REMINDER)
    }

    suspend fun cancelOne(context: Context, taskId: Long) {
        WorkManager.getInstance(context).cancelUniqueWork(reminderWorkName(taskId))
    }

    /** 每日复查（默认晚上 22:30）：当天已过提醒点仍未完成的任务汇总提醒，Worker 触发后自动重排明天 */
    fun scheduleReview(context: Context) {
        val now = System.currentTimeMillis()
        val target = DateUtils.nextTimeMillis(REVIEW_TIME)
        val delay = (target - now).coerceAtLeast(60_000L)

        val request = OneTimeWorkRequestBuilder<CheckinReviewWorker>()
            .setInitialDelay(delay, TimeUnit.MILLISECONDS)
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            REVIEW_WORK_NAME,
            ExistingWorkPolicy.REPLACE,
            request
        )
    }

    fun cancelReview(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(REVIEW_WORK_NAME)
    }

    private fun scheduleTask(context: Context, task: CheckinTask) {
        val now = System.currentTimeMillis()
        val target = DateUtils.nextTimeMillis(task.remindTime)
        val delay = (target - now).coerceAtLeast(60_000L)

        val request = OneTimeWorkRequestBuilder<CheckinReminderWorker>()
            .setInitialDelay(delay, TimeUnit.MILLISECONDS)
            .setInputData(CheckinReminderWorker.inputData(task.id))
            .addTag(TAG_REMINDER)
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            reminderWorkName(task.id),
            ExistingWorkPolicy.REPLACE,
            request
        )
    }

    private const val TAG_REMINDER = "checkin_reminder"

    /** 每日复查提醒时间（默认 10:30） */
    const val REVIEW_TIME = "10:30"

    /** 每日复查任务唯一名称 */
    const val REVIEW_WORK_NAME = "daily_review"
}
