package ru.kainlight.lightcutter.api

import ru.kainlight.lightcutter.Main
import ru.kainlight.lightcutter.data.EconomyType

class ILightCutterAPI(plugin: Main) : LightCutterAPI {

    override val economyHandler: EconomyHandler = IEconomyHandler(plugin, EconomyType.getCurrent())
    override val regionHandler: RegionHandler = plugin.database

}