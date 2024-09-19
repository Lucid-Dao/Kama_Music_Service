package com.kanavi.automotive.kama.kama_music_service.service.mediaSource

import android.content.Context
import android.os.Bundle
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaDescriptionCompat
import androidx.core.content.edit
import androidx.core.net.toUri
import com.kanavi.automotive.kama.kama_music_service.common.constant.Constants.MUSIC_PACKAGE_NAME
import com.kanavi.automotive.kama.kama_music_service.common.extension.albumArtUri
import com.kanavi.automotive.kama.kama_music_service.data.database.model.song.Song
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class PersistentStorage(val context: Context) {

    // Store any data which must persist between restarts, such as the most recently played song.
    private val prefs = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    /**
     * After booting, Android will attempt to build static media controls for the most
     * recently played song. Artwork for these media controls should not be loaded
     * from the network as it may be too slow or unavailable immediately after boot. Instead
     * we convert the iconUri to point to the Glide on-disk cache.
     */
    suspend fun saveRecentSong(song: Song, position: Long = 0L) {
        withContext(Dispatchers.IO) {
            prefs.edit {
                putLong(RECENT_SONG_MEDIA_ID_KEY, song.id)
                putString(RECENT_SONG_TITLE_KEY, song.title)
                putString(RECENT_SONG_ARTIST_KEY, song.artist)
                putString(RECENT_SONG_ICON_URI_KEY, song.albumArtUri.toString())
                putLong(RECENT_SONG_POSITION_KEY, position)
            }
        }

    }

    fun loadRecentSong(): MediaBrowserCompat.MediaItem? {
        val mediaId = prefs.getString(RECENT_SONG_MEDIA_ID_KEY, null)
        return if (mediaId == null) {
            null
        } else {
            val extras = Bundle().also {
                val position = prefs.getLong(RECENT_SONG_POSITION_KEY, 0L)
                it.putLong(MEDIA_DESCRIPTION_EXTRAS_START_PLAYBACK_POSITION_MS, position)
            }
            return MediaBrowserCompat.MediaItem(
                MediaDescriptionCompat.Builder()
                    .setMediaId(prefs.getLong(RECENT_SONG_MEDIA_ID_KEY, 0L).toString())
                    .setTitle(prefs.getString(RECENT_SONG_TITLE_KEY, ""))
                    .setSubtitle(prefs.getString(RECENT_SONG_ARTIST_KEY, ""))
                    .setIconUri(prefs.getString(RECENT_SONG_ICON_URI_KEY, "")?.toUri())
                    .setExtras(extras)
                    .build(), MediaBrowserCompat.MediaItem.FLAG_PLAYABLE
            )
        }

    }

    companion object {
        const val PREFERENCES_NAME = "$MUSIC_PACKAGE_NAME.persistent_storage"
        const val RECENT_SONG_MEDIA_ID_KEY = "$MUSIC_PACKAGE_NAME.recent_song_media_id"
        const val RECENT_SONG_TITLE_KEY = "$MUSIC_PACKAGE_NAME.recent_song_title"
        const val RECENT_SONG_ARTIST_KEY = "$MUSIC_PACKAGE_NAME.recent_song_artist"
        const val RECENT_SONG_ICON_URI_KEY = "$MUSIC_PACKAGE_NAME.recent_song_icon_uri"
        const val RECENT_SONG_POSITION_KEY = "$MUSIC_PACKAGE_NAME.recent_song_position"
        const val MEDIA_DESCRIPTION_EXTRAS_START_PLAYBACK_POSITION_MS =
            "$MUSIC_PACKAGE_NAME.playback_start_position_ms"
    }
}