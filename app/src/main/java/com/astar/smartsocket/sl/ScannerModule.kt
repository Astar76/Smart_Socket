package com.astar.smartsocket.sl

import com.astar.smartsocket.ui.scanner.DeviceListCommunication
import com.astar.smartsocket.ui.scanner.DeviceListUiMapper
import com.astar.smartsocket.ui.scanner.DevicesViewModel
import com.astar.smartsocket.ui.scanner.ScannerStateCommunication

class ScannerModule(private val coreModule: CoreModule) : BaseModule<DevicesViewModel> {
    override fun viewModel(): DevicesViewModel {

        return DevicesViewModel(
            scannerStateCommunication = ScannerStateCommunication.Base(),
            devicesCommunication = DeviceListCommunication.Base(),
            bluetoothAdapter = coreModule.bluetoothAdapter,
            devicesUiMapper = DeviceListUiMapper.Base(),
            connection = coreModule.connection,
            scanner = coreModule.scanner
        )
    }
}