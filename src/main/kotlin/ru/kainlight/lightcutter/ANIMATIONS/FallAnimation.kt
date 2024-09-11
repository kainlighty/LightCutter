package ru.kainlight.lightcutter.ANIMATIONS

import org.bukkit.*
import org.bukkit.block.Block
import org.bukkit.block.BlockFace
import org.bukkit.block.data.BlockData
import org.bukkit.entity.BlockDisplay
import org.bukkit.entity.FallingBlock
import org.bukkit.util.Transformation
import org.joml.Quaternionf
import org.joml.Vector3f
import ru.kainlight.lightcutter.Main
import ru.kainlight.lightcutter.UTILS.Debug
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.CopyOnWriteArraySet
import java.util.logging.Level
import kotlin.math.abs


class FallAnimation(private val plugin: Main,
    private val origin: Block,
    private var wood: Material = origin.type,
    private var leave: Material = getLeaveType(wood),
    private var isNatural: Boolean? = null,
    val brokenWoods: MutableSet<Block> = mutableSetOf(),
    val brokenLeaves: MutableSet<Block> = mutableSetOf(),
    private val blocksToRestore: MutableMap<Location, Material> = ConcurrentHashMap<Location, Material>(),
    private val scannedBlocks: MutableSet<Location> = CopyOnWriteArraySet()
) {

    init {
        scanFrom(origin)
        isNatural = (brokenWoods.size > 3 && brokenLeaves.size > 10)
    }

    fun start() {
        if (!isNatural!!) return;
        val allowDrop: Boolean = plugin.config.getBoolean("woodcutter-settings.allow-drop", true)
        val isAnimated: Boolean = plugin.config.getBoolean("woodcutter-settings.animation", true)

        Debug.message("Animation started [$isAnimated]", Level.INFO)

        if(!isAnimated) {
            this.treeCapitator(allowDrop)
        } else {
            if (lower_1_19_4) {
                this.brokenWoods.forEach { fall(it, allowDrop) }
                this.brokenLeaves.forEach { fall(it, allowDrop) }
            } else {
                this.brokenWoods.forEach { spawnDisplay1_20(it, allowDrop) }
                this.brokenLeaves.forEach { spawnDisplay1_20(it, allowDrop) }
            }
        }
    }

    private fun treeCapitator(allowDrop: Boolean) {
        this.brokenWoods.forEach {
            if(!allowDrop) it.drops.clear()
            it.breakNaturally()
        }
        this.brokenLeaves.forEach {
            if(!allowDrop) it.drops.clear()
            it.breakNaturally()
        }
    }

    private fun fall(block: Block, allowDrop: Boolean) {
        val fallingBlock = block.world.spawnFallingBlock(block.location, block.blockData)

        fallingBlock.setHurtEntities(false)
        fallingBlock.dropItem = false
        fallingBlock.isInvulnerable = true

        if (allowDrop) block.breakNaturally()
        else block.type = Material.AIR
        fallingBlocks.add(fallingBlock)
    }

    private fun spawnDisplay1_20(block: Block, allowDrop: Boolean) {
        val location = block.location;
        val blockData = block.type.createBlockData();

        // Создаем экземпляр BlockDisplay
        val blockDisplayInstance: Any = location.world.spawn(location, BlockDisplay::class.java);

        if (blockDisplayInstance is BlockDisplay) {
            blockDisplayInstance.block = blockData;
            if (block != origin) block.type = Material.AIR;

            val transformationY: Int = - 1 + (origin.y - (block.y));
            val transformationZ: Float = (origin.y - block.y) + (origin.y - block.y) / 0.9F;

            plugin.server.scheduler.scheduleSyncDelayedTask(plugin, {
                val transformation = Transformation(
                    Vector3f(0F, transformationY + (origin.y - (block.y + 0.6F)) / 2, transformationZ),
                    Quaternionf(- 1.0, 0.0, 0.0, 0.1),   //left rotation
                    Vector3f(1F, 1F, 1F),    //scale
                    blockDisplayInstance.transformation.rightRotation  //right rotation
                )

                blockDisplayInstance.interpolationDuration = 40;
                blockDisplayInstance.interpolationDelay = - 1;
                blockDisplayInstance.transformation = transformation;

                plugin.server.scheduler.scheduleSyncDelayedTask(plugin, {
                    val loc = blockDisplayInstance.location.add(0.5, (origin.y - (block.y + 0.7)) + 1, transformationY.toDouble())
                    blockDisplayInstance.location.world.spawnParticle(Particle.BLOCK_CRACK, loc, 50, blockData)

                    this.removeTree(blockDisplayInstance, transformationY.toFloat(), blockData, allowDrop)
                }, 18L)

                if (allowDrop) block.breakNaturally();
                else block.type = Material.AIR;

            }, 2L);
        } else {
            // Если не удалось создать экземпляр BlockDisplay
            plugin.logger.warning("Unable to create BlockDisplay instance")
        }
    }

    private fun removeTree(
        blockDisplay: BlockDisplay,
        transformationY: Float,
        blockData: BlockData,
        allowDrop: Boolean
    ) {
        plugin.server.scheduler.scheduleSyncDelayedTask(plugin, {
            if (allowDrop) {
                val b =
                    blockDisplay.location.add(0.0, (transformationY + 2).toDouble(), transformationY.toDouble()).block;
                if (b.type == Material.AIR) {
                    b.type = blockData.material;
                    b.breakNaturally();
                }
            }/* else {
                blockDisplay.getLocation().getWorld().dropItem(blockDisplay.getLocation().add(0, transformationY + 2, transformationY), new ItemStack(blockData.getMaterial()));
            }*/
            blockDisplay.remove();
        }, 4L);
    }

    fun restore() {
        val seconds = plugin.config.getInt("region-settings.regeneration.seconds");
        if (seconds > 0) {
            Bukkit.getScheduler().runTaskLater(plugin, Runnable {
                for (location in blocksToRestore.keys) {
                    val block = location.block;
                    if (block.type != Material.AIR) continue;
                    val world = block.world;
                    block.type = blocksToRestore.get(location)!!;

                    val particle = Particle.valueOf(plugin.getConfig().getString("region-settings.regeneration.particle.type")!!);
                    val particleCount = plugin.getConfig().getInt("region-settings.regeneration.particle.count");
                    world.spawnParticle(particle, location, particleCount);

                    val sound = Sound.valueOf(plugin.getConfig().getString("region-settings.regeneration.sound.type") !!);
                    val soundVolume: Float = plugin.getConfig().getDouble("region-settings.regeneration.sound.volume").toFloat()
                    world.playSound(location, sound, soundVolume, soundVolume);
                }
            }, 20L * seconds);
        }
    }

    private fun scanFrom(block: Block) {
        if (!scannedBlocks.add(block.location)) return
        if (abs(block.x - this.origin.x) > 3 || abs(block.z - this.origin.z) > 3) return;

        handleBlock(block, this.wood, brokenWoods, 90);
        handleBlock(block, this.leave, brokenLeaves, 100);

        for (face in BlockFace.entries) {
            val relativeBlock = block.getRelative(face);
            val relativeType = relativeBlock.type;
            if (relativeType == this.wood || relativeType == this.leave) {
                scanFrom(relativeBlock);
            }
        }
    }

    private fun handleBlock(block: Block, type: Material, blocks: MutableSet<Block> , limit: Int) {
        if (block.type == type) {
            if (blocks.size < limit) {
                blocks.add(block);
                blocksToRestore.put(block.location, block.type);
            }
        }
    }




    companion object {
        @JvmStatic val fallingBlocks: MutableList<FallingBlock> = CopyOnWriteArrayList()

        @JvmStatic
        fun isWood(wood: Material): Boolean {
            return wood.name.endsWith("_LOG")
        }

        @JvmStatic
        private fun getLeaveType(wood: Material): Material {
            return when (wood) {
                Material.OAK_LOG -> Material.OAK_LEAVES
                Material.DARK_OAK_LOG -> Material.DARK_OAK_LEAVES
                Material.JUNGLE_LOG -> Material.JUNGLE_LEAVES
                Material.ACACIA_LOG -> Material.ACACIA_LEAVES
                Material.BIRCH_LOG -> Material.BIRCH_LEAVES
                Material.SPRUCE_LOG -> Material.SPRUCE_LEAVES
                Material.CHERRY_LOG -> Material.CHERRY_LEAVES
                Material.MANGROVE_LOG -> Material.MANGROVE_LEAVES
                else -> Material.AIR
            }
        }
        @JvmStatic
        fun isLeave(leave: Material ): Boolean {
            return when (leave) {
                Material.OAK_LEAVES -> true
                Material.DARK_OAK_LEAVES -> true
                Material.JUNGLE_LEAVES -> true
                Material.ACACIA_LEAVES -> true
                Material.BIRCH_LEAVES -> true
                Material.SPRUCE_LEAVES -> true
                Material.CHERRY_LEAVES -> true
                Material.MANGROVE_LEAVES -> true
                else -> false
            }
        }

        @JvmStatic
        private val lower_1_19_4: Boolean = lower("1.19.3")

        @JvmStatic private fun lower(targetVersion: String): Boolean {
            val serverVersion = Bukkit.getVersion();

            // Извлечение версии Minecraft из строки
            val versionParts = serverVersion.split("MC: ")[1].split("\\)");
            val serverMinecraftVersion = versionParts[0];

            // Сравнение версий
            return serverMinecraftVersion.compareTo(targetVersion) < 0;
        }
    }


}