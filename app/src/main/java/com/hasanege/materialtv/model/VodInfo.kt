package com.hasanege.materialtv.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class VodInfo(
    @SerialName("movie_image") val movieImage: String? = null,
    @SerialName("plot") val plot: String? = null,
    @SerialName("cast") val cast: String? = null,
    @SerialName("director") val director: String? = null,
    @SerialName("genre") val genre: String? = null,
    @SerialName("releaseDate") val releaseDate: String? = null,
    @SerialName("rating") val rating: String? = null,
    @SerialName("rating_5based") val rating5based: Float = 0f,
    @SerialName("backdrop_path") val backdropPath: List<String>? = null,
    @SerialName("youtube_trailer") val youtubeTrailer: String? = null,
    @SerialName("duration_secs") val durationSecs: Int? = null,
    @SerialName("duration") val duration: String? = null,
    @SerialName("name") val name: String? = null,
    @SerialName("year") val year: String? = null
)
