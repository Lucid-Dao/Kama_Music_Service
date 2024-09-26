package com.kanavi.automotive.kama.kama_music_service.service.mediaSource

import android.content.Context
import android.net.Uri
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaDescriptionCompat
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.drawable.toBitmap
import androidx.core.os.bundleOf


object UsbMediaItem {
    fun with(context: Context): Builder {
        return Builder(context)
    }

    class Builder(private val mContext: Context) {
        private var mBuilder: MediaDescriptionCompat.Builder?
        private var mFlags = 0
        fun mediaID(mediaID: String): Builder {
            mBuilder?.setMediaId(mediaID)
            return this
        }

        fun mediaID(category: String?, id: Long): Builder {
            return mediaID(MediaIDHelper.createMediaID(id.toString(), category))
        }

        fun title(title: String): Builder {
            mBuilder?.setTitle(title)
            return this
        }

        fun subTitle(subTitle: String): Builder {
            mBuilder?.setSubtitle(subTitle)
            return this
        }

        fun description(description: String): Builder {
            mBuilder?.setDescription(description)
            return this
        }

        fun icon(uri: Uri?): Builder {
            mBuilder?.setIconUri(uri)
            return this
        }

        fun icon(iconDrawableId: Int): Builder {
            mBuilder?.setIconBitmap(
                ResourcesCompat.getDrawable(
                    mContext.resources,
                    iconDrawableId,
                    mContext.theme
                )?.toBitmap()
            )
            return this
        }

        fun gridLayout(isGrid: Boolean): Builder {

            val hints = bundleOf(
                CONTENT_STYLE_SUPPORTED to true,
                CONTENT_STYLE_BROWSABLE_HINT to
                        if (isGrid) CONTENT_STYLE_GRID_ITEM_HINT_VALUE
                        else CONTENT_STYLE_LIST_ITEM_HINT_VALUE,
                CONTENT_STYLE_PLAYABLE_HINT to
                        if (isGrid) CONTENT_STYLE_GRID_ITEM_HINT_VALUE
                        else CONTENT_STYLE_LIST_ITEM_HINT_VALUE
            )
            mBuilder?.setExtras(hints)
            return this
        }

        fun asBrowsable(): Builder {
            mFlags = mFlags or MediaBrowserCompat.MediaItem.FLAG_BROWSABLE
            return this
        }

        fun asPlayable(): Builder {
            mFlags = mFlags or MediaBrowserCompat.MediaItem.FLAG_PLAYABLE
            return this
        }

        fun setExtraProperties(
            isEmptyItem: Boolean = false,
            itemType: Int = ITEM_EMPTY,
            isUsbAttached: Boolean = false,
            currentUsbSelected: String = "",
            isUsbRootItem: Boolean = false,
            isSelected: Boolean = false,
            duration: Long = 0L,
            path: String = "",
            dateTaken: String = "",
            isFavorite: Boolean = false,
            songCount: Int = 0
        ): Builder {
            val property = bundleOf(
                EXTRA_IS_EMPTY_ITEM to isEmptyItem,
                EXTRA_ITEM_TYPE to itemType,
                EXTRA_IS_USB_ATTACHED to isUsbAttached,
                EXTRA_IS_USB_ROOT_ITEM to isUsbRootItem,
                EXTRA_CURRENT_SELECTED_USB to currentUsbSelected,
                EXTRA_IS_CURRENT_SELECTED_USB to isSelected,
                EXTRA_SONG_DURATION to duration,
                EXTRA_SONG_PATH to path,
                EXTRA_DATE_TAKEN to dateTaken,
                EXTRA_SONG_FAVORITE to isFavorite,
                EXTRA_SONG_NUMBER to songCount
            )
            mBuilder?.setExtras(property)
            return this
        }

        fun build(): MediaBrowserCompat.MediaItem {
            val result = MediaBrowserCompat.MediaItem(mBuilder!!.build(), mFlags)
            mBuilder = null
            mFlags = 0
            return result
        }

        init {
            mBuilder = MediaDescriptionCompat.Builder()
        }

        companion object {
            // Hints - see https://developer.android.com/training/cars/media#default-content-style
            const val CONTENT_STYLE_SUPPORTED = "android.media.browse.CONTENT_STYLE_SUPPORTED"
            const val CONTENT_STYLE_BROWSABLE_HINT =
                "android.media.browse.CONTENT_STYLE_BROWSABLE_HINT"
            const val CONTENT_STYLE_PLAYABLE_HINT =
                "android.media.browse.CONTENT_STYLE_PLAYABLE_HINT"
            const val CONTENT_STYLE_LIST_ITEM_HINT_VALUE = 1
            const val CONTENT_STYLE_GRID_ITEM_HINT_VALUE = 2

            const val EXTRA_IS_USB_ROOT_ITEM = "IS_USB_ROOT_ITEM"
            const val EXTRA_CURRENT_SELECTED_USB = "CURRENT_SELECTED_USB"
            const val EXTRA_IS_USB_ATTACHED = "IS_USB_ATTACHED"
            const val EXTRA_IS_CURRENT_SELECTED_USB = "IS_CURRENT_SELECTED_USB"
            const val EXTRA_IS_EMPTY_ITEM = "IS_EMPTY_ITEM"

            const val EXTRA_SONG_DURATION = "SONG_DURATION"
            const val EXTRA_SONG_PATH = "SONG_PATH"
            const val EXTRA_DATE_TAKEN = "DATE_TAKEN"
            const val EXTRA_SONG_FAVORITE = "SONG_FAVORITE"
            const val EXTRA_SONG_NUMBER = "SONG_NUMBER"

            const val EXTRA_ITEM_TYPE = "ITEM_TYPE"
            const val ITEM_EMPTY = -1
            const val ITEM_USB = 0
            const val ITEM_SONG = 1
            const val ITEM_FOLDER = 2
            const val ITEM_ALBUM = 3
            const val ITEM_ARTIST = 4
            const val ITEM_FAVORITE = 5
        }

    }
}