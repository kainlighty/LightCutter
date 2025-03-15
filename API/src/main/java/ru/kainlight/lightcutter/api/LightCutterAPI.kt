package ru.kainlight.lightcutter.api

interface LightCutterAPI {

    companion object {
        private var instance: LightCutterAPI? = null

        fun getProvider(): LightCutterAPI {
            return instance ?: throw IllegalStateException("LightCutterAPI is not initialized")
        }

        fun setProvider(instance: LightCutterAPI?) {
            if(instance == null) this.instance = null
            else this.instance = instance
        }
    }

    val economyHandler: EconomyHandler
    val regionHandler: RegionHandler

}