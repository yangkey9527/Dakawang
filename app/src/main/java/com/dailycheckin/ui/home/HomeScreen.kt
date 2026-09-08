package com.dailycheckin.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AddCircleOutline
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.dailycheckin.data.db.CheckinRecord
import com.dailycheckin.data.db.CheckinTask
import com.dailycheckin.data.repo.HomeTaskItem
import com.dailycheckin.ui.common.TaskIcon
import com.dailycheckin.ui.theme.taskColor
import com.dailycheckin.util.DateUtils

@Composable
fun HomeScreen(
    onAddTask: () -> Unit,
    onEditTask: (Long) -> Unit,
    onHistory: (Long) -> Unit,
    viewModel: HomeViewModel = viewModel()
) {
    val items by viewModel.items.collectAsState()
    val stats by viewModel.stats.collectAsState()

    Column(modifier = Modifier.fillMaxSize()) {
        HomeHeader(done = stats.second, total = stats.first, onAddTask = onAddTask)

        if (items.isEmpty()) {
            EmptyState(onAddTask = onAddTask)
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(items, key = { it.task.id }) { item ->
                    TaskCard(
                        item = item,
                        overdue = viewModel.isReminderOverdue(item),
                        onLaunch = { viewModel.launch(item) },
                        onMarkDone = { viewModel.markDone(item) },
                        onMarkPending = { viewModel.markPending(item) },
                        onMarkSkipped = { viewModel.markSkipped(item) },
                        onEdit = { onEditTask(item.task.id) },
                        onHistory = { onHistory(item.task.id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun HomeHeader(done: Int, total: Int, onAddTask: () -> Unit) {
    Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp)) {
        Text(
            text = "今日打卡",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "${DateUtils.today()} · 已完成 $done / $total",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f)
            )
            TextButton(onClick = onAddTask) {
                Icon(Icons.Outlined.AddCircleOutline, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(4.dp))
                Text("添加任务")
            }
        }
    }
}

@Composable
private fun EmptyState(onAddTask: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Outlined.RadioButtonUnchecked,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.outlineVariant,
            modifier = Modifier.size(64.dp)
        )
        Spacer(Modifier.height(16.dp))
        Text("还没有打卡任务", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))
        Text(
            "从模板库一键添加红果、拼多多、蚂蚁森林等，\n或自定义任意 APP",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(16.dp))
        FilledTonalButton(onClick = onAddTask) {
            Text("去模板库添加")
        }
    }
}

@Composable
private fun TaskCard(
    item: HomeTaskItem,
    overdue: Boolean,
    onLaunch: () -> Unit,
    onMarkDone: () -> Unit,
    onMarkPending: () -> Unit,
    onMarkSkipped: () -> Unit,
    onEdit: () -> Unit,
    onHistory: () -> Unit
) {
    val task = item.task
    val done = item.status == CheckinRecord.STATUS_DONE
    val skipped = item.status == CheckinRecord.STATUS_SKIPPED
    val accent = taskColor(task.colorIndex)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onEdit),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // 任务图标：有目标 APP 时显示应用图标，否则首字圆饼
                TaskIcon(
                    packageName = task.packageName,
                    name = task.name,
                    accent = accent
                )
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = task.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Medium
                        )
                        if (task.platform != CheckinTask.PLATFORM_MOBILE) {
                            Spacer(Modifier.width(6.dp))
                            PlatformBadge(task.platform)
                        }
                    }
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = statusText(item, overdue),
                        style = MaterialTheme.typography.bodySmall,
                        color = if (overdue) MaterialTheme.colorScheme.error
                        else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = onHistory) {
                    Icon(
                        Icons.Outlined.CalendarMonth,
                        contentDescription = "查看打卡历史",
                        tint = MaterialTheme.colorScheme.outline,
                        modifier = Modifier.size(20.dp)
                    )
                }
                StatusIcon(done = done, skipped = skipped, accent = accent)
            }

            Spacer(Modifier.height(12.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (done) {
                    OutlinedButton(onClick = onMarkPending, modifier = Modifier.weight(1f)) {
                        Text("撤销")
                    }
                } else {
                    FilledTonalButton(onClick = onLaunch, modifier = Modifier.weight(1f)) {
                        Text("去打卡")
                    }
                    if (skipped) {
                        OutlinedButton(onClick = onMarkPending, modifier = Modifier.weight(1f)) {
                            Text("恢复")
                        }
                    } else {
                        OutlinedButton(onClick = onMarkDone, modifier = Modifier.weight(1f)) {
                            Text("标记完成")
                        }
                    }
                }
                TextButton(
                    onClick = if (skipped || done) onMarkPending else onMarkSkipped,
                    modifier = Modifier.weight(0.6f)
                ) {
                    Icon(Icons.Outlined.MoreHoriz, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(2.dp))
                    Text(if (skipped || done) "改回" else "跳过")
                }
            }
        }
    }
}

@Composable
private fun PlatformBadge(platform: String) {
    val text = when (platform) {
        CheckinTask.PLATFORM_PC -> "PC"
        CheckinTask.PLATFORM_WEB -> "网页"
        else -> return
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
private fun StatusIcon(done: Boolean, skipped: Boolean, accent: Color) {
    when {
        done -> Icon(
            Icons.Outlined.CheckCircle,
            contentDescription = "已打卡",
            tint = accent,
            modifier = Modifier.size(24.dp)
        )
        skipped -> Icon(
            Icons.Outlined.MoreHoriz,
            contentDescription = "已跳过",
            tint = MaterialTheme.colorScheme.outline,
            modifier = Modifier.size(24.dp)
        )
        else -> Icon(
            Icons.Outlined.RadioButtonUnchecked,
            contentDescription = "待打卡",
            tint = MaterialTheme.colorScheme.outlineVariant,
            modifier = Modifier.size(24.dp)
        )
    }
}

private fun statusText(item: HomeTaskItem, overdue: Boolean): String {
    val task = item.task
    return when (item.status) {
        CheckinRecord.STATUS_DONE -> "已打卡 · ${item.record?.checkinTime?.let { DateUtils.formatDateTime(it) } ?: ""}"
        CheckinRecord.STATUS_SKIPPED -> "已跳过 · ${task.remindTime} 提醒"
        else -> if (overdue) "待打卡（已过 ${task.remindTime}）" else "${task.remindTime} 提醒 · 待打卡"
    }
}
