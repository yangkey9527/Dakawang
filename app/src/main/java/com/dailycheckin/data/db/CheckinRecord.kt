package com.dailycheckin.data.db

import androidx.room.Entity
import androidx.room.Index

/**
 * 打卡记录：按"任务 + 日期"唯一，记录当天该任务的打卡状态。
 */
@Entity(
    tableName = "records",
    primaryKeys = ["taskId", "date"],
    indices = [Index(value = ["date"])]
)
data class CheckinRecord(
    val taskId: Long,
    /** 日期 yyyy-MM-dd */
    val date: String,
    /** 0=待打卡 1=已打卡 2=已跳过/错过 */
    val status: Int = STATUS_PENDING,
    /** 实际打卡时间戳 */
    val checkinTime: Long? = null,
    val note: String? = null
) {
    companion object {
        const val STATUS_PENDING = 0
        const val STATUS_DONE = 1
        const val STATUS_SKIPPED = 2
    }
}
