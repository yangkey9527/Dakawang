package com.dailycheckin.ui.edit

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.AccessTime
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Apps
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TimePickerState
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.viewmodel.compose.viewModel
import com.dailycheckin.data.db.CheckinTask
import com.dailycheckin.ui.common.TaskIcon
import com.dailycheckin.ui.theme.TaskColors
import com.dailycheckin.ui.theme.taskColor
import com.dailycheckin.util.AppLauncher
import com.dailycheckin.util.DateUtils
import com.dailycheckin.util.InstalledApp
import java.time.LocalTime

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskEditScreen(
    taskId: Long,
    onBack: () -> Unit,
    viewModel: TaskEditViewModel = viewModel(factory = TaskEditViewModel.Factory)
) {
    var showTimePicker by remember { mutableStateOf(false) }
    var showAppPicker by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (viewModel.isNew) "添加任务" else "编辑任务") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    if (!viewModel.isNew) {
                        IconButton(onClick = { viewModel.delete(onBack) }) {
                            Icon(Icons.Outlined.Delete, contentDescription = "删除")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            OutlinedTextField(
                value = viewModel.name,
                onValueChange = { viewModel.name = it },
                label = { Text("任务名称") },
                placeholder = { Text("如：红果、蚂蚁森林") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            if (viewModel.error != null) {
                Text(
                    text = viewModel.error.orEmpty(),
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            Spacer(Modifier.height(12.dp))

            OutlinedTextField(
                value = viewModel.packageName,
                onValueChange = { viewModel.packageName = it },
                label = { Text("目标应用包名") },
                placeholder = { Text("如 com.xunmeng.pinduoduo") },
                singleLine = true,
                trailingIcon = {
                    TextButton(onClick = { showAppPicker = true }) {
                        Icon(Icons.Outlined.Apps, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("选择应用")
                    }
                },
                supportingText = { Text("选择已安装应用后自动填充；留空则点击通知只打开本应用") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(12.dp))

            Text("打卡平台", style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(
                    CheckinTask.PLATFORM_MOBILE to "手机 APP",
                    CheckinTask.PLATFORM_PC to "PC 客户端",
                    CheckinTask.PLATFORM_WEB to "网页"
                ).forEach { (value, label) ->
                    FilterChip(
                        selected = viewModel.platform == value,
                        onClick = { viewModel.platform = value },
                        label = { Text(label) }
                    )
                }
            }
            if (viewModel.platform != CheckinTask.PLATFORM_MOBILE) {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "PC/网页任务无需包名与直达链接，提醒时提示在电脑或浏览器上操作",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(Modifier.height(12.dp))

            OutlinedTextField(
                value = viewModel.deepLink,
                onValueChange = { viewModel.deepLink = it },
                label = { Text("直达链接（可选）") },
                placeholder = { Text("如 alipays://platformapi/startapp?appId=60000048") },
                singleLine = true,
                supportingText = { Text("填写后可直达打卡页；留空则打开应用首页") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(12.dp))

            OutlinedTextField(
                value = viewModel.remindTime,
                onValueChange = { if (DateUtils.isValidTime(it)) viewModel.remindTime = it },
                label = { Text("每日提醒时间") },
                singleLine = true,
                readOnly = true,
                trailingIcon = {
                    IconButton(onClick = { showTimePicker = true }) {
                        Icon(Icons.Outlined.AccessTime, contentDescription = "选择时间")
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(12.dp))

            OutlinedTextField(
                value = viewModel.targetHint,
                onValueChange = { viewModel.targetHint = it },
                label = { Text("打卡入口提示（显示在通知里）") },
                placeholder = { Text("如：进入后点底部「我的」-「每日签到」") },
                minLines = 2,
                maxLines = 3,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(12.dp))

            OutlinedTextField(
                value = viewModel.checkKeywords,
                onValueChange = { viewModel.checkKeywords = it },
                label = { Text("打卡成功关键词（逗号分隔）") },
                placeholder = { Text("已签到,今日已签到,已打卡") },
                singleLine = true,
                supportingText = { Text("自动确认检测：在目标应用看到这些文字即标记完成") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(16.dp))

            Text("任务颜色", style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                TaskColors.forEachIndexed { index, color ->
                    val selected = viewModel.colorIndex == index
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(color)
                            .border(
                                width = if (selected) 3.dp else 0.dp,
                                color = if (selected) MaterialTheme.colorScheme.onSurface else Color.Transparent,
                                shape = CircleShape
                            )
                            .clickable { viewModel.colorIndex = index }
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("启用任务", style = MaterialTheme.typography.titleSmall)
                    Text(
                        "关闭后不再提醒，也不出现在今日看板",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = viewModel.enabled,
                    onCheckedChange = { viewModel.enabled = it }
                )
            }

            Spacer(Modifier.height(24.dp))

            Button(
                onClick = { viewModel.save(onBack) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                Text("保存")
            }
            Spacer(Modifier.height(24.dp))
        }
    }

    if (showTimePicker) {
        TimePickerDialog(
            initial = DateUtils.parseTime(viewModel.remindTime),
            onConfirm = { viewModel.remindTime = it.format(java.time.format.DateTimeFormatter.ofPattern("HH:mm")); showTimePicker = false },
            onDismiss = { showTimePicker = false }
        )
    }

    if (showAppPicker) {
        InstalledAppsDialog(
            onSelect = { app ->
                viewModel.packageName = app.packageName
                if (viewModel.name.isBlank()) viewModel.name = app.name
                showAppPicker = false
            },
            onDismiss = { showAppPicker = false }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimePickerDialog(
    initial: LocalTime,
    onConfirm: (LocalTime) -> Unit,
    onDismiss: () -> Unit
) {
    var state by remember {
        mutableStateOf(TimePickerState(initial.hour, initial.minute, is24Hour = true))
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("选择提醒时间") },
        text = {
            TimePicker(state = state)
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(LocalTime.of(state.hour, state.minute)) }) {
                Text("确定")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    )
}

@Composable
private fun InstalledAppsDialog(
    onSelect: (InstalledApp) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val apps = remember { AppLauncher.installedApps(context) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("选择已安装应用") },
        text = {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(360.dp)
            ) {
                items(apps, key = { it.packageName }) { app ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { onSelect(app) }
                            .padding(horizontal = 8.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            bitmap = app.icon.toBitmap(64, 64).asImageBitmap(),
                            contentDescription = null,
                            tint = Color.Unspecified,
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text(app.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                            Text(
                                app.packageName,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    )
}
