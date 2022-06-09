package com.astar.smartsocket.ui.core

class Event<T>(private val data: T) {

    var handled = false

    fun value(): T? {
        if (handled) return null
        handled = true
        return data
    }
}