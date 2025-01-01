package ru.kainlight.lightcutter

import net.kyori.adventure.audience.Audience
import net.kyori.adventure.platform.bukkit.BukkitAudiences
import org.bukkit.command.CommandSender
import org.bukkit.configuration.file.FileConfiguration
import org.bukkit.entity.Player
import ru.kainlight.lightcutter.COMMANDS.MainCommand
import ru.kainlight.lightcutter.DATA.Database
import ru.kainlight.lightcutter.LISTENERS.BlockListener
import ru.kainlight.lightcutter.UTILS.Debug
import ru.kainlight.lightcutter.UTILS.EconomyManager
import ru.kainlight.lightlibrary.LightConfig
import ru.kainlight.lightlibrary.LightPlugin
import ru.kainlight.lightlibrary.UTILS.Init
import ru.kainlight.lightlibrary.UTILS.Parser
import ru.kainlight.lightlibrary.equalsIgnoreCase

class Main : LightPlugin() {

    lateinit var bukkitAudiences: BukkitAudiences

    lateinit var database: Database
    lateinit var economyManager: EconomyManager

    val disabledWorlds: MutableList<String> = mutableListOf()

    override fun onLoad() {
        this.saveDefaultConfig()
        this.configurationVersion = 2.1
        updateConfig()
        LightConfig.saveLanguages(this, "main-settings.language")
        messageConfig.configurationVersion = 2.1
        messageConfig.updateConfig()
    }

    override fun onEnable() {
        INSTANCE = this

        Debug.setStatus(config.getBoolean("debug"))

        this.bukkitAudiences = BukkitAudiences.create(this)

        this.loader()

        Init.start(this, true)
    }

    override fun onDisable() {
        database.disconnect()
        this.disable()
        this.server.scheduler.cancelTasks(this)
    }

    private fun loader() {
        this.enable()

        database = Database(this)
        database.connect()
        database.createTables()

        economyManager = EconomyManager(this, this.config.getString("woodcutter-settings.economy", "VAULT")!!)

        if (this.config.getString("woodcutter-settings.mode")!!.equalsIgnoreCase("REGION")) {
            Debug.message("Regions " + database.getRegions().map { it.name } + " successfully loaded")
        }

        disabledWorlds.addAll(config.getStringList("woodcutter-settings.disabled-worlds"))

        this.registerCommand("lightcutter", MainCommand(this))
        this.registerListener(BlockListener(this))

        this.reloadParseMode()

        Debug.checkWorldGuardExtension()
    }

    fun getMessageConfig(): FileConfiguration {
        return this.messageConfig.getConfig()
    }

    fun reloadParseMode() {
        Parser.parseMode = this.config.getString("main-settings.parse_mode", "MINIMESSAGE")!!
    }

    companion object {
        @JvmStatic lateinit var INSTANCE: Main
    }
}

fun Player.getAudience(): Audience {
    return Main.INSTANCE.bukkitAudiences.player(this)
}
fun CommandSender.getAudience(): Audience {
    return Main.INSTANCE.bukkitAudiences.sender(this)
}
