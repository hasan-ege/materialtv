package com.example.materialtv.repository

import com.example.materialtv.model.Category
import com.example.materialtv.model.LiveStream
import com.example.materialtv.model.SeriesInfoResponse
import com.example.materialtv.model.SeriesItem
import com.example.materialtv.model.VodInfoResponse
import com.example.materialtv.model.VodItem
import com.example.materialtv.network.XtreamApiService
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonNull

class XtreamRepository(private val apiService: XtreamApiService) {
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun getVodCategories(username: String, password: String): List<Category> {
        return try {
            val response = apiService.getVodCategories(username, password)
            if (response is JsonNull || (response is JsonArray && response.isEmpty())) return emptyList()
            json.decodeFromJsonElement(ListSerializer(Category.serializer()), response)
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun getSeriesCategories(username: String, password: String): List<Category> {
        return try {
            val response = apiService.getSeriesCategories(username, password)
            if (response is JsonNull || (response is JsonArray && response.isEmpty())) return emptyList()
            json.decodeFromJsonElement(ListSerializer(Category.serializer()), response)
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun getLiveCategories(username: String, password: String): List<Category> {
        return try {
            val response = apiService.getLiveCategories(username, password)
            if (response is JsonNull || (response is JsonArray && response.isEmpty())) return emptyList()
            json.decodeFromJsonElement(ListSerializer(Category.serializer()), response)
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun getVodStreams(
        username: String,
        password: String,
        categoryId: String?
    ): List<VodItem> {
        return try {
            val response = apiService.getVodStreams(username, password, categoryId = categoryId)
            if (response is JsonNull || (response is JsonArray && response.isEmpty())) return emptyList()
            json.decodeFromJsonElement(ListSerializer(VodItem.serializer()), response)
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun getSeries(
        username: String,
        password: String,
        categoryId: String?
    ): List<SeriesItem> {
        return try {
            val response = apiService.getSeries(username, password, categoryId = categoryId)
            if (response is JsonNull || (response is JsonArray && response.isEmpty())) return emptyList()
            json.decodeFromJsonElement(ListSerializer(SeriesItem.serializer()), response)
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun getLiveStreams(
        username: String,
        password: String,
        categoryId: String?
    ): List<LiveStream> {
        return try {
            val response = apiService.getLiveStreams(username, password, categoryId = categoryId)
            if (response is JsonNull || (response is JsonArray && response.isEmpty())) return emptyList()
            json.decodeFromJsonElement(ListSerializer(LiveStream.serializer()), response)
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun getSeriesInfo(
        username: String,
        password: String,
        seriesId: Int
    ): SeriesInfoResponse {
        return apiService.getSeriesInfo(username, password, seriesId = seriesId)
    }

    suspend fun getVodInfo(username: String, password: String, vodId: Int): VodItem? {
        val response = apiService.getVodInfo(username, password, vodId = vodId)
        val info = response.info
        val movieData = response.movieData
        return if (info != null && movieData != null) {
            VodItem(
                streamId = movieData.streamId?.toIntOrNull() ?: 0,
                name = info.name ?: "",
                streamIcon = info.movieImage,
                rating5Based = info.rating5based.toDouble(),
                categoryId = movieData.categoryId,
                containerExtension = movieData.containerExtension,
                year = info.year
            )
        } else {
            null
        }
    }
}