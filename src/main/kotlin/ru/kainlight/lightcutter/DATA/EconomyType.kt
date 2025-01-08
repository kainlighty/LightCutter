package ru.kainlight.lightcutter.DATA

import ru.kainlight.lightcutter.Main

enum class EconomyType {
    VAULT,
    PLAYERPOINTS;

    companion object {

        private val configType: String by lazy { Main.instance.config.getString("woodcutter-settings.economy")?.uppercase() ?: "VAULT" }
        fun getCurrent(): EconomyType = EconomyType.entries.find { it.name == configType } ?: VAULT

        /*fun getCurrent(): MessageType {
            val configMode = Main.instance.config.getString("woodcutter-settings.messages-type")?.uppercase()
            return MessageType.entries.find { it.name == configMode } ?: CHAT
        }*/
    }
}
