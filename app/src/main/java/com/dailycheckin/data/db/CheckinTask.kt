package com.dailycheckin.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 打卡任务：一个任务对应一个目标 APP 的每日打卡入口。
 */
@Entity(tableName = "tasks")
data class CheckinTask(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    /** 任务名称，如"红果"、"蚂蚁森林" */
    val name: String,
    /** 目标 APP 包名，如 com.xunmeng.pinduoduo */
    val packageName: String,
    /** 可选深链，优先用深链直达打卡页；为空则直接启动 APP */
    val deepLink: String? = null,
    /** 每日提醒时间 HH:mm */
    val remindTime: String = "09:00",
    /** 是否启用（启用才会提醒与出现在今日看板） */
    val enabled: Boolean = true,
    /** 打卡成功关键词，逗号分隔，供无障碍检测使用 */
    val checkKeywords: String = "已签到,今日已签到,已打卡",
    /** 进入 APP 后打卡入口提示文案 */
    val targetHint: String = "",
    /** 打卡平台：mobile=手机 APP / pc=PC 客户端 / web=网页 */
    val platform: String = PLATFORM_MOBILE,
    /** 界面强调色索引 */
    val colorIndex: Int = 0,
    val sortOrder: Int = 0
) {
    companion object {
        const val PLATFORM_MOBILE = "mobile"
        const val PLATFORM_PC = "pc"
        const val PLATFORM_WEB = "web"
    }
}
