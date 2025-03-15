package ru.kainlight.lightcutter.api

import org.bukkit.entity.Player

interface EconomyHandler {

    fun depositWithRegion(player: Player, earn: String)
    fun depositWithoutRegion(player: Player, blockName: String)

}