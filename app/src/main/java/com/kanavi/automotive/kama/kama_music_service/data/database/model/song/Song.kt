package com.kanavi.automotive.kama.kama_music_service.data.database.model.song

import android.content.ContentUris
import android.net.Uri
import android.os.Parcelable
import android.provider.MediaStore
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.kanavi.automotive.kama.kama_music_service.common.util.AlphanumericComparator
import com.kanavi.automotive.kama.kama_music_service.common.constant.Constants.PLAYER_SORT_BY_ARTIST_TITLE
import com.kanavi.automotive.kama.kama_music_service.common.constant.Constants.PLAYER_SORT_BY_DATE_ADDED
import com.kanavi.automotive.kama.kama_music_service.common.constant.Constants.PLAYER_SORT_BY_TITLE
import com.kanavi.automotive.kama.kama_music_service.common.constant.Constants.SORT_DESCENDING
import com.kanavi.automotive.kama.kama_music_service.common.constant.DatabaseEntry.SONG_ALBUM
import com.kanavi.automotive.kama.kama_music_service.common.constant.DatabaseEntry.SONG_ALBUM_ID
import com.kanavi.automotive.kama.kama_music_service.common.constant.DatabaseEntry.SONG_ARTIST
import com.kanavi.automotive.kama.kama_music_service.common.constant.DatabaseEntry.SONG_ARTIST_ID
import com.kanavi.automotive.kama.kama_music_service.common.constant.DatabaseEntry.SONG_COVER_ART
import com.kanavi.automotive.kama.kama_music_service.common.constant.DatabaseEntry.SONG_DATE_ADDED
import com.kanavi.automotive.kama.kama_music_service.common.constant.DatabaseEntry.SONG_DURATION
import com.kanavi.automotive.kama.kama_music_service.common.constant.DatabaseEntry.SONG_FAVORITE
import com.kanavi.automotive.kama.kama_music_service.common.constant.DatabaseEntry.SONG_FOLDER_NAME
import com.kanavi.automotive.kama.kama_music_service.common.constant.DatabaseEntry.SONG_ID
import com.kanavi.automotive.kama.kama_music_service.common.constant.DatabaseEntry.SONG_MEDIA_STORE_ID
import com.kanavi.automotive.kama.kama_music_service.common.constant.DatabaseEntry.SONG_PATH
import com.kanavi.automotive.kama.kama_music_service.common.constant.DatabaseEntry.SONG_TABLE_NAME
import com.kanavi.automotive.kama.kama_music_service.common.constant.DatabaseEntry.SONG_TITLE
import com.kanavi.automotive.kama.kama_music_service.common.constant.DatabaseEntry.SONG_YEAR
import com.kanavi.automotive.kama.kama_music_service.common.constant.DatabaseEntry.USB_ID
import com.kanavi.automotive.kama.kama_music_service.common.extension.getFormattedDuration
import com.kanavi.automotive.kama.kama_music_service.common.extension.sortSafely
import java.io.File
import kotlinx.parcelize.Parcelize

@Entity(
    tableName = SONG_TABLE_NAME,
    indices = [Index(value = [SONG_PATH], unique = true)]
)
@Parcelize
data class Song(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = SONG_ID)
    var id: Long = 0L,
    @ColumnInfo(name = SONG_MEDIA_STORE_ID) var mediaStoreId: Long = 0L,
    @ColumnInfo(name = SONG_TITLE) var title: String = "unknown",
    @ColumnInfo(name = SONG_ARTIST) var artist: String = "unknown",
    @ColumnInfo(name = SONG_PATH) var path: String = "",
    @ColumnInfo(name = SONG_DURATION) var duration: Long = 0L,
    @ColumnInfo(name = SONG_ALBUM) var album: String = "unknown",
    @ColumnInfo(name = SONG_COVER_ART) var coverArt: String = "",
    @ColumnInfo(name = SONG_FOLDER_NAME) var folderName: String = "",
    @ColumnInfo(name = SONG_ALBUM_ID) var albumId: Long = 0L,
    @ColumnInfo(name = SONG_ARTIST_ID) var artistId: Long = 0L,
    @ColumnInfo(name = SONG_YEAR) var year: Int = 0,
    @ColumnInfo(name = SONG_DATE_ADDED) var dateAdded: Long = 0L,
    @ColumnInfo(name = USB_ID) var usbId: String = "",
    @ColumnInfo(name = SONG_FAVORITE) var favorite: Boolean = false,

    ) : Parcelable {
    override fun hashCode(): Int {
        var result = title.hashCode()
        result = 31 * result + artist.hashCode()
        result = 31 * result + path.hashCode()
        result = 31 * result + duration.toInt()
        result = 31 * result + album.hashCode()
        result = 31 * result + folderName.hashCode()
        result = 31 * result + year
        result = 31 * result + dateAdded.hashCode()
        result = 31 * result + usbId.hashCode()
        result = 31 * result + favorite.hashCode()
        return result
    }

    override fun toString(): String {
        return "Song(id=$id, mediaStoreId=$mediaStoreId, title='$title', artist='$artist', path='$path', duration=$duration, album='$album', coverArt='$coverArt', folderName='$folderName', albumId=$albumId, artistId=$artistId, year=$year, dateAdded=$dateAdded, usbId='$usbId')"
    }

    companion object {
        @JvmStatic
        val emptySong = Song(
            id = -1,
            mediaStoreId = -1,
            title = "",
            artist = "",
            path = "",
            duration = 0,
            album = "",
            coverArt = "",
            folderName = "",
            albumId = -1,
            artistId = -1,
            year = 0,
            dateAdded = 0,
            usbId = "",
            favorite = false
        )

        fun getComparator(sorting: Int) = Comparator<Song> { first, second ->
            var result = when {
                sorting and PLAYER_SORT_BY_TITLE != 0 -> {
                    when {
                        first.title == MediaStore.UNKNOWN_STRING && second.title != MediaStore.UNKNOWN_STRING -> 1
                        first.title != MediaStore.UNKNOWN_STRING && second.title == MediaStore.UNKNOWN_STRING -> -1
                        else -> AlphanumericComparator().compare(
                            first.title.lowercase(),
                            second.title.lowercase()
                        )
                    }
                }

                sorting and PLAYER_SORT_BY_ARTIST_TITLE != 0 -> {
                    when {
                        first.artist == MediaStore.UNKNOWN_STRING && second.artist != MediaStore.UNKNOWN_STRING -> 1
                        first.artist != MediaStore.UNKNOWN_STRING && second.artist == MediaStore.UNKNOWN_STRING -> -1
                        else -> AlphanumericComparator().compare(
                            first.artist.lowercase(),
                            second.artist.lowercase()
                        )
                    }
                }

                sorting and PLAYER_SORT_BY_DATE_ADDED != 0 -> first.dateAdded.compareTo(second.dateAdded)
                else -> first.duration.compareTo(second.duration)
            }

            if (sorting and SORT_DESCENDING != 0) {
                result *= -1
            }

            return@Comparator result
        }
    }

    fun getBubbleText(sorting: Int) = when {
        sorting and PLAYER_SORT_BY_TITLE != 0 -> title
        sorting and PLAYER_SORT_BY_ARTIST_TITLE != 0 -> artist
        else -> duration.getFormattedDuration()
    }

    fun getUri(): Uri = if (mediaStoreId == 0L) {
        Uri.fromFile(File(path))
    } else {
        ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, mediaStoreId)
    }
}

fun ArrayList<Song>.sortSafety(sorting: Int) = sortSafely(Song.getComparator(sorting))
