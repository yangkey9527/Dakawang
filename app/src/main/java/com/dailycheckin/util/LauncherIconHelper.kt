package com.dailycheckin.util

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import com.dailycheckin.data.repo.CheckinRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 动态桌面图标：今日所有启用任务全部完成 → 绿色对勾；否则 → 灰色空心圆环。
 * 通过切换两个 activity-alias 的 enabled 状态实现（类似多邻国）。
 */
object LauncherIconHelper {
    private const val ALIAS_DONE = "com.dailycheckin.IconAllDone"
    private const val ALIAS_PENDING = "com.dailycheckin.IconPending"

    /** 幂等同步：启用任务全部今日 DONE → done 图标；否则（含任务为空）→ pending 图标 */
    suspend fun sync(context: Context) = withContext(Dispatchers.IO) {
        runCatching {
            val repo = CheckinRepository(context)
            val tasks = repo.getAllEnabledTasks()
            val allDone = tasks.isNotEmpty() && repo.countDoneToday() >= tasks.size
            apply(context, allDone)
        }
    }

    private fun apply(context: Context, done: Boolean) {
        val pm = context.packageManager
        val toEnable = if (done) ALIAS_DONE else ALIAS_PENDING
        val toDisable = if (done) ALIAS_PENDING else ALIAS_DONE
        if (!isEnabled(pm, context, toEnable)) {
            pm.setComponentEnabledSetting(
                ComponentName(context, toEnable),
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                PackageManager.DONT_KILL_APP
            )
        }
        if (isEnabled(pm, context, toDisable)) {
            pm.setComponentEnabledSetting(
                ComponentName(context, toDisable),
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                PackageManager.DONT_KILL_APP
            )
        }
    }

    /** 未 set 过（DEFAULT）时回退 manifest 默认：Pending=true, Done=false */
    private fun isEnabled(pm: PackageManager, ctx: Context, name: String): Boolean =
        when (pm.getComponentEnabledSetting(ComponentName(ctx, name))) {
            PackageManager.COMPONENT_ENABLED_STATE_ENABLED -> true
            PackageManager.COMPONENT_ENABLED_STATE_DISABLED -> false
            else -> name == ALIAS_PENDING
        }
}
