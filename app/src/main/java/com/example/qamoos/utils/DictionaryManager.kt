package com.example.qamoos.utils

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import okhttp3.Call
import okhttp3.Dns
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.dnsoverhttps.DnsOverHttps
import org.json.JSONArray
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.net.InetAddress
import java.util.concurrent.TimeUnit

class DictionaryManager(context: Context) {
    private val TAG = "DictionaryManager"
    private val appContext = context.applicationContext
    
    private val bootstrapClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .build()
        
    private val googleDns = DnsOverHttps.Builder()
        .client(bootstrapClient)
        .url("https://dns.google/dns-query".toHttpUrl())
        .bootstrapDnsHosts(listOf(
            InetAddress.getByName("8.8.8.8"),
            InetAddress.getByName("8.8.4.4"),
            InetAddress.getByName("2001:4860:4860::8888"),
            InetAddress.getByName("2001:4860:4860::8844")
        ))
        .build()

    private val cloudflareDns = DnsOverHttps.Builder()
        .client(bootstrapClient)
        .url("https://cloudflare-dns.com/dns-query".toHttpUrl())
        .bootstrapDnsHosts(listOf(
            InetAddress.getByName("1.1.1.1"),
            InetAddress.getByName("1.0.0.1"),
            InetAddress.getByName("2606:4700:4700::1111"),
            InetAddress.getByName("2606:4700:4700::1001")
        ))
        .build()

    private val aliyunDns = DnsOverHttps.Builder()
        .client(bootstrapClient)
        .url("https://dns.alidns.com/dns-query".toHttpUrl())
        .bootstrapDnsHosts(listOf(
            InetAddress.getByName("223.5.5.5"),
            InetAddress.getByName("223.6.6.6"),
            InetAddress.getByName("2400:3200::1"),
            InetAddress.getByName("2400:3200:baba::1")
        ))
        .build()

    private val quad9Dns = DnsOverHttps.Builder()
        .client(bootstrapClient)
        .url("https://dns.quad9.net/dns-query".toHttpUrl())
        .bootstrapDnsHosts(listOf(
            InetAddress.getByName("9.9.9.9"),
            InetAddress.getByName("149.112.112.112"),
            InetAddress.getByName("2620:fe::fe"),
            InetAddress.getByName("2620:fe::9")
        ))
        .build()

    private val staticDnsMapping = mapOf(
        "github.com" to listOf(
            "140.82.121.3", "140.82.121.4", "140.82.112.3", "140.82.113.3",
            "140.82.114.3", "140.82.112.4", "140.82.113.4", "140.82.114.4"
        ),
        "objects.githubusercontent.com" to listOf(
            "185.199.108.133", "185.199.109.133", "185.199.110.133", "185.199.111.133",
            "2606:50c0:8000::154", "2606:50c0:8001::154", "2606:50c0:8002::154", "2606:50c0:8003::154"
        ),
        "raw.githubusercontent.com" to listOf(
            "185.199.108.133", "185.199.109.133", "185.199.110.133", "185.199.111.133",
            "2606:50c0:8000::154", "2606:50c0:8001::154", "2606:50c0:8002::154", "2606:50c0:8003::154"
        )
    )

    private val compositeDns = object : Dns {
        override fun lookup(hostname: String): List<InetAddress> {
            // Try DoH providers in sequence
            val providers = listOf(
                "Google" to googleDns,
                "Cloudflare" to cloudflareDns,
                "Aliyun" to aliyunDns,
                "Quad9" to quad9Dns
            )

            for ((name, provider) in providers) {
                try {
                    val results = provider.lookup(hostname)
                    if (results.isNotEmpty()) {
                        Log.i(TAG, "$name DoH resolved $hostname to ${results.map { it.hostAddress }}")
                        return results
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "$name DoH failed for $hostname: ${e.message}")
                }
            }
            
            // Try System DNS
            try {
                val results = Dns.SYSTEM.lookup(hostname)
                if (results.isNotEmpty()) return results
            } catch (e: Exception) {
                Log.w(TAG, "System DNS failed for $hostname, trying static fallback")
            }

            // Absolute last resort: Static Mapping
            val staticIps = staticDnsMapping[hostname]
            if (staticIps != null) {
                Log.i(TAG, "Using static fallback IPs for $hostname: $staticIps")
                return staticIps.map { InetAddress.getByName(it) }
            }
            
            throw java.net.UnknownHostException("All resolution methods failed for $hostname")
        }
    }

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .dns(compositeDns)
        .build()
    
    private val baseUrl = "https://github.com/akpsaheercp/qamoos/releases/download/v1.0-databases/"
    private val userAgent = "Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/114.0.0.0 Mobile Safari/537.36"
    
    private val _downloadProgress = MutableStateFlow<Map<String, Float>>(emptyMap())
    val downloadProgress: StateFlow<Map<String, Float>> = _downloadProgress

    private val activeCalls = mutableMapOf<String, Call>()
    private val _isPaused = MutableStateFlow<Map<String, Boolean>>(emptyMap())
    val isPaused: StateFlow<Map<String, Boolean>> = _isPaused

    private val _downloadingTables = MutableStateFlow<Set<String>>(emptySet())
    val downloadingTables: StateFlow<Set<String>> = _downloadingTables

    private fun getDictDir(): File {
        val dbDir = appContext.getDatabasePath("dummy_db").parentFile ?: File(appContext.applicationInfo.dataDir, "databases")
        if (!dbDir.exists()) dbDir.mkdirs()
        val dictDir = File(dbDir, "dictionaries")
        if (!dictDir.exists()) dictDir.mkdirs()
        return dictDir
    }

    fun isDownloaded(tableName: String): Boolean {
        val fileName = "${tableName.lowercase()}.db"
        val file = File(getDictDir(), fileName)
        return file.exists() && file.length() > 1024
    }

    fun deleteDictionary(tableName: String): Boolean {
        val file = File(getDictDir(), "${tableName.lowercase()}.db")
        return if (file.exists()) file.delete() else false
    }

    suspend fun downloadDictionary(tableName: String, retryCount: Int = 3): Unit = withContext(Dispatchers.IO) {
        val fileName = "${tableName.lowercase()}.db"
        val tableLower = tableName.lowercase()
        val dictDir = getDictDir()
        val localFile = File(dictDir, fileName)
        val tempFile = File(dictDir, "$fileName.tmp")
        
        Log.i(TAG, "Attempting to download: $tableName (Attempt: ${4 - retryCount})")

        val downloadedBytes = if (tempFile.exists()) tempFile.length() else 0L
        
        val urls = listOf(
            "$baseUrl$fileName",
            "https://ghproxy.com/$baseUrl$fileName"
        )

        var lastException: Exception? = null
        for (url in urls) {
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", userAgent)
                .apply {
                    if (downloadedBytes > 0) {
                        addHeader("Range", "bytes=$downloadedBytes-")
                    }
                }
                .build()
            
            val call = client.newCall(request)
            activeCalls[tableName] = call
            _isPaused.value = _isPaused.value + (tableName to false)
            _downloadingTables.value = _downloadingTables.value + tableLower

            try {
                val response = call.execute()
                Log.i(TAG, "Response for $tableName from ${url.take(30)}...: ${response.code}")
                
                if (response.code == 416) {
                    Log.w(TAG, "Range not satisfiable, deleting temp file for $tableName")
                    response.close()
                    tempFile.delete()
                    return@withContext downloadDictionary(tableName, retryCount)
                }

                if (!response.isSuccessful && response.code != 206) {
                    throw IOException("Server returned ${response.code} for $tableName")
                }

                val body = response.body ?: throw IOException("Response body is null")
                val contentLength = body.contentLength()
                val totalBytes = if (response.code == 206) {
                    val contentRange = response.header("Content-Range")
                    contentRange?.substringAfterLast("/")?.toLongOrNull() ?: (contentLength + downloadedBytes)
                } else {
                    contentLength
                }
                
                body.byteStream().use { input ->
                    FileOutputStream(tempFile, response.code == 206).use { output ->
                        val buffer = ByteArray(8192)
                        var bytesRead: Int
                        var totalRead = downloadedBytes
                        while (input.read(buffer).also { bytesRead = it } != -1) {
                            output.write(buffer, 0, bytesRead)
                            totalRead += bytesRead
                            if (totalBytes > 0) {
                                val progress = (100.0 * totalRead / totalBytes).toFloat()
                                _downloadProgress.value = _downloadProgress.value + (tableName to progress)
                            }
                        }
                    }
                }

                if (tempFile.exists()) {
                    if (localFile.exists()) localFile.delete()
                    tempFile.renameTo(localFile)
                    Log.i(TAG, "Successfully saved $tableName from mirror: ${url.take(30)}...")
                    return@withContext
                }
            } catch (e: Exception) {
                Log.e(TAG, "Download failed for $tableName from ${url.take(30)}...: ${e.message}")
                lastException = e
                if (call.isCanceled()) {
                    Log.i(TAG, "Download canceled for $tableName")
                    return@withContext
                }
            } finally {
                activeCalls.remove(tableName)
                _downloadingTables.value = _downloadingTables.value - tableLower
                _downloadProgress.value = _downloadProgress.value - tableName
                _isPaused.value = _isPaused.value - tableName
            }
        }

        if (retryCount > 0 && lastException is IOException) {
            Log.w(TAG, "All URLs failed for $tableName, retrying in 2s... ($retryCount left)")
            delay(2000)
            return@withContext downloadDictionary(tableName, retryCount - 1)
        } else {
            Log.e(TAG, "Final download error for $tableName: ${lastException?.message}")
            throw lastException ?: IOException("Download failed for $tableName")
        }
    }

    fun pauseDownload(tableName: String) {
        activeCalls[tableName]?.cancel()
        _isPaused.value = _isPaused.value + (tableName to true)
    }

    fun resumeDownload(tableName: String) {
        _isPaused.value = _isPaused.value - tableName
    }

    fun cancelDownload(tableName: String) {
        activeCalls[tableName]?.cancel()
        activeCalls.remove(tableName)
        File(getDictDir(), "${tableName.lowercase()}.db.tmp").delete()
        
        _downloadProgress.value = _downloadProgress.value - tableName
        _isPaused.value = _isPaused.value - tableName
        _downloadingTables.value = _downloadingTables.value - tableName.lowercase()
    }

    fun getDictionaryPath(tableName: String): String? {
        val file = File(getDictDir(), "${tableName.lowercase()}.db")
        return if (file.exists() && file.length() > 1024) file.absolutePath else null
    }

    suspend fun getOnlineDictionaryNames(): List<String> = withContext(Dispatchers.IO) {
        val manifestName = "manifest.json"
        val urls = listOf(
            "$baseUrl$manifestName",
            "https://ghproxy.com/$baseUrl$manifestName"
        )

        for (url in urls) {
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", userAgent)
                .build()
            try {
                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        Log.e(TAG, "Failed to get manifest from $url: ${response.code}")
                        continue
                    }
                    val body = response.body?.string() ?: continue
                    val jsonArray = JSONArray(body)
                    val list = mutableListOf<String>()
                    for (i in 0 until jsonArray.length()) {
                        list.add(jsonArray.getString(i))
                    }
                    Log.i(TAG, "Successfully fetched manifest from $url")
                    return@withContext list
                }
            } catch (e: Exception) {
                Log.e(TAG, "Exception while getting manifest from $url: ${e.message}")
            }
        }
        emptyList()
    }
}
