package com.dailycheckin.receiver

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.dailycheckin.data.repo.CheckinRepository
import com.dailycheckin.reminder.CheckinScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * 通知按钮"我已打卡"：标记今日完成并取消当天提醒。
 */
class CheckinActionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val taskId = intent.getLongExtra(EXTRA_TASK_ID, -1L)
        if (taskId <= 0) return
        val pendingResult = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                val repo = CheckinRepository(context)
                repo.markDone(taskId)
                CheckinScheduler.cancelOne(context, taskId)
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        const val ACTION_MARK_DONE = "com.dailycheckin.action.MARK_DONE"
        const val EXTRA_TASK_ID = "task_id"

        fun markDoneIntent(context: Context, taskId: Long): PendingIntent {
            val intent = Intent(context, CheckinActionReceiver::class.java).apply {
                action = ACTION_MARK_DONE
                putExtra(EXTRA_TASK_ID, taskId)
            }
            return PendingIntent.getBroadcast(
                context,
                taskId.toInt(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }
    }
}
