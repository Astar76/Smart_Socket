package com.astar.smartsocket.ui.control

import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.setFragmentResult
import com.astar.smartsocket.databinding.DialogTimePickerBinding

class TimePickerDialog: DialogFragment() {

    private var _binding: DialogTimePickerBinding? = null
    private val binding: DialogTimePickerBinding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogTimePickerBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onStart() {
        super.onStart()
        val width = (resources.displayMetrics.widthPixels * 0.85).toInt()
        dialog!!.window?.setLayout(width, ViewGroup.LayoutParams.WRAP_CONTENT)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.timePicker.setIs24HourView(true)
        binding.cancelButton.setOnClickListener { dismiss() }
        binding.applyButton.setOnClickListener {
            val bundle = Bundle()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                bundle.putInt(HOURS, binding.timePicker.hour)
                bundle.putInt(MINUTES, binding.timePicker.minute)
            } else {
                bundle.putInt(HOURS, binding.timePicker.currentHour)
                bundle.putInt(MINUTES, binding.timePicker.currentMinute)
            }
            setFragmentResult(REQUEST_CODE, bundle)
            dismiss()
        }
    }

    companion object {
        const val TAG = "TimePickerDialog"
        const val REQUEST_CODE = "time_picker_request_code"
        const val HOURS = "hours"
        const val MINUTES = "minutes"
    }
}