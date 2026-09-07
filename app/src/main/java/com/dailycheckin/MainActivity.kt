package com.dailycheckin

import android.Manifest
import android.app.NotificationManager
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.dailycheckin.ui.navigation.AppNavHost
import com.dailycheckin.ui.theme.DailyCheckinTheme

class MainActivity : ComponentActivity() {

    private val permissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // 不使用沉浸式（edge-to-edge）：部分 ROM 上底部导航栏会被系统手势区遮挡导致按钮点击失效，
        // 采用系统标准布局保证底部栏始终完整可点击
        requestNotificationPermissionIfNeeded()
        setContent {
            DailyCheckinTheme {
                AppNavHost()
            }
        }
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                this, Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            val enabled = getSystemService(NotificationManager::class.java).areNotificationsEnabled()
            if (!granted && enabled) {
                permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }
}
