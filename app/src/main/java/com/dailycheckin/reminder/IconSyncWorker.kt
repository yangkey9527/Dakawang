package com.dailycheckin.reminder

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.dailycheckin.util.LauncherIconHelper

/**
 * 每日凌晨图标重置：新一天开始时任务必然未完成，
 * 把桌面图标从"全部完成"切回"未完成"。执行完成后自动重排到明天。
 */
class IconSyncWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        runCatching { LauncherIconHelper.sync(applicationContext) }
        CheckinScheduler.scheduleIconSync(applicationContext)
        return Result.success()
    }
}
