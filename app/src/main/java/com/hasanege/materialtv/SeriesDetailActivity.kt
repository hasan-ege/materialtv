package com.hasanege.materialtv

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.media3.common.util.UnstableApi
import com.hasanege.materialtv.network.SessionManager
import com.hasanege.materialtv.ui.theme.MaterialTVTheme

@UnstableApi
class SeriesDetailActivity : ComponentActivity() {

    private val viewModel: SeriesDetailViewModel by viewModels { SeriesDetailViewModelFactory }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val seriesId = intent.getIntExtra("SERIES_ID", -1)
        val username = SessionManager.username ?: ""
        val password = SessionManager.password ?: ""

        if (seriesId != -1) {
            viewModel.loadSeriesInfo(username, password, seriesId)
        }

        setContent {
            MaterialTVTheme {
                when (val state = viewModel.seriesInfoState) {
                    is UiState.Success -> {
                        DetailScreen(
                            series = state.data,
                            onBack = { finish() },
                            onPlayEpisode = { episode ->
                                val intent = Intent(this, PlayerActivity::class.java).apply {
                                    putExtra(
                                        "url",
                                        "${SessionManager.serverUrl}/series/${SessionManager.username}/${SessionManager.password}/${episode.id}.${episode.containerExtension}"
                                    )
                                    putExtra("title", episode.title)
                                    putExtra("SERIES_ID", seriesId)
                                }
                                startActivity(intent)
                            },
                            seriesId = seriesId
                        )
                    }
                    is UiState.Loading -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    }
                    is UiState.Error -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(state.message)
                        }
                    }
                }
            }
        }
    }
}
