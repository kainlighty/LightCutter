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
                this.log("PlaceholderAPI extension «WorldGuard» not found", Level.SEVERE)
                this.log("Install: /papi ecloud download WorldGuard", Level.SEVERE)
            }
        }
    }

    fun log(message: String, level: Level? = Level.INFO) {
        if (this.isDebug) Main.instance.logger.log(level, message)
    }

    fun setStatus(status: Boolean) {
        this.isDebug = status
    }

}