package com.hasanege.materialtv.repository

import com.hasanege.materialtv.model.Category
import com.hasanege.materialtv.model.LiveStream
import com.hasanege.materialtv.model.SeriesInfoResponse
import com.hasanege.materialtv.model.SeriesItem
import com.hasanege.materialtv.model.VodInfoResponse
import com.hasanege.materialtv.model.VodItem
import com.hasanege.materialtv.network.XtreamApiService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonNull

class XtreamRepository(private val apiService: XtreamApiService?) {
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun getVodCategories(username: String, password: String): List<Category> = withContext(Dispatchers.IO) {
        if (apiService == null) return@withContext emptyList()
        try {
            val response = apiService.getVodCategories(username, password)
            if (response is JsonNull || (response is JsonArray && response.isEmpty())) return@withContext emptyList()
            json.decodeFromJsonElement(ListSerializer(Category.serializer()), response)
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun getSeriesCategories(username: String, password: String): List<Category> = withContext(Dispatchers.IO) {
        if (apiService == null) return@withContext emptyList()
        try {
            val response = apiService.getSeriesCategories(username, password)
            if (response is JsonNull || (response is JsonArray && response.isEmpty())) return@withContext emptyList()
            json.decodeFromJsonElement(ListSerializer(Category.serializer()), response)
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun getLiveCategories(username: String, password: String): List<Category> = withContext(Dispatchers.IO) {
        if (apiService == null) return@withContext emptyList()
        try {
            val response = apiService.getLiveCategories(username, password)
            if (response is JsonNull || (response is JsonArray && response.isEmpty())) return@withContext emptyList()
            json.decodeFromJsonElement(ListSerializer(Category.serializer()), response)
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun getVodStreams(
        username: String,
        password: String,
        categoryId: String?
    ): List<VodItem> = withContext(Dispatchers.IO) {
        if (apiService == null) return@withContext emptyList()
        try {
            val response = apiService.getVodStreams(username, password, categoryId = categoryId)
            if (response is JsonNull || (response is JsonArray && response.isEmpty())) return@withContext emptyList()
            json.decodeFromJsonElement(ListSerializer(VodItem.serializer()), response)
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun getSeries(
        username: String,
        password: String,
        categoryId: String?
    ): List<SeriesItem> = withContext(Dispatchers.IO) {
        if (apiService == null) return@withContext emptyList()
        try {
            val response = apiService.getSeries(username, password, categoryId = categoryId)
            if (response is JsonNull || (response is JsonArray && response.isEmpty())) return@withContext emptyList()
            json.decodeFromJsonElement(ListSerializer(SeriesItem.serializer()), response)
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun getLiveStreams(
        username: String,
        password: String,
        categoryId: String?
    ): List<LiveStream> = withContext(Dispatchers.IO) {
        if (apiService == null) return@withContext emptyList()
        try {
            val response = apiService.getLiveStreams(username, password, categoryId = categoryId)
            if (response is JsonNull || (response is JsonArray && response.isEmpty())) return@withContext emptyList()
            json.decodeFromJsonElement(ListSerializer(LiveStream.serializer()), response)
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun getSeriesInfo(
        username: String,
        password: String,
        seriesId: Int
    ): SeriesInfoResponse? = withContext(Dispatchers.IO) {
        val response = apiService?.getSeriesInfo(username, password, seriesId = seriesId) ?: return@withContext null
        try {
            if (response is kotlinx.serialization.json.JsonObject) {
                json.decodeFromJsonElement(SeriesInfoResponse.serializer(), response)
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    suspend fun getVodInfo(username: String, password: String, vodId: Int): VodItem? = withContext(Dispatchers.IO) {
        if (apiService == null) return@withContext null
        try {
            val response = apiService.getVodInfo(username, password, vodId = vodId)
            val info = response.info
            val movieData = response.movieData
            if (info != null && movieData != null) {
                VodItem(
                    streamId = movieData.streamId?.toIntOrNull() ?: 0,
                    name = info.name ?: "",
                    streamIcon = info.movieImage,
                    rating5Based = try { info.rating5based?.toDouble() } catch(e: Exception) { 0.0 },
                    categoryId = movieData.categoryId,
                    containerExtension = movieData.containerExtension,
                    year = info.year
                )
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    suspend fun getVodDetails(username: String, password: String, vodId: Int): VodInfoResponse? = withContext(Dispatchers.IO) {
        if (apiService == null) return@withContext null
        try {
            apiService.getVodInfo(username, password, vodId = vodId)
        } catch (e: Exception) {
            null
        }
    }
}
