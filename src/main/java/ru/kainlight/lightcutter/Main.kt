package ru.kainlight.lightcutter

import ru.kainlight.lightcutter.api.ILightCutterAPI
import ru.kainlight.lightcutter.api.LightCutterAPI
import ru.kainlight.lightcutter.commands.Completer
import ru.kainlight.lightcutter.commands.MainCommand
import ru.kainlight.lightcutter.data.Database
import ru.kainlight.lightcutter.data.WoodCutterMode
import ru.kainlight.lightcutter.listeners.BlockListener
import ru.kainlight.lightlibrary.LightConfig
import ru.kainlight.lightlibrary.LightPlugin
import ru.kainlight.lightlibrary.UTILS.DebugBukkit
import ru.kainlight.lightlibrary.UTILS.Parser
import java.util.concurrent.CopyOnWriteArrayList

class Main : LightPlugin() {

    internal lateinit var database: Database

    val disabledWorlds = CopyOnWriteArrayList<String>()

    override fun onLoad() {
        this.saveDefaultConfig()
        this.configurationVersion = 2.4
        updateConfig()

        LightConfig.saveLanguages(this, "main-settings.language")
        messageConfig.configurationVersion = 2.1
        messageConfig.updateConfig()
    }

    override fun onEnable() {
        instance = this
        setLightPluginInstance(this)

        createBukkitAudience()

        this.reloadDatabase()
        LightCutterAPI.setProvider(ILightCutterAPI(this))
        this.reloadConfigurations()

        if (WoodCutterMode.getCurrent() == WoodCutterMode.REGION) {
            val regions = database.getRegions()
            if(regions.isEmpty()) DebugBukkit.warn("The list of regions is empty")
            else DebugBukkit.info("Regions " + regions.map { it.name } + " successfully loaded")
        }

        this.registerCommand("lightcutter", MainCommand(this), Completer(this))
        this.registerListener(BlockListener(this))

        checkUpdates()
        enableMessage()
    }

    override fun onDisable() {
        database.disconnect()

        stop()
    }

    fun reloadConfigurations() {
        this.saveDefaultConfig()
        this.reloadConfig()
        Parser.parseMode = this.config.getString("main-settings.parse_mode", "MINIMESSAGE")!!
        DebugBukkit.isEnabled =  this.config.getBoolean("debug")
        this.disabledWorlds.addAll(this.config.getStringList("woodcutter-settings.disabled-worlds"))

        this.messageConfig.saveDefaultConfig()
        this.messageConfig.reloadLanguage("main-settings.language")
        this.messageConfig.reloadConfig()
    }

    internal fun reloadDatabase() {
        database = Database(this)
        database.connect()
        database.createTables()
    }

    companion object {
        private lateinit var instance: Main

        internal fun getInstance(): Main {
            return instance
        }
    }
}
