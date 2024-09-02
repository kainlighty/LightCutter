package ru.kainlight.lightcutter.COMMANDS

import net.kyori.adventure.text.event.ClickEvent
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import ru.kainlight.lightcutter.DATA.Region
import ru.kainlight.lightcutter.Main
import ru.kainlight.lightcutter.getAudience
import ru.kainlight.lightlibrary.LightPAPIRedefined
import ru.kainlight.lightlibrary.equalsIgnoreCase
import ru.kainlight.lightlibrary.legacyMessage

@Suppress("WARNINGS")
class MainCommand(private val plugin: Main) : CommandExecutor {

/*
    private val ru_messages: String = """
                          &f&m   &c&l LightCutter &f&m   
                        &c&l » &f/lightcutter list &8- &7список добавленных регионов
                        &c&l » &f/lightcutter info <name> &8- &7информация о регионе
                        &c&l » &f/lightcutter add <name> <earn> <need break> <cooldown> &8- &7добавить/обновить регион
                        &c&l » &f/lightcutter remove <name> &8- &7удалить регион
                        &c&l » &f/lightcutter drop &8- &7включить/выключить выпадение дерева
                        &c&l » &f/lightcutter reload &8- &7перезагрузить конфигурации
                        """.trimIndent()
    private val en_messages: String = """
                          &f&m   &c&l LightCutter &f&m   
                        &c&l » &f/lightcutter list &8- &7region list
                        &c&l » &f/lightcutter info <name> &8- &7region information
                        &c&l » &f/lightcutter add <name> <earn> <need break> <cooldown> &8- &7add/update region
                        &c&l » &f/lightcutter remove <name> &8- &7remove region
                        &c&l » &f/lightcutter drop &8- &7enable/disable drops
                        &c&l » &f/lightcutter reload &8- &7reload configurations
                        """.trimIndent()
*/

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<String>): Boolean {
        if (! sender.hasPermission("lightcutter.admin")) return true;

        if (args.isEmpty() && command.name.equalsIgnoreCase("lightcutter")) {
            val helpCommands = plugin.getMessageConfig().getStringList("help.commands")
            helpCommands?.forEach {
                sender.getAudience().legacyMessage(it)
            }
            return true;
        }

        when (args[0].lowercase()) {
            "add" -> {
                if (args.size <= 4) {
                    val helpMessage = plugin.getMessageConfig().getString("help.add")
                    sender.getAudience().legacyMessage(helpMessage);
                    return true;
                }

                val regionName = args[1];
                val earn = args[2]
                val needBreak = args[3].toInt()
                val cooldown = args[4].toInt()

                if (plugin.database.getRegion(regionName) == null) {
                    val regionAdded = plugin.getMessageConfig().getString("region.added")?.replace("<region>", regionName);
                    val newRegion = Region(regionName, earn, needBreak, cooldown);
                    plugin.database.insertRegion(newRegion);
                    sender.getAudience().legacyMessage(message = regionAdded, hover = newRegion.getInfo(), event = ClickEvent.Action.RUN_COMMAND, "/lc info $regionName")
                    return true;
                } else {
                    val regionExists = plugin.getMessageConfig().getString("region.exists")?.replace("<region>", regionName);
                    sender.getAudience().legacyMessage(regionExists);
                    return true;
                }
            }

            "update" -> {
                if (args.size <= 4) {
                    val helpMessage = plugin.getMessageConfig().getString("help.update")
                    sender.getAudience().legacyMessage(helpMessage);
                    return true;
                }

                val regionName = args[1]
                val earn = args[2]
                val needBreak = args[3].toInt()
                val cooldown = args[4].toInt()

                if (plugin.database.getRegion(regionName) != null) {
                    val regionUpdated = plugin.getMessageConfig().getString("region.updated")?.replace("<region>", regionName);
                    val newRegion = Region(regionName, earn, needBreak, cooldown);
                    plugin.database.updateRegion(newRegion);
                    sender.getAudience().legacyMessage(message = regionUpdated, hover = newRegion.getInfo(), event = ClickEvent.Action.RUN_COMMAND, "/lc info $regionName")
                    return true;
                } else {
                    val regionExists = plugin.getMessageConfig().getString("region.not-exists")?.replace("<region>", regionName);
                    sender.getAudience().legacyMessage(regionExists);
                    return true;
                }
            }

            "remove" -> {
                if (args.size == 1) {
                    val helpMessage = plugin.getMessageConfig().getString("help.remove")
                    sender.getAudience().legacyMessage(helpMessage);
                    return true;
                }

                val regionName = args[1];
                val region = plugin.database.getRegion(regionName);
                if (region != null) {
                    plugin.database.removeRegion(regionName);

                    val message = plugin.getMessageConfig().getString("region.removed")?.replace("<region>", regionName);
                    sender.getAudience().legacyMessage(message);
                    return true;
                } else {
                    val message = plugin.getMessageConfig().getString("region.not-exists")?.replace("<region>", regionName);
                    sender.getAudience().legacyMessage(message);
                    return true;
                }
            }

            "information", "info", "i" -> {
                if (args.size == 1) {
                    val helpMessage = plugin.getMessageConfig().getString("help.info")
                    sender.getAudience().legacyMessage(helpMessage);
                    return true;
                }

                val regionName = args[1];
                val region = plugin.database.getRegion(regionName);
                if (region != null) {
                    sender.getAudience().legacyMessage(region.getInfo());
                    return true;
                } else {
                    val message = plugin.getMessageConfig().getString("region.not-exists")?.replace("<region>", regionName);
                    sender.getAudience().legacyMessage(message);
                    return true;
                }
            }

            "list" -> {
                //val workingRegionsList = plugin.database.getRegions().joinToString(", ")
                val header = plugin.getMessageConfig().getString("list.header")!!
                val separator = plugin.getMessageConfig().getString("list.separator")!!

                val builder: StringBuilder = StringBuilder(" ${header.replace("<separator>", separator)}").appendLine()
                plugin.database.getRegions().forEach {
                    builder.append("${it.name} $separator ${it.earn} $separator ${it.needBreak} $separator ${it.cooldown}").appendLine()
                }

                sender.getAudience().legacyMessage(builder.toString())
                return true;
            }

            "drop" -> {
                val drop = plugin.getConfig().getBoolean("woodcutter-settings.allow-drop")
                plugin.config.set("woodcutter-settings.allow-drop", ! drop);
                plugin.saveConfig();
                val newValue = ! drop;

                val notify = plugin.getMessageConfig().getString("changed.drop")!!
                    .replace("<value>", newValue.toString());
                sender.getAudience().legacyMessage(notify);
                return true;
            }

            "reload" -> {
                plugin.saveDefaultConfig();
                plugin.reloadConfig();
                plugin.messageConfig.saveDefaultConfig();
                plugin.messageConfig.reloadLanguage("language");

                plugin.disabledWorlds.addAll(plugin.config.getStringList("woodcutter-settings.disabled-worlds"));

                val notify = plugin.getMessageConfig().getString("reload-config");
                sender.getAudience().legacyMessage(notify);
                return true;
            }
        }
        return true;
    }
}