package com.yourcompany.tetheringrouter

import android.app.*
import android.content.Context
import android.content.Intent
import android.net.*
import android.net.wifi.WifiManager
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import java.lang.reflect.Method

class TetheringService : Service() {
    private lateinit var connectivityManager: ConnectivityManager
    private lateinit var wifiManager: WifiManager
    private var isTetheringActive = false
    
    companion object {
        const val ACTION_START_TETHERING = "START_TETHERING"
        const val ACTION_STOP_TETHERING = "STOP_TETHERING"
        const val BROADCAST_TETHERING_STATUS = "TETHERING_STATUS"
        const val EXTRA_IS_ACTIVE = "IS_ACTIVE"
        const val EXTRA_CLIENTS_COUNT = "CLIENTS_COUNT"
    }
    
    override fun onCreate() {
        super.onCreate()
        connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        wifiManager = applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        createNotificationChannel()
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START_TETHERING -> startTethering()
            ACTION_STOP_TETHERING -> stopTethering()
        }
        return START_STICKY
    }
    
    private fun startTethering() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                connectivityManager.startTethering(
                    ConnectivityManager.TETHERING_WIFI,
                    true,
                    object : ConnectivityManager.OnTetheringStartedCallback() {
                        override fun onTetheringStarted() {
                            isTetheringActive = true
                            updateStatus()
                            startForegroundService()
                        }
                    }
                )
            } else {
                // For older versions using reflection
                setWifiTetheringEnabled(true)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    private fun stopTethering() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                connectivityManager.stopTethering(ConnectivityManager.TETHERING_WIFI)
            } else {
                setWifiTetheringEnabled(false)
            }
            isTetheringActive = false
            updateStatus()
            stopForeground(true)
            stopSelf()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    @Suppress("DEPRECATION")
    private fun setWifiTetheringEnabled(enable: Boolean) {
        try {
            val method: Method = wifiManager.javaClass.getMethod(
                "setWifiApEnabled",
                android.net.wifi.WifiConfiguration::class.java,
                Boolean::class.javaPrimitiveType
            )
            method.invoke(wifiManager, null, enable)
            isTetheringActive = enable
            updateStatus()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    private fun updateStatus() {
        val intent = Intent(BROADCAST_TETHERING_STATUS).apply {
            putExtra(EXTRA_IS_ACTIVE, isTetheringActive)
            putExtra(EXTRA_CLIENTS_COUNT, getConnectedClientsCount())
        }
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent)
    }
    
    private fun getConnectedClientsCount(): Int {
        return try {
            val method: Method = wifiManager.javaClass.getMethod("getClientList")
            val clients = method.invoke(wifiManager) as? List<*> ?: emptyList<Any>()
            clients.size
        } catch (e: Exception) {
            0
        }
    }
    
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "tethering_channel",
                "Tethering Service",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }
    
    private fun startForegroundService() {
        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE
        )
        
        val notification = NotificationCompat.Builder(this, "tethering_channel")
            .setContentTitle("Tethering Router Active")
            .setContentText("Device is serving as a modem/router")
            .setSmallIcon(R.drawable.ic_network)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
        
        startForeground(1, notification)
    }
    
    override fun onBind(intent: Intent?): IBinder? = null
}
