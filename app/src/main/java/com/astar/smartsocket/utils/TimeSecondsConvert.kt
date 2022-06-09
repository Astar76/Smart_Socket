package com.astar.smartsocket.utils

class TimeSecondsConvert(private val seconds: Int) {

    fun hms(): String {
        return String.format("%02d:%02d:%02d", hours(), minutes(), seconds())
    }

    fun hours(): Int {
        return seconds / 3600
    }

    fun minutes(): Int {
        return (seconds % 3600) / 60
    }

    fun seconds(): Int {
        return seconds % 60
    }
}