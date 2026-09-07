package com.dailycheckin.data.repo

import android.content.Context
import com.dailycheckin.data.db.AppDatabase
import com.dailycheckin.data.db.CheckinRecord
import com.dailycheckin.data.db.CheckinTask
import com.dailycheckin.util.DateUtils
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

/** 今日看板的展示项：任务 + 当日记录 */
data class HomeTaskItem(
    val task: CheckinTask,
    val record: CheckinRecord?,
    val status: Int
)

class CheckinRepository(context: Context) {

    private val db = AppDatabase.get(context)
    private val taskDao = db.taskDao()
    private val recordDao = db.recordDao()

    // ---------- 任务 ----------

    fun observeTasks(): Flow<List<CheckinTask>> = taskDao.observeAll()

    fun observeTask(id: Long): Flow<CheckinTask?> = taskDao.observeById(id)

    suspend fun getTask(id: Long): CheckinTask? = taskDao.getById(id)

    suspend fun getAllEnabledTasks(): List<CheckinTask> = taskDao.getEnabled()

    suspend fun saveTask(task: CheckinTask): Long = taskDao.upsert(task)

    suspend fun deleteTask(task: CheckinTask) {
        taskDao.delete(task)
        recordDao.deleteByTask(task.id)
    }

    /** 为所有启用任务补齐今天的记录（首次打开 / 跨天时调用） */
    suspend fun ensureTodayRecords(): List<HomeTaskItem> {
        val today = DateUtils.today()
        val tasks = taskDao.getEnabled()
        val existing = recordDao.getByDate(today).associateBy { it.taskId }
        val missing = tasks.filter { existing[it.id] == null }
            .map { CheckinRecord(taskId = it.id, date = today, status = CheckinRecord.STATUS_PENDING) }
        if (missing.isNotEmpty()) recordDao.upsertAll(missing)
        return buildItems(tasks, recordDao.getByDate(today))
    }

    // ---------- 今日看板 ----------

    fun observeToday(): Flow<List<HomeTaskItem>> {
        val today = DateUtils.today()
        return combine(taskDao.observeAll(), recordDao.observeByDate(today)) { tasks, records ->
            buildItems(tasks.filter { it.enabled }, records)
        }
    }

    /** 完整任务列表（含禁用），用于管理页 */
    fun observeAllWithToday(): Flow<List<HomeTaskItem>> {
        val today = DateUtils.today()
        return combine(taskDao.observeAll(), recordDao.observeByDate(today)) { tasks, records ->
            buildItems(tasks, records)
        }
    }

    fun observeTodayStats(): Flow<Pair<Int, Int>> {
        val today = DateUtils.today()
        return combine(
            taskDao.observeAll(),
            recordDao.observeCountByStatus(today, CheckinRecord.STATUS_DONE)
        ) { tasks, done -> tasks.count { it.enabled } to done }
    }

    private fun buildItems(tasks: List<CheckinTask>, records: List<CheckinRecord>): List<HomeTaskItem> {
        val byTask = records.associateBy { it.taskId }
        return tasks.sortedWith(compareBy({ it.sortOrder }, { it.id })).map { task ->
            val record = byTask[task.id]
            HomeTaskItem(task, record, record?.status ?: CheckinRecord.STATUS_PENDING)
        }
    }

    // ---------- 打卡状态 ----------

    suspend fun markDone(taskId: Long, note: String? = null) {
        val today = DateUtils.today()
        val rec = recordDao.get(taskId, today)
            ?: CheckinRecord(taskId = taskId, date = today, status = CheckinRecord.STATUS_PENDING)
        recordDao.upsert(rec.copy(status = CheckinRecord.STATUS_DONE, checkinTime = System.currentTimeMillis(), note = note))
    }

    suspend fun markPending(taskId: Long) {
        val today = DateUtils.today()
        val rec = recordDao.get(taskId, today)
            ?: CheckinRecord(taskId = taskId, date = today, status = CheckinRecord.STATUS_PENDING)
        recordDao.upsert(rec.copy(status = CheckinRecord.STATUS_PENDING, checkinTime = null))
    }

    suspend fun markSkipped(taskId: Long) {
        val today = DateUtils.today()
        val rec = recordDao.get(taskId, today)
            ?: CheckinRecord(taskId = taskId, date = today, status = CheckinRecord.STATUS_PENDING)
        recordDao.upsert(rec.copy(status = CheckinRecord.STATUS_SKIPPED))
    }

    /** 无障碍检测：命中关键词后自动标记完成（只读检测，无点击） */
    suspend fun autoConfirmByKeywords(keywords: List<String>): Long? {
        val today = DateUtils.today()
        val tasks = taskDao.getEnabled()
        for (task in tasks) {
            val rec = recordDao.get(task.id, today)
            if (rec?.status == CheckinRecord.STATUS_DONE) continue
            val taskKeywords = task.checkKeywords.split(",").map { it.trim() }.filter { it.isNotEmpty() }
            if (keywords.any { k -> taskKeywords.any { tk -> k.contains(tk) } }) {
                recordDao.upsert(
                    rec?.copy(status = CheckinRecord.STATUS_DONE, checkinTime = System.currentTimeMillis())
                        ?: CheckinRecord(taskId = task.id, date = today, status = CheckinRecord.STATUS_DONE, checkinTime = System.currentTimeMillis())
                )
                return task.id
            }
        }
        return null
    }
}
