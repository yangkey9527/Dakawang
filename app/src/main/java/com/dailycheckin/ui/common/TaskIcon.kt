package com.dailycheckin.ui.common

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap

/**
 * 任务图标：已指定目标 APP 时直接显示其应用图标（圆形裁剪），
 * 未指定包名或应用未安装时回退为"首字圆饼"。
 */
@Composable
fun TaskIcon(
    packageName: String,
    name: String,
    accent: Color,
    size: Dp = 44.dp
) {
    val context = LocalContext.current
    val bitmap = remember(packageName) {
        if (packageName.isBlank()) return@remember null
        runCatching {
            context.packageManager.getApplicationIcon(packageName).toBitmap(128, 128)
        }.getOrNull()
    }

    if (bitmap != null) {
        Box(
            modifier = Modifier
                .size(size)
                .background(accent.copy(alpha = 0.12f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = name,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(size)
                    .clip(CircleShape)
            )
        }
    } else {
        // 回退：首字圆饼
        Box(
            modifier = Modifier
                .size(size)
                .background(accent.copy(alpha = 0.14f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = name.take(1),
                color = accent,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}
