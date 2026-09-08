package com.dailycheckin.ui.history

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.dailycheckin.data.db.CheckinRecord
import com.dailycheckin.data.db.CheckinTask
import com.dailycheckin.ui.common.TaskIcon
import com.dailycheckin.ui.theme.taskColor
import com.dailycheckin.util.DateUtils
import java.time.LocalDate
import java.time.YearMonth

@Composable
fun HistoryScreen(
    onBack: () -> Unit,
    viewModel: HistoryViewModel = viewModel(factory = HistoryViewModel.Factory)
) {
    val task by viewModel.task.collectAsState()
    val records by viewModel.records.collectAsState()
    val recordMap = remember(records) { records.associateBy { it.date } }

    var month by remember { mutableStateOf(YearMonth.now()) }
    val accent = taskColor(task?.colorIndex ?: 0)

    Column(modifier = Modifier.fillMaxSize()) {
        HistoryHeader(task = task, onBack = onBack, accent = accent)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            MonthNavigator(
                month = month,
                onPrev = { month = month.minusMonths(1) },
                onNext = {
                    if (month.isBefore(YearMonth.now())) month = month.plusMonths(1)
                }
            )
            WeekdayHeader()
            MonthGrid(
                month = month,
                recordMap = recordMap,
                accent = accent
            )
            Spacer(Modifier.height(12.dp))
            HistoryStats(recordMap = recordMap, month = month)
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun HistoryHeader(task: CheckinTask?, onBack: () -> Unit, accent: Color) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBack) {
            Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "返回")
        }
        TaskIcon(
            packageName = task?.packageName ?: "",
            name = task?.name ?: "?",
            accent = accent,
            size = 36.dp
        )
        Spacer(Modifier.width(12.dp))
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = task?.name ?: "任务",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium
                )
                val platform = task?.platform
                if (platform != null && platform != CheckinTask.PLATFORM_MOBILE) {
                    Spacer(Modifier.width(6.dp))
                    PlatformBadge(platform)
                }
            }
            Text(
                text = "打卡历史",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun PlatformBadge(platform: String) {
    val text = when (platform) {
        CheckinTask.PLATFORM_PC -> "PC"
        CheckinTask.PLATFORM_WEB -> "网页"
        else -> ""
    }
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier
            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), RoundedCornerShape(4.dp))
            .padding(horizontal = 4.dp, vertical = 1.dp)
    )
}

@Composable
private fun MonthNavigator(month: YearMonth, onPrev: () -> Unit, onNext: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onPrev) {
            Icon(Icons.AutoMirrored.Outlined.KeyboardArrowLeft, contentDescription = "上个月")
        }
        Text(
            text = "${month.year}年${month.monthValue}月",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
            modifier = Modifier.weight(1f)
        )
        IconButton(onClick = onNext, enabled = month.isBefore(YearMonth.now())) {
            Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, contentDescription = "下个月")
        }
    }
}

@Composable
private fun WeekdayHeader() {
    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
        listOf("一", "二", "三", "四", "五", "六", "日").forEach { w ->
            Text(
                text = w,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.outline,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun MonthGrid(
    month: YearMonth,
    recordMap: Map<String, CheckinRecord>,
    accent: Color
) {
    val cells = remember(month) {
        val list = ArrayList<LocalDate?>()
        val offset = month.atDay(1).dayOfWeek.value - 1 // 周一为行首
        repeat(offset) { list.add(null) }
        for (d in 1..month.lengthOfMonth()) {
            list.add(month.atDay(d))
        }
        while (list.size % 7 != 0) list.add(null)
        list
    }
    val today = DateUtils.today()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        cells.chunked(7).forEach { week ->
            Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                week.forEach { date ->
                    DayCell(
                        date = date,
                        record = date?.let { recordMap[it.toString()] },
                        accent = accent,
                        isToday = date?.toString() == today,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun DayCell(
    date: LocalDate?,
    record: CheckinRecord?,
    accent: Color,
    isToday: Boolean,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.aspectRatio(1f).padding(2.dp)) {
        if (date != null) {
            val done = record?.status == CheckinRecord.STATUS_DONE
            val skipped = record?.status == CheckinRecord.STATUS_SKIPPED
            val cellColor = when {
                done -> accent
                skipped -> MaterialTheme.colorScheme.surfaceVariant
                else -> Color.Transparent
            }
            val textColor = when {
                done -> MaterialTheme.colorScheme.onPrimary
                skipped -> MaterialTheme.colorScheme.outline
                else -> MaterialTheme.colorScheme.onSurface
            }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape)
                    .background(cellColor)
                    .then(
                        if (isToday && !done) Modifier.border(
                            1.5.dp,
                            accent,
                            CircleShape
                        ) else Modifier
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = date.dayOfMonth.toString(),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal,
                    color = textColor
                )
            }
        }
    }
}

@Composable
private fun HistoryStats(
    recordMap: Map<String, CheckinRecord>,
    month: YearMonth
) {
    val doneCount = recordMap.values.count { it.status == CheckinRecord.STATUS_DONE }
    val monthDone = recordMap.entries.count {
        it.key.startsWith(month.toString()) && it.value.status == CheckinRecord.STATUS_DONE
    }
    val streak = remember(recordMap) {
        var count = 0
        var cursor = LocalDate.now()
        if (recordMap[cursor.toString()]?.status != CheckinRecord.STATUS_DONE) {
            cursor = cursor.minusDays(1)
        }
        while (recordMap[cursor.toString()]?.status == CheckinRecord.STATUS_DONE) {
            count++
            cursor = cursor.minusDays(1)
        }
        count
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            LegendDot(color = MaterialTheme.colorScheme.primary, label = "已打卡")
            LegendDot(color = MaterialTheme.colorScheme.surfaceVariant, label = "已跳过")
            LegendDot(color = Color.Transparent, label = "未打卡")
        }
        Spacer(Modifier.height(12.dp))
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Row(modifier = Modifier.padding(vertical = 16.dp)) {
                StatItem(label = "本月打卡", value = monthDone, modifier = Modifier.weight(1f))
                StatItem(label = "连续打卡", value = streak, modifier = Modifier.weight(1f))
                StatItem(label = "累计打卡", value = doneCount, modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun LegendDot(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(if (color == Color.Transparent) MaterialTheme.colorScheme.surfaceVariant else color)
                .then(if (color == Color.Transparent) Modifier.border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape) else Modifier)
        )
        Spacer(Modifier.width(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun StatItem(label: String, value: Int, modifier: Modifier = Modifier) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value.toString(),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
