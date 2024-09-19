package com.kanavi.automotive.kama.kama_music_service.data.database.model.artist

import androidx.room.Embedded
import androidx.room.Relation
import com.kanavi.automotive.kama.kama_music_service.common.constant.DatabaseEntry
import com.kanavi.automotive.kama.kama_music_service.data.database.model.album.Album
import com.kanavi.automotive.kama.kama_music_service.data.database.model.artist.Artist

data class ArtistWithAlbums(
    @Embedded val artist: Artist,
    @Relation(
        parentColumn = DatabaseEntry.ARTIST_ID,
        entityColumn = DatabaseEntry.ALBUM_ARTIST_ID
    )
    val listAlbum: List<Album>
)