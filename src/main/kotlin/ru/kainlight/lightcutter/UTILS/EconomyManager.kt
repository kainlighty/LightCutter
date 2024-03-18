package ru.kainlight.lightcutter.UTILS

import org.bukkit.block.Block
import org.bukkit.entity.Player
import ru.kainlight.lightcutter.DATA.Region
import ru.kainlight.lightcutter.Main
import ru.kainlight.lightcutter.getAudience
import ru.kainlight.lightlibrary.ECONOMY.LightEconomy
import ru.kainlight.lightlibrary.equalsIgnoreCase
import ru.kainlight.lightlibrary.legacyMessage
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.*
import kotlin.random.Random

class EconomyManager(val plugin: Main, val economy: String) {

    fun depositWithRegion(player: Player, regionName: String) {
        val region: Region = plugin.database.getRegion(regionName) ?: return

        val treeCost: Double = getRandomCost(region.earn)
        val ecoMessage: String  = plugin.messageConfig.getConfig().getString("region.earn")!!
        salary(player, treeCost, ecoMessage);
    }

    fun depositWithoutRegion(player: Player, block: Block) {
        val blockName: String  = block.type.name.lowercase()
        val logName: String = plugin.messageConfig.getConfig().getString("log-names.$blockName")!!
        val ecoMessage: String = plugin.messageConfig.getConfig().getString("world.earn")!!.replace("<block>", logName);

        val treeCost: Double = getRandomCost(plugin.config.getString("world-settings.costs.$blockName"))

        this.salary(player, treeCost, ecoMessage)
    }

    private fun salary(player: Player, treeCost: Double, ecoMessage: String) {
        if (economy.equalsIgnoreCase("VAULT")) {
            val isDeposited = LightEconomy.VAULT!!.deposit(player, treeCost)

            if (isDeposited) player.getAudience().legacyMessage(ecoMessage.replace("<amount>", treeCost.toString()))
            else Main.debug("Deposit problem")
        } else if (economy.equalsIgnoreCase("PLAYERPOINTS")) {
            val treeCostInt: Int  = treeCost.toInt()

            val isDeposited = LightEconomy.POINTS?.deposit(player, treeCost)
            if(isDeposited == true) player.getAudience().legacyMessage(ecoMessage.replace("<amount>", treeCostInt.toString()))
            else Main.debug("Deposit problem")
        }
    }

    fun getRandomCost(costString: String?): Double {
        if(costString.isNullOrEmpty()) return 0.0

        //val random: Random = Random()
        if (costString.contains("-")) {
            val parts: List<String> = costString.split("-");
            val min: Double  = parts[0].toDouble()
            val max: Double  = parts[1].toDouble()
            val randomValue: Double = min + (max - min) * Random.nextDouble()

            val format = plugin.config.getString("woodcutter-settings.economy-format")
            val df = DecimalFormat(format)
            return df.format(randomValue).replace(",", ".").toDouble()
        } else {
            return costString.toDouble()
        }
    }

}