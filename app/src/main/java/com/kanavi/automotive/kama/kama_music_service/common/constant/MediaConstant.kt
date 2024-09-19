package com.kanavi.automotive.kama.kama_music_service.common.constant

object MediaConstant {
    // Media IDs used on browsable items of MediaBrowser
    const val MEDIA_ID_EMPTY_ROOT = "__EMPTY_MUSIC_ROOT__"
    const val MEDIA_ID_RECENT_ROOT = "__MUSIC_RECENT__"
    const val MEDIA_ID_ROOT = "__MUSIC_ROOT__"
    const val MEDIA_ID_SDCARD = "__MUSIC_SDCARD__"
    const val MEDIA_ID_USB_LIST = "__MUSIC_USB_LIST__"
    const val MEDIA_ID_ROOT_USB = "__ROOT_USB__"
    const val KEY_USB_ATTACHED = "KEY_USB_ATTACHED"
    const val MEDIA_ID_ATTACHED_STATE = "MEDIA_ID_ATTACHED_STATE"


    const val IS_SELECTED_USB = "IS_SELECTED_USB"
    const val IS_NOT_SELECTED_USB = "IS_NOT_SELECTED_USB"


    const val MEDIA_ID_MUSICS_BY_SEARCH = "__BY_SEARCH__"
    const val MEDIA_ID_MUSICS_BY_FILE = "__BY_FILE__"
    const val MEDIA_ID_MUSICS_BY_SONGS = "__BY_SONGS__"
    const val MEDIA_ID_MUSICS_BY_ALBUM = "__BY_ALBUM__"
    const val MEDIA_ID_MUSICS_BY_FAVORITE = "__BY_FAVORITE__"

    val listOfMediaIdAlwaysHaveToNotifyChanged = emptyList<String>()
}