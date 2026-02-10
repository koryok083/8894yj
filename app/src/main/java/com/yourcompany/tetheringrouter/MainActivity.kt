package com.yourcompany.tetheringrouter

import android.content.Intent
import android.content.IntentFilter
import android.net.wifi.WifiManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.yourcompany.tetheringrouter.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        setupUI()
        setupBroadcastReceiver()
        checkPermissions()
    }
    
    private fun setupUI() {
        binding.btnStart.setOnClickListener {
            startTetheringService()
            startWebServer()
        }
        
        binding.btnStop.setOnClickListener {
            stopTetheringService()
        }
        
        binding.btnOpenWebUI.setOnClickListener {
            openWebUI()
        }
    }
    
    private fun startTetheringService() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(Intent(this, TetheringService::class.java).apply {
                action = TetheringService.ACTION_START_TETHERING
            })
        } else {
            startService(Intent(this, TetheringService::class.java).apply {
                action = TetheringService.ACTION_START_TETHERING
            })
        }
        Toast.makeText(this, "Starting tethering...", Toast.LENGTH_SHORT).show()
    }
    
    private fun stopTetheringService() {
        startService(Intent(this, TetheringService::class.java).apply {
            action = TetheringService.ACTION_STOP_TETHERING
        })
    }
    
    private fun startWebServer() {
        startService(Intent(this, WebServerService::class.java))
    }
    
    private fun openWebUI() {
        val ip = NetworkUtils.getLocalIpAddress()
        val url = "http://$ip:8080"
        val intent = Intent(Intent.ACTION_VIEW).apply {
            data = android.net.Uri.parse(url)
        }
        startActivity(intent)
    }
    
    private fun setupBroadcastReceiver() {
        LocalBroadcastManager.getInstance(this).registerReceiver(
            broadcastReceiver,
            IntentFilter(TetheringService.BROADCAST_TETHERING_STATUS)
        )
    }
    
    private val broadcastReceiver = object : android.content.BroadcastReceiver() {
        override fun onReceive(context: android.content.Context?, intent: Intent?) {
            if (intent?.action == TetheringService.BROADCAST_TETHERING_STATUS) {
                val isActive = intent.getBooleanExtra(
                    TetheringService.EXTRA_IS_ACTIVE, false
                )
                val clientsCount = intent.getIntExtra(
                    TetheringService.EXTRA_CLIENTS_COUNT, 0
                )
                
                updateUI(isActive, clientsCount)
            }
        }
    }
    
    private fun updateUI(isActive: Boolean, clientsCount: Int) {
        binding.statusText.text = if (isActive) "Active" else "Inactive"
        binding.clientsText.text = "Connected: $clientsCount"
        binding.btnStart.isEnabled = !isActive
        binding.btnStop.isEnabled = isActive
    }
    
    private fun checkPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            requestPermissions(
                arrayOf(
                    android.Manifest.permission.ACCESS_WIFI_STATE,
                    android.Manifest.permission.CHANGE_WIFI_STATE,
                    android.Manifest.permission.ACCESS_NETWORK_STATE,
                    android.Manifest.permission.INTERNET,
                    android.Manifest.permission.FOREGROUND_SERVICE
                ),
                100
            )
        }
    }
    
    override fun onDestroy() {
        LocalBroadcastManager.getInstance(this).unregisterReceiver(broadcastReceiver)
        super.onDestroy()
    }
}
