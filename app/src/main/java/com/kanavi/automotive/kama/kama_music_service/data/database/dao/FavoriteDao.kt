package com.kanavi.automotive.kama.kama_music_service.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.kanavi.automotive.kama.kama_music_service.common.constant.DatabaseEntry.FAVORITE_PATH_SONG
import com.kanavi.automotive.kama.kama_music_service.common.constant.DatabaseEntry.FAVORITE_TABLE_NAME
import com.kanavi.automotive.kama.kama_music_service.common.constant.DatabaseEntry.USB_ID
import com.kanavi.automotive.kama.kama_music_service.data.database.model.favorite.Favorite

@Dao
interface FavoriteDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(favorite: Favorite)

    @Query("DELETE FROM $FAVORITE_TABLE_NAME WHERE $FAVORITE_PATH_SONG = :path")
    suspend fun removeFavoriteSong(path: String)

    @Query("SELECT * FROM $FAVORITE_TABLE_NAME WHERE $USB_ID = :usbId")
    suspend fun getAllFavoriteByUsbID(usbId: String): List<Favorite>

}