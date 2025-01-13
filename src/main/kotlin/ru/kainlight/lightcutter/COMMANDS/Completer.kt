package ru.kainlight.lightcutter.COMMANDS

import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter
import org.bukkit.entity.Player
import ru.kainlight.lightcutter.Main
import ru.kainlight.lightlibrary.API.WorldGuardAPI
import ru.kainlight.lightlibrary.equalsIgnoreCase

class Completer(private val plugin: Main) : TabCompleter {

    override fun onTabComplete(sender: CommandSender, command: Command, alias: String, args: Array<out String>): List<String>? {
        if(!command.name.equalsIgnoreCase("lightcutter") || !command.aliases.contains("lc")) return null
        if (!sender.hasPermission("lightcutter.help")) return null

        val completions = mutableListOf<String>()
        when (args.size) {
            1 -> { // Первый арг
                completions.addAll(listOf("add", "update", "remove", "info", "list", "reload"))
            }

            2 -> {
                when (args[0].lowercase()) {
                    "update", "remove", "info" -> {
                        val regions = plugin.database.getRegions().map { it.name }
                        completions.add("<name>")
                        if (regions.isNotEmpty()) {
                            completions.addAll(regions)
                        }
                    }

                    "add" -> {
                        completions.add("<name>")
                        if (sender is Player) {
                            val regions = WorldGuardAPI.getRegions(sender.location)
                            if (regions.isNotEmpty()) {
                                completions.addAll(regions)
                            }
                        }
                    }

                    "reload" -> completions.add("+database")
                }
            }

            3 -> {
                when (args[0].lowercase()) {
                    "add", "update" -> {
                        completions.addAll(listOf("<earn>", "5", "10", "30"))
                    }
                }
            }

            4 -> {
                when (args[0].lowercase()) {
                    "add", "update" -> {
                        completions.addAll(listOf("<need_break>", "5", "10", "30"))
                    }
                }
            }

            5 -> {
                when (args[0].lowercase()) {
                    "add", "update" -> {
                        completions.addAll(listOf("<cooldown>", "5", "10", "30"))
                    }
                }
            }
        }

        // Фильтрация предложений на основе введённых данных
        return completions.distinct().filter { it.startsWith(args.last(), ignoreCase = true) }
    }
}