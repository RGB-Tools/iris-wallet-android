package com.iriswallet.ui

import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.iriswallet.utils.AppContainer
import com.iriswallet.utils.TAG
import java.net.InetSocketAddress
import java.net.Socket
import java.net.URI
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit

class ConnectivityService : Service() {

    private val binder = ConnectivityBinder()
    private lateinit var scheduleExecutor: ScheduledFuture<*>

    private val serviceURLs: List<String> by lazy {
        listOf(
            AppContainer.electrumURL,
            AppContainer.proxyURL,
        )
    }

    private var serviceMap: Map<String, Boolean> = mapOf()

    private val _services = MutableLiveData<Map<String, Boolean>>()
    val services: LiveData<Map<String, Boolean>>
        get() = _services

    inner class ConnectivityBinder : Binder() {
        fun getService(): ConnectivityService = this@ConnectivityService
    }

    override fun onBind(intent: Intent): IBinder {
        return binder
    }

    override fun onCreate() {
        super.onCreate()
        val executorService = Executors.newSingleThreadScheduledExecutor()
        scheduleExecutor =
            executorService.scheduleAtFixedRate({ checkConnectivity() }, 0, 5, TimeUnit.SECONDS)
        Log.d(TAG, "Started connectivity check service")
    }

    override fun onDestroy() {
        scheduleExecutor.cancel(true)
        Log.d(TAG, "Stopped connectivity check service")
        super.onDestroy()
        android.os.Process.killProcess(android.os.Process.myPid())
    }

    private fun checkConnectivity() {
        val updatedServiceMap = serviceURLs.associateWith { isReachable(it) }
        if (updatedServiceMap != serviceMap) {
            Log.d(TAG, "Connectivity changed: $updatedServiceMap")
            _services.postValue(updatedServiceMap)
            serviceMap = updatedServiceMap
        }
    }

    private fun isReachable(host: String): Boolean {
        val uri = URI(host)
        val port = if (uri.port != -1) uri.port else if (uri.scheme.equals("https")) 443 else 80
        return try {
            val socket = Socket()
            val socketAddress = InetSocketAddress(uri.host, port)
            socket.use { sock ->
                sock.connect(socketAddress, 2000)
                sock.isConnected
            }
        } catch (e: Exception) {
            false
        }
    }
}
