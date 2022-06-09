package com.astar.smartsocket.ui

interface Navigation {

    fun toolbarTitle(text: String)

    fun openControlScreen(address: String)
}

sealed class Screen {

    object Scanner : Screen()
    class Control(val address: String) : Screen()
}