package com.astar.smartsocket.ui.scanner

import android.widget.ProgressBar
import androidx.core.view.isVisible
import com.astar.smartsocket.R
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.Snackbar

interface ScannerStateUi {

    fun map(progress: ProgressBar, fab: FloatingActionButton)

    open class Base : ScannerStateUi {
        override fun map(progress: ProgressBar, fab: FloatingActionButton) {
            progress.isVisible = false
            fab.setImageResource(R.drawable.ic_scanner_search)
        }
    }

    class Connection: ScannerStateUi {
        override fun map(progress: ProgressBar, fab: FloatingActionButton) {
            progress.isVisible = true
        }
    }

    class Ready(private val name: String) : ScannerStateUi {
        override fun map(progress: ProgressBar, fab: FloatingActionButton) {
            progress.isVisible = false
        }
    }

    class Running: ScannerStateUi {
        override fun map(progress: ProgressBar, fab: FloatingActionButton) {
            Snackbar.make(progress, "Сканируем...", Snackbar.LENGTH_SHORT).show()
            progress.isVisible = true
            fab.setImageResource(R.drawable.ic_scanner_stop)
        }
    }

    class Stopped: Base(), ScannerStateUi {
        override fun map(progress: ProgressBar, fab: FloatingActionButton) {
            Snackbar.make(progress, "Сканирование завершено!", Snackbar.LENGTH_SHORT).show()
            super.map(progress, fab)
        }
    }

    object Empty: ScannerStateUi {
        override fun map(progress: ProgressBar, fab: FloatingActionButton)  = Unit
    }
}