package com.example.util

import android.content.Context
import android.os.Bundle
import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import android.print.PageRange
import android.print.PrintAttributes
import android.print.PrintDocumentAdapter
import android.print.PrintDocumentInfo
import android.print.PrintManager
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

object SystemPrintHelper {

    fun printPdfFile(context: Context, pdfFile: File, jobName: String): Result<Unit> {
        return try {
            val printManager = context.getSystemService(Context.PRINT_SERVICE) as? PrintManager
                ?: return Result.failure(IllegalStateException("Print service unavailable on this device"))

            val adapter = object : PrintDocumentAdapter() {
                override fun onLayout(
                    oldAttributes: PrintAttributes?,
                    newAttributes: PrintAttributes,
                    cancellationSignal: CancellationSignal?,
                    callback: LayoutResultCallback,
                    extras: Bundle?
                ) {
                    if (cancellationSignal?.isCanceled == true) {
                        callback.onLayoutCancelled()
                        return
                    }
                    val info = PrintDocumentInfo.Builder(pdfFile.name)
                        .setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
                        .setPageCount(PrintDocumentInfo.PAGE_COUNT_UNKNOWN)
                        .build()
                    callback.onLayoutFinished(info, true)
                }

                override fun onWrite(
                    pages: Array<out PageRange>,
                    destination: ParcelFileDescriptor,
                    cancellationSignal: CancellationSignal?,
                    callback: WriteResultCallback
                ) {
                    try {
                        FileInputStream(pdfFile).use { input ->
                            FileOutputStream(destination.fileDescriptor).use { output ->
                                val buffer = ByteArray(8192)
                                var bytesRead: Int
                                while (input.read(buffer).also { bytesRead = it } > 0) {
                                    if (cancellationSignal?.isCanceled == true) {
                                        callback.onWriteCancelled()
                                        return
                                    }
                                    output.write(buffer, 0, bytesRead)
                                }
                            }
                        }
                        callback.onWriteFinished(arrayOf(PageRange.ALL_PAGES))
                    } catch (e: Exception) {
                        callback.onWriteFailed(e.message)
                    }
                }
            }
            printManager.print(jobName, adapter, PrintAttributes.Builder().build())
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
