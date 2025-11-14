
package com.example.materialtv.ui.screens.downloads

import android.content.Intent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.materialtv.DownloadsViewModel
import com.example.materialtv.PlayerActivity

@Composable
fun DownloadsScreen(viewModel: DownloadsViewModel) {
    val downloadedFiles by viewModel.downloadedFiles.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.fetchDownloadedFiles()
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp)
    ) {
        items(downloadedFiles) { file ->
             Text(
                text = file.name,
                modifier = Modifier.padding(vertical = 8.dp).clickable {
                    val intent = Intent(context, PlayerActivity::class.java).apply {
                        putExtra("TITLE", file.name)
                        putExtra("URI", file.path)
                    }
                    context.startActivity(intent)
                }
            )
        }
    }
}
