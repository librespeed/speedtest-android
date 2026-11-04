package org.librespeed.speedtest.data

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiInfo
import android.net.wifi.WifiManager
import android.os.Build
import android.telephony.TelephonyManager
import androidx.core.content.ContextCompat

object NetworkInfo {

    /** Best-effort description like "Wi-Fi 6", "Ethernet", "5G" or "Cellular"; null when unknown. */
    fun describe(context: Context): String? {
        return try {
            val connectivity = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val capabilities = connectivity.getNetworkCapabilities(connectivity.activeNetwork) ?: return null
            when {
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> wifiName(context, capabilities)
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> "Ethernet"
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> cellularName(context)
                else -> null
            }
        } catch (_: Exception) {
            null
        }
    }

    fun isCellular(context: Context): Boolean = try {
        val connectivity = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        connectivity.getNetworkCapabilities(connectivity.activeNetwork)
            ?.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) == true
    } catch (_: Exception) {
        false
    }

    /** Needs READ_PHONE_STATE for the generation; falls back to plain "Cellular" without it. */
    private fun cellularName(context: Context): String {
        return try {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE)
                != PackageManager.PERMISSION_GRANTED
            ) return "Cellular"
            val telephony = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
            when (telephony.dataNetworkType) {
                TelephonyManager.NETWORK_TYPE_NR -> "5G"
                TelephonyManager.NETWORK_TYPE_LTE -> "4G LTE"
                TelephonyManager.NETWORK_TYPE_HSPAP, TelephonyManager.NETWORK_TYPE_HSPA,
                TelephonyManager.NETWORK_TYPE_HSDPA, TelephonyManager.NETWORK_TYPE_HSUPA,
                TelephonyManager.NETWORK_TYPE_UMTS -> "3G"
                TelephonyManager.NETWORK_TYPE_EDGE, TelephonyManager.NETWORK_TYPE_GPRS -> "2G"
                else -> "Cellular"
            }
        } catch (_: Exception) {
            "Cellular"
        }
    }

    /** Cellular extras like "T-Mobile CZ · signal 3/4"; null on other transports or when unavailable. */
    fun detail(context: Context): String? {
        return try {
            val connectivity = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val capabilities = connectivity.getNetworkCapabilities(connectivity.activeNetwork) ?: return null
            if (!capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)) return null
            val telephony = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
            val operator = telephony.networkOperatorName?.takeIf { it.isNotBlank() }
            val signal = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                telephony.signalStrength?.level?.let { "signal $it/4" }
            } else {
                null
            }
            listOfNotNull(operator, signal).joinToString(" · ").ifEmpty { null }
        } catch (_: Exception) {
            null
        }
    }

    private fun wifiName(context: Context, capabilities: NetworkCapabilities): String {
        val standard = try {
            val info = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                capabilities.transportInfo as? WifiInfo
            } else {
                null
            } ?: @Suppress("DEPRECATION") run {
                (context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager)?.connectionInfo
            }
            if (info != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) info.wifiStandard else 0
        } catch (_: Exception) {
            0
        }
        return when (standard) {
            8 -> "Wi-Fi 7"  //ScanResult.WIFI_STANDARD_11BE
            6 -> "Wi-Fi 6"  //WIFI_STANDARD_11AX
            5 -> "Wi-Fi 5"  //WIFI_STANDARD_11AC
            4 -> "Wi-Fi 4"  //WIFI_STANDARD_11N
            else -> "Wi-Fi"
        }
    }

}
