package ru.kainlight.lightcutter.LISTENERS

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
import ru.kainlight.lightcutter.ANIMATIONS.FallAnimation
import ru.kainlight.lightcutter.Main
import ru.kainlight.lightcutter.UTILS.Debug
import ru.kainlight.lightcutter.getAudience
import ru.kainlight.lightlibrary.*

@Suppress("WARNINGS")
class BlockListener(private val plugin: Main) : Listener {

    @EventHandler(priority = EventPriority.LOWEST)
    fun onBlockBreak(event: BlockBreakEvent) {
        val block = event.block;
        if (! FallAnimation.isWood(block.type)) return;
        if (plugin.disabledWorlds.contains(block.world.name)) return;

        val player = event.player;
        val mode = plugin.config.getString("woodcutter-settings.mode")!!
        val WGRegion = LightPAPIRedefined.getRegion(player)

        if (mode.equalsIgnoreCase("REGION") && !WGRegion.isEmpty()) {

            val region = plugin.database.getRegion(WGRegion) ?: return;
            if (!this.checkModes(player)) {
                event.setCancelled(true);
                return;
            }

            val cooldown = region.cooldown
            val cooldownEndTime = System.currentTimeMillis() + cooldown * 1000L;
            if (this.sendCooldownMessageIfPresent(player)) {
                event.setCancelled(true);
                return;
            }

            // Получаем количество сломанных блоков для игрока и отнимаем
            val needBreak = region.needBreak
            var blockCount = plugin.playerBlockCount.getOrDefault(player, needBreak);
            // Обновляем количество сломанных блоков
            blockCount--;
            plugin.playerBlockCount.put(player, blockCount);

            // Проверяем количество сломанных блоков
            if (blockCount > 0) {
                this.sendBreakMessage(player, blockCount);
                event.setCancelled(true);
                return;
            }

            // Оплата игроку
            plugin.economyManager.depositWithRegion(player, WGRegion);

            // Запускаем анимацию дерева и его восстановление
            val fallAnimation = FallAnimation(plugin, block);
            fallAnimation.start();
            fallAnimation.restore();

            // Сбрасываем количество сломанных блоков для игрока
            plugin.playerBlockCount.remove(player)
            if(cooldown != 0) plugin.playerCooldown.put(player.uniqueId, cooldownEndTime)
        } else if (mode.equalsIgnoreCase("WORLD") && WGRegion.isEmpty()) {
            if (!this.checkModes(player)) return;
            plugin.economyManager.depositWithoutRegion(player, block); // Оплата игроку
        }
    }

    @EventHandler
    fun onFallingBlock(event: EntityChangeBlockEvent) {
        if (event.entityType != EntityType.FALLING_BLOCK) return;
        val fallingBlock = event.entity as FallingBlock
        if (FallAnimation.fallingBlocks.isEmpty()) return;
        if (!FallAnimation.fallingBlocks.contains(fallingBlock)) return;

        val location = fallingBlock.location;
        val world = location.getWorld();
        val data = fallingBlock.blockData;

        plugin.server.scheduler.scheduleSyncDelayedTask(plugin, {
            world.getBlockAt(location).type = Material.AIR;
            FallAnimation.fallingBlocks.remove(fallingBlock);
            if (fallingBlock.isOnGround) {
                world.spawnParticle(Particle.BLOCK_CRACK, location, 50, data);
            }
        }, 3L);
    }

    private fun checkModes(player: Player): Boolean {
        val inModes = plugin.getConfig().getBoolean("woodcutter-settings.breaking-in-modes");

        if (!player.hasPermission("lightcutter.modes.bypass") && inModes) {
            if (player.gameMode != GameMode.SURVIVAL) {
                val survivalMessage = plugin.getMessageConfig().getString("warnings.not-survival");
                if (!survivalMessage.isNullOrBlank()) player.getAudience().message(survivalMessage)
                return false
            }
            if (player.allowFlight) {
                val flyingMessage = plugin.getMessageConfig().getString("warnings.is-flying");
                if (!flyingMessage.isNullOrBlank()) player.getAudience().message(flyingMessage)
                return false
            }
            if (player.isInvisible || player.hasMetadata("vanished")) {
                val invisibleMessage = plugin.getMessageConfig().getString("warnings.is-invisible");
                if (!invisibleMessage.isNullOrBlank()) player.getAudience().message(invisibleMessage)
                return false
            }
        }
        return true;
    }

    private fun sendBreakMessage(player: Player , blockCount: Int) {
        val type = plugin.getConfig().getString("region-settings.messages-type")!!
        val message = plugin.getMessageConfig().getString("region.remained")!!
            .replace("#value#", blockCount.toString());

        if (type.equalsIgnoreCase("actionbar")) {
            player.getAudience().actionbar(message)
        } else if (type.equalsIgnoreCase("chat")) {
            player.getAudience().actionbar(message)
        }
    }

    private fun sendCooldownMessageIfPresent(player: Player): Boolean {
        val type = plugin.getConfig().getString("region-settings.messages-type")!!
        val currentTime = System.currentTimeMillis();

        if (!player.hasPermission("lightcutter.cooldown.bypass") && !plugin.playerBlockCount.containsKey(player)) {
            val playerCooldown = plugin.playerCooldown.get(player.getUniqueId());
            if (playerCooldown != null && playerCooldown > currentTime) {
                val remained = (playerCooldown - currentTime) / 1000L;
                val message = plugin.getMessageConfig().getString("warnings.cooldown")!!.replace("#value#", remained.toString());

                if (type.equalsIgnoreCase("actionbar")) {
                    player.getAudience().actionbar(message)
                } else if (type.equalsIgnoreCase("chat")) {
                    player.getAudience().message(message)
                }

                return true;
            }
        }

        return false;
    }

}