package com.astar.smartsocket.ui.control

import android.widget.Button
import android.widget.TextView
import com.astar.smartsocket.utils.TimeSecondsConvert
import com.google.android.material.snackbar.Snackbar

interface ControlUi {

    fun map(timeText: TextView, setTimer: Button, startTimer: Button) = Unit

    fun runOrStop(timer: Timer) = Unit

    class Connected : ControlUi {
        override fun map(timeText: TextView, setTimer: Button, startTimer: Button) {
            setTimer.isEnabled = true
            Snackbar.make(timeText, "Успешно подключено!", Snackbar.LENGTH_SHORT).show()
        }
    }

    class State(private val timerStarted: Boolean) : ControlUi {

        override fun runOrStop(timer: Timer) {
            if (timerStarted) timer.stop() else timer.start()
        }

        override fun map(timeText: TextView, setTimer: Button, startTimer: Button) {
            setTimer.isEnabled = !timerStarted
            if (timerStarted) startTimer.text = "Stop timer"
            else startTimer.text = "Start timer"

        }
    }

    class SetTimer(seconds: Int) : ControlUi {

        private val converter = TimeSecondsConvert(seconds)

        override fun runOrStop(timer: Timer) {
            timer.start()
        }

        override fun map(timeText: TextView, setTimer: Button, startTimer: Button) {
            timeText.text = converter.hms()
        }
    }

    class Tick(seconds: Int) : ControlUi {

        private val converter = TimeSecondsConvert(seconds)

        override fun map(timeText: TextView, setTimer: Button, startTimer: Button) {
            timeText.text = converter.hms()
        }
    }

    class Base: ControlUi {

        override fun runOrStop(timer: Timer) {
            timer.start()
        }

        override fun map(timeText: TextView, setTimer: Button, startTimer: Button) = Unit
    }
}

interface Timer {

    fun start()
    fun stop()
}