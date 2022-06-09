package com.astar.smartsocket.sl

import com.astar.smartsocket.ui.control.ControlCommunication
import com.astar.smartsocket.ui.control.ControlViewModel

class ControlModule(private val coreModule: CoreModule) : BaseModule<ControlViewModel> {

    override fun viewModel(): ControlViewModel {

        return ControlViewModel(
            controlCommunication = ControlCommunication.Base(),
            connection = coreModule.connection
        )
    }
}