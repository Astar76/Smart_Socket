package com.astar.smartsocket.ui.control

import com.astar.smartsocket.ui.core.Communication

interface ControlCommunication: Communication<ControlUi> {

    class Base: Communication.Base<ControlUi>(), ControlCommunication
}