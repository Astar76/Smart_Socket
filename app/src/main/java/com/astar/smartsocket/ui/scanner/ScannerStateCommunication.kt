package com.astar.smartsocket.ui.scanner

import com.astar.smartsocket.ui.core.Communication

interface ScannerStateCommunication : Communication<ScannerStateUi> {

    class Base: Communication.Base<ScannerStateUi>(), ScannerStateCommunication
}