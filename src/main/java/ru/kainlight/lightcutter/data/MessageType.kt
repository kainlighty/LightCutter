package ru.kainlight.lightcutter.data

import ru.kainlight.lightcutter.Main

internal enum class MessageType {
    CHAT,
    ACTIONBAR;

    companion object {

        private val configType: String by lazy { Main.getInstance().config.getString("woodcutter-settings.messages-type")?.uppercase() ?: "CHAT" }
        fun getCurrent(): MessageType = MessageType.entries.find { it.name == configType } ?: CHAT
    }
}
