package ru.kainlight.lightcutter

import net.kyori.adventure.audience.Audience
import net.kyori.adventure.platform.bukkit.BukkitAudiences
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import ru.kainlight.lightcutter.COMMANDS.Completer
import ru.kainlight.lightcutter.COMMANDS.MainCommand
import ru.kainlight.lightcutter.DATA.Database
import ru.kainlight.lightcutter.DATA.EconomyType
import ru.kainlight.lightcutter.DATA.WoodCutterMode
import ru.kainlight.lightcutter.LISTENERS.BlockListener
import ru.kainlight.lightcutter.UTILS.Debug
import ru.kainlight.lightcutter.UTILS.EconomyManager
import ru.kainlight.lightlibrary.LightConfig
import ru.kainlight.lightlibrary.LightPlugin
import ru.kainlight.lightlibrary.UTILS.Init
import ru.kainlight.lightlibrary.UTILS.Parser
import java.util.concurrent.CopyOnWriteArrayList
import java.util.logging.Level

class Main : LightPlugin() {

    lateinit var bukkitAudiences: BukkitAudiences

    lateinit var database: Database
    lateinit var economyManager: EconomyManager

    val disabledWorlds: CopyOnWriteArrayList<String> = CopyOnWriteArrayList()

    override fun onLoad() {
        this.saveDefaultConfig()
        this.configurationVersion = 2.3
        updateConfig()

        LightConfig.saveLanguages(this, "main-settings.language")
        messageConfig.configurationVersion = 2.1
        messageConfig.updateConfig()
    }

    override fun onEnable() {
        instance = this
        this.enable()

        this.bukkitAudiences = BukkitAudiences.create(this)

        this.reloadConfigurations()
        this.reloadDatabase()

        economyManager = EconomyManager(this, EconomyType.getCurrent())

        if (WoodCutterMode.getCurrent() == WoodCutterMode.REGION) {
            val regions = database.getRegions()
            if(regions.isEmpty()) Debug.log("The list of regions is empty", Level.WARNING)
            else Debug.log("Regions " + regions.map { it.name } + " successfully loaded")
        }

        this.registerCommand("lightcutter", MainCommand(this), Completer(this))
        this.registerListener(BlockListener(this))

        Init.start(this, true)
    }

    override fun onDisable() {
        database.disconnect()
        this.disable()
        this.cancelTasks()
    }

    fun reloadConfigurations() {
        this.saveDefaultConfig()
        this.reloadConfig()
        this.messageConfig.saveDefaultConfig()
        this.messageConfig.reloadLanguage("main-settings.language")

        Parser.parseMode = this.config.getString("main-settings.parse_mode", "MINIMESSAGE")!!

        Debug.updateStatus()

        this.disabledWorlds.addAll(this.config.getStringList("woodcutter-settings.disabled-worlds"))
    }

    fun reloadDatabase() {
        database = Database(this)
        database.connect()
        database.createTables()
        database.initializeCache()
    }

    companion object { @JvmStatic lateinit var instance: Main }
}

fun Player.getAudience(): Audience {
    return Main.instance.bukkitAudiences.player(this)
}
fun CommandSender.getAudience(): Audience {
    return Main.instance.bukkitAudiences.sender(this)
}
