package ru.kainlight.lightcutter.api

import org.bukkit.entity.Player
import ru.kainlight.lightcutter.Main
import ru.kainlight.lightcutter.data.EconomyType
import ru.kainlight.lightlibrary.API.ECONOMY.LightEconomy
import ru.kainlight.lightlibrary.UTILS.DebugBukkit
import ru.kainlight.lightlibrary.getAudience
import ru.kainlight.lightlibrary.multiMessage
import java.text.DecimalFormat
import kotlin.random.Random

internal class IEconomyHandler(val plugin: Main, val economy: EconomyType) : EconomyHandler {

    override fun depositWithRegion(player: Player, earn: String) {
        val cost = this.getOrRandomCost(earn)
        val message = plugin.getMessages().getString("region.earn").orEmpty()

        this.salary(player, cost, message)
    }

    override fun depositWithoutRegion(player: Player, blockName: String) {
        val logName = plugin.getMessages()
            .getString("log-names.$blockName")
            ?: "Unnamed block: $blockName"

        val message = plugin.getMessages()
            .getString("world.earn")
            ?.replace("#block#", logName)
            .orEmpty()

        val cost = this.getOrRandomCost(plugin.config.getString("world-settings.costs.$blockName"))

        this.salary(player, cost, message)
    }

    private fun salary(player: Player, treeCost: Double, message: String) {
        val treeCostInt: Int  = treeCost.toInt()

        val isDeposited = when (economy) {
            EconomyType.VAULT -> LightEconomy.VAULT.deposit(player, treeCost)
            EconomyType.PLAYERPOINTS -> LightEconomy.POINTS.deposit(player, treeCost)
        }

        val formattedMessage = message.replace("#amount#", treeCost.toString())
            .replace("#amount_rounded#", treeCostInt.toString())

        if (isDeposited) player.getAudience().multiMessage(formattedMessage)
        else DebugBukkit.warn("Deposit problem")
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