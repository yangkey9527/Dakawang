package com.dailycheckin.data.model

import com.dailycheckin.data.db.CheckinTask

/**
 * 预置打卡模板：从模板库一键创建任务。
 * 包名基于公开信息整理，若与真机安装版本不符，可在编辑页通过"从已安装应用选择"修正。
 */
data class PresetTemplate(
    val name: String,
    val packageName: String,
    val deepLink: String?,
    val defaultTime: String,
    val checkKeywords: List<String>,
    val targetHint: String,
    val colorIndex: Int,
    val description: String,
    /** 打卡平台，默认手机 APP */
    val platform: String = CheckinTask.PLATFORM_MOBILE
)

object PresetTemplates {

    val all: List<PresetTemplate> = listOf(
        PresetTemplate(
            name = "红果短剧",
            packageName = "",
            deepLink = null,
            defaultTime = "09:00",
            checkKeywords = listOf("已签到", "今日已签到", "签到成功"),
            targetHint = "打开 APP 后点击底部「我的」，找到「每日签到」入口",
            colorIndex = 4,
            description = "红果免费短剧 / 小说，签到领金币"
        ),
        PresetTemplate(
            name = "拼多多",
            packageName = "com.xunmeng.pinduoduo",
            deepLink = null,
            defaultTime = "09:30",
            checkKeywords = listOf("已签到", "打卡成功", "今日已打卡"),
            targetHint = "打开 APP 后进入「个人中心」，点击签到/领现金入口",
            colorIndex = 3,
            description = "拼多多每日签到领现金"
        ),
        PresetTemplate(
            name = "多邻国",
            packageName = "com.duolingo",
            deepLink = null,
            defaultTime = "10:00",
            checkKeywords = listOf("已学习", "今日已学", "连胜"),
            targetHint = "打开 APP 完成一课学习即视为打卡",
            colorIndex = 1,
            description = "多邻国每日学习打卡，保住连胜"
        ),
        PresetTemplate(
            name = "蚂蚁森林",
            packageName = "com.eg.android.AlipayGphone",
            deepLink = "alipays://platformapi/startapp?appId=60000048",
            defaultTime = "08:30",
            checkKeywords = listOf("能量", "已收取", "蚂蚁森林"),
            targetHint = "深链直达蚂蚁森林，收取能量即视为打卡",
            colorIndex = 2,
            description = "支付宝蚂蚁森林每日收能量"
        ),
        PresetTemplate(
            name = "抖音极速版",
            packageName = "com.ss.android.ugc.aweme.lite",
            deepLink = null,
            defaultTime = "09:00",
            checkKeywords = listOf("已签到", "签到成功", "今日已签到"),
            targetHint = "打开 APP 后点击底部「我的」，找到签到/金币入口",
            colorIndex = 3,
            description = "抖音极速版每日签到领金币，可提现"
        ),
        PresetTemplate(
            name = "今日头条极速版",
            packageName = "com.ss.android.article.lite",
            deepLink = null,
            defaultTime = "09:00",
            checkKeywords = listOf("已签到", "签到成功", "今日已签到"),
            targetHint = "打开 APP 后进入「我的」，点击签到/金币入口",
            colorIndex = 2,
            description = "今日头条极速版看资讯签到领金币"
        ),
        PresetTemplate(
            name = "番茄免费小说",
            packageName = "com.dragon.read",
            deepLink = null,
            defaultTime = "09:30",
            checkKeywords = listOf("已签到", "签到成功", "今日已签到"),
            targetHint = "打开 APP 后进入「我的」，找到签到/金币福利入口",
            colorIndex = 4,
            description = "番茄小说阅读签到领金币，可提现"
        ),
        PresetTemplate(
            name = "快手极速版",
            packageName = "com.kuaishou.nebula",
            deepLink = null,
            defaultTime = "10:00",
            checkKeywords = listOf("已签到", "签到成功", "今日已签到"),
            targetHint = "打开 APP 后进入「我的」，点击签到领现金入口",
            colorIndex = 5,
            description = "快手极速版看视频签到领现金"
        ),
        PresetTemplate(
            name = "吉利汽车",
            packageName = "",
            deepLink = null,
            defaultTime = "09:00",
            checkKeywords = listOf("已签到", "今日已签到", "签到成功"),
            targetHint = "打开 APP 后进入「我的」，找到签到/积分入口",
            colorIndex = 0,
            description = "吉利汽车 APP 签到领积分（包名请手动选择）"
        ),
        PresetTemplate(
            name = "WorkBuddy（PC）",
            packageName = "",
            deepLink = null,
            defaultTime = "21:00",
            checkKeywords = listOf("已完成", "打卡"),
            targetHint = "在电脑上打开 WorkBuddy 完成当日任务打卡",
            colorIndex = 5,
            platform = CheckinTask.PLATFORM_PC,
            description = "PC 客户端每日任务打卡（需在电脑上操作）"
        )
    )
}
