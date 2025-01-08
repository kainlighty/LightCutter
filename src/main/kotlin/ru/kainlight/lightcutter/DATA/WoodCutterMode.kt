package ru.kainlight.lightcutter.DATA

import ru.kainlight.lightcutter.Main

enum class WoodCutterMode {
    WORLD,
    REGION;

    companion object {

        private val configMode: String by lazy { Main.instance.config.getString("woodcutter-settings.mode")?.uppercase() ?: "WORLD" }
        fun getCurrent(): WoodCutterMode = WoodCutterMode.entries.find { it.name == configMode } ?: WORLD

        /*fun getCurrent(): WoodCutterMode {
            val configMode = Main.instance.config.getString("woodcutter-settings.mode")?.uppercase()
            return WoodCutterMode.entries.find { it.name == configMode } ?: WORLD
        }*/
    }
}
