package ru.kainlight.lightcutter.DATA

import ru.kainlight.lightcutter.Main

enum class MessageType {
    CHAT,
    ACTIONBAR;

    companion object {

        private val configType: String by lazy { Main.instance.config.getString("woodcutter-settings.messages-type")?.uppercase() ?: "CHAT" }
        fun getCurrent(): MessageType = MessageType.entries.find { it.name == configType } ?: CHAT
    }
}
