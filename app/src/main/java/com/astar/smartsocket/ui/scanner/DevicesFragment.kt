package com.astar.smartsocket.ui.scanner

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import com.astar.smartsocket.databinding.FragmentDevicesBinding
import com.astar.smartsocket.ui.Screen
import com.astar.smartsocket.ui.core.BaseFragment
import com.astar.smartsocket.ui.core.ClickListener

class DevicesFragment : BaseFragment<FragmentDevicesBinding, DevicesViewModel>() {

    override fun createViewBinding(inflater: LayoutInflater, container: ViewGroup?) =
        FragmentDevicesBinding.inflate(inflater, container, false)

    override fun viewModelClass() = DevicesViewModel::class.java

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val devicesAdapter = DevicesAdapter(onItemClick)
        val layoutManager = LinearLayoutManager(requireContext())
        binding.devicesRecycler.layoutManager = layoutManager
        binding.devicesRecycler.adapter = devicesAdapter

        binding.scanButton.setOnClickListener {
            checkPermissionAndExecute { viewModel.startOrStopScan() }
        }

        viewModel.observe(viewLifecycleOwner) { it.map(devicesAdapter) }
        viewModel.observeState(viewLifecycleOwner) {
            it.map(binding.scannerProgress, binding.scanButton)
        }

        // todo fix navigation
        viewModel.navigation.observe(viewLifecycleOwner) { event ->
            when (val screen = event.value()) {
                is Screen.Control -> navigation.openControlScreen(screen.address)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        navigation.toolbarTitle("Search devices")
    }

    override fun onStop() {
        super.onStop()
        viewModel.stopScan()
    }

    private val onItemClick = object : ClickListener<DeviceUi> {
        override fun onClick(item: DeviceUi) {
            item.control(viewModel)
        }
    }
}