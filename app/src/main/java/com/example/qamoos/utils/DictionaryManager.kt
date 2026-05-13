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
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.net.InetAddress
import java.security.MessageDigest
import java.util.concurrent.TimeUnit
import kotlin.text.Charsets

class DictionaryManager(context: Context) {
    private val TAG = "DictionaryManager"
    val appContext = context.applicationContext
    
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
            
            try {
                val results = Dns.SYSTEM.lookup(hostname)
                if (results.isNotEmpty()) return results
            } catch (e: Exception) {
                Log.w(TAG, "System DNS failed for $hostname, trying static fallback")
            }

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

    private var cachedChecksums: Map<String, String>? = null

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
        return file.exists() && file.length() > 1024 && isValidSQLiteFile(file)
    }

    private fun isValidSQLiteFile(file: File): Boolean {
        if (!file.exists() || file.length() < 16) return false
        return try {
            FileInputStream(file).use { input ->
                val header = ByteArray(16)
                if (input.read(header) != 16) return false
                val expected = "SQLite format 3\u0000".toByteArray(Charsets.US_ASCII)
                header.contentEquals(expected)
            }
        } catch (e: Exception) {
            false
        }
    }

    fun deleteDictionary(tableName: String): Boolean {
        val file = File(getDictDir(), "${tableName.lowercase()}.db")
        return if (file.exists()) file.delete() else false
    }

    private suspend fun fetchChecksums(): Map<String, String> = withContext(Dispatchers.IO) {
        cachedChecksums?.let { return@withContext it }
        
        val urls = listOf(
            "${baseUrl}checksums.txt",
            "https://ghproxy.com/${baseUrl}checksums.txt"
        )

        for (url in urls) {
            try {
                val request = Request.Builder()
                    .url(url)
                    .header("User-Agent", userAgent)
                    .build()
                
                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) return@use
                    val body = response.body?.string() ?: return@use
                    val map = body.lines()
                        .filter { it.contains("  ") }
                        .associate { 
                            val parts = it.split("  ")
                            val hash = parts[0].trim().lowercase()
                            val name = parts[1].trim().lowercase()
                            name to hash
                        }
                    if (map.isNotEmpty()) {
                        cachedChecksums = map
                        return@withContext map
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to fetch checksums from $url: ${e.message}")
            }
        }
        emptyMap()
    }

    private fun calculateSHA256(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        FileInputStream(file).use { input ->
            val buffer = ByteArray(8192)
            var bytesRead: Int
            while (input.read(buffer).also { bytesRead = it } != -1) {
                digest.update(buffer, 0, bytesRead)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    suspend fun downloadDictionary(tableName: String, retryCount: Int = 3): Unit = withContext(Dispatchers.IO) {
        val fileName = "${tableName.lowercase()}.db"
        val tableLower = tableName.lowercase()
        val dictDir = getDictDir()
        val localFile = File(dictDir, fileName)
        val tempFile = File(dictDir, "$fileName.tmp")
        
        Log.i(TAG, "Attempting to download: $tableName (Attempt: ${4 - retryCount})")

        // Always clear partial data before starting a new download attempt
        if (tempFile.exists()) tempFile.delete()
        
        val urls = listOf(
            "$baseUrl$fileName",
            "https://ghproxy.com/$baseUrl$fileName"
        )

        var lastException: Exception? = null
        for (url in urls) {
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", userAgent)
                .build() // resumer removed
            
            val call = client.newCall(request)
            activeCalls[tableName] = call
            _isPaused.value = _isPaused.value + (tableName to false)
            _downloadingTables.value = _downloadingTables.value + tableLower

            try {
                val response = call.execute()
                Log.i(TAG, "Response for $tableName from ${url.take(30)}...: ${response.code}")
                
                if (!response.isSuccessful) {
                    throw IOException("Server returned ${response.code} for $tableName")
                }

                val body = response.body ?: throw IOException("Response body is null")
                val totalBytes = body.contentLength()
                
                body.byteStream().use { input ->
                    FileOutputStream(tempFile, false).use { output ->
                        val buffer = ByteArray(8192)
                        var bytesRead: Int
                        var totalRead = 0L
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
                    // Pre-verification: Check if it's even a SQLite file
                    _downloadProgress.value = _downloadProgress.value + (tableName to -1f) // Signal verification
                    if (!isValidSQLiteFile(tempFile)) {
                        Log.e(TAG, "Downloaded file for $tableName is not a valid SQLite database!")
                        tempFile.delete()
                        throw IOException("Verification failed: $tableName is not a valid SQLite database")
                    }

                    // Hash Verification
                    Log.i(TAG, "Verifying integrity of $tableName...")
                    val checksums = fetchChecksums()
                    val expectedHash = checksums[fileName]
                    
                    if (expectedHash != null) {
                        val actualHash = calculateSHA256(tempFile)
                        if (actualHash != expectedHash) {
                            Log.e(TAG, "Hash mismatch for $tableName! Expected: $expectedHash, Actual: $actualHash")
                            tempFile.delete()
                            throw IOException("Verification failed: Hash mismatch for $tableName")
                        }
                        Log.i(TAG, "Hash verification successful for $tableName")
                    } else {
                        Log.w(TAG, "No checksum found for $tableName, performing size check...")
                        if (tempFile.length() < 1000) {
                            tempFile.delete()
                            throw IOException("Verification failed: File too small for $tableName")
                        }
                    }

                    if (localFile.exists()) localFile.delete()
                    tempFile.renameTo(localFile)
                    Log.i(TAG, "Successfully saved $tableName")
                    return@withContext
                }
            } catch (e: Exception) {
                Log.e(TAG, "Download failed for $tableName: ${e.message}")
                if (tempFile.exists()) tempFile.delete() // Clear partially downloaded data
                
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

        if (retryCount > 0 && lastException is IOException && !lastException.message!!.contains("Verification failed")) {
            Log.w(TAG, "Retrying download for $tableName ($retryCount left)")
            delay(2000)
            return@withContext downloadDictionary(tableName, retryCount - 1)
        } else {
            throw lastException ?: IOException("Download failed for $tableName")
        }
    }

    fun pauseDownload(tableName: String) {
        activeCalls[tableName]?.cancel()
        _isPaused.value = _isPaused.value + (tableName to true)
        // Note: Data will be cleared because resume is not supported anymore as per user request
        File(getDictDir(), "${tableName.lowercase()}.db.tmp").delete()
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
        return if (file.exists() && file.length() > 1024 && isValidSQLiteFile(file)) file.absolutePath else null
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
                    if (!response.isSuccessful) return@use
                    val body = response.body?.string() ?: return@use
                    val jsonArray = JSONArray(body)
                    val list = mutableListOf<String>()
                    for (i in 0 until jsonArray.length()) {
                        list.add(jsonArray.getString(i))
                    }
                    return@withContext list
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to get manifest from $url")
            }
        }
        emptyList()
    }
}
