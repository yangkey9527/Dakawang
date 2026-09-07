package com.dailycheckin.util

import android.content.Context
import android.content.Intent
import android.graphics.drawable.Drawable
import android.net.Uri
import android.widget.Toast
import com.dailycheckin.data.db.CheckinTask

data class InstalledApp(
    val name: String,
    val packageName: String,
    val icon: Drawable
)

/**
 * 目标 APP 跳转：优先深链直达打卡页，回退到启动 APP 首页。
 */
object AppLauncher {

    /** 返回是否成功发起跳转 */
    fun launchTask(context: Context, task: CheckinTask): Boolean {
        if (task.platform != CheckinTask.PLATFORM_MOBILE) {
            val where = if (task.platform == CheckinTask.PLATFORM_PC) "电脑端" else "浏览器"
            Toast.makeText(context, "「${task.name}」需在$where 完成打卡", Toast.LENGTH_SHORT).show()
            return false
        }
        val deepLink = task.deepLink?.trim()
        if (!deepLink.isNullOrEmpty()) {
            val ok = runCatching {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(deepLink))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
                true
            }.getOrDefault(false)
            if (ok) return true
        }
        return launchPackage(context, task.packageName)
    }

    /** 供通知 PendingIntent 使用：返回可直接启动的 Intent（优先深链） */
    fun launchTaskIntent(context: Context, task: CheckinTask): Intent {
        val deepLink = task.deepLink?.trim()
        if (!deepLink.isNullOrEmpty()) {
            runCatching {
                return Intent(Intent.ACTION_VIEW, Uri.parse(deepLink))
            }
        }
        val launch = runCatching {
            context.packageManager.getLaunchIntentForPackage(task.packageName)
        }.getOrNull()
        return launch ?: Intent(Intent.ACTION_MAIN)
    }

    fun launchPackage(context: Context, packageName: String): Boolean {
        if (packageName.isBlank()) {
            Toast.makeText(context, "该任务未配置目标应用", Toast.LENGTH_SHORT).show()
            return false
        }
        return runCatching {
            val intent = context.packageManager.getLaunchIntentForPackage(packageName)
            if (intent != null) {
                context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                true
            } else {
                Toast.makeText(context, "未找到应用（可能未安装）", Toast.LENGTH_SHORT).show()
                false
            }
        }.getOrDefault(false)
    }

    /** 查询已安装的可启动应用（用于编辑页"从已安装应用选择"） */
    fun installedApps(context: Context): List<InstalledApp> {
        val pm = context.packageManager
        val intents = pm.queryIntentActivities(
            Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER),
            0
        )
        return intents.mapNotNull { resolve ->
            runCatching {
                InstalledApp(
                    name = resolve.loadLabel(pm).toString(),
                    packageName = resolve.activityInfo.packageName,
                    icon = resolve.loadIcon(pm)
                )
            }.getOrNull()
        }.distinctBy { it.packageName }.sortedBy { it.name }
    }
}
