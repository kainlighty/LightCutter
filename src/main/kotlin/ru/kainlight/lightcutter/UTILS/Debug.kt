package ru.kainlight.lightcutter.UTILS

import ru.kainlight.lightcutter.Main
import java.util.logging.Level

object Debug {

    private var isEnabled: Boolean = false

    fun log(message: String, level: Level? = Level.INFO) {
        if (this.isEnabled) Main.instance.logger.log(level, message)
    }

    fun updateStatus() {
        this.isEnabled = Main.instance.config.getBoolean("debug")
    }

}