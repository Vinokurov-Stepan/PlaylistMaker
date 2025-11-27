package com.practicum.playlistmaker.core.util

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import com.practicum.playlistmaker.R

class NetworkStateBroadcastReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {
        Handler(Looper.getMainLooper()).postDelayed({
            if (intent?.action == "android.net.conn.CONNECTIVITY_CHANGE") {
                if (!isConnected(context)) {
                    Toast.makeText(context, R.string.network_disconnect, Toast.LENGTH_LONG).show()
                }
            }
        }, CHECK_DEBOUNCE_DELAY)
    }

    private fun isConnected(context: Context?): Boolean {
        if (context == null) return false
        val connectivityManager = context.getSystemService(
            Context.CONNECTIVITY_SERVICE
        ) as ConnectivityManager
        val capabilities =
            connectivityManager.getNetworkCapabilities(connectivityManager.activeNetwork)
        if (capabilities != null) {
            return capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) || capabilities.hasTransport(
                NetworkCapabilities.TRANSPORT_WIFI
            ) || capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)
        }
        return false
    }

    companion object {
        private const val CHECK_DEBOUNCE_DELAY = 1_000L
    }
}
