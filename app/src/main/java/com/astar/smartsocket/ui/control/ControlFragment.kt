package com.astar.smartsocket.ui.control

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.setFragmentResultListener
import com.astar.smartsocket.databinding.FragmentControlBinding
import com.astar.smartsocket.ui.core.BaseFragment

class ControlFragment : BaseFragment<FragmentControlBinding, ControlViewModel>() {

    override fun createViewBinding(inflater: LayoutInflater, container: ViewGroup?) =
        FragmentControlBinding.inflate(inflater, container, false)

    override fun viewModelClass() = ControlViewModel::class.java

    override fun onResume() {
        super.onResume()
        navigation.toolbarTitle("Control ${address()}" )
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setFragmentResultListener(TimePickerDialog.REQUEST_CODE) { _, bundle ->
            val hours = bundle.getInt(TimePickerDialog.HOURS)
            val minutes = bundle.getInt(TimePickerDialog.MINUTES)
            viewModel.setTimerValue(hours, minutes)
        }

        binding.setTimerButton.setOnClickListener {
            TimePickerDialog().show(parentFragmentManager, TimePickerDialog.TAG)
        }

        binding.startTimer.setOnClickListener {
            viewModel.runOnStop()
        }

        viewModel.observe(viewLifecycleOwner) {
            it.map(
                binding.displayTimeText,
                binding.setTimerButton,
                binding.startTimer
            )
        }
    }

    private fun address() = requireArguments().getString(ADDRESS_ARG)

    companion object {

        const val ADDRESS_ARG = "address"

        fun newInstance(address: String) = ControlFragment().apply {
            arguments = bundleOf(ADDRESS_ARG to address)
        }
    }
}