package ru.kainlight.lightcutter.UTILS

import org.bukkit.entity.Player
import ru.kainlight.lightcutter.DATA.EconomyType
import ru.kainlight.lightcutter.Main
import ru.kainlight.lightcutter.getAudience
import ru.kainlight.lightlibrary.ECONOMY.LightEconomy
import ru.kainlight.lightlibrary.multiMessage
import java.text.DecimalFormat
import java.util.logging.Level
import kotlin.random.Random

class EconomyManager(val plugin: Main, val economy: EconomyType) {

    fun depositWithRegion(player: Player, earn: String) {
        val cost = getOrRandomCost(earn)
        val message = plugin.getMessagesConfig().getString("region.earn").orEmpty()
        salary(player, cost, message)
    }

    fun depositWithoutRegion(player: Player, blockName: String) {
        val logName = plugin.getMessagesConfig()
            .getString("log-names.$blockName")
            ?: "Unnamed block: $blockName"

        val message = plugin.getMessagesConfig()
            .getString("world.earn")
            ?.replace("#block#", logName)
            .orEmpty()

        val cost = getOrRandomCost(plugin.config.getString("world-settings.costs.$blockName"))
        salary(player, cost, message)
    }

    private fun salary(player: Player, treeCost: Double, message: String) {
        val treeCostInt: Int  = treeCost.toInt()

        val isDeposited = when (economy) {
            EconomyType.VAULT -> LightEconomy.VAULT?.deposit(player, treeCost)
            EconomyType.PLAYERPOINTS -> LightEconomy.POINTS?.deposit(player, treeCost)
            else -> {
                Debug.log("The installed Economy was not found", Level.SEVERE)
                return
            }
        }

        val formattedMessage = message.replace("#amount#", treeCost.toString()).replace("#amount_rounded#", treeCostInt.toString())
        if (isDeposited == true) player.getAudience().multiMessage(formattedMessage)
        else Debug.log("Deposit problem", Level.WARNING)
    }

    private fun getOrRandomCost(costString: String?): Double {
        if(costString.isNullOrEmpty()) return 0.0

        //val random: Random = Random()
        if (costString.contains("-")) {
            val parts: List<String> = costString.split("-")
            val min: Double  = parts[0].toDouble()
            val max: Double  = parts[1].toDouble()
            val randomValue: Double = min + (max - min) * Random.nextDouble()

            val format: String = plugin.config.getString("woodcutter-settings.economy-format", "#.#")!!
            val df = DecimalFormat(format)
            return df.format(randomValue).replace(",", ".").toDouble()
        } else {
            return costString.toDouble()
        }
    }

}