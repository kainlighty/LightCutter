package ru.kainlight.lightcutter.DATA

import org.bukkit.configuration.file.FileConfiguration
import ru.kainlight.lightcutter.Main

data class Region(
    val name: String,
    val earn: String,
    val needBreak: Int,
    val cooldown: Int,
) {
    fun getInfo(regionName: String): String? {
        val region: Region? = Main.INSTANCE.database.getRegion(regionName);
        val messages: FileConfiguration = Main.INSTANCE.messageConfig.getConfig();
        if(region == null) return messages.getString("region.not-exists");

        val info: String  = messages.getString("region.info")!!
        return info.replace("<region>", this.name)
            .replace("<earn>", this.earn.toString())
            .replace("<count>", this.needBreak.toString())
            .replace("<cooldown>", this.cooldown.toString());
    }
}
