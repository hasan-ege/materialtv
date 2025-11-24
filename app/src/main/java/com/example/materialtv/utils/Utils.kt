package com.example.materialtv.utils

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.webkit.MimeTypeMap
import android.widget.Toast
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import java.io.File

fun startDownload(context: Context, url: String, title: String, subpath: String) {
    try {
        // Sanitize the filename to remove invalid characters
        val sanitizedTitle = title.replace(Regex("[\\/:*?\"<>|]"), "_")
        val destination = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        val finalDestination = File(destination, subpath + File.separator + sanitizedTitle)

        val request = DownloadManager.Request(Uri.parse(url))
            .setTitle(sanitizedTitle)
            .setDescription("Downloading...")
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setDestinationUri(Uri.fromFile(finalDestination))
            .setAllowedOverMetered(true)
            .setAllowedOverRoaming(true)

        val mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(MimeTypeMap.getFileExtensionFromUrl(url))
        request.setMimeType(mimeType)

        val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        downloadManager.enqueue(request)

        Toast.makeText(context, "Download started: $sanitizedTitle", Toast.LENGTH_SHORT).show()
    } catch (e: Exception) {
        Toast.makeText(context, "Error starting download: ${e.message}", Toast.LENGTH_LONG).show()
    }
}

@Composable
fun DownloadConfirmationDialog(title: String, text: String, onDismiss: () -> Unit, onConfirm: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(text) },
        confirmButton = {
            TextButton(onClick = {
                onConfirm()
                onDismiss()
            }) { Text("Download") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
