package com.hasanege.materialtv.repository

import com.hasanege.materialtv.data.AppDatabase
import com.hasanege.materialtv.data.FavoriteEntity
import com.hasanege.materialtv.data.FavoriteListEntity
import com.hasanege.materialtv.model.FavoriteItem
import com.hasanege.materialtv.model.FavoriteList
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class FavoritesRepository(private val database: AppDatabase) {
    private val favoriteDao = database.favoriteDao()
    private val listDao = database.favoriteListDao()

    // Favorites operations
    suspend fun addFavorite(favorite: FavoriteItem): Long {
        return favoriteDao.insert(favorite.toEntity())
    }

    suspend fun updateFavorite(favorite: FavoriteItem) {
        favoriteDao.update(favorite.toEntity())
    }

    suspend fun removeFavorite(favorite: FavoriteItem) {
        favoriteDao.delete(favorite.toEntity())
    }

    suspend fun removeFavoriteByContent(contentId: Int, contentType: String) {
        favoriteDao.deleteByContent(contentId, contentType)
    }

    suspend fun isFavorite(contentId: Int, contentType: String): Boolean {
        return favoriteDao.isFavorite(contentId, contentType)
    }

    suspend fun getFavoriteByContent(contentId: Int, contentType: String): FavoriteItem? {
        return favoriteDao.getFavoriteByContent(contentId, contentType)?.toModel()
    }

    fun getAllFavorites(): Flow<List<FavoriteItem>> {
        return favoriteDao.getAllFavorites().map { list -> list.map { it.toModel() } }
    }

    fun getFavoritesByList(listId: Long): Flow<List<FavoriteItem>> {
        return favoriteDao.getFavoritesByList(listId).map { list -> list.map { it.toModel() } }
    }

    fun getFavoritesByType(type: String): Flow<List<FavoriteItem>> {
        return favoriteDao.getFavoritesByType(type).map { list -> list.map { it.toModel() } }
    }

    fun getFavoritesByGenre(genre: String): Flow<List<FavoriteItem>> {
        return favoriteDao.getFavoritesByGenre(genre).map { list -> list.map { it.toModel() } }
    }

    fun getFavoritesByWatchedStatus(watched: Boolean): Flow<List<FavoriteItem>> {
        return favoriteDao.getFavoritesByWatchedStatus(watched).map { list -> list.map { it.toModel() } }
    }

    suspend fun updateOrderIndex(favoriteId: Long, newIndex: Int) {
        favoriteDao.updateOrderIndex(favoriteId, newIndex)
    }

    suspend fun reorderFavorites(favorites: List<FavoriteItem>) {
        favorites.forEachIndexed { index, favorite ->
            favoriteDao.updateOrderIndex(favorite.id, index)
        }
    }

    // Custom lists operations
    suspend fun createList(list: FavoriteList): Long {
        return listDao.insert(list.toEntity())
    }

    suspend fun updateList(list: FavoriteList) {
        listDao.update(list.toEntity())
    }

    suspend fun deleteList(list: FavoriteList) {
        // First delete all favorites in this list
        favoriteDao.deleteAllByList(list.listId)
        // Then delete the list itself
        listDao.delete(list.toEntity())
    }

    fun getAllLists(): Flow<List<FavoriteList>> {
        return listDao.getAllLists().map { list -> 
            list.map { entity ->
                val count = favoriteDao.getCountByList(entity.listId)
                entity.toModel(count)
            }
        }
    }

    suspend fun getListById(listId: Long): FavoriteList? {
        val entity = listDao.getListById(listId) ?: return null
        val count = favoriteDao.getCountByList(listId)
        return entity.toModel(count)
    }

    suspend fun updateListOrderIndex(listId: Long, newIndex: Int) {
        listDao.updateOrderIndex(listId, newIndex)
    }

    suspend fun reorderLists(lists: List<FavoriteList>) {
        lists.forEachIndexed { index, list ->
            listDao.updateOrderIndex(list.listId, index)
        }
    }

    // Extension functions for conversions
    private fun FavoriteItem.toEntity() = FavoriteEntity(
        id = id,
        contentId = contentId,
        contentType = contentType,
        name = name,
        thumbnailUrl = thumbnailUrl,
        addedAt = addedAt,
        customThumbnail = customThumbnail,
        rating = rating,
        notes = notes,
        listId = listId,
        orderIndex = orderIndex,
        genre = genre,
        year = year,
        isWatched = isWatched,
        categoryId = categoryId,
        seriesId = seriesId,
        streamIcon = streamIcon
    )

    private fun FavoriteEntity.toModel() = FavoriteItem(
        id = id,
        contentId = contentId,
        contentType = contentType,
        name = name,
        thumbnailUrl = thumbnailUrl,
        addedAt = addedAt,
        customThumbnail = customThumbnail,
        rating = rating,
        notes = notes,
        listId = listId,
        orderIndex = orderIndex,
        genre = genre,
        year = year,
        isWatched = isWatched,
        categoryId = categoryId,
        seriesId = seriesId,
        streamIcon = streamIcon
    )

    private fun FavoriteList.toEntity() = FavoriteListEntity(
        listId = listId,
        listName = listName,
        createdAt = createdAt,
        orderIndex = orderIndex,
        iconName = iconName,
        colorHex = colorHex
    )

    private fun FavoriteListEntity.toModel(count: Int) = FavoriteList(
        listId = listId,
        listName = listName,
        createdAt = createdAt,
        orderIndex = orderIndex,
        iconName = iconName,
        colorHex = colorHex,
        itemCount = count
    )
}
