package com.dailycheckin.util

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter

object DateUtils {

    private val dateFmt = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    private val timeFmt = DateTimeFormatter.ofPattern("HH:mm")

    fun today(): String = LocalDate.now().format(dateFmt)

    fun formatDate(date: String): String = date

    fun formatDateTime(ts: Long): String =
        LocalDateTime.ofEpochSecond(ts / 1000, 0, java.time.ZoneOffset.systemDefault().rules.getOffset(java.time.Instant.ofEpochMilli(ts)))
            .format(DateTimeFormatter.ofPattern("HH:mm"))

    /** "HH:mm" -> LocalTime，非法输入回退 09:00 */
    fun parseTime(hhmm: String): LocalTime =
        runCatching { LocalTime.parse(hhmm, timeFmt) }.getOrDefault(LocalTime.of(9, 0))

    fun isValidTime(hhmm: String): Boolean =
        runCatching { LocalTime.parse(hhmm, timeFmt) }.isSuccess

    /** 今天 hh:mm 对应的时间戳；若已过则返回明天的同一时刻（用于一次性提醒调度） */
    fun nextTimeMillis(hhmm: String): Long {
        val time = parseTime(hhmm)
        var dt = LocalDateTime.of(LocalDate.now(), time)
        if (dt.isBefore(LocalDateTime.now())) dt = dt.plusDays(1)
        return dt.atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
    }
}
