package com.astar.smartsocket.ui.core

import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModel

abstract class BaseViewModel<E : Communication<T>, T>(protected val communication: E) :
    ViewModel(), Observe<T> {

    override fun observe(owner: LifecycleOwner, observer: Observer<T>) {
        communication.observe(owner, observer)
    }
}