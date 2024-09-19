package com.kanavi.automotive.kama.kama_music_service.data.database.model

import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import com.kanavi.automotive.kama.kama_music_service.common.util.AlphanumericComparator
import com.kanavi.automotive.kama.kama_music_service.common.constant.Constants.SORT_BY_DATE_MODIFIED
import com.kanavi.automotive.kama.kama_music_service.common.constant.Constants.SORT_BY_NAME
import com.kanavi.automotive.kama.kama_music_service.common.constant.Constants.SORT_BY_SIZE
import com.kanavi.automotive.kama.kama_music_service.common.constant.Constants.SORT_DESCENDING
import com.kanavi.automotive.kama.kama_music_service.common.constant.Constants.SORT_USE_NUMERIC_VALUE
import com.kanavi.automotive.kama.kama_music_service.common.extension.getAlbum
import com.kanavi.automotive.kama.kama_music_service.common.extension.getArtist
import com.kanavi.automotive.kama.kama_music_service.common.extension.getDirectChildrenCount
import com.kanavi.automotive.kama.kama_music_service.common.extension.getDuration
import com.kanavi.automotive.kama.kama_music_service.common.extension.getFileCount
import com.kanavi.automotive.kama.kama_music_service.common.extension.getFormattedDuration
import com.kanavi.automotive.kama.kama_music_service.common.extension.getParentPath
import com.kanavi.automotive.kama.kama_music_service.common.extension.getProperSize
import com.kanavi.automotive.kama.kama_music_service.common.extension.getSizeFromContentUri
import com.kanavi.automotive.kama.kama_music_service.common.extension.getTitle
import com.kanavi.automotive.kama.kama_music_service.common.extension.isAudioFast
import com.kanavi.automotive.kama.kama_music_service.common.extension.normalizeString
import java.io.File

open class FileDirItem(
    val path: String,
    val name: String = "",
    var isDirectory: Boolean = false,
    var children: Int = 0,
    var size: Long = 0L,
    var modified: Long = 0L,
    var mediaStoreId: Long = 0L
) :
    Comparable<FileDirItem> {
    companion object {
        var sorting = 0
    }

    override fun toString() =
        "FileDirItem(path=$path, name=$name, isDirectory=$isDirectory, children=$children, size=$size, modified=$modified, mediaStoreId=$mediaStoreId)"

    override fun compareTo(other: FileDirItem): Int {
        return if (isDirectory && !other.isDirectory) {
            -1
        } else if (!isDirectory && other.isDirectory) {
            1
        } else {
            var result: Int
            when {
                sorting and SORT_BY_NAME != 0 -> {
                    result = if (sorting and SORT_USE_NUMERIC_VALUE != 0) {
                        AlphanumericComparator().compare(
                            name.normalizeString().toLowerCase(),
                            other.name.normalizeString().toLowerCase()
                        )
                    } else {
                        name.normalizeString().toLowerCase()
                            .compareTo(other.name.normalizeString().toLowerCase())
                    }
                }

                sorting and SORT_BY_SIZE != 0 -> result = when {
                    size == other.size -> 0
                    size > other.size -> 1
                    else -> -1
                }

                sorting and SORT_BY_DATE_MODIFIED != 0 -> {
                    result = when {
                        modified == other.modified -> 0
                        modified > other.modified -> 1
                        else -> -1
                    }
                }

                else -> {
                    result =
                        getExtension().toLowerCase().compareTo(other.getExtension().toLowerCase())
                }
            }

            if (sorting and SORT_DESCENDING != 0) {
                result *= -1
            }
            result
        }
    }

    fun getExtension() = if (isDirectory) name else path.substringAfterLast('.', "")

    fun getProperSize(context: Context, countHidden: Boolean): Long {
        return when {

            path.startsWith("content://") -> {
                try {
                    context.contentResolver.openInputStream(Uri.parse(path))?.available()?.toLong()
                        ?: 0L
                } catch (e: Exception) {
                    context.getSizeFromContentUri(Uri.parse(path))
                }
            }

            else -> File(path).getProperSize(countHidden)
        }
    }

    fun getProperFileCount(context: Context, countHidden: Boolean): Int {
        return File(path).getFileCount(countHidden)
    }

    fun getDirectChildrenCount(context: Context, countHiddenItems: Boolean): Int {
        return File(path).getDirectChildrenCount(context, countHiddenItems)
    }


    fun getParentPath() = path.getParentPath()

    fun getDuration(context: Context) = context.getDuration(path)?.getFormattedDuration()

    fun getFileDurationSeconds(context: Context) = context.getDuration(path)

    fun getArtist(context: Context) = context.getArtist(path)

    fun getAlbum(context: Context) = context.getAlbum(path)

    fun getTitle(context: Context) = context.getTitle(path)


    fun getSignature(): String {
        val lastModified = if (modified > 1) {
            modified
        } else {
            File(path).lastModified()
        }

        return "$path-$lastModified-$size"
    }


    fun assembleContentUri(): Uri {
        val uri = when {
            path.isAudioFast() -> MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
            else -> MediaStore.Files.getContentUri("external")
        }

        return Uri.withAppendedPath(uri, mediaStoreId.toString())
    }
}
