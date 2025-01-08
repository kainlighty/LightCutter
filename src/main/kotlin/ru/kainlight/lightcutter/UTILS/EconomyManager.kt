package ru.kainlight.lightcutter.UTILS

import org.bukkit.entity.Player
import ru.kainlight.lightcutter.DATA.EconomyType
import ru.kainlight.lightcutter.DATA.Region
import ru.kainlight.lightcutter.Main
import ru.kainlight.lightcutter.getAudience
import ru.kainlight.lightlibrary.ECONOMY.LightEconomy
import ru.kainlight.lightlibrary.multiMessage
import java.text.DecimalFormat
import java.util.logging.Level
import kotlin.random.Random

class EconomyManager(val plugin: Main, val economy: EconomyType) {

    fun depositWithRegion(player: Player, region: Region) {
        val cost = getOrRandomCost(region.earn)
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


    /*fun depositWithRegion(player: Player, region: Region) {
        val treeCost: Double = this.getOrRandomCost(region.earn)
        val ecoMessage: String  = plugin.getMessageConfig().getString("region.earn")!!
        salary(player, treeCost, ecoMessage)
    }

    fun depositWithoutRegion(player: Player, blockName: String) {
        val logName: String = plugin.getMessageConfig().getString("log-names.$blockName") ?: "Unnamed block: $blockName"
        val ecoMessage: String = plugin.getMessageConfig().getString("world.earn")!!.replace("#block#", logName)

        val treeCost: Double = this.getOrRandomCost(plugin.config.getString("world-settings.costs.$blockName"))

        this.salary(player, treeCost, ecoMessage)
    }

    private fun salary(player: Player, treeCost: Double, ecoMessage: String) {
        if (economy.equalsIgnoreCase("VAULT")) {
            val isDeposited = LightEconomy.VAULT?.deposit(player, treeCost)

            if (isDeposited == true) {
                player.getAudience().multiMessage(
                    ecoMessage.replace("#amount#", treeCost.toString()).replace("#amount_rounded#", treeCost.toInt().toString())
                )
            } else Debug.message("Deposit problem", Level.WARNING)
        } else if (economy.equalsIgnoreCase("PLAYERPOINTS")) {
            val treeCostInt: Int  = treeCost.toInt()

            val isDeposited = LightEconomy.POINTS?.deposit(player, treeCost)
            if(isDeposited == true) {
                player.getAudience().multiMessage(
                    ecoMessage.replace("#amount#", treeCostInt.toString()).replace("#amount_rounded#", treeCost.toInt().toString())
                )
            } else Debug.message("Deposit problem", Level.WARNING)
        } else {
            Debug.message("The installed Economy was not found", Level.SEVERE)
        }
    }*/

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