package com.kanavi.automotive.kama.kama_music_service.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.kanavi.automotive.kama.kama_music_service.data.database.model.album.Album
import com.kanavi.automotive.kama.kama_music_service.data.database.model.album.AlbumWithSongs
import com.kanavi.automotive.kama.kama_music_service.common.constant.DatabaseEntry
import com.kanavi.automotive.kama.kama_music_service.common.constant.DatabaseEntry.ALBUM_ARTIST_ID
import com.kanavi.automotive.kama.kama_music_service.common.constant.DatabaseEntry.ALBUM_ID
import com.kanavi.automotive.kama.kama_music_service.common.constant.DatabaseEntry.ALBUM_TABLE_NAME

@Dao
interface AlbumDAO {
    @Transaction
    @Query("SELECT * FROM $ALBUM_TABLE_NAME")
    fun getAlbumWithSongs(): List<AlbumWithSongs>

    @Query("SELECT * FROM $ALBUM_TABLE_NAME")
    fun getAll(): List<Album>

    @Query("SELECT * FROM $ALBUM_TABLE_NAME WHERE ${DatabaseEntry.USB_ID} = :usbId")
    fun getAllByUsbID(usbId: String): List<Album>

    @Query("SELECT * FROM $ALBUM_TABLE_NAME WHERE $ALBUM_ID  = :id")
    fun getAlbumWithId(id: Long): Album?

    @Query("SELECT * FROM $ALBUM_TABLE_NAME WHERE $ALBUM_ARTIST_ID = :artistId")
    fun getArtistAlbums(artistId: Long): List<Album>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(album: Album): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(albums: List<Album>)


    @Query("DELETE FROM $ALBUM_TABLE_NAME WHERE $ALBUM_ID = :id")
    suspend fun deleteAlbum(id: Long)
}