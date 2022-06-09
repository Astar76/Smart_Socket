package com.astar.smartsocket.sl

import androidx.lifecycle.ViewModel

interface BaseModule<T: ViewModel> {

    fun viewModel(): T
}