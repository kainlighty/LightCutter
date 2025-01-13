package ru.kainlight.lightcutter.COMMANDS

import net.kyori.adventure.text.event.ClickEvent
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import ru.kainlight.lightcutter.DATA.Region
import ru.kainlight.lightcutter.Main
import ru.kainlight.lightcutter.getAudience
import ru.kainlight.lightlibrary.API.WorldGuardAPI
import ru.kainlight.lightlibrary.equalsIgnoreCase
import ru.kainlight.lightlibrary.multiMessage
import kotlin.text.replace

@Suppress("WARNINGS")
class MainCommand(private val plugin: Main) : CommandExecutor {

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<String>): Boolean {
        if (args.isEmpty() && command.name.equalsIgnoreCase("lightcutter")) {
            sender.hasNoPermissionAndMessage("lightcutter.help")
            plugin.getMessagesConfig().getStringList("help.commands").forEach { sender.getAudience().multiMessage(it) }
            return true
        }

        when (args[0].lowercase()) {
            "add" -> {
                if (sender.hasNoPermissionAndMessage("lightcutter.add")) return true

                if (args.size <= 4) {
                    plugin.getMessagesConfig().getString("help.add").let {
                        sender.getAudience().multiMessage(it)
                    }
                    return true
                }

                val regionName = args[1]

                if(sender is Player) {
                    if(!WorldGuardAPI.hasRegion(sender.world, regionName)) {
                        plugin.getMessagesConfig().getString("region.not-exists")?.let {
                            sender.messageWithReplacedRegion(it, regionName)
                        }
                        return true
                    }
                }

                val earn = args[2]
                val needBreak = args[3].toInt()
                val cooldown = args[4].toInt()

                val newRegion = Region(regionName, earn, needBreak, cooldown)
                if (plugin.database.insertRegion(newRegion) > 0) {
                    plugin.getMessagesConfig().getString("region.added")?.let {
                        sender.messageWithReplacedAll(it, newRegion)
                    }
                    return true
                } else {
                    plugin.getMessagesConfig().getString("region.exists")?.let {
                        sender.messageWithReplacedRegion(it, regionName)
                    }
                    return true
                }
            }

            "update" -> {
                if (sender.hasNoPermissionAndMessage("lightcutter.update")) return true

                if (args.size <= 4) {
                    plugin.getMessagesConfig().getString("help.update").let {
                        sender.getAudience().multiMessage(it)
                    }
                    return true
                }

                val regionName = args[1]
                val earn = args[2]
                val needBreak = args[3].toInt()
                val cooldown = args[4].toInt()

                val newRegion = Region(regionName, earn, needBreak, cooldown)
                if (/*plugin.database.hasRegion(regionName) &&*/ plugin.database.updateRegion(newRegion) > 0) {
                    plugin.getMessagesConfig().getString("region.updated")?.let {
                        sender.messageWithReplacedAll(it, newRegion)
                    }
                    return true
                } else {
                    plugin.getMessagesConfig().getString("region.not-exists")?.let {
                        sender.messageWithReplacedRegion(it, regionName)
                    }
                    return true
                }
            }

            "remove" -> {
                if (sender.hasNoPermissionAndMessage("lightcutter.remove")) return true

                if (args.size == 1) {
                    plugin.getMessagesConfig().getString("help.remove").let {
                        sender.getAudience().multiMessage(it)
                    }
                    return true
                }

                val regionName = args[1]
                if(plugin.database.removeRegion(regionName) > 0) {
                    plugin.getMessagesConfig().getString("region.removed")?.let {
                        sender.messageWithReplacedRegion(it, regionName)
                    }
                } else {
                    plugin.getMessagesConfig().getString("region.not-exists")?.let {
                        sender.messageWithReplacedRegion(it, regionName)
                    }
                }
            }

            "information", "info", "i" -> {
                if (sender.hasNoPermissionAndMessage("lightcutter.info")) return true

                if (args.size == 1) {
                    plugin.getMessagesConfig().getString("help.info").let {
                        sender.getAudience().multiMessage(it)
                    }
                    return true
                }

                val regionName = args[1]
                val region = plugin.database.getRegion(regionName)
                if (region != null) {
                    sender.getAudience().multiMessage(region.getInfo())
                    return true
                } else {
                    plugin.getMessagesConfig().getString("region.not-exists")?.let {
                        sender.messageWithReplacedRegion(it, regionName)
                    }
                    return true
                }
            }

            "list" -> {
                if (sender.hasNoPermissionAndMessage("lightcutter.list")) return true

                val separator = plugin.getMessagesConfig().getString("list.separator") ?: "|"
                val header = plugin.getMessagesConfig().getString("list.header")
                    ?: "<white>Name #separator# Earn #separator# Need break #separator# Cooldown<reset>"

                val builder: StringBuilder = StringBuilder()
                    .appendLine()
                    .append(header.replace("#separator#", separator))
                    .appendLine()
                plugin.database.getRegions().forEach {
                    builder.append(it.name).append(" ").append(separator).append(" ")
                        .append(it.earn).append(" ").append(separator).append(" ")
                        .append(it.needBreak).append(" ").append(separator).append(" ")
                        .append(it.cooldown)
                        .appendLine()
                }

                val msg = builder.toString()
                sender.getAudience().multiMessage(msg)
                return true
            }

            "reload" -> {
                if (sender.hasNoPermissionAndMessage("lightcutter.reload")) return true

                val arg = args.getOrNull(1)
                if(arg != null && arg.equalsIgnoreCase("+database")) {
                    plugin.reloadDatabase()
                }

                plugin.reloadConfigurations()

                plugin.getMessagesConfig().getString("reload-config").let {
                    sender.getAudience().multiMessage(it)
                }
                return true
            }
        }
        return true
    }

    private fun CommandSender.hasNoPermissionAndMessage(permission: String): Boolean {
        if (!this.hasPermission(permission)) {
            plugin.getMessagesConfig().getString("warnings.no-permissions")?.let {
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
                                          "/lightcutter info $regionName")
    }

}