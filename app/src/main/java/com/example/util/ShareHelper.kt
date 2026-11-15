package com.example.util

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream

sealed class WhatsAppShareResult {
    data object Launched : WhatsAppShareResult()
    data object NotInstalled : WhatsAppShareResult()
    data class Error(val message: String) : WhatsAppShareResult()
}

data class LocalBackupInfo(
    val file: File,
    val name: String,
    val timestamp: Long,
    val sizeBytes: Long
)

object ShareHelper {

    fun copyPickedImageToInternalStorage(context: Context, uri: Uri): String? {
        return try {
            val dir = File(context.filesDir, "product_images").apply { mkdirs() }
            val file = File(dir, "prod_${System.currentTimeMillis()}.jpg")
            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(file).use { output ->
                    input.copyTo(output)
                }
            }
            file.absolutePath
        } catch (e: Exception) {
            null
        }
    }

    fun getUriForFile(context: Context, file: File): Uri {
        return FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
    }

    fun isPackageInstalled(context: Context, packageName: String): Boolean {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.packageManager.getPackageInfo(
                    packageName,
                    PackageManager.PackageInfoFlags.of(0)
                )
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getPackageInfo(packageName, 0)
            }
            true
        } catch (_: PackageManager.NameNotFoundException) {
            false
        }
    }

    fun shareFileToWhatsApp(
        context: Context,
        file: File,
        mimeType: String,
        captionText: String
    ): WhatsAppShareResult {
        val targetPkg = when {
            isPackageInstalled(context, "com.whatsapp") -> "com.whatsapp"
            isPackageInstalled(context, "com.whatsapp.w4b") -> "com.whatsapp.w4b"
            else -> return WhatsAppShareResult.NotInstalled
        }
        return try {
            val uri = getUriForFile(context, file)
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = mimeType
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_TEXT, captionText)
                setPackage(targetPkg)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            WhatsAppShareResult.Launched
        } catch (e: Exception) {
            WhatsAppShareResult.Error(e.message ?: "Unable to open WhatsApp")
        }
    }

    fun shareFileGeneric(
        context: Context,
        file: File,
        mimeType: String,
        chooserTitle: String,
        captionText: String = ""
    ) {
        val uri = getUriForFile(context, file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            if (captionText.isNotBlank()) putExtra(Intent.EXTRA_TEXT, captionText)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val chooser = Intent.createChooser(intent, chooserTitle).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(chooser)
    }

    fun copyFileToSafUri(context: Context, sourceFile: File, destUri: Uri): Boolean {
        return try {
            context.contentResolver.openOutputStream(destUri)?.use { out ->
                sourceFile.inputStream().use { inp -> inp.copyTo(out) }
            }
            true
        } catch (_: Exception) {
            false
        }
    }

    fun writeTextToSafUri(context: Context, text: String, destUri: Uri): Boolean {
        return try {
            context.contentResolver.openOutputStream(destUri)?.use { out ->
                out.write(text.toByteArray(Charsets.UTF_8))
                out.flush()
            }
            true
        } catch (_: Exception) {
            false
        }
    }

    fun readTextFromSafUri(context: Context, uri: Uri): String? {
        return try {
            context.contentResolver.openInputStream(uri)?.bufferedReader(Charsets.UTF_8)?.use {
                it.readText()
            }
        } catch (_: Exception) {
            null
        }
    }

    fun writeCsvFile(context: Context, fileName: String, headers: List<String>, rows: List<List<String>>): File {
        val dir = File(context.filesDir, "reports").apply { mkdirs() }
        val file = File(dir, fileName)
        file.bufferedWriter(Charsets.UTF_8).use { writer ->
            fun escapeCsv(cell: String): String {
                val clean = cell.replace("\"", "\"\"")
                return "\"$clean\""
            }
            writer.appendLine(headers.joinToString(",") { escapeCsv(it) })
            rows.forEach { row ->
                writer.appendLine(row.joinToString(",") { escapeCsv(it) })
            }
        }
        return file
    }

    fun saveLocalBackupFile(context: Context, jsonContent: String): File {
        val dir = File(context.filesDir, "auto_backups").apply { mkdirs() }
        val file = File(dir, "NaqshBackup_${IndianFormatters.formatFileTimestamp()}.naqshbackup")
        file.writeText(jsonContent, Charsets.UTF_8)
        // Retain latest 10 local backups
        val existing = dir.listFiles()?.sortedByDescending { it.lastModified() } ?: emptyList()
        existing.drop(10).forEach { it.delete() }
        return file
    }

    fun listLocalBackups(context: Context): List<LocalBackupInfo> {
        val dir = File(context.filesDir, "auto_backups")
        if (!dir.exists()) return emptyList()
        return (dir.listFiles() ?: emptyArray())
            .filter { it.isFile }
            .sortedByDescending { it.lastModified() }
            .map { LocalBackupInfo(it, it.name, it.lastModified(), it.length()) }
    }
}
