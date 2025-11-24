package com.hasanege.materialtv.player

import android.content.Context
import android.view.ViewGroup

interface PlayerEngine {
    fun initialize(context: Context)
    fun attach(container: ViewGroup)
    fun detach()
    fun prepare(url: String)
    fun play()
    fun pause()
    fun stop()
    fun release()
    fun seekTo(position: Long)
    fun seekBack()
    fun seekForward()
    fun isPlaying(): Boolean
    fun getDuration(): Long
    fun getCurrentPosition(): Long
    fun setVolume(volume: Float)
    
    // Stats
    fun getVideoFormat(): String?
    fun getBitrate(): Long
    fun getDroppedFrames(): Int
    
    // Track selection
    fun getSubtitleTracks(): List<Pair<Int, String>>
    fun getAudioTracks(): List<Pair<Int, String>>
    fun setSubtitleTrack(trackId: Int)
    fun setAudioTrack(trackId: Int)
    fun getCurrentSubtitleTrack(): Int
    fun getCurrentAudioTrack(): Int
}
