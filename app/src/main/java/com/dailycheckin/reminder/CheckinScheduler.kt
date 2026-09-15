package com.dailycheckin.reminder

import android.content.Context
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.dailycheckin.data.db.CheckinRecord
import com.dailycheckin.data.db.CheckinTask
import com.dailycheckin.data.db.AppDatabase
import com.dailycheckin.data.repo.SettingsRepository
import com.dailycheckin.util.DateUtils
import java.time.LocalTime
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.flow.first

/**
 * 提醒调度：每个启用任务维护一条唯一的一次性提醒链。
 * 每次提醒触发后由 Worker 自动重排到明天，无需额外的周期任务。
 * 每日汇总时间与重复提醒间隔均读全局设置。
 */
object CheckinScheduler {

    fun reminderWorkName(taskId: Long) = "reminder_$taskId"

    /** 重复提醒链（到点未完成后的间隔提醒），与主链分离避免互相 REPLACE */
    fun retryWorkName(taskId: Long) = "reminder_${taskId}_retry"

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

    fun cancelRetry(context: Context, taskId: Long) {
        WorkManager.getInstance(context).cancelUniqueWork(retryWorkName(taskId))
    }

    /**
     * 到点未完成处理：发提醒；若开启了重复提醒且当天仍有空间，排下一次间隔提醒。
     * @return true = 已排下一次重复提醒（今天还有后续）；false = 应由调用方重排明天主提醒
     */
    suspend fun notifyIfPending(context: Context, taskId: Long, intervalMin: Int): Boolean {
        val appContext = context.applicationContext
        val db = AppDatabase.get(appContext)
        val task = db.taskDao().getById(taskId)
        if (task == null || !task.enabled) return false
        val today = DateUtils.today()
        val record = db.recordDao().get(taskId, today)
        if (record?.status == CheckinRecord.STATUS_DONE) return false

        NotificationHelper.showReminder(appContext, task)
        if (intervalMin > 0 && LocalTime.now().plusMinutes(intervalMin.toLong()) <= DAY_REPEAT_CUTOFF) {
            scheduleRetry(appContext, taskId, intervalMin)
            return true
        }
        return false
    }

    /** 排下一次重复提醒（独立唯一名，每次 REPLACE） */
    private fun scheduleRetry(context: Context, taskId: Long, delayMinutes: Int) {
        val request = OneTimeWorkRequestBuilder<CheckinRetryWorker>()
            .setInitialDelay(delayMinutes.toLong(), TimeUnit.MINUTES)
            .setInputData(CheckinReminderWorker.inputData(taskId))
            .addTag(TAG_REMINDER)
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(
            retryWorkName(taskId),
            ExistingWorkPolicy.REPLACE,
            request
        )
    }

    /** 每日复查（时间读全局设置，默认 23:00）：当天已过提醒点仍未完成的任务汇总提醒，Worker 触发后自动重排 */
    suspend fun scheduleReview(context: Context) {
        val reviewTime = SettingsRepository(context).reviewTime.first()
        val now = System.currentTimeMillis()
        val target = DateUtils.nextTimeMillis(reviewTime)
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

    /** 每日凌晨图标重置（零点后新一天任务未完成 → 切回未完成图标），Worker 触发后自动重排 */
    fun scheduleIconSync(context: Context) {
        val now = System.currentTimeMillis()
        val target = DateUtils.nextTimeMillis(ICON_SYNC_TIME)
        val delay = (target - now).coerceAtLeast(60_000L)

        val request = OneTimeWorkRequestBuilder<IconSyncWorker>()
            .setInitialDelay(delay, TimeUnit.MILLISECONDS)
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            ICON_SYNC_WORK_NAME,
            ExistingWorkPolicy.REPLACE,
            request
        )
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

    /** 重复提醒最晚截止：超过 23:50 不再重复，避免打扰睡眠 */
    private val DAY_REPEAT_CUTOFF: LocalTime = LocalTime.of(23, 50)

    /** 每日凌晨图标重置时间 */
    private const val ICON_SYNC_TIME = "00:05"

    /** 每日复查任务唯一名称 */
    const val REVIEW_WORK_NAME = "daily_review"

    /** 每日图标同步任务唯一名称 */
    const val ICON_SYNC_WORK_NAME = "daily_icon_sync"
}
