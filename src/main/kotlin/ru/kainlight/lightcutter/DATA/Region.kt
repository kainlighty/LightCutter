package ru.kainlight.lightcutter.DATA

import org.bukkit.configuration.file.FileConfiguration
import ru.kainlight.lightcutter.Main

data class Region(
    val name: String,
    val earn: String,
    val needBreak: Int,
    val cooldown: Int,
) {
    fun getInfo(): String {
        val messages: FileConfiguration = Main.INSTANCE.messageConfig.getConfig()

        val info: String  = messages.getString("region.info") ?: "null"
        return info.replace("#region#", this.name)
            .replace("#earn#", this.earn.toString())
            .replace("#count#", this.needBreak.toString())
            .replace("#cooldown#", this.cooldown.toString());
    }
}
