package com.astar.smartsocket.ui.scanner

import com.astar.smartsocket.ui.core.Communication

interface DeviceListCommunication : Communication<DeviceListUi> {

    class Base: Communication.Base<DeviceListUi>(), DeviceListCommunication
}