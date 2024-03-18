package ru.kainlight.lightcutter.COMMANDS

import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import ru.kainlight.lightcutter.DATA.Region
import ru.kainlight.lightcutter.Main
import ru.kainlight.lightcutter.getAudience
import ru.kainlight.lightlibrary.equalsIgnoreCase
import ru.kainlight.lightlibrary.legacyMessage

class MainCommand(private val plugin: Main) : CommandExecutor {

    private val ru_messages: String = """
                          &f&m   &c&l LightCutter Help &f&m   
                        &c&l » &f/lightcutter list &8- &7список добавленных регионов
                        &c&l » &f/lightcutter info <name> &8- &7информация о регионе
                        &c&l » &f/lightcutter add <name> <earn> <need break> <cooldown> &8- &7добавить/обновить регион
                        &c&l » &f/lightcutter remove <name> &8- &7удалить регион
                        &c&l » &f/lightcutter drop &8- &7включить/выключить выпадение дерева
                        &c&l » &f/lightcutter reload &8- &7перезагрузить конфигурации
                        """.trimIndent()
    private val en_messages: String = """
                          &f&m   &c&l LightCutter Help &f&m   
                        &c&l » &f/lightcutter list &8- &7region list
                        &c&l » &f/lightcutter info <name> &8- &7region information
                        &c&l » &f/lightcutter add <name> <earn> <need break> <cooldown> &8- &7add/update region
                        &c&l » &f/lightcutter remove <name> &8- &7remove region
                        &c&l » &f/lightcutter drop &8- &7enable/disable drops
                        &c&l » &f/lightcutter reload &8- &7reload configurations
                        """.trimIndent()

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<String>): Boolean {
        if (! sender.hasPermission("lightcutter.admin")) return true;

        if (args.isEmpty() && command.name.equalsIgnoreCase("lightcutter")) {
            val lang: String = plugin.getConfig().getString("language") !!
            sender.sendMessage("");
            if (lang.equalsIgnoreCase("RUSSIAN")) sender.getAudience().legacyMessage(ru_messages)
            else sender.getAudience().legacyMessage(en_messages)
            return true;
        }

        when (args[0].lowercase()) {
            "add" -> {
                if (args.size <= 4) {
                    sender.getAudience().legacyMessage("&c&l » &fEnter the name, earn, number of broken blocks and cooldown");
                    return true;
                }

                val regionName = args[1];
                val earn = args[2]
                val needBreak = args[3].toInt()
                val cooldown = args[4].toInt()

                if (! plugin.database.getRegions().contains(regionName)) {
                    val regionAdded = plugin.messageConfig.getConfig().getString("region.added")?.replace("<region>", regionName);
                    val newRegion = Region(regionName, earn, needBreak, cooldown);
                    plugin.database.insertRegion(newRegion);
                    sender.getAudience().legacyMessage(message = regionAdded, hover = newRegion.getInfo(regionName))
                    return true;
                } else {
                    val regionExists = plugin.messageConfig.getConfig().getString("region.exists")?.replace("<region>", regionName);
                    sender.getAudience().legacyMessage(regionExists);
                    return true;
                }
            }

            "update" -> {

                if (args.size <= 4) {
                    sender.getAudience().legacyMessage("&c&l » &fEnter the name, earn, number of broken blocks and cooldown");
                    return true;
                }

                val regionName = args[1]
                val earn = args[2]
                val needBreak = args[3].toInt()
                val cooldown = args[4].toInt()

                if (plugin.database.getRegion(regionName) != null) {
                    val regionUpdated = plugin.messageConfig.getConfig().getString("region.updated")?.replace("<region>", regionName);
                    val newRegion = Region(regionName, earn, needBreak, cooldown);
                    plugin.database.updateRegion(newRegion);
                    sender.getAudience().legacyMessage(message = regionUpdated, hover = newRegion.getInfo(regionName))
                    return true;
                } else {
                    val regionExists = plugin.messageConfig.getConfig().getString("region.not-exists")?.replace("<region>", regionName);
                    sender.getAudience().legacyMessage(regionExists);
                    return true;
                }
            }

            "remove" -> {
                if (args.size == 1) {
                    sender.getAudience().legacyMessage("&c&l » &fEnter the name");
                    return true;
                }

                val regionName = args[1];
                val region = plugin.database.getRegion(regionName);
                if (region != null) {
                    plugin.database.removeRegion(regionName);

                    val message = plugin.messageConfig.getConfig().getString("region.removed")?.replace("<region>", regionName);
                    sender.getAudience().legacyMessage(message);
                    return true;
                } else {
                    val message = plugin.messageConfig.getConfig().getString("region.not-exists")?.replace("<region>", regionName);
                    sender.getAudience().legacyMessage(message);
                    return true;
                }
            }

            "info", "i" -> {
                if (args.size == 1) {
                    sender.getAudience().legacyMessage("&c&l » &fEnter the name");
                    return true;
                }

                val regionName = args[1];
                val region = plugin.database.getRegion(regionName);
                if (region != null) {
                    sender.getAudience().legacyMessage(region.getInfo(regionName));
                    return true;
                } else {
                    val message = plugin.messageConfig.getConfig().getString("region.not-exists")?.replace("<region>", regionName);
                    sender.getAudience().legacyMessage(message);
                    return true;
                }
            }

            "list" -> {
                //val workingRegionsList: String = String.join(", ", plugin.database.getRegions());
                val workingRegionsList = plugin.database.getRegions().joinToString(", ")

                sender.getAudience().legacyMessage("Regions: \n$workingRegionsList")
                return true;
            }

            "drop" -> {
                val drop = plugin.getConfig().getBoolean("woodcutter-settings.allow-drop")
                plugin.config.set("woodcutter-settings.allow-drop", ! drop);
                plugin.saveConfig();
                val newValue = ! drop;

                val notify = plugin.messageConfig.getConfig().getString("changed.drop")!!
                    .replace("<value>", newValue.toString());
                sender.getAudience().legacyMessage(notify);
                return true;
            }

            "reload" -> {
                plugin.saveDefaultConfig();
                plugin.reloadConfig();
                plugin.messageConfig.saveDefaultConfig();
                plugin.messageConfig.reloadLanguage("language");

                val notify = plugin.messageConfig.getConfig().getString("reload-config");
                sender.getAudience().legacyMessage(notify);
                return true;
            }
        }
        return true;
    }
}