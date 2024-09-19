package com.kanavi.automotive.kama.kama_music_service.common.util

import android.content.Context
import com.kanavi.automotive.kama.kama_music_service.common.extension.getAlbum
import com.kanavi.automotive.kama.kama_music_service.common.extension.getArtist
import com.kanavi.automotive.kama.kama_music_service.common.extension.getDuration
import com.kanavi.automotive.kama.kama_music_service.common.extension.getFilenameFromPath
import com.kanavi.automotive.kama.kama_music_service.common.extension.getMediaStoreLastModified
import com.kanavi.automotive.kama.kama_music_service.common.extension.getParentPath
import com.kanavi.automotive.kama.kama_music_service.common.extension.getTitle
import com.kanavi.automotive.kama.kama_music_service.common.extension.getUsbID
import com.kanavi.automotive.kama.kama_music_service.data.database.model.MediaData
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

object FileUtil: KoinComponent {
    fun getMediaData(path: String?): MediaData? {
        return if (path.isNullOrEmpty()) null else {
            val context: Context by inject()
            val title = context.getTitle(path)
            val artist = context.getArtist(path)
            val album = context.getAlbum(path)
            val duration = context.getDuration(path)
            val lastModified = context.getMediaStoreLastModified(path)
            val parentFolder = path.getParentPath().getFilenameFromPath()
            val usbID = path.getUsbID()
            MediaData(
                usbId = usbID,
                path = path,
                parentFolder = parentFolder,
                title = title ?: "",
                artist = artist ?: "",
                album = album ?: "",
                duration = duration ?: 0,
                lastModified = lastModified
            )
        }

    }
}