package ru.kainlight.lightcutter.api

interface Region {

    val name: String
    val earn: String
    val needBreak: Int
    val cooldown: Int

    fun getInfo(): String
}
