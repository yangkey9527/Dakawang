package com.dailycheckin

import android.app.Application
import com.dailycheckin.data.repo.CheckinRepository
import com.dailycheckin.reminder.CheckinScheduler
import com.dailycheckin.reminder.NotificationHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class DailyCheckinApp : Application() {

    override fun onCreate() {
        super.onCreate()
        NotificationHelper.ensureChannels(this)

        // 应用启动：补齐今日记录 + 恢复所有任务的提醒链
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            runCatching {
                CheckinRepository(this@DailyCheckinApp).ensureTodayRecords()
                CheckinScheduler.scheduleAll(this@DailyCheckinApp)
                CheckinScheduler.scheduleReview(this@DailyCheckinApp)
            }
        }
    }
}
