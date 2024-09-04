package ru.kainlight.lightcutter.UTILS

import me.clip.placeholderapi.PlaceholderAPI
import ru.kainlight.lightcutter.Main
import java.util.logging.Level

object Debug {

    private var isDebug: Boolean = false

    fun checkWorldGuardExtension() {
        if(isDebug) {
            val registeredIdentifiers = PlaceholderAPI.getRegisteredIdentifiers()
            if (!registeredIdentifiers.contains("worldguard")) {
                this.message("WorldGuard extension not found for PlaceholderAPI")
            }
        }
    }

    fun message(message: String, level: Level? = Level.WARNING) {
        if (this.isDebug) {
            Main.INSTANCE.logger.log(level, message)
        }
    }

    fun setStatus(status: Boolean) {
        this.isDebug = status
    }

}