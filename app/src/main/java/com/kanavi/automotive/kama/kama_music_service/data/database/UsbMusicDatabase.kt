package com.kanavi.automotive.kama.kama_music_service.data.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.kanavi.automotive.kama.kama_music_service.common.constant.DatabaseEntry.DATABASE_VERSION
import com.kanavi.automotive.kama.kama_music_service.data.database.dao.AlbumDAO
import com.kanavi.automotive.kama.kama_music_service.data.database.dao.ArtistDAO
import com.kanavi.automotive.kama.kama_music_service.data.database.dao.FavoriteDao
import com.kanavi.automotive.kama.kama_music_service.data.database.dao.SongDAO
import com.kanavi.automotive.kama.kama_music_service.data.database.model.album.Album
import com.kanavi.automotive.kama.kama_music_service.data.database.model.artist.Artist
import com.kanavi.automotive.kama.kama_music_service.data.database.model.favorite.Favorite
import com.kanavi.automotive.kama.kama_music_service.data.database.model.song.Song

@Database(
    entities = [Song::class, Album::class, Artist::class, Favorite::class],
    version = DATABASE_VERSION,
    exportSchema = false
)
abstract class UsbMusicDatabase : RoomDatabase() {
    abstract fun songDAO(): SongDAO
    abstract fun albumDAO(): AlbumDAO
    abstract fun artistDAO(): ArtistDAO
    abstract fun favoriteDao(): FavoriteDao
}