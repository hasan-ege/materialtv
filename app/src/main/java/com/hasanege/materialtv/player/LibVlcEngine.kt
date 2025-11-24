package com.hasanege.materialtv.player

import android.content.Context
import android.net.Uri
import android.view.SurfaceView
import android.view.ViewGroup
import android.widget.FrameLayout
import org.videolan.libvlc.LibVLC
import org.videolan.libvlc.Media
import org.videolan.libvlc.MediaPlayer
import org.videolan.libvlc.util.VLCVideoLayout

class LibVlcEngine : PlayerEngine {
    private var libVlc: LibVLC? = null
    private var mediaPlayer: MediaPlayer? = null
    private var context: Context? = null
    private var videoLayout: VLCVideoLayout? = null
    private var surfaceView: SurfaceView? = null

    override fun initialize(context: Context) {
        this.context = context
        
        try {
            // Simplified VLC arguments for better compatibility
            val args = ArrayList<String>().apply {
                // Network optimization
                add("--network-caching=3000")
                add("--live-caching=3000")
                
                // Hardware acceleration
                add("--codec=mediacodec_ndk,all")
                
                // Audio
                add("--aout=opensles")
                add("--audio-time-stretch")
                
                // FAST ZAPPING Performance optimizations
                add("--file-caching=300")      // Reduced from 2000ms to 300ms for instant start
                add("--network-caching=500")   // Minimal network caching for fast zapping
                add("--live-caching=300")      // Live stream caching
                add("--clock-jitter=0")        // Disable clock jitter for instant playback
                add("--clock-synchro=0")       // Disable clock sync for faster start
                
                // Skip loop filter for faster decoding
                add("--avcodec-skiploopfilter=4")
                add("--avcodec-skip-frame=0")
                add("--avcodec-skip-idct=0")
                
                // Disable unnecessary features
                add("--no-stats")
                add("--no-osd")
                add("--no-video-title-show")   // Don't show video title
                
                // Threading optimizations
                add("--avcodec-threads=0")     // Auto-detect threads
            }
            
            libVlc = LibVLC(context, args)
            mediaPlayer = MediaPlayer(libVlc).apply {
                // Set scale mode for proper aspect ratio
                videoScale = MediaPlayer.ScaleType.SURFACE_BEST_FIT
            }
        } catch (e: Exception) {
            android.util.Log.e("LibVlcEngine", "Error initializing VLC: ${e.message}")
            throw e
        }
    }

    override fun attach(container: ViewGroup) {
        context?.let { ctx ->
            // Use VLCVideoLayout for better aspect ratio handling
            val layout = VLCVideoLayout(ctx).apply {
                layoutParams = FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT
                )
            }
            
            container.addView(layout)
            videoLayout = layout
            
            // Attach media player to the layout
            mediaPlayer?.attachViews(layout, null, false, false)
        }
    }

    override fun detach() {
        mediaPlayer?.detachViews()
        videoLayout?.let { layout ->
            (layout.parent as? ViewGroup)?.removeView(layout)
        }
        videoLayout = null
        surfaceView = null
    }

    override fun prepare(url: String) {
        libVlc?.let { vlc ->
            try {
                val media = Media(vlc, Uri.parse(url)).apply {
                    // Enable hardware decoding
                    setHWDecoderEnabled(true, false)
                    
                    // FAST ZAPPING: Aggressive media options for instant playback
                    addOption(":network-caching=500")     // Reduced from 3000ms to 500ms
                    addOption(":live-caching=300")        // Live stream caching
                    addOption(":file-caching=300")        // File caching
                    addOption(":clock-jitter=0")          // Instant playback
                    addOption(":clock-synchro=0")         // No sync delay
                    addOption(":avcodec-skiploopfilter=4") // Skip loop filter for speed
                }
                
                mediaPlayer?.media = media
                media.release()
            } catch (e: Exception) {
                android.util.Log.e("LibVlcEngine", "Error preparing media: ${e.message}")
            }
        }
    }

    override fun play() {
        try {
            mediaPlayer?.play()
        } catch (e: Exception) {
            android.util.Log.e("LibVlcEngine", "Error playing: ${e.message}")
        }
    }

    override fun pause() {
        try {
            mediaPlayer?.pause()
        } catch (e: Exception) {
            android.util.Log.e("LibVlcEngine", "Error pausing: ${e.message}")
        }
    }

    override fun stop() {
        try {
            mediaPlayer?.stop()
        } catch (e: Exception) {
            android.util.Log.e("LibVlcEngine", "Error stopping: ${e.message}")
        }
    }

    override fun release() {
        try {
            // Proper cleanup sequence
            mediaPlayer?.let { player ->
                if (player.isPlaying) {
                    player.stop()
                }
                player.detachViews()
                player.release()
            }
            
            libVlc?.release()
        } catch (e: Exception) {
            android.util.Log.e("LibVlcEngine", "Error releasing: ${e.message}")
        } finally {
            mediaPlayer = null
            libVlc = null
            videoLayout = null
            surfaceView = null
            context = null
        }
    }

    override fun seekTo(position: Long) {
        try {
            mediaPlayer?.time = position
        } catch (e: Exception) {
            android.util.Log.e("LibVlcEngine", "Error seeking: ${e.message}")
        }
    }

    override fun seekBack() {
        try {
            mediaPlayer?.let {
                it.time = (it.time - 10000).coerceAtLeast(0)
            }
        } catch (e: Exception) {
            android.util.Log.e("LibVlcEngine", "Error seeking back: ${e.message}")
        }
    }

    override fun seekForward() {
        try {
            mediaPlayer?.let {
                val newTime = it.time + 10000
                val maxTime = it.length
                it.time = if (maxTime > 0) newTime.coerceAtMost(maxTime) else newTime
            }
        } catch (e: Exception) {
            android.util.Log.e("LibVlcEngine", "Error seeking forward: ${e.message}")
        }
    }

    override fun isPlaying(): Boolean {
        return try {
            mediaPlayer?.isPlaying == true
        } catch (e: Exception) {
            false
        }
    }

    override fun getDuration(): Long {
        return try {
            mediaPlayer?.length ?: 0L
        } catch (e: Exception) {
            0L
        }
    }

    override fun getCurrentPosition(): Long {
        return try {
            mediaPlayer?.time ?: 0L
        } catch (e: Exception) {
            0L
        }
    }

    override fun setVolume(volume: Float) {
        try {
            mediaPlayer?.volume = (volume * 100).toInt().coerceIn(0, 100)
        } catch (e: Exception) {
            android.util.Log.e("LibVlcEngine", "Error setting volume: ${e.message}")
        }
    }

    override fun getVideoFormat(): String? {
        return try {
            val track = mediaPlayer?.currentVideoTrack
            if (track != null) {
                "${track.width}x${track.height}@${track.frameRateDen}fps"
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    override fun getBitrate(): Long {
        return try {
            val media = mediaPlayer?.media
            val stats = media?.stats
            val bitrate = stats?.inputBitrate?.toLong() ?: 0L
            media?.release()
            bitrate
        } catch (e: Exception) {
            0L
        }
    }

    override fun getDroppedFrames(): Int {
        return try {
            val media = mediaPlayer?.media
            val stats = media?.stats
            val dropped = stats?.lostPictures ?: 0
            media?.release()
            dropped
        } catch (e: Exception) {
            0
        }
    }
    
    /**
     * Set aspect ratio mode
     * @param mode: "best_fit", "fill", "16:9", "4:3", etc.
     */
    fun setAspectRatio(mode: String) {
        try {
            when (mode.lowercase()) {
                "best_fit" -> mediaPlayer?.videoScale = MediaPlayer.ScaleType.SURFACE_BEST_FIT
                "fill" -> mediaPlayer?.videoScale = MediaPlayer.ScaleType.SURFACE_FILL
                "fit_screen" -> mediaPlayer?.videoScale = MediaPlayer.ScaleType.SURFACE_FIT_SCREEN
                "16:9" -> mediaPlayer?.aspectRatio = "16:9"
                "4:3" -> mediaPlayer?.aspectRatio = "4:3"
                else -> mediaPlayer?.videoScale = MediaPlayer.ScaleType.SURFACE_BEST_FIT
            }
        } catch (e: Exception) {
            android.util.Log.e("LibVlcEngine", "Error setting aspect ratio: ${e.message}")
        }
    }
    
    // Track selection implementation
    override fun getSubtitleTracks(): List<Pair<Int, String>> {
        return try {
            val tracks = mutableListOf<Pair<Int, String>>()
            tracks.add(Pair(-1, "Disabled"))
            
            mediaPlayer?.spuTracks?.forEach { track ->
                val name = track.name ?: "Track ${track.id}"
                tracks.add(Pair(track.id, name))
            }
            tracks
        } catch (e: Exception) {
            android.util.Log.e("LibVlcEngine", "Error getting subtitle tracks: ${e.message}")
            emptyList()
        }
    }
    
    override fun getAudioTracks(): List<Pair<Int, String>> {
        return try {
            val tracks = mutableListOf<Pair<Int, String>>()
            
            mediaPlayer?.audioTracks?.forEach { track ->
                val name = track.name ?: "Track ${track.id}"
                tracks.add(Pair(track.id, name))
            }
            tracks
        } catch (e: Exception) {
            android.util.Log.e("LibVlcEngine", "Error getting audio tracks: ${e.message}")
            emptyList()
        }
    }
    
    override fun setSubtitleTrack(trackId: Int) {
        try {
            mediaPlayer?.spuTrack = trackId
        } catch (e: Exception) {
            android.util.Log.e("LibVlcEngine", "Error setting subtitle track: ${e.message}")
        }
    }
    
    override fun setAudioTrack(trackId: Int) {
        try {
            mediaPlayer?.audioTrack = trackId
        } catch (e: Exception) {
            android.util.Log.e("LibVlcEngine", "Error setting audio track: ${e.message}")
        }
    }
    
    override fun getCurrentSubtitleTrack(): Int {
        return try {
            mediaPlayer?.spuTrack ?: -1
        } catch (e: Exception) {
            -1
        }
    }
    
    override fun getCurrentAudioTrack(): Int {
        return try {
            mediaPlayer?.audioTrack ?: -1
        } catch (e: Exception) {
            -1
        }
    }
}
