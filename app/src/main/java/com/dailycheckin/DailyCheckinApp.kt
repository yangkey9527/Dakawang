package com.dailycheckin

import android.app.Application
import android.util.Log
import com.dailycheckin.data.db.AppDatabase
import com.dailycheckin.data.repo.CheckinRepository
import com.dailycheckin.reminder.CheckinScheduler
import com.dailycheckin.reminder.NotificationHelper
import com.dailycheckin.util.LauncherIconHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class DailyCheckinApp : Application() {

    override fun onCreate() {
        super.onCreate()
        NotificationHelper.ensureChannels(this)

        // 应用启动：修复旧深链 + 补齐今日记录 + 恢复所有任务的提醒链
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            fixAntForestDeepLink()
            runCatching { CheckinRepository(this@DailyCheckinApp).ensureTodayRecords() }
                .onFailure { Log.e(TAG, "ensureTodayRecords 失败", it) }
            runCatching { LauncherIconHelper.sync(this@DailyCheckinApp) }
                .onFailure { Log.e(TAG, "LauncherIconHelper.sync 失败", it) }
            runCatching { CheckinScheduler.scheduleAll(this@DailyCheckinApp) }
                .onFailure { Log.e(TAG, "scheduleAll 失败", it) }
            runCatching { CheckinScheduler.scheduleReview(this@DailyCheckinApp) }
                .onFailure { Log.e(TAG, "scheduleReview 失败", it) }
            runCatching { CheckinScheduler.scheduleIconSync(this@DailyCheckinApp) }
                .onFailure { Log.e(TAG, "scheduleIconSync 失败", it) }
        }
    }

    /** 修复已知失效的蚂蚁森林深链（旧 appId=60000048 → 60000002），已创建的任务同样生效 */
    private suspend fun fixAntForestDeepLink() {
        val OLD = "alipays://platformapi/startapp?appId=60000048"
        val NEW = "alipays://platformapi/startapp?appId=60000002"
        try {
            val db = AppDatabase.get(this)
            var updated = 0
            db.taskDao().getAll().forEach { task ->
                // 蚂蚁森林任务：旧深链替换为新深链；深链缺失的补上新深链
                val isAntForest = task.name == "蚂蚁森林" || task.packageName == "com.eg.android.AlipayGphone"
                if (isAntForest && (task.deepLink == null || task.deepLink == OLD)) {
                    db.taskDao().upsert(task.copy(deepLink = NEW))
                    updated++
                }
            }
            Log.d(TAG, "蚂蚁森林深链修复完成，更新 $updated 个任务")
        } catch (e: Exception) {
            Log.e(TAG, "蚂蚁森林深链修复失败", e)
        }
    }

    companion object {
        private const val TAG = "DailyCheckinApp"
    }
}
