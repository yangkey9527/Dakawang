package com.dailycheckin.reminder

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.dailycheckin.MainActivity
import com.dailycheckin.R
import com.dailycheckin.data.db.CheckinTask
import com.dailycheckin.receiver.CheckinActionReceiver
import com.dailycheckin.util.AppLauncher

object NotificationHelper {

    const val CHANNEL_REMINDER = "reminder"
    const val CHANNEL_CONFIRM = "confirm"

    fun ensureChannels(context: Context) {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL_REMINDER, "打卡提醒", NotificationManager.IMPORTANCE_HIGH).apply {
                description = "到点提醒打卡，点击直达目标 APP"
            }
        )
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL_CONFIRM, "打卡确认", NotificationManager.IMPORTANCE_DEFAULT).apply {
                description = "自动检测到打卡成功后的确认提示"
            }
        )
    }

    fun showReminder(context: Context, task: CheckinTask) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val contentIntent = PendingIntent.getActivity(
            context,
            task.id.toInt(),
            AppLauncher.launchTaskIntent(context, task),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val doneIntent = CheckinActionReceiver.markDoneIntent(context, task.id)

        val builder = NotificationCompat.Builder(context, CHANNEL_REMINDER)
            .setSmallIcon(R.drawable.ic_stat_checkin)
            .setContentTitle("该打卡啦：${task.name}")
            .setContentText(task.targetHint.ifBlank { "点击打开 ${task.name} 完成今日打卡" })
            .setContentIntent(contentIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .addAction(0, "我已打卡", doneIntent)

        manager.notify(notificationId(task.id), builder.build())
    }

    fun showAutoConfirmed(context: Context, taskName: String) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val builder = NotificationCompat.Builder(context, CHANNEL_CONFIRM)
            .setSmallIcon(R.drawable.ic_stat_checkin)
            .setContentTitle("${taskName} 打卡成功 ✓")
            .setContentText("已自动确认今日打卡完成")
            .setAutoCancel(true)

        manager.notify(notificationId(taskName.hashCode().toLong()), builder.build())
    }

    fun notificationId(taskId: Long): Int = (taskId % Int.MAX_VALUE).toInt()

    /** 每日复查汇总通知（独立固定 ID，避免与任务提醒冲突） */
    private const val REVIEW_NOTIFICATION_ID = 999

    fun showReviewReminder(context: Context, pendingNames: List<String>) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val contentIntent = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val names = pendingNames.joinToString("、")
        val builder = NotificationCompat.Builder(context, CHANNEL_REMINDER)
            .setSmallIcon(R.drawable.ic_stat_checkin)
            .setContentTitle("别错过！还有 ${pendingNames.size} 项没打卡")
            .setContentText("$names —— 过 12 点就没机会了，快去补打！")
            .setContentIntent(contentIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .addAction(0, "打开打卡王", contentIntent)

        manager.notify(REVIEW_NOTIFICATION_ID, builder.build())
    }
}
