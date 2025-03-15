package ru.kainlight.lightcutter.data

import ru.kainlight.lightcutter.Main

internal enum class WoodCutterMode {
    WORLD,
    REGION;

    companion object {

        private val configMode: String by lazy { Main.getInstance().config.getString("woodcutter-settings.mode")?.uppercase() ?: "WORLD" }
        fun getCurrent(): WoodCutterMode = WoodCutterMode.entries.find { it.name == configMode } ?: WORLD
    }
}
