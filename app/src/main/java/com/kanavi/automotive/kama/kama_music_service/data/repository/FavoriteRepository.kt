package com.kanavi.automotive.kama.kama_music_service.data.repository


import com.kanavi.automotive.kama.kama_music_service.data.database.dao.FavoriteDao
import com.kanavi.automotive.kama.kama_music_service.data.database.model.favorite.Favorite

class FavoriteRepository(private val favoriteDao: FavoriteDao) {

    suspend fun insert(favorite: Favorite) = favoriteDao.insert(favorite)

    suspend fun removeFavoriteSong(path: String) = favoriteDao.removeFavoriteSong(path)

    suspend fun getAllFavoriteByUsbID(usbID: String) = favoriteDao.getAllFavoriteByUsbID(usbID)
}