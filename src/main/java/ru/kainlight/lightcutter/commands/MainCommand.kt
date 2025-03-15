package ru.kainlight.lightcutter.commands

import net.kyori.adventure.text.event.ClickEvent
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import ru.kainlight.lightcutter.Main
import ru.kainlight.lightcutter.api.IRegion
import ru.kainlight.lightcutter.api.LightCutterAPI
import ru.kainlight.lightcutter.api.Region
import ru.kainlight.lightlibrary.API.WorldGuardAPI
import ru.kainlight.lightlibrary.equalsIgnoreCase
import ru.kainlight.lightlibrary.getAudience
import ru.kainlight.lightlibrary.multiMessage
import kotlin.text.replace

@Suppress("WARNINGS")
internal class MainCommand(private val plugin: Main) : CommandExecutor {

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<String>): Boolean {
        val senderAudience = sender.getAudience()

        if (args.isEmpty() && command.name.equalsIgnoreCase("lightcutter")) {
            sender.hasNoPermissionAndMessage("lightcutter.help")
            plugin.getMessages().getStringList("help.commands").forEach { senderAudience.multiMessage(it) }
            return true
        }

        val regionHandler = LightCutterAPI.getProvider().regionHandler

        when (args[0].lowercase()) {
            "add" -> {
                if (sender.hasNoPermissionAndMessage("lightcutter.add")) return true

                if (args.size <= 4) {
                    plugin.getMessages().getString("help.add").let {
                        senderAudience.multiMessage(it)
                    }
                    return true
                }

                val regionName = args[1]

                if(sender is Player) {
                    if(!WorldGuardAPI.hasRegion(sender.world, regionName)) {
                        plugin.getMessages().getString("region.not-exists")?.let {
                            sender.messageWithReplacedRegion(it, regionName)
                        }
                        return true
                    }
                }

                val earn = args[2]
                val needBreak = args[3].toInt()
                val cooldown = args[4].toInt()

                val newRegion = regionHandler.addRegion(regionName, earn, needBreak, cooldown)
                if (newRegion != null) {
                    plugin.getMessages().getString("region.added")?.let {
                        sender.messageWithReplacedAll(it, newRegion)
                    }
                    return true
                } else {
                    plugin.getMessages().getString("region.exists")?.let {
                        sender.messageWithReplacedRegion(it, regionName)
                    }
                    return true
                }
            }

            "update" -> {
                if (sender.hasNoPermissionAndMessage("lightcutter.update")) return true

                if (args.size <= 4) {
                    plugin.getMessages().getString("help.update").let {
                        senderAudience.multiMessage(it)
                    }
                    return true
                }

                val regionName = args[1]
                val earn = args[2]
                val needBreak = args[3].toInt()
                val cooldown = args[4].toInt()

                val newIRegion = IRegion(regionName, earn, needBreak, cooldown)
                if (regionHandler.updateRegion(newIRegion) > 0) {
                    plugin.getMessages().getString("region.updated")?.let {
                        sender.messageWithReplacedAll(it, newIRegion)
                    }
                    return true
                } else {
                    plugin.getMessages().getString("region.not-exists")?.let {
                        sender.messageWithReplacedRegion(it, regionName)
                    }
                    return true
                }
            }

            "remove" -> {
                if (sender.hasNoPermissionAndMessage("lightcutter.remove")) return true

                if (args.size == 1) {
                    plugin.getMessages().getString("help.remove").let {
                        senderAudience.multiMessage(it)
                    }
                    return true
                }

                val regionName = args[1]
                if(regionHandler.removeRegion(regionName) > 0) {
                    plugin.getMessages().getString("region.removed")?.let {
                        sender.messageWithReplacedRegion(it, regionName)
                    }
                } else {
                    plugin.getMessages().getString("region.not-exists")?.let {
                        sender.messageWithReplacedRegion(it, regionName)
                    }
                }
            }

            "information", "info", "i" -> {
                if (sender.hasNoPermissionAndMessage("lightcutter.info")) return true

                if (args.size == 1) {
                    plugin.getMessages().getString("help.info").let {
                        senderAudience.multiMessage(it)
                    }
                    return true
                }

                val regionName = args[1]
                val region = regionHandler.getRegion(regionName)
                if (region != null) {
                    senderAudience.multiMessage(region.getInfo())
                    return true
                } else {
                    plugin.getMessages().getString("region.not-exists")?.let {
                        sender.messageWithReplacedRegion(it, regionName)
                    }
                    return true
                }
            }

            "list" -> {
                if (sender.hasNoPermissionAndMessage("lightcutter.list")) return true

                val separator = plugin.getMessages().getString("list.separator") ?: "|"
                val header = plugin.getMessages().getString("list.header")
                    ?: "<white>Name #separator# Earn #separator# Need break #separator# Cooldown<reset>"

                val builder: StringBuilder = StringBuilder()
                    .appendLine()
                    .append(header.replace("#separator#", separator))
                    .appendLine()
                regionHandler.getRegions().forEach {
                    builder.append(it.name).append(" ").append(separator).append(" ")
                        .append(it.earn).append(" ").append(separator).append(" ")
                        .append(it.needBreak).append(" ").append(separator).append(" ")
                        .append(it.cooldown)
                        .appendLine()
                }

                val msg = builder.toString()
                senderAudience.multiMessage(msg)
                return true
            }

            "reload" -> {
                if (sender.hasNoPermissionAndMessage("lightcutter.reload")) return true

                val arg = args.getOrNull(1)
                if(arg != null && arg.equalsIgnoreCase("+database")) {
                    plugin.reloadDatabase()
                }

                plugin.reloadConfigurations()
                plugin.getMessages().getString("reload-config").let {
                    senderAudience.multiMessage(it)
                }
                return true
            }
        }
        return true
    }

    private fun CommandSender.hasNoPermissionAndMessage(permission: String): Boolean {
        if (!this.hasPermission(permission)) {
            plugin.getMessages().getString("warnings.no-permissions")?.let {
                this.getAudience().multiMessage(it.replace("#permission#", permission))
            }
            return true
        } else return false
    }

    private fun CommandSender.messageWithReplacedRegion(it: String, regionName: String) {
        this.getAudience().multiMessage(it.replace("#region#", regionName))
    }

    private fun CommandSender.messageWithReplacedAll(it: String, region: Region) {
        val regionName = region.name
        this.getAudience().multiMessage(message = it.replace("#region#", regionName),
                                        hover = region.getInfo(),
                                        event = ClickEvent.Action.RUN_COMMAND,
                                        action = "/lightcutter info $regionName")
    }

}