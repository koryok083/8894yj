package com.yourcompany.tetheringrouter

import android.app.Service
import android.content.Intent
import android.os.IBinder
import fi.iki.elonen.NanoHTTPD
import java.io.IOException

class WebServerService : Service() {
    private var webServer: WebServer? = null
    private val port = 8080
    
    override fun onCreate() {
        super.onCreate()
        startWebServer()
    }
    
    private fun startWebServer() {
        webServer = WebServer(port)
        try {
            webServer?.start(NanoHTTPD.SOCKET_READ_TIMEOUT, false)
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }
    
    inner class WebServer(port: Int) : NanoHTTPD(port) {
        override fun serve(session: IHTTPSession): Response {
            return when (session.uri) {
                "/" -> newFixedLengthResponse(
                    Response.Status.OK,
                    "text/html",
                    getWebUI("dashboard")
                )
                "/api/status" -> {
                    val json = """{
                        "tethering_active": ${TetheringManager.isActive},
                        "connected_clients": ${TetheringManager.getConnectedClientsCount()},
                        "ssid": "${TetheringManager.getSSID()}",
                        "ip_address": "${NetworkUtils.getLocalIpAddress()}"
                    }"""
                    newFixedLengthResponse(Response.Status.OK, "application/json", json)
                }
                "/api/start" -> {
                    TetheringManager.startTethering()
                    newFixedLengthResponse(Response.Status.OK, "application/json", 
                        """{"status": "starting"}""")
                }
                "/api/stop" -> {
                    TetheringManager.stopTethering()
                    newFixedLengthResponse(Response.Status.OK, "application/json", 
                        """{"status": "stopping"}""")
                }
                else -> newFixedLengthResponse(Response.Status.NOT_FOUND, 
                    "text/plain", "404 Not Found")
            }
        }
    }
    
    private fun getWebUI(page: String): String {
        return try {
            when (page) {
                "dashboard" -> resources.openRawResource(R.raw.webui_dashboard)
                    .bufferedReader().use { it.readText() }
                else -> "<html><body>Page not found</body></html>"
            }
        } catch (e: Exception) {
            "<html><body>Error loading page</body></html>"
        }
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }
    
    override fun onDestroy() {
        webServer?.stop()
        super.onDestroy()
    }
    
    override fun onBind(intent: Intent?): IBinder? = null
}
