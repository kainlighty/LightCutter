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
        if (this.noPermissionsMessages(sender, "help")) return true

        if (args.isEmpty() && command.name.equalsIgnoreCase("lightcutter")) {
            val helpCommands = plugin.getMessageConfig().getStringList("help.commands")
            helpCommands?.forEach {
                sender.getAudience().multiMessage(it)
            }
            return true;
        }

        when (args[0].lowercase()) {
            "add" -> {
                if(noPermissionsMessages(sender, "add")) return true

                if (args.size <= 4) {
                    val helpMessage = plugin.getMessageConfig().getString("help.add")
                    sender.getAudience().multiMessage(helpMessage);
                    return true;
                }

                val regionName = args[1];
                val earn = args[2]
                val needBreak = args[3].toInt()
                val cooldown = args[4].toInt()

                if (plugin.database.getRegion(regionName) == null) {
                    val regionAdded = plugin.getMessageConfig().getString("region.added")?.replace("#region#", regionName);
                    val newRegion = Region(regionName, earn, needBreak, cooldown);
                    plugin.database.insertRegion(newRegion);
                    sender.getAudience().multiMessage(message = regionAdded,
                                                 hover = newRegion.getInfo(),
                                                 event = ClickEvent.Action.RUN_COMMAND,
                                                 "/lightcutter info $regionName")
                    return true;
                } else {
                    val regionExists =
                        plugin.getMessageConfig().getString("region.exists")?.replace("#region#", regionName);
                    sender.getAudience().multiMessage(regionExists);
                    return true;
                }
            }

            "update" -> {
                if(noPermissionsMessages(sender, "update")) return true

                if (args.size <= 4) {
                    val helpMessage = plugin.getMessageConfig().getString("help.update")
                    sender.getAudience().multiMessage(helpMessage);
                    return true;
                }

                val regionName = args[1]
                val earn = args[2]
                val needBreak = args[3].toInt()
                val cooldown = args[4].toInt()

                if (plugin.database.getRegion(regionName) != null) {
                    val regionUpdated =
                        plugin.getMessageConfig().getString("region.updated")?.replace("#region#", regionName);
                    val newRegion = Region(regionName, earn, needBreak, cooldown);
                    plugin.database.updateRegion(newRegion);
                    sender.getAudience().multiMessage(message = regionUpdated,
                                                 hover = newRegion.getInfo(),
                                                 event = ClickEvent.Action.RUN_COMMAND,
                                                 "/lightcutter info $regionName")
                    return true;
                } else {
                    val regionExists =
                        plugin.getMessageConfig().getString("region.not-exists")?.replace("#region#", regionName);
                    sender.getAudience().multiMessage(regionExists);
                    return true;
                }
            }

            "remove" -> {
                if(noPermissionsMessages(sender, "remove")) return true

                if (args.size == 1) {
                    val helpMessage = plugin.getMessageConfig().getString("help.remove")
                    sender.getAudience().multiMessage(helpMessage);
                    return true;
                }

                val regionName = args[1];
                val region = plugin.database.getRegion(regionName);
                if (region != null) {
                    plugin.database.removeRegion(regionName);

                    val message =
                        plugin.getMessageConfig().getString("region.removed")?.replace("#region#", regionName);
                    sender.getAudience().multiMessage(message);
                    return true;
                } else {
                    val message =
                        plugin.getMessageConfig().getString("region.not-exists")?.replace("#region#", regionName);
                    sender.getAudience().multiMessage(message);
                    return true;
                }
            }

            "information", "info", "i" -> {
                if(noPermissionsMessages(sender, "info")) return true

                if (args.size == 1) {
                    val helpMessage = plugin.getMessageConfig().getString("help.info")
                    sender.getAudience().multiMessage(helpMessage);
                    return true;
                }

                val regionName = args[1];
                val region = plugin.database.getRegion(regionName);
                if (region != null) {
                    sender.getAudience().multiMessage(region.getInfo());
                    return true;
                } else {
                    val message =
                        plugin.getMessageConfig().getString("region.not-exists")?.replace("#region#", regionName);
                    sender.getAudience().multiMessage(message);
                    return true;
                }
            }

            "list" -> {
                if(noPermissionsMessages(sender, "list")) return true

                val separator = plugin.getMessageConfig().getString("list.separator") ?: "|"
                val header = plugin.getMessageConfig().getString("list.header") ?: "<white>Name #separator# Earn #separator# Need break #separator# Cooldown<reset>"

                val builder: StringBuilder = StringBuilder()

                builder.append(" ${header.replace("#separator#", separator)}").appendLine()
                plugin.database.getRegions().forEach {
                    builder.append("${it.name} $separator ${it.earn} $separator ${it.needBreak} $separator ${it.cooldown}").appendLine()
                }

                val msg = builder.toString()
                sender.getAudience().multiMessage(msg)
                return true;
            }

            "reload" -> {
                if(noPermissionsMessages(sender, "reload")) return true

                plugin.saveDefaultConfig();
                plugin.reloadConfig();
                plugin.messageConfig.saveDefaultConfig();
                plugin.messageConfig.reloadLanguage("main-settings.language");

                plugin.reloadParseMode()

                plugin.disabledWorlds.addAll(plugin.config.getStringList("woodcutter-settings.disabled-worlds"));

                val notify = plugin.getMessageConfig().getString("reload-config");
                sender.getAudience().multiMessage(notify);
                return true;
            }
        }
        return true;
    }

    private fun noPermissionsMessages(sender: CommandSender, permission: String): Boolean {
        if (! sender.hasPermission("lightcutter.$permission")) {
            val msg = plugin.getMessageConfig().getString("warnings.no-permissions")
            sender.getAudience().multiMessage(msg)
            return true
        } else return false
    }

    /*class Completer(private val plugin: Main): TabCompleter {
        override fun onTabComplete(p0: CommandSender, command: Command, label: String, args: Array<String>?): MutableList<String>? {

        }

    }*/

}