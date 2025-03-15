package ru.kainlight.lightcutter.api

interface RegionHandler {

    fun addRegion(name: String, earn: String, needBreak: Int, cooldown: Int): Region?
    fun updateRegion(region: Region): Int
    fun removeRegion(name: String): Int

    fun getRegion(name: String): Region?
    fun hasRegion(name: String): Boolean
    fun getRegions(): List<Region>



}