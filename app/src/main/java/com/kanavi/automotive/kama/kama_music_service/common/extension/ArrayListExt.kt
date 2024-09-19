package com.kanavi.automotive.kama.kama_music_service.common.extension

fun <T> ArrayList<T>.sortSafely(comparator: Comparator<T>) {
    try {
        sortWith(comparator)
    } catch (ignored: Exception) {
    }
}
