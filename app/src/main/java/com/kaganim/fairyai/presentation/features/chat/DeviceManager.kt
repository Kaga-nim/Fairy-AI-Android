package com.kaganim.fairyai.presentation.features.chat

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.BatteryManager
import android.os.Environment
import android.os.StatFs
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DeviceManager @Inject constructor(
    @ApplicationContext private val context: Context
) {

    fun getDeviceInfo(): String {
        val battery = getBatteryStatus()
        val connection = getNetworkStatus()
        val storage = getAvailableStorage()
        val time = LocalDateTime.now().format(DateTimeFormatter.ofPattern("EEEE, dd MMMM yyyy, HH:mm:ss", Locale("id", "ID")))

        return "[STATUS_PERANGKAT: Waktu=$time, Baterai=$battery, Koneksi=$connection, Sisa Storage=$storage]"
    }

    private fun getBatteryStatus(): String {
        val intent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val level = intent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = intent?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
        val status = intent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        
        val pct = if (level != -1 && scale != -1) (level * 100 / scale.toFloat()).toInt() else 0
        val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL
        
        return "$pct% ${if (isCharging) "(Sedang Diisi)" else "(Melepas Kabel)"}"
    }

    private fun getNetworkStatus(): String {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val nw = cm.activeNetwork ?: return "Offline"
        val actNw = cm.getNetworkCapabilities(nw) ?: return "Offline"
        return when {
            actNw.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "WiFi Terhubung"
            actNw.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> "Data Seluler Aktif"
            else -> "Terhubung"
        }
    }

    private fun getAvailableStorage(): String {
        val stat = StatFs(Environment.getDataDirectory().path)
        val bytesAvailable = stat.blockSizeLong * stat.availableBlocksLong
        val gigabytes = bytesAvailable / (1024 * 1024 * 1024).toDouble()
        return String.format("%.2f GB", gigabytes)
    }
}
