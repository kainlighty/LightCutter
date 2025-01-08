package ru.kainlight.lightcutter.COMMANDS

import net.kyori.adventure.text.event.ClickEvent
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import ru.kainlight.lightcutter.DATA.Region
import ru.kainlight.lightcutter.Main
import ru.kainlight.lightcutter.getAudience
import ru.kainlight.lightlibrary.equalsIgnoreCase
import ru.kainlight.lightlibrary.multiMessage

@Suppress("WARNINGS")
class MainCommand(private val plugin: Main) : CommandExecutor {

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<String>): Boolean {
        if (sender.hasNoPermissionAndMessage("help")) return true

        if (args.isEmpty() && command.name.equalsIgnoreCase("lightcutter")) {
            plugin.getMessagesConfig().getStringList("help.commands").forEach { sender.getAudience().multiMessage(it) }
            return true
        }

        when (args[0].lowercase()) {
            "add" -> {
                if (sender.hasNoPermissionAndMessage("add")) return true

                if (args.size <= 4) {
                    plugin.getMessagesConfig().getString("help.add").let {
                        sender.getAudience().multiMessage(it)
                    }
                    return true
                }

                val regionName = args[1]
                val earn = args[2]
                val needBreak = args[3].toInt()
                val cooldown = args[4].toInt()

                if (plugin.database.getRegion(regionName) == null) {
                    val regionAdded =
                        plugin.getMessagesConfig().getString("region.added")?.replace("#region#", regionName)
                    val newRegion = Region(regionName, earn, needBreak, cooldown)
                    plugin.database.insertRegion(newRegion)
                    sender.getAudience().multiMessage(message = regionAdded,
                                                      hover = newRegion.getInfo(),
                                                      event = ClickEvent.Action.RUN_COMMAND,
                                                      "/lightcutter info $regionName")
                    return true
                } else {
                    plugin.getMessagesConfig().getString("region.exists")?.replace("#region#", regionName).let {
                        sender.getAudience().multiMessage(it)
                    }
                    return true
                }
            }

            "update" -> {
                if (sender.hasNoPermissionAndMessage("update")) return true

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

                if (plugin.database.getRegion(regionName) != null) {
                    val regionUpdated =
                        plugin.getMessagesConfig().getString("region.updated")?.replace("#region#", regionName)
                    val newRegion = Region(regionName, earn, needBreak, cooldown)
                    plugin.database.updateRegion(newRegion)
                    sender.getAudience().multiMessage(message = regionUpdated,
                                                      hover = newRegion.getInfo(),
                                                      event = ClickEvent.Action.RUN_COMMAND,
                                                      "/lightcutter info $regionName")
                    return true
                } else {
                    plugin.getMessagesConfig().getString("region.not-exists")?.replace("#region#", regionName).let {
                        sender.getAudience().multiMessage(it)
                    }
                    return true
                }
            }

            "remove" -> {
                if (sender.hasNoPermissionAndMessage("remove")) return true

                if (args.size == 1) {
                    plugin.getMessagesConfig().getString("help.remove").let {
                        sender.getAudience().multiMessage(it)
                    }
                    return true
                }

                val regionName = args[1]
                val region = plugin.database.getRegion(regionName)
                if (region != null) {
                    plugin.database.removeRegion(regionName)

                    plugin.getMessagesConfig().getString("region.removed")?.replace("#region#", regionName).let {
                        sender.getAudience().multiMessage(it)
                    }
                    return true
                } else {
                    plugin.getMessagesConfig().getString("region.not-exists")?.replace("#region#", regionName).let {
                        sender.getAudience().multiMessage(it)
                    }
                    return true
                }
            }

            "information", "info", "i" -> {
                if (sender.hasNoPermissionAndMessage("info")) return true

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
                    plugin.getMessagesConfig().getString("region.not-exists")?.replace("#region#", regionName).let {
                        sender.getAudience().multiMessage(it)
                    }
                    return true
                }
            }

            "list" -> {
                if (sender.hasNoPermissionAndMessage("list")) return true

                val separator = plugin.getMessagesConfig().getString("list.separator") ?: "|"
                val header = plugin.getMessagesConfig().getString("list.header")
                    ?: "<white>Name #separator# Earn #separator# Need break #separator# Cooldown<reset>"

                val builder: StringBuilder = StringBuilder(" ")
                    .append(header.replace("#separator#", separator))
                    .appendLine()
                plugin.database.getRegions().forEach {
                    builder.append(it.name).append(separator)
                        .append(it.earn).append(separator)
                        .append(it.needBreak).append(separator)
                        .append(it.cooldown)
                        .appendLine()
                    //builder.append("${it.name} $separator ${it.earn} $separator ${it.needBreak} $separator ${it.cooldown}").appendLine()
                }

                val msg = builder.toString()
                sender.getAudience().multiMessage(msg)
                return true
            }

            "reload" -> {
                if (sender.hasNoPermissionAndMessage("reload")) return true

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
        if (! this.hasPermission("lightcutter.$permission")) {
            plugin.getMessagesConfig().getString("warnings.no-permissions").let {
                this.getAudience().multiMessage(it)
            }
            return true
        } else return false
    }

    /*class Completer(private val plugin: Main): TabCompleter {
        override fun onTabComplete(p0: CommandSender, command: Command, label: String, args: Array<String>?): MutableList<String>? {

        }

    }*/

}