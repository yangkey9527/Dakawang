package com.dailycheckin.accessibility

import android.accessibilityservice.AccessibilityService
import android.os.SystemClock
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.dailycheckin.data.db.CheckinTask
import com.dailycheckin.data.repo.CheckinRepository
import com.dailycheckin.data.repo.SettingsRepository
import com.dailycheckin.reminder.NotificationHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * 无障碍只读检测：当目标 APP 位于前台时，读取界面文本；
 * 若命中任务的"已签到"类关键词，自动把当日记录标记为已完成。
 * 本服务只读取界面内容，绝不执行任何点击/模拟操作。
 */
class CheckinAccessibilityService : AccessibilityService() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val lastScanAt = HashMap<String, Long>()
    private val scannedTexts = HashSet<String>()

    override fun onServiceConnected() {
        super.onServiceConnected()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        val pkg = event?.packageName?.toString() ?: return
        // 仅扫描目标 APP 的窗口事件
        if (event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED &&
            event.eventType != AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED
        ) return

        // 节流：同一 APP 每 30 秒最多扫描一次
        val now = SystemClock.elapsedRealtime()
        val last = lastScanAt[pkg] ?: 0L
        if (now - last < 30_000L) return
        lastScanAt[pkg] = now

        val root = rootInActiveWindow ?: return
        if (root.packageName?.toString() != pkg) return

        val texts = mutableListOf<String>()
        collectTexts(root, texts)
        if (texts.isEmpty()) return

        scope.launch {
            runCatching {
                val context = this@CheckinAccessibilityService
                val repo = CheckinRepository(context)
                val tasks = repo.getAllEnabledTasks()
                val detectEnabled = SettingsRepository(context).detectEnabled.first()
                if (!detectEnabled || tasks.isEmpty()) return@runCatching

                val matched = tasks.firstOrNull { task ->
                    task.packageName == pkg && task.keywordSet().any { kw ->
                        texts.any { it.contains(kw) }
                    }
                }
                if (matched != null) {
                    repo.markDone(matched.id, note = "无障碍自动确认")
                    NotificationHelper.showAutoConfirmed(context, matched.name)
                }
            }
        }
    }

    private fun collectTexts(node: AccessibilityNodeInfo, out: MutableList<String>) {
        node.text?.toString()?.trim()?.takeIf { it.isNotEmpty() }?.let { out.add(it) }
        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            collectTexts(child, out)
            child.recycle()
        }
    }

    override fun onInterrupt() {}
}

/** 解析逗号分隔的关键词 */
private fun CheckinTask.keywordSet(): Set<String> =
    checkKeywords.split(",").map { it.trim() }.filter { it.isNotEmpty() }.toSet()
