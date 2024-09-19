package com.kanavi.automotive.kama.kama_music_service.data.database.model


import com.kanavi.automotive.kama.kama_music_service.common.util.AlphanumericComparator
import com.kanavi.automotive.kama.kama_music_service.common.constant.Constants.PLAYER_SORT_BY_TITLE
import com.kanavi.automotive.kama.kama_music_service.common.constant.Constants.SORT_DESCENDING
import com.kanavi.automotive.kama.kama_music_service.common.extension.sortSafely


data class Folder(val title: String, val trackCount: Int, val path: String) {
    companion object {
        fun getComparator(sorting: Int) = Comparator<Folder> { first, second ->
            var result = when {
                sorting and PLAYER_SORT_BY_TITLE != 0 -> AlphanumericComparator().compare(
                    first.title.lowercase(),
                    second.title.lowercase()
                )

                else -> first.trackCount.compareTo(second.trackCount)
            }

            if (sorting and SORT_DESCENDING != 0) {
                result *= -1
            }

            return@Comparator result
        }
    }
}

fun ArrayList<Folder>.sortSafely(sorting: Int) = sortSafely(Folder.getComparator(sorting))
