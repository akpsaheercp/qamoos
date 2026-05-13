package com.example.qamoos.utils

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.qamoos.MainActivity
import com.example.qamoos.R
import com.example.qamoos.data.AppDatabase
import com.example.qamoos.data.UserPreferences
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.ConcurrentHashMap

class DictionaryDownloadService : Service() {
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val activeDownloads = ConcurrentHashMap<String, Job>()
    
    companion object {
        const val CHANNEL_ID = "dictionary_download_channel"
        const val NOTIFICATION_ID = 1001
        
        private val _downloadProgress = MutableStateFlow<Map<String, Float>>(emptyMap())
        val downloadProgress = _downloadProgress.asStateFlow()
        
        private val _downloadingTables = MutableStateFlow<Set<String>>(emptySet())
        val downloadingTables = _downloadingTables.asStateFlow()

        fun startDownload(context: Context, tableName: String) {
            val intent = Intent(context, DictionaryDownloadService::class.java).apply {
                action = "START_DOWNLOAD"
                putExtra("table_name", tableName)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val tableName = intent?.getStringExtra("table_name")
        if (intent?.action == "START_DOWNLOAD" && tableName != null) {
            startDictionaryDownload(tableName)
        }
        
        if (activeDownloads.isEmpty()) {
            stopForeground(true)
            stopSelf()
        } else {
            updateNotification()
        }
        
        return START_NOT_STICKY
    }

    private fun startDictionaryDownload(tableName: String) {
        if (activeDownloads.containsKey(tableName)) return

        val job = serviceScope.launch {
            try {
                _downloadingTables.value = _downloadingTables.value + tableName.lowercase()
                val dictionaryManager = DictionaryManager(this@DictionaryDownloadService)
                
                // Track progress from DictionaryManager
                val progressJob = launch {
                    dictionaryManager.downloadProgress.collect { progressMap ->
                        progressMap[tableName]?.let { progress ->
                            _downloadProgress.value = _downloadProgress.value + (tableName to progress)
                            updateNotification()
                        }
                    }
                }

                dictionaryManager.downloadDictionary(tableName)
                
                // Success: update preferences
                val userPreferences = UserPreferences(this@DictionaryDownloadService)
                userPreferences.toggleDictionary(tableName, true)
                
                progressJob.cancel()
            } catch (e: Exception) {
                android.util.Log.e("DownloadService", "Error downloading $tableName", e)
            } finally {
                activeDownloads.remove(tableName)
                _downloadingTables.value = _downloadingTables.value - tableName.lowercase()
                _downloadProgress.value = _downloadProgress.value - tableName
                
                if (activeDownloads.isEmpty()) {
                    stopForeground(true)
                    stopSelf()
                } else {
                    updateNotification()
                }
            }
        }
        activeDownloads[tableName] = job
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Dictionary Downloads",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows progress of dictionary downloads"
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    private fun updateNotification() {
        val downloadingCount = activeDownloads.size
        if (downloadingCount == 0) return

        val title = if (downloadingCount == 1) {
            "Downloading ${activeDownloads.keys().nextElement()}"
        } else {
            "Downloading $downloadingCount dictionaries"
        }

        val totalProgressValue = if (_downloadProgress.value.isEmpty()) 0f else {
            _downloadProgress.value.values.sum() / downloadingCount
        }

        val contentText = if (totalProgressValue < 0) {
            "Verifying..."
        } else {
            "${totalProgressValue.toInt()}%"
        }

        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent, 
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(contentText)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setProgress(100, if (totalProgressValue < 0) 0 else totalProgressValue.toInt(), totalProgressValue < 0)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIFICATION_ID, notification, android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        serviceScope.cancel()
        super.onDestroy()
    }
}
