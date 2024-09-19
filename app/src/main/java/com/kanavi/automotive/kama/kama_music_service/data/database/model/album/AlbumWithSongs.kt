package com.kanavi.automotive.kama.kama_music_service.data.database.model.album

import androidx.room.Embedded
import androidx.room.Relation
import com.kanavi.automotive.kama.kama_music_service.common.constant.DatabaseEntry
import com.kanavi.automotive.kama.kama_music_service.data.database.model.song.Song

data class AlbumWithSongs(
    @Embedded val album: Album,
    @Relation(
        parentColumn = DatabaseEntry.ALBUM_ID,
        entityColumn = DatabaseEntry.SONG_ALBUM_ID
    )
    val listSong: List<Song>
)