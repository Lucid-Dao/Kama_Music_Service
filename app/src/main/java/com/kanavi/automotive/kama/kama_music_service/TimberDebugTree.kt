package com.kanavi.automotive.kama.kama_music_service

import timber.log.Timber

class TimberDebugTree : Timber.DebugTree() {
    override fun createStackElementTag(element: StackTraceElement): String {
        return String.format(
            "[%s] %s %s() Line: %s",
            "KAMA-Music",
            super.createStackElementTag(element),
            element.methodName,
            element.lineNumber
        )
    }
}