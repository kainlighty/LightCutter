package ru.kainlight.lightcutter.listeners

import org.bukkit.GameMode
import org.bukkit.Material
import org.bukkit.Particle
import org.bukkit.entity.EntityType
import org.bukkit.entity.FallingBlock
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.entity.EntityChangeBlockEvent
import org.jetbrains.annotations.ApiStatus.ScheduledForRemoval
import ru.kainlight.lightcutter.animations.TreeAnimation
import ru.kainlight.lightcutter.animations.isWood
import ru.kainlight.lightcutter.data.MessageType
import ru.kainlight.lightcutter.data.WoodCutterMode
import ru.kainlight.lightcutter.Main
import ru.kainlight.lightcutter.api.LightCutterAPI
import ru.kainlight.lightcutter.api.Region
import ru.kainlight.lightlibrary.API.WorldGuardAPI
import ru.kainlight.lightlibrary.getAudience
import ru.kainlight.lightlibrary.multiActionbar
import ru.kainlight.lightlibrary.multiMessage
import java.util.UUID

@Suppress("WARNINGS")
internal class BlockListener(private val plugin: Main) : Listener {

    private val playerBlockCount: MutableMap<Player, Int> = mutableMapOf()
    private val playerCooldown: MutableMap<UUID, Long> = mutableMapOf()

    @EventHandler(priority = EventPriority.LOWEST)
    fun onBlockBreak(event: BlockBreakEvent) {
        val block = event.block
        if (plugin.disabledWorlds.contains(block.world.name)) return
        val blockType = block.type
        if (! blockType.isWood()) return

        val api = LightCutterAPI.getProvider()

        val currentWoodCutterMode = WoodCutterMode.getCurrent()
        val region: Region? = WorldGuardAPI.getRegionNames(block.location, true)
            .asSequence() // Последовательная ленивая обработка
            .mapNotNull { api.regionHandler.getRegion(it) } // Берём регион из базы данных, исключая наллы.
            .firstOrNull() // Находим первый совпадающий регион
            ?: return // Если увы и ах — пока-пока

        val player = event.player
        if (currentWoodCutterMode == WoodCutterMode.REGION) {
            val region: Region? = WorldGuardAPI.getRegionNames(block.location, true)
                .asSequence() // Последовательная ленивая обработка
                .mapNotNull { api.regionHandler.getRegion(it) } // Берём регион из базы данных, исключая наллы.
                .firstOrNull() // Находим первый совпадающий регион
            if(region == null) return

            val ownerBypass = plugin.config.getBoolean("region-settings.owner-bypass")
            if (ownerBypass && WorldGuardAPI.isOwnerOfRegion(player, region.name)) return

            if (! player.isDefaultGamemode()) {
                event.isCancelled = true
                return
            }

            val currentMessageType = MessageType.getCurrent()

            val cooldown = region.cooldown
            val currentTime = System.currentTimeMillis()
            val cooldownEndTime = currentTime + cooldown * 1000L
            if (player.sendCooldownMessageIfPresent(currentTime, currentMessageType)) {
                event.isCancelled = true
                return
            }

            // Получаем количество сломанных блоков для игрока и отнимаем
            val needBreak = region.needBreak
            var blockCount = this.playerBlockCount.getOrDefault(player, needBreak)

            // Обновляем количество сломанных блоков
            blockCount --
            this.playerBlockCount.put(player, blockCount)

            // Проверяем количество сломанных блоков
            if (blockCount > 0) {
                player.sendBreakMessage(blockCount, currentMessageType)
                event.isCancelled = true
                return
            }

            // Запускаем анимацию дерева и его восстановление
            TreeAnimation.start(plugin, event)

            // Оплата
            api.economyHandler.depositWithRegion(player, region.earn)

            // Сбрасываем количество сломанных блоков для игрока
            this.playerBlockCount.remove(player)
            if (cooldown != 0) this.playerCooldown.put(player.uniqueId, cooldownEndTime)

            event.isCancelled = true
        } else if (currentWoodCutterMode == WoodCutterMode.WORLD && region == null) {
            if (! player.isDefaultGamemode()) return
            api.economyHandler.depositWithoutRegion(player, blockType.name.lowercase()) // Оплата
        }
    }

    @Deprecated("Removed after switching to JDK21") @ScheduledForRemoval
    // For < 1.19.4
    @EventHandler
    fun onFallingBlock(event: EntityChangeBlockEvent) {
        if (TreeAnimation.fallingBlocks.isEmpty()) return

        if (event.entityType != EntityType.FALLING_BLOCK) return
        val fallingBlock = event.entity as FallingBlock
        if (! TreeAnimation.fallingBlocks.contains(fallingBlock)) return

        val location = fallingBlock.location
        val world = location.world
        val data = fallingBlock.blockData

        plugin.lightScheduler.scheduleSyncDelayedTask(3L) {
            world.getBlockAt(location).type = Material.AIR
            TreeAnimation.fallingBlocks.remove(fallingBlock)
            if (fallingBlock.isOnGround) {
                world.spawnParticle(Particle.BLOCK_CRACK, location, 50, data)
            }
        }
    }

    private fun Player.isDefaultGamemode(): Boolean {
        val inModes = plugin.config.getBoolean("woodcutter-settings.breaking-in-modes", true)

        if (! hasPermission("lightcutter.modes.bypass") && inModes) {
            val warnings = listOfNotNull(
                "warnings.not-survival".takeIf { gameMode != GameMode.SURVIVAL },
                "warnings.is-flying".takeIf { allowFlight },
                "warnings.is-invisible".takeIf { isInvisible || hasMetadata("vanished") }
            ).mapNotNull { plugin.getMessages().getString(it)?.takeIf { it.isNotBlank() } }
            warnings.forEach { getAudience().multiMessage(it) }

            return warnings.isEmpty()
        }
        return true
    }

    private fun Player.sendBreakMessage(blockCount: Int, currentMessageType: MessageType) {
        plugin.getMessages().getString("region.remained") !!.replace("#value#", blockCount.toString()).let {
            if (currentMessageType == MessageType.ACTIONBAR) this.getAudience().multiActionbar(it)
            else if (currentMessageType == MessageType.CHAT) this.getAudience().multiMessage(it)
        }
    }

    private fun Player.sendCooldownMessageIfPresent(currentTime: Long, currentMessageType: MessageType): Boolean {
        if (! this.hasPermission("lightcutter.cooldown.bypass") && ! playerBlockCount.containsKey(this)) {
            val playerCooldown = playerCooldown.get(this.uniqueId) ?: return false

            if (playerCooldown > currentTime) {
                val remained: Long = (playerCooldown - currentTime) / 1000L
                plugin.getMessages().getString("warnings.cooldown")?.replace("#value#", remained.toString()).let {
                    if (currentMessageType == MessageType.ACTIONBAR) this.getAudience().multiActionbar(it)
                    else if (currentMessageType == MessageType.CHAT) this.getAudience().multiMessage(it)
                }
                return true
            }
        }
        return false
    }

}