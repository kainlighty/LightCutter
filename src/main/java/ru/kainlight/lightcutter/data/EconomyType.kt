package ru.kainlight.lightcutter.data

import ru.kainlight.lightcutter.Main

enum class EconomyType {
    VAULT,
    PLAYERPOINTS;

    companion object {
        private val economyTypeWithConfig: String = Main.getInstance().config.getString("woodcutter-settings.economy")?.uppercase() ?: "VAULT"
        fun getCurrent(): EconomyType {
            return EconomyType.entries.find { it.name == economyTypeWithConfig } ?: VAULT
        }
    }
}