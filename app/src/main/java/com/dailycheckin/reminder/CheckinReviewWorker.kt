package com.dailycheckin.reminder

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.dailycheckin.data.db.AppDatabase
import com.dailycheckin.data.db.CheckinRecord
import com.dailycheckin.data.repo.SettingsRepository
import com.dailycheckin.util.DateUtils
import java.time.LocalTime
import kotlinx.coroutines.flow.first

/**
 * 每日复查提醒（默认晚上 22:30）：遍历当天"提醒时间已过但仍未完成"的任务，
 * 汇总发一条通知提醒补打。很多打卡过 12 点就失效，所以统一再复查一次。
 * 执行完成后自动重排到明天。
 */
class CheckinReviewWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val appContext = applicationContext
        if (!SettingsRepository(appContext).remindersEnabled.first()) {
            reschedule()
            return Result.success()
        }

        val today = DateUtils.today()
        val now = LocalTime.now()
        val db = AppDatabase.get(appContext)
        val pending = db.taskDao().getEnabled().filter { task ->
            val record = db.recordDao().get(task.id, today)
            record?.status != CheckinRecord.STATUS_DONE &&
                DateUtils.parseTime(task.remindTime).isBefore(now)
        }

        if (pending.isNotEmpty()) {
            NotificationHelper.showReviewReminder(appContext, pending.map { it.name })
        }

        reschedule()
        return Result.success()
    }

    private suspend fun reschedule() {
        CheckinScheduler.scheduleReview(applicationContext)
    }
}
