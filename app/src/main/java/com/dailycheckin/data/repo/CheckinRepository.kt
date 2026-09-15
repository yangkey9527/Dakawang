package com.dailycheckin.data.repo

import android.content.Context
import com.dailycheckin.data.db.AppDatabase
import com.dailycheckin.data.db.CheckinRecord
import com.dailycheckin.data.db.CheckinTask
import com.dailycheckin.util.DateUtils
import com.dailycheckin.util.LauncherIconHelper
import com.dailycheckin.util.TaskStats
import com.dailycheckin.util.computeStats
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

/** 今日看板的展示项：任务 + 当日记录 + 打卡统计 */
data class HomeTaskItem(
    val task: CheckinTask,
    val record: CheckinRecord?,
    val status: Int,
    val streak: Int = 0,
    val totalDays: Int = 0
)

class CheckinRepository(context: Context) {

    private val db = AppDatabase.get(context)
    private val taskDao = db.taskDao()
    private val recordDao = db.recordDao()
    private val appContext = context.applicationContext

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
        return buildItems(tasks, recordDao.getByDate(today), recordDao.getAll(), today)
    }

    // ---------- 今日看板 ----------

    fun observeToday(): Flow<List<HomeTaskItem>> {
        val today = DateUtils.today()
        return combine(
            taskDao.observeAll(),
            recordDao.observeByDate(today),
            recordDao.observeAll()
        ) { tasks, todayRecords, allRecords ->
            buildItems(tasks.filter { it.enabled }, todayRecords, allRecords, today)
        }
    }

    /** 完整任务列表（含禁用），用于管理页 */
    fun observeAllWithToday(): Flow<List<HomeTaskItem>> {
        val today = DateUtils.today()
        return combine(
            taskDao.observeAll(),
            recordDao.observeByDate(today),
            recordDao.observeAll()
        ) { tasks, todayRecords, allRecords ->
            buildItems(tasks, todayRecords, allRecords, today)
        }
    }

    fun observeTodayStats(): Flow<Pair<Int, Int>> {
        val today = DateUtils.today()
        return combine(
            taskDao.observeAll(),
            recordDao.observeCountByStatus(today, CheckinRecord.STATUS_DONE)
        ) { tasks, done -> tasks.count { it.enabled } to done }
    }

    private fun buildItems(
        tasks: List<CheckinTask>,
        todayRecords: List<CheckinRecord>,
        allRecords: List<CheckinRecord>,
        today: String
    ): List<HomeTaskItem> {
        val byTask = todayRecords.associateBy { it.taskId }
        val statsByTask = allRecords
            .filter { it.status == CheckinRecord.STATUS_DONE }
            .groupBy { it.taskId }
            .mapValues { (_, done) -> computeStats(done.map { it.date }.toSet(), DateUtils.parse(today)) }
        return tasks.sortedWith(compareBy({ it.sortOrder }, { it.id })).map { task ->
            val record = byTask[task.id]
            val s = statsByTask[task.id] ?: TaskStats(0, 0)
            HomeTaskItem(task, record, record?.status ?: CheckinRecord.STATUS_PENDING, s.streak, s.totalDays)
        }
    }

    // ---------- 打卡状态 ----------

    /** 今日已完成数量（用于动态图标判定） */
    suspend fun countDoneToday(): Int =
        recordDao.getByDate(DateUtils.today()).count { it.status == CheckinRecord.STATUS_DONE }

    suspend fun markDone(taskId: Long, note: String? = null) {
        val today = DateUtils.today()
        val rec = recordDao.get(taskId, today)
            ?: CheckinRecord(taskId = taskId, date = today, status = CheckinRecord.STATUS_PENDING)
        recordDao.upsert(rec.copy(status = CheckinRecord.STATUS_DONE, checkinTime = System.currentTimeMillis(), note = note))
        syncLauncherIcon()
    }

    suspend fun markPending(taskId: Long) {
        val today = DateUtils.today()
        val rec = recordDao.get(taskId, today)
            ?: CheckinRecord(taskId = taskId, date = today, status = CheckinRecord.STATUS_PENDING)
        recordDao.upsert(rec.copy(status = CheckinRecord.STATUS_PENDING, checkinTime = null))
        syncLauncherIcon()
    }

    suspend fun markSkipped(taskId: Long) {
        val today = DateUtils.today()
        val rec = recordDao.get(taskId, today)
            ?: CheckinRecord(taskId = taskId, date = today, status = CheckinRecord.STATUS_PENDING)
        recordDao.upsert(rec.copy(status = CheckinRecord.STATUS_SKIPPED))
        syncLauncherIcon()
    }

    /** 写入后同步桌面图标（覆盖 UI 操作与无障碍自动确认路径） */
    private suspend fun syncLauncherIcon() {
        runCatching { LauncherIconHelper.sync(appContext) }
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
