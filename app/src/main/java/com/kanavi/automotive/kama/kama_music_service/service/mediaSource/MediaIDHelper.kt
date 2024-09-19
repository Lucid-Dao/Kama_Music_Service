package com.kanavi.automotive.kama.kama_music_service.service.mediaSource

object MediaIDHelper {
    private const val CATEGORY_SEPARATOR = "__/__"
    private const val LEAF_SEPARATOR = "__|__"

    /**
     * Create a String value that represents a playable or a browsable media.
     *
     *
     * Encode the media browsable categories, if any, and the unique music ID, if any,
     * into a single String mediaID.
     *
     *
     * MediaIDs are of the form <categoryType>__/__<categoryValue>__|__<musicUniqueId>, to make it
     * easy to find the category (like genre) that a music was selected from, so we
     * can correctly build the playing queue. This is specially useful when
     * one music can appear in more than one list, like "by genre -> genre_1"
     * and "by artist -> artist_1".
     *
     * @param mediaID    Unique ID for playable items, or null for browsable items.
     * @param categories Hierarchy of categories representing this item's browsing parents.
     * @return A hierarchy-aware media ID.
    </musicUniqueId></categoryValue></categoryType> */
    fun createMediaID(mediaID: String?, vararg categories: String?): String {
        val sb = StringBuilder()
        for (i in categories.indices) {
            require(isValidCategory(categories[i])) { "Invalid category: " + categories[i] }
            sb.append(categories[i])
            if (i < categories.size - 1) {
                sb.append(CATEGORY_SEPARATOR)
            }
        }
        if (mediaID != null) {
            sb.append(LEAF_SEPARATOR).append(mediaID)
        }
        return sb.toString()
    }

    /**
     * Extracts category and categoryValue from the mediaID. mediaID is, by this sample's
     * convention, a concatenation of category (eg "by_genre"), categoryValue (eg "Classical") and
     * mediaID. This is necessary so we know where the user selected the music from, when the music
     * exists in more than one music list, and thus we are able to correctly build the playing queue.
     *
     * @param mediaID that contains a category and categoryValue.
     */
    fun extractCategory(mediaID: String): String {
        val pos = mediaID.indexOf(LEAF_SEPARATOR)
        return if (pos >= 0) {
            mediaID.substring(0, pos)
        } else mediaID
    }

    /**
     * Extracts unique musicID from the mediaID. mediaID is, by this sample's convention, a
     * concatenation of category (eg "by_genre"), categoryValue (eg "Classical") and unique
     * musicID. This is necessary so we know where the user selected the music from, when the music
     * exists in more than one music list, and thus we are able to correctly build the playing queue.
     *
     * @param mediaID that contains the musicID
     * @return musicID
     */

    fun extractMusicID(mediaID: String): String? {
        val pos = mediaID.indexOf(LEAF_SEPARATOR)
        return if (pos >= 0) {
            mediaID.substring(pos + LEAF_SEPARATOR.length)
        } else null
    }

    fun isBrowsable(mediaID: String): Boolean {
        return !mediaID.contains(LEAF_SEPARATOR)
    }

    private fun isValidCategory(category: String?): Boolean {
        return category == null || !category.contains(CATEGORY_SEPARATOR) && !category.contains(
            LEAF_SEPARATOR
        )
    }
}