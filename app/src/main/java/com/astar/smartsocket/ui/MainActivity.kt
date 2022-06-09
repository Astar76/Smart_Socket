package com.astar.smartsocket.ui

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.astar.smartsocket.R
import com.astar.smartsocket.databinding.ActivityMainBinding
import com.astar.smartsocket.ui.control.ControlFragment
import com.astar.smartsocket.ui.scanner.DevicesFragment

class MainActivity : AppCompatActivity(), Navigation {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater).also { setContentView(it.root) }

        setSupportActionBar(binding.toolbar)

        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .add(R.id.fragment_container, DevicesFragment())
                .commit()
        }
    }

    override fun openControlScreen(address: String) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, ControlFragment.newInstance(address))
            .addToBackStack(null)
            .commit()
    }

    override fun toolbarTitle(text: String) {
        binding.toolbar.title = text
    }
}