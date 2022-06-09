package com.astar.smartsocket.ui.core

import android.Manifest
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.StringRes
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.viewbinding.ViewBinding
import com.astar.smartsocket.R
import com.astar.smartsocket.core.SmartSocketApp
import com.astar.smartsocket.ui.MainActivity
import com.astar.smartsocket.ui.Navigation
import com.google.android.material.snackbar.Snackbar
import com.permissionx.guolindev.PermissionX

abstract class BaseFragment<VB: ViewBinding, VM: ViewModel>: Fragment() {

    private var _binding: VB? = null
    protected val binding:VB get() = _binding!!

    protected lateinit var viewModel: VM

    protected val navigation: Navigation by lazy { requireActivity() as Navigation }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel = (requireActivity().application as SmartSocketApp)
            .viewModel(viewModelClass(), this)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = createViewBinding(inflater, container)
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    abstract fun createViewBinding(inflater: LayoutInflater, container: ViewGroup?): VB

    abstract fun viewModelClass(): Class<VM>

    protected fun checkPermissionAndExecute(block: () -> Unit) {
        val permissions: List<String> = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            listOf(Manifest.permission.BLUETOOTH_SCAN)
        } else {
            listOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        }

        PermissionX.init(this)
            .permissions(permissions)
            .request { allGranted, _, _ ->
                if (allGranted) {
                    block()
                } else {
                    snack(R.string.text_no_permission)
                }
            }
    }

    protected fun snack(@StringRes resId: Int, view: View = requireView()) {
        Snackbar.make(view, resId, Snackbar.LENGTH_SHORT).show()
    }
}