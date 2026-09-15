package com.dailycheckin.util

import java.time.LocalDate

/** 单个任务的打卡统计 */
data class TaskStats(val streak: Int, val totalDays: Int)

/**
 * 连续打卡天数：今天 DONE 则从今天往前数，否则从昨天往前数（口径与历史页一致）。
 */
fun computeStreak(doneDates: Set<String>, today: LocalDate = LocalDate.now()): Int {
    var count = 0
    var cursor = today
    if (DateUtils.format(cursor) !in doneDates) cursor = cursor.minusDays(1)
    while (DateUtils.format(cursor) in doneDates) { count++; cursor = cursor.minusDays(1) }
    return count
}

/** 连续 + 累计打卡天数 */
fun computeStats(doneDates: Set<String>, today: LocalDate = LocalDate.now()): TaskStats =
    TaskStats(streak = computeStreak(doneDates, today), totalDays = doneDates.size)
