package com.astar.smartsocket.sl

import com.astar.smartsocket.core.Feature
import java.lang.IllegalStateException

interface DependencyContainer {

    fun module(feature: Feature): BaseModule<*>

    class Base(private val coreModule: CoreModule): DependencyContainer {
        override fun module(feature: Feature): BaseModule<*> = when(feature) {
            Feature.CONTROL -> ControlModule(coreModule)
            Feature.SCANNER -> ScannerModule(coreModule)
            else -> throw IllegalStateException("unknown feature $feature")
        }
    }
}
