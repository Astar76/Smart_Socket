package com.astar.smartsocket.core

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.astar.smartsocket.sl.DependencyContainer
import com.astar.smartsocket.ui.control.ControlViewModel
import com.astar.smartsocket.ui.scanner.DevicesViewModel
import java.lang.IllegalStateException

class ViewModelsFactory(
    private val dependencyContainer: DependencyContainer,
) : ViewModelProvider.Factory {

    private val map = HashMap<Class<*>, Feature>().apply {
        put(DevicesViewModel::class.java, Feature.SCANNER)
        put(ControlViewModel::class.java, Feature.CONTROL)
    }

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        val feature = map[modelClass] ?: throw IllegalStateException("Unknown ViewModel $modelClass")
        return dependencyContainer.module(feature).viewModel() as T
    }
}
