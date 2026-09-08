package com.dailycheckin.data.backup

import android.content.Context
import com.dailycheckin.data.db.AppDatabase
import com.dailycheckin.data.db.CheckinRecord
import com.dailycheckin.data.db.CheckinTask
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

/**
 * 打卡数据备份：导出全部任务与打卡记录为 JSON，支持导入恢复。
 * 导入采用"合并"策略：任务按名称合并（同名不重复建），记录按 任务+日期 去重（已存在跳过）。
 */
object BackupManager {

    private const val FORMAT_VERSION = 1

    suspend fun exportJson(context: Context): String = withContext(Dispatchers.IO) {
        val db = AppDatabase.get(context)
        val tasks = db.taskDao().getAll()
        val records = db.recordDao().getAll()

        val root = JSONObject()
        root.put("app", "DakaWang")
        root.put("version", FORMAT_VERSION)
        root.put("exportedAt", System.currentTimeMillis())

        val tasksArr = JSONArray()
        tasks.forEach { t ->
            tasksArr.put(JSONObject().apply {
                put("id", t.id)
                put("name", t.name)
                put("packageName", t.packageName)
                put("deepLink", t.deepLink ?: "")
                put("remindTime", t.remindTime)
                put("enabled", t.enabled)
                put("checkKeywords", t.checkKeywords)
                put("targetHint", t.targetHint)
                put("platform", t.platform)
                put("colorIndex", t.colorIndex)
                put("sortOrder", t.sortOrder)
            })
        }
        root.put("tasks", tasksArr)

        val recordsArr = JSONArray()
        records.forEach { r ->
            recordsArr.put(JSONObject().apply {
                put("taskId", r.taskId)
                put("date", r.date)
                put("status", r.status)
                put("checkinTime", r.checkinTime ?: JSONObject.NULL)
                put("note", r.note ?: JSONObject.NULL)
            })
        }
        root.put("records", recordsArr)

        root.toString(2)
    }

    /** 返回 (导入任务数, 导入记录数) */
    suspend fun importJson(context: Context, json: String): Pair<Int, Int> =
        withContext(Dispatchers.IO) {
            val root = JSONObject(json)
            if (root.optInt("version", -1) != FORMAT_VERSION) {
                throw IllegalArgumentException("备份文件版本不受支持")
            }

            val db = AppDatabase.get(context)
            val taskDao = db.taskDao()
            val recordDao = db.recordDao()

            val existing = taskDao.getAll().associateBy { it.name }
            val idMap = mutableMapOf<Long, Long>() // 备份内 taskId -> 本机 taskId
            var taskCount = 0

            val tasksArr = root.optJSONArray("tasks") ?: JSONArray()
            for (i in 0 until tasksArr.length()) {
                val o = tasksArr.getJSONObject(i)
                val fileId = o.optLong("id")
                val name = o.optString("name").trim()
                if (name.isEmpty()) continue

                val task = existing[name] ?: CheckinTask(
                    name = name,
                    packageName = o.optString("packageName"),
                    deepLink = o.optString("deepLink").ifBlank { null },
                    remindTime = o.optString("remindTime", "09:00"),
                    enabled = o.optBoolean("enabled", true),
                    checkKeywords = o.optString("checkKeywords", "已签到,今日已签到,已打卡"),
                    targetHint = o.optString("targetHint"),
                    platform = o.optString("platform", CheckinTask.PLATFORM_MOBILE),
                    colorIndex = o.optInt("colorIndex", 0),
                    sortOrder = o.optInt("sortOrder", 0)
                )
                if (task.id == 0L) {
                    idMap[fileId] = taskDao.upsert(task)
                    taskCount++
                } else {
                    idMap[fileId] = task.id
                }
            }

            var recordCount = 0
            val recordsArr = root.optJSONArray("records") ?: JSONArray()
            for (i in 0 until recordsArr.length()) {
                val o = recordsArr.getJSONObject(i)
                val taskId = idMap[o.optLong("taskId")] ?: continue
                val date = o.optString("date")
                if (date.isEmpty()) continue
                if (recordDao.get(taskId, date) != null) continue

                recordDao.upsert(
                    CheckinRecord(
                        taskId = taskId,
                        date = date,
                        status = o.optInt("status", CheckinRecord.STATUS_DONE),
                        checkinTime = if (o.isNull("checkinTime")) null else o.optLong("checkinTime"),
                        note = if (o.isNull("note")) null else o.optString("note")
                    )
                )
                recordCount++
            }

            taskCount to recordCount
        }
}
