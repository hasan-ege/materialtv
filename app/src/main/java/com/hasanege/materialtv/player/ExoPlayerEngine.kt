package com.hasanege.materialtv.player

import android.content.Context
import android.net.Uri
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.media3.common.Format
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.analytics.AnalyticsListener
import androidx.media3.exoplayer.DecoderReuseEvaluation
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.upstream.DefaultLoadErrorHandlingPolicy
import androidx.media3.ui.PlayerView
import java.io.File

class ExoPlayerEngine : PlayerEngine {
    private var player: ExoPlayer? = null
    fun getExoPlayer(): ExoPlayer? = player
    private var playerView: PlayerView? = null
    private var context: Context? = null
    
    // Stats
    private var currentBitrate: Long = 0
    private var droppedFrames: Int = 0
    private var videoFormat: String? = null
    
    // Error callback
    private var errorCallback: ((Exception) -> Unit)? = null

    override fun setOnErrorCallback(callback: (Exception) -> Unit) {
        this.errorCallback = callback
    }

    override fun initialize(context: Context) {
        this.context = context
        
        // Configure RenderersFactory to prefer FFMPEG software audio decoder
        val renderersFactory = androidx.media3.exoplayer.DefaultRenderersFactory(context).apply {
            // EXTENSION_RENDERER_MODE_PREFER: Prefer extension renderers (FFMPEG) over platform renderers
            setExtensionRendererMode(androidx.media3.exoplayer.DefaultRenderersFactory.EXTENSION_RENDERER_MODE_PREFER)
            
            // Enable decoder fallback in case of errors
            setEnableDecoderFallback(true)
        }

        val trackSelector = androidx.media3.exoplayer.trackselection.DefaultTrackSelector(context).apply {
            setParameters(
                buildUponParameters()
                    .setAllowAudioMixedMimeTypeAdaptiveness(true)
                    .setAllowAudioMixedSampleRateAdaptiveness(true)
                    .setAllowAudioMixedChannelCountAdaptiveness(true)
            )
        }

        val audioAttributes = androidx.media3.common.AudioAttributes.Builder()
            .setUsage(androidx.media3.common.C.USAGE_MEDIA)
            .setContentType(androidx.media3.common.C.AUDIO_CONTENT_TYPE_MOVIE)
            .build()

        // Aggressive load control for FAST ZAPPING (instant channel switching)
        val loadControl = androidx.media3.exoplayer.DefaultLoadControl.Builder()
            .setBufferDurationsMs(
                500,   // Min buffer 500ms - VERY aggressive for instant start
                8000,  // Max buffer 8s - reduced for fast zapping
                250,   // Buffer for playback - instant start
                500    // Buffer for rebuffer - quick recovery
            )
            .setBackBuffer(
                2000,  // Keep 2s back buffer for quick rewind
                true   // Retain back buffer
            )
            .setPrioritizeTimeOverSizeThresholds(true) // Prioritize time for streaming
            .setTargetBufferBytes(-1) // No target buffer size limit
            .build()

        player = ExoPlayer.Builder(context, renderersFactory)
            .setTrackSelector(trackSelector)
            .setAudioAttributes(audioAttributes, true)
            .setHandleAudioBecomingNoisy(true)
            .setSeekBackIncrementMs(10000)
            .setSeekForwardIncrementMs(10000)
            .setLoadControl(loadControl)
            .build()
        
        player?.addAnalyticsListener(object : AnalyticsListener {
            override fun onBandwidthEstimate(
                eventTime: AnalyticsListener.EventTime,
                totalLoadTimeMs: Int,
                totalBytesLoaded: Long,
                bitrateEstimate: Long
            ) {
                // Keep bandwidth estimate as fallback or secondary metric if needed
                // But for "Stats for Nerds", format bitrate is more relevant for the stream quality
            }

            override fun onDroppedVideoFrames(
                eventTime: AnalyticsListener.EventTime,
                droppedFrames: Int,
                elapsedMs: Long
            ) {
                this@ExoPlayerEngine.droppedFrames += droppedFrames
            }
            
            override fun onLoadCompleted(
                eventTime: AnalyticsListener.EventTime,
                loadEventInfo: androidx.media3.exoplayer.source.LoadEventInfo,
                mediaLoadData: androidx.media3.exoplayer.source.MediaLoadData
            ) {
                if (loadEventInfo.loadDurationMs > 0) {
                    val bitrate = (loadEventInfo.bytesLoaded * 8 * 1000) / loadEventInfo.loadDurationMs
                    currentBitrate = bitrate
                }
            }

            override fun onVideoInputFormatChanged(
                eventTime: AnalyticsListener.EventTime,
                format: Format,
                decoderReuseEvaluation: DecoderReuseEvaluation?
            ) {
                videoFormat = "${format.width}x${format.height} (${format.sampleMimeType})"
                // Fallback to format bitrate if we haven't calculated one yet
                if (currentBitrate == 0L && format.bitrate != Format.NO_VALUE) {
                    currentBitrate = format.bitrate.toLong()
                }
            }
            
            // Log audio decoder information
            override fun onAudioInputFormatChanged(
                eventTime: AnalyticsListener.EventTime,
                format: Format,
                decoderReuseEvaluation: DecoderReuseEvaluation?
            ) {
                android.util.Log.d("ExoPlayerEngine", "Audio format changed: ${format.sampleMimeType}, " +
                        "channels: ${format.channelCount}, sampleRate: ${format.sampleRate}")
            }
            
            override fun onAudioDecoderInitialized(
                eventTime: AnalyticsListener.EventTime,
                decoderName: String,
                initializedTimestampMs: Long
            ) {
                android.util.Log.d("ExoPlayerEngine", "Audio decoder initialized: $decoderName")
            }
        })
        
        // Add error listener
        player?.addListener(object : Player.Listener {
            override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                errorCallback?.invoke(Exception(error.message, error))
            }
        })
    }

    override fun attach(container: ViewGroup) {
        context?.let { ctx ->
            playerView = PlayerView(ctx).apply {
                layoutParams = FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT
                )
                player = this@ExoPlayerEngine.player
                useController = false // Disable default controls
                resizeMode = androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_FIT // Best fit for mobile
            }
            container.addView(playerView)
        }
    }

    override fun detach() {
        playerView?.let {
            (it.parent as? ViewGroup)?.removeView(it)
            it.player = null
        }
        playerView = null
    }

    override fun prepare(url: String) {
        // Lokal dosyalar ve HTTP URL'leri ayır
        val isNetworkUrl = url.startsWith("http://") || url.startsWith("https://")

        if (isNetworkUrl) {
            // Aggressive HTTP settings for FAST ZAPPING
            val httpDataSourceFactory = androidx.media3.datasource.DefaultHttpDataSource.Factory()
                .setUserAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                .setAllowCrossProtocolRedirects(true)
                .setConnectTimeoutMs(5000)  // Reduced from 60s to 5s for fast zapping
                .setReadTimeoutMs(8000)     // Reduced from 60s to 8s
                .setKeepPostFor302Redirects(true)
                .setDefaultRequestProperties(
                    mapOf(
                        "Accept" to "*/*",
                        "Accept-Language" to "en-US,en;q=0.9,tr;q=0.8",
                        "Accept-Encoding" to "gzip, deflate, br",
                        "Connection" to "keep-alive",
                        "Referer" to url.substringBefore("?").substringBeforeLast("/") + "/",
                        "Origin" to url.substringBefore("://") + "://" + url.substringAfter("://").substringBefore("/"),
                        "Sec-Fetch-Dest" to "video",
                        "Sec-Fetch-Mode" to "cors",
                        "Sec-Fetch-Site" to "cross-site"
                    )
                )

            val dataSourceFactory = androidx.media3.datasource.DefaultDataSource.Factory(
                context!!,
                httpDataSourceFactory
            )

            // Optimized media source factory for fast loading
            val mediaSourceFactory = androidx.media3.exoplayer.source.DefaultMediaSourceFactory(context!!)
                .setDataSourceFactory(dataSourceFactory)
                .setLoadErrorHandlingPolicy(
                    androidx.media3.exoplayer.upstream.DefaultLoadErrorHandlingPolicy(2) // Reduced retries from 3 to 2
                )

            val mediaItem = MediaItem.fromUri(url)
            val mediaSource = mediaSourceFactory.createMediaSource(mediaItem)

            player?.apply {
                setMediaSource(mediaSource)
                prepare()
                // Instant playback for fast zapping
                playWhenReady = true
            }
        } else {
            // Yerel dosya: doğrudan file:// Uri kullan
            val file = File(url)
            val fileUri = Uri.fromFile(file)
            val mediaItem = MediaItem.fromUri(fileUri)

            val dataSourceFactory = androidx.media3.datasource.DefaultDataSource.Factory(context!!)
            val mediaSourceFactory = androidx.media3.exoplayer.source.DefaultMediaSourceFactory(context!!)
                .setDataSourceFactory(dataSourceFactory)

            val mediaSource = mediaSourceFactory.createMediaSource(mediaItem)

            player?.apply {
                setMediaSource(mediaSource)
                prepare()
                playWhenReady = true
            }
        }
    }

    override fun play() {
        player?.play()
    }

    override fun isPlaying(): Boolean {
        return player?.isPlaying == true
    }

    override fun pause() {
        player?.pause()
    }

    override fun stop() {
        player?.stop()
    }

    override fun release() {
        try {
            player?.let {
                it.stop()
                it.release()
            }
        } catch (e: Exception) {
            android.util.Log.e("ExoPlayerEngine", "Error releasing: ${e.message}")
        } finally {
            player = null
            playerView = null
            context = null
        }
    }

    override fun seekTo(position: Long) {
        player?.seekTo(position)
    }

    override fun seekBack() {
        player?.seekBack()
    }

    override fun seekForward() {
        player?.seekForward()
    }

    override fun getDuration(): Long {
        return player?.duration ?: 0L
    }

    override fun getCurrentPosition(): Long {
        return player?.currentPosition ?: 0L
    }

    override fun setVolume(volume: Float) {
        player?.volume = volume.coerceIn(0f, 1f)
    }

    override fun getBitrate(): Long = currentBitrate
    override fun getDroppedFrames(): Int = droppedFrames
    
    override fun getVideoFormat(): String? {
        return try {
            player?.videoFormat?.let { format ->
                "${format.width}x${format.height} ${format.codecs ?: "Unknown"}"
            }
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * Set resize mode for aspect ratio handling
     * @param mode: RESIZE_MODE_FIT, RESIZE_MODE_FILL, RESIZE_MODE_ZOOM, RESIZE_MODE_FIXED_WIDTH, RESIZE_MODE_FIXED_HEIGHT
     */
    fun setResizeMode(mode: Int) {
        playerView?.resizeMode = mode
    }
    
    // Track selection implementation
    override fun getSubtitleTracks(): List<Pair<Int, String>> {
        return try {
            val tracks = mutableListOf<Pair<Int, String>>()
            tracks.add(Pair(-1, "Disabled"))
            
            player?.currentTracks?.groups?.forEachIndexed { groupIndex, group ->
                if (group.type == androidx.media3.common.C.TRACK_TYPE_TEXT) {
                    for (i in 0 until group.length) {
                        val format = group.getTrackFormat(i)
                        val language = format.language ?: "Unknown"
                        val label = format.label ?: language
                        // Use unique ID combining group and track index
                        tracks.add(Pair(groupIndex * 1000 + i, label))
                    }
                }
            }
            tracks
        } catch (e: Exception) {
            android.util.Log.e("ExoPlayerEngine", "Error getting subtitle tracks: ${e.message}")
            emptyList()
        }
    }
    
    override fun getAudioTracks(): List<Pair<Int, String>> {
        return try {
            val tracks = mutableListOf<Pair<Int, String>>()
            
            player?.currentTracks?.groups?.forEachIndexed { groupIndex, group ->
                if (group.type == androidx.media3.common.C.TRACK_TYPE_AUDIO) {
                    for (i in 0 until group.length) {
                        val format = group.getTrackFormat(i)
                        val language = format.language ?: "Unknown"
                        val label = format.label ?: language
                        // Use unique ID combining group and track index
                        tracks.add(Pair(groupIndex * 1000 + i, label))
                    }
                }
            }
            tracks
        } catch (e: Exception) {
            android.util.Log.e("ExoPlayerEngine", "Error getting audio tracks: ${e.message}")
            emptyList()
        }
    }
    
    override fun setSubtitleTrack(trackId: Int) {
        try {
            val params = player?.trackSelectionParameters?.buildUpon() ?: return
            
            if (trackId == -1) {
                // Disable subtitles
                params.setTrackTypeDisabled(androidx.media3.common.C.TRACK_TYPE_TEXT, true)
            } else {
                // Enable subtitles and select specific track
                params.setTrackTypeDisabled(androidx.media3.common.C.TRACK_TYPE_TEXT, false)
                
                // Find the track group and index from trackId
                val groupIndex = trackId / 1000
                val trackIndex = trackId % 1000
                
                player?.currentTracks?.groups?.getOrNull(groupIndex)?.let { group ->
                    if (group.type == androidx.media3.common.C.TRACK_TYPE_TEXT && trackIndex < group.length) {
                        val format = group.getTrackFormat(trackIndex)
                        val override = androidx.media3.common.TrackSelectionOverride(
                            group.mediaTrackGroup,
                            listOf(trackIndex)
                        )
                        params.addOverride(override)
                    }
                }
            }
            
            player?.trackSelectionParameters = params.build()
            android.util.Log.d("ExoPlayerEngine", "Subtitle track set to: $trackId")
        } catch (e: Exception) {
            android.util.Log.e("ExoPlayerEngine", "Error setting subtitle track: ${e.message}", e)
        }
    }
    
    override fun setAudioTrack(trackId: Int) {
        try {
            val params = player?.trackSelectionParameters?.buildUpon() ?: return
            
            // Find the track group and index from trackId
            val groupIndex = trackId / 1000
            val trackIndex = trackId % 1000
            
            player?.currentTracks?.groups?.getOrNull(groupIndex)?.let { group ->
                if (group.type == androidx.media3.common.C.TRACK_TYPE_AUDIO && trackIndex < group.length) {
                    val format = group.getTrackFormat(trackIndex)
                    val override = androidx.media3.common.TrackSelectionOverride(
                        group.mediaTrackGroup,
                        listOf(trackIndex)
                    )
                    params.clearOverridesOfType(androidx.media3.common.C.TRACK_TYPE_AUDIO)
                    params.addOverride(override)
                    player?.trackSelectionParameters = params.build()
                    android.util.Log.d("ExoPlayerEngine", "Audio track set to: $trackId (${format.language})")
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("ExoPlayerEngine", "Error setting audio track: ${e.message}", e)
        }
    }
    
    override fun getCurrentSubtitleTrack(): Int {
        return try {
            val params = player?.trackSelectionParameters
            val isDisabled = params?.disabledTrackTypes?.contains(androidx.media3.common.C.TRACK_TYPE_TEXT) ?: true
            
            if (isDisabled) {
                -1
            } else {
                // Find currently selected subtitle track
                player?.currentTracks?.groups?.forEachIndexed { groupIndex, group ->
                    if (group.type == androidx.media3.common.C.TRACK_TYPE_TEXT && group.isSelected) {
                        for (i in 0 until group.length) {
                            if (group.isTrackSelected(i)) {
                                return groupIndex * 1000 + i
                            }
                        }
                    }
                }
                -1
            }
        } catch (e: Exception) {
            -1
        }
    }
    
    override fun getCurrentAudioTrack(): Int {
        return try {
            // Find currently selected audio track
            player?.currentTracks?.groups?.forEachIndexed { groupIndex, group ->
                if (group.type == androidx.media3.common.C.TRACK_TYPE_AUDIO && group.isSelected) {
                    for (i in 0 until group.length) {
                        if (group.isTrackSelected(i)) {
                            return groupIndex * 1000 + i
                        }
                    }
                }
            }
            0 // Return 0 as default if nothing found
        } catch (e: Exception) {
            -1
        }
    }
}
