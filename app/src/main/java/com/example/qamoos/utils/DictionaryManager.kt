package com.example.qamoos.utils

import android.content.Context
import com.google.firebase.storage.FileDownloadTask
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.tasks.await
import java.io.File
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class DictionaryManager(private val context: Context) {
    private val storage = FirebaseStorage.getInstance()
    private val _downloadProgress = MutableStateFlow<Map<String, Float>>(emptyMap())
    val downloadProgress: StateFlow<Map<String, Float>> = _downloadProgress

    private val activeTasks = mutableMapOf<String, FileDownloadTask>()
    private val _isPaused = MutableStateFlow<Map<String, Boolean>>(emptyMap())
    val isPaused: StateFlow<Map<String, Boolean>> = _isPaused

    private val _downloadingTables = MutableStateFlow<Set<String>>(emptySet())
    val downloadingTables: StateFlow<Set<String>> = _downloadingTables

    fun isDownloaded(tableName: String): Boolean {
        val fileName = "${tableName.lowercase()}.db"
        val file = File(context.getDatabasePath("dictionaries"), fileName)
        // Only consider it downloaded if it exists AND is not currently being downloaded
        return file.exists() && !_downloadingTables.value.contains(tableName.lowercase())
    }

    fun deleteDictionary(tableName: String): Boolean {
        val file = File(context.getDatabasePath("dictionaries"), "${tableName.lowercase()}.db")
        return if (file.exists()) file.delete() else false
    }

    suspend fun downloadDictionary(tableName: String) {
        val fileName = "${tableName.lowercase()}.db"
        val tableLower = tableName.lowercase()
        val localFile = File(context.getDatabasePath("dictionaries"), fileName)
        val tempFile = File(context.getDatabasePath("dictionaries"), "$fileName.tmp")
        
        if (!localFile.parentFile.exists()) {
            localFile.parentFile.mkdirs()
        }

        val storageRef = storage.reference.child("dictionaries/$fileName")
        
        try {
            _downloadingTables.value = _downloadingTables.value + tableLower
            val downloadTask = storageRef.getFile(tempFile)
            activeTasks[tableName] = downloadTask
            _isPaused.value = _isPaused.value + (tableName to false)

            downloadTask.addOnProgressListener { taskSnapshot ->
                val progress = (100.0 * taskSnapshot.bytesTransferred / taskSnapshot.totalByteCount).toFloat()
                _downloadProgress.value = _downloadProgress.value + (tableName to progress)
            }

            downloadTask.await()
            
            // On success, rename temp file to local file
            if (tempFile.exists()) {
                if (localFile.exists()) localFile.delete()
                tempFile.renameTo(localFile)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            if (tempFile.exists()) tempFile.delete()
            throw e
        } finally {
            activeTasks.remove(tableName)
            _downloadingTables.value = _downloadingTables.value - tableLower
            _downloadProgress.value = _downloadProgress.value - tableName
            _isPaused.value = _isPaused.value - tableName
        }
    }

    fun pauseDownload(tableName: String) {
        activeTasks[tableName]?.let { task ->
            if (task.isInProgress) {
                task.pause()
                _isPaused.value = _isPaused.value + (tableName to true)
            }
        }
    }

    fun resumeDownload(tableName: String) {
        activeTasks[tableName]?.let { task ->
            if (task.isPaused) {
                task.resume()
                _isPaused.value = _isPaused.value + (tableName to false)
            }
        }
    }

    fun cancelDownload(tableName: String) {
        activeTasks[tableName]?.cancel()
        activeTasks.remove(tableName)
        _downloadProgress.value = _downloadProgress.value - tableName
        _isPaused.value = _isPaused.value - tableName
    }

    fun getDictionaryPath(tableName: String): String? {
        val file = File(context.getDatabasePath("dictionaries"), "${tableName.lowercase()}.db")
        return if (file.exists()) file.absolutePath else null
    }

    suspend fun getOnlineDictionaryNames(): List<String> {
        return try {
            val listResult = storage.reference.child("dictionaries").listAll().await()
            listResult.items.map { it.name.removeSuffix(".db") }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }
}
