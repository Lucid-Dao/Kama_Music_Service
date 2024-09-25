package com.kanavi.automotive.kama.kama_music_service.data.database.model.artist

import android.provider.MediaStore
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.kanavi.automotive.kama.kama_music_service.common.util.AlphanumericComparator
import com.kanavi.automotive.kama.kama_music_service.common.constant.Constants.SORT_DESCENDING
import com.kanavi.automotive.kama.kama_music_service.common.constant.DatabaseEntry.ARTIST_ALBUM_COUNT
import com.kanavi.automotive.kama.kama_music_service.common.constant.DatabaseEntry.ARTIST_COVER_ART
import com.kanavi.automotive.kama.kama_music_service.common.constant.DatabaseEntry.ARTIST_ID
import com.kanavi.automotive.kama.kama_music_service.common.constant.DatabaseEntry.ARTIST_SONG_COUNT
import com.kanavi.automotive.kama.kama_music_service.common.constant.DatabaseEntry.ARTIST_TABLE_NAME
import com.kanavi.automotive.kama.kama_music_service.common.constant.DatabaseEntry.ARTIST_TITLE
import com.kanavi.automotive.kama.kama_music_service.common.constant.DatabaseEntry.USB_ID
import com.kanavi.automotive.kama.kama_music_service.common.extension.sortSafely

@Entity(tableName = ARTIST_TABLE_NAME, indices = [(Index(value = [ARTIST_TITLE], unique = true))])
data class Artist(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = ARTIST_ID)
    var id: Long,
    @ColumnInfo(name = ARTIST_TITLE) val title: String,
    @ColumnInfo(name = ARTIST_ALBUM_COUNT) var albumCount: Int,
    @ColumnInfo(name = ARTIST_SONG_COUNT) var songCount: Int,
    @ColumnInfo(name = ARTIST_COVER_ART) var albumArt: String,
    @ColumnInfo(name = USB_ID) var usbId: String,

    ) {
    override fun hashCode(): Int {
        var result = title.hashCode()
        result = 31 * result + albumCount
        result = 31 * result + songCount
        result = 31 * result + usbId.hashCode()
        return result
    }

    companion object {
        fun getComparator(sorting: Int) = Comparator<Artist> { first, second ->
            var result =
                when {
                    first.title == MediaStore.UNKNOWN_STRING && second.title != MediaStore.UNKNOWN_STRING -> 1
                    first.title != MediaStore.UNKNOWN_STRING && second.title == MediaStore.UNKNOWN_STRING -> -1
                    else -> AlphanumericComparator().compare(
                        first.title.lowercase(),
                        second.title.lowercase()
                    )
                }

            if (sorting and SORT_DESCENDING != 0) {
                result *= -1
            }

            return@Comparator result
        }
    }
}

fun ArrayList<Artist>.sortSafely(sorting: Int) = sortSafely(Artist.getComparator(sorting))
