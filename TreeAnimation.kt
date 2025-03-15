package ru.kainlight.lightcutter.ANIMATIONS

import org.bukkit.*
import org.bukkit.block.Block
import org.bukkit.block.BlockFace
import org.bukkit.block.data.BlockData
import org.bukkit.block.data.type.Leaves
import org.bukkit.entity.BlockDisplay
import org.bukkit.entity.Display
import org.bukkit.entity.FallingBlock
import org.bukkit.entity.Player
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.metadata.FixedMetadataValue
import org.bukkit.util.Transformation
import org.joml.Quaternionf
import org.joml.Vector3f
import ru.kainlight.lightcutter.Main
import ru.kainlight.lightcutter.UTILS.Debug
import ru.kainlight.lightlibrary.LightCommon
import java.util.logging.Level
import kotlin.math.abs

@FancyPhysics("https://github.com/max1mde/FancyPhysics")
class TreeAnimation(private val plugin: Main, private val origin: Block) {

    private val lower_1_19_4: Boolean = LightCommon.lower(Bukkit.getVersion(), "1.19.3")

    /** Returns true if the tree's properties are characteristic of a naturally generated tree. */
    private var isNatural: Boolean

    /** The material of the tree's stem. */
    private val woodMaterial: Material

    /** The material of the tree's leaves. */
    private val leaveMaterial: Material

    private val woods: MutableList<Block> = mutableListOf()
    private val leaves: MutableList<Block> = mutableListOf()

    private val scannedBlocks: MutableList<Block> = mutableListOf()
    private val oldBlocklist: MutableMap<Location, Material> = mutableMapOf()

    private var blockDistanceToLastValid = 0
    private var scanAmount = 0

    init {
        val aboveOrigin = origin.location.clone().add(0.0, 1.0, 0.0).block
        this.woodMaterial = aboveOrigin.type
        this.leaveMaterial = getLeaveType(this.woodMaterial)
        scanTree(aboveOrigin)
        woods.add(origin)
        this.isNatural = this.woods.size > 3 && this.leaves.size > 5
    }

    companion object {
        // For < 1.19.4
        val fallingBlocks: MutableList<FallingBlock> = mutableListOf()

        private val displayList = mutableListOf<Display>()

        /** Creates a new Tree object and plays a break animation */
        fun start(plugin: Main, event: BlockBreakEvent) {
            if (event.isCancelled) return

            val block = event.block
            if(!block.getRelative(BlockFace.UP).type.isWood()) return

            val tree = TreeAnimation(plugin, block)

            event.player.apply {
                if (tree.woods.isNotEmpty()) incrementStatistic(Statistic.MINE_BLOCK, tree.woodMaterial, tree.woods.size)
                if (tree.leaves.isNotEmpty()) incrementStatistic(Statistic.MINE_BLOCK, tree.leaveMaterial, tree.leaves.size)
            }

            tree.breakAndFall(event.player)

            val regenerationEnabled = plugin.config.getBoolean("region-settings.regeneration.enable", false)
            if(regenerationEnabled) {
                val animationEnabled = plugin.config.getBoolean("region-settings.regeneration.animation.enable", false)
                val delayBeforeRestore = plugin.config.getInt("region-settings.regeneration.delay")

                if(animationEnabled) tree.restoreAnimated(delayBeforeRestore)
                else tree.restore(delayBeforeRestore)
            }
        }

        //internal fun getStripedLog(blockName: String): Material = Material.valueOf("STRIPPED_" + blockName.uppercase())
    }

    /** Breaks the tree with a falling animation if the tree is natural. */
    private fun breakAndFall(player: Player) {
        if (!isNatural) return
        val allowDrop: Boolean = plugin.config.getBoolean("woodcutter-settings.allow-drop", true)
        val isAnimated: Boolean = plugin.config.getBoolean("woodcutter-settings.animation", false)

        Debug.message("Animation is [$isAnimated]", Level.INFO)

        if(!isAnimated) this.treeCapitator(allowDrop)
        else {
            if (lower_1_19_4) {
                Debug.message("Animation «spawnFallingBlocks» started", Level.INFO)
                this.woods.forEach { spawnFallingBlocks(it, allowDrop) }
                this.leaves.forEach { spawnFallingBlocks(it, allowDrop) }
            } else {
                Debug.message("Animation «spawnDisplay» started", Level.INFO)
                this.woods.forEach { spawnDisplay(it, allowDrop) }
                this.leaves.forEach { spawnDisplay(it, allowDrop) }
            }
        }
    }

    @Suppress("DEPRECATED")
    private fun spawnFallingBlocks(block: Block, allowDrop: Boolean) {
        val fallingBlock = block.world.spawnFallingBlock(block.location, block.blockData)

        fallingBlock.setHurtEntities(false)
        fallingBlock.dropItem = false
        fallingBlock.isInvulnerable = true

        if (allowDrop) block.breakNaturally()
        else block.type = Material.AIR
        fallingBlocks.add(fallingBlock)
    }

    /** Breaks the tree instantly without any animation if the tree is natural. */
    private fun treeCapitator(allowDrop: Boolean) {
        this.woods.forEach {
            if (allowDrop) it.breakNaturally()
            else it.type = Material.AIR
        }
        this.leaves.forEach {
            if (allowDrop) it.breakNaturally()
            else it.type = Material.AIR
        }
    }

    private fun spawnDisplay(block: Block, allowDrop: Boolean) {
        val location = block.location
        val blockData = block.type.createBlockData()

        // Найти нижний блок дерева
        val lowestBlock = woods.minByOrNull { it.location.y } ?: origin
        val baseY = lowestBlock.y

        location.world.spawn(location, BlockDisplay::class.java) { blockDisplay ->
            displayList.add(blockDisplay)
            blockDisplay.block = blockData
            blockDisplay.addScoreboardTag("lightcutter_tree")
            block.type = Material.AIR

            val transformationY = -1 + (baseY - block.y).toInt()
            val transformationZ = (baseY - block.y + (baseY - block.y) / 0.9).toFloat()

            /** Transform display (Falling animation) */
            plugin.server.scheduler.scheduleSyncDelayedTask(plugin, Runnable {
                val yPlus = (baseY - (block.y + 0.7)).toFloat()
                val loc = blockDisplay.location.add(0.0, yPlus + 1.5, (transformationY - 0.5f).toDouble())

                var impactLocation = loc.block
                if (impactLocation.type.isSolid) {
                    var tries = 0
                    while (impactLocation.type.isSolid && tries < 5) {
                        impactLocation = impactLocation.getRelative(BlockFace.UP)
                        tries++
                    }
                }

                val transformation = Transformation(
                    // Translation
                    Vector3f(0f, transformationY + (baseY - (block.y + 0.6)).toFloat() / 2, transformationZ),
                    // Left rotation
                    Quaternionf(-1.0f + loc.distance(impactLocation.location).toFloat() / 10, 0f, 0f, 0.1f),
                    // Scale
                    Vector3f(1f, 1f, 1f),
                    // Right rotation
                    blockDisplay.transformation.rightRotation
                )
                blockDisplay.interpolationDuration = 30
                blockDisplay.interpolationDelay = -1
                blockDisplay.transformation = transformation

                /** Break tree */
                val finalImpactLocation = impactLocation
                val dist = (loc.distance(impactLocation.location) * 2).toInt()

                plugin.server.scheduler.scheduleSyncDelayedTask(plugin, Runnable {
                    blockDisplay.location.world.spawnParticle(Particle.BLOCK_CRACK, finalImpactLocation.location, 50, blockData)
                    removeTree(blockDisplay, transformationY.toFloat(), blockData, allowDrop)
                }, 12L - minOf(11, dist).toLong())

            }, 2L)
        }
    }

    /**
     * Removes the tree after the falling animation is completed.
     *
     * @param blockDisplay     The block display to remove.
     * @param transformationY  The Y transformation value.
     * @param blockData        The block data of the block display to get the material.
     */
    private fun removeTree(blockDisplay: BlockDisplay, transformationY: Float, blockData: BlockData, allowDrop: Boolean) {
        plugin.server.scheduler.scheduleSyncDelayedTask(plugin, Runnable {
            val b = blockDisplay.location.add(0.0, (transformationY + 2).toDouble(), transformationY.toDouble()).block
            if (b.type == Material.AIR) {
                b.type = blockData.material

                if (allowDrop) b.breakNaturally()
                else b.type = Material.AIR
            }

            displayList.remove(blockDisplay)
            blockDisplay.remove()
        }, 4L)
    }

    private fun restore(delayBeforeRestore: Int?) {
        if (delayBeforeRestore == null || delayBeforeRestore <= 0) return

        val particlesEnabled = plugin.config.getBoolean("region-settings.regeneration.particle.enable")
        val particle = Particle.valueOf(plugin.config.getStringList("region-settings.regeneration.particle.types").random())
        val particleCount = plugin.config.getInt("region-settings.regeneration.particle.count")

        val soundEnabled = plugin.config.getBoolean("region-settings.regeneration.sound.enable")
        val sound = Sound.valueOf(plugin.config.getStringList("region-settings.regeneration.sound.types").random())
        val soundVolume: Float = plugin.config.getDouble("region-settings.regeneration.sound.volume").toFloat()
        val soundPitch: Float = plugin.config.getDouble("region-settings.regeneration.sound.pitch").toFloat()

        plugin.server.scheduler.runTaskLater(plugin, Runnable {
            for (location in oldBlocklist.keys) {
                val block = location.block
                if (block.type != Material.AIR) continue
                block.type = oldBlocklist.get(location)!!

                val world = block.world
                if(particlesEnabled) world.spawnParticle(particle, location, particleCount)
                if(soundEnabled) world.playSound(location, sound, soundVolume, soundPitch)
            }
            origin.type = woodMaterial
        }, 20L * delayBeforeRestore)
    }

    private fun restoreAnimated(delayBeforeRestore: Int?) {
        if (delayBeforeRestore == null || delayBeforeRestore <= 0) return

        val particlesEnabled = plugin.config.getBoolean("region-settings.regeneration.particle.enable")
        val particle = Particle.valueOf(plugin.config.getStringList("region-settings.regeneration.particle.types").random())
        val particleCount = plugin.config.getInt("region-settings.regeneration.particle.count")

        val soundEnabled = plugin.config.getBoolean("region-settings.regeneration.sound.enable")
        val sound = Sound.valueOf(plugin.config.getStringList("region-settings.regeneration.sound.types").random())
        val soundVolume: Float = plugin.config.getDouble("region-settings.regeneration.sound.volume").toFloat() * 2
        val soundPitch: Float = plugin.config.getDouble("region-settings.regeneration.sound.pitch").toFloat()

        // Разделение блоков дерева и листвы
        val inverse = plugin.config.getBoolean("region-settings.regeneration.animation.inverse")
        var woodBlocks: List<Location> = ArrayList()
        var leafBlocks: List<Location> = ArrayList()
        if(inverse) {
            woodBlocks = oldBlocklist.filterValues { it == woodMaterial  }.keys.sortedBy { it.y }
            leafBlocks = oldBlocklist.filterValues { it == leaveMaterial }.keys.sortedBy { it.y }
        } else {
            woodBlocks = oldBlocklist.filterValues { it == woodMaterial }.keys.sortedByDescending { it.y }
            leafBlocks = oldBlocklist.filterValues { it == leaveMaterial }.keys.sortedByDescending { it.y }
        }

        println("Restoring: Old blocklist Size: ${oldBlocklist.keys.size}")

        //val totalBlocks = woodBlocks.size + leafBlocks.size
        var delayPerBlock = plugin.config.getLong("region-settings.regeneration.animation.delayPerBlock", 1L)
        if(delayPerBlock <= 0) delayPerBlock = 1L
        var currentDelay = 0L

        // Ожидание перед запуском восстановления
        plugin.server.scheduler.runTaskLaterAsynchronously(plugin, Runnable {
            // Восстанавливаем блоки листвы
            for (location in leafBlocks) {
                plugin.server.scheduler.runTaskLater(plugin, Runnable {
                    restoreBlockWithEffects(location, particle, particleCount, sound, soundVolume, soundPitch, particlesEnabled, soundEnabled)
                }, currentDelay)
                currentDelay += delayPerBlock
            }

            // Восстанавливаем блоки дерева
            for (location in woodBlocks) {
                plugin.server.scheduler.runTaskLater(plugin, Runnable {
                    restoreBlockWithEffects(location, particle, particleCount, sound, soundVolume, soundPitch, particlesEnabled, soundEnabled)
                }, currentDelay)
                currentDelay += delayPerBlock
            }

            // Завершающий блок (если есть "origin")
            plugin.server.scheduler.runTaskLater(plugin, Runnable {
                origin.type = woodMaterial
            }, currentDelay)
        }, 20L * delayBeforeRestore)
    }

    /** Recursively scans the tree structure, populating the stem and leaves lists. */
    private fun scanTree(scannedBlock: Block) {
        scannedBlocks.add(scannedBlock)
        scanAmount ++
        if (abs(scannedBlock.x - origin.x) > 10 || abs(scannedBlock.z - origin.z) > 10) return

        val scanMaxStemSize = 200
        val scanMaxLeavesSize = 260

        if (scannedBlock.type == woodMaterial) {
            if (woods.size < scanMaxStemSize) {
                if (this.woods.contains(scannedBlock)) return

                this.woods.add(scannedBlock)
                this.oldBlocklist.put(scannedBlock.location, scannedBlock.type)
            } else {
                isNatural = false
                return
            }
        } else if (scannedBlock.type == leaveMaterial) {
            if (leaves.size < scanMaxLeavesSize) {
                val leaveBlockData = scannedBlock.blockData
                if (leaveBlockData is Leaves) {
                    leaveBlockData.isPersistent = true
                    scannedBlock.blockData = leaveBlockData
                }

                if (this.leaves.contains(scannedBlock)) return

                this.leaves.add(scannedBlock)
                this.oldBlocklist.put(scannedBlock.location, scannedBlock.type)

                println("Leaves size: ${leaves.size}")
            } else {
                isNatural = false
                return
            }
        }

        val advancedStemScan = false
        val maxInvalidScans = 2700
        val maxInvalidBlockDistance = 2

        if (listOf(Material.COCOA_BEANS, Material.VINE, Material.SNOW).contains(scannedBlock.type) && advancedStemScan) {
            scannedBlock.breakNaturally()
        }

        listOf(BlockFace.DOWN, BlockFace.UP, BlockFace.SOUTH, BlockFace.NORTH, BlockFace.WEST, BlockFace.EAST).forEach { blockFace ->
            val currentBlock = scannedBlock.getRelative(blockFace)

            var scan = (currentBlock.type == this.woodMaterial || currentBlock.type == this.leaveMaterial)
            ///if (blockFace == BlockFace.DOWN && currentBlock.y <= origin.y + 12) scan = false

            if (blockFace == BlockFace.DOWN) {
                // Проверяем, не вышли ли мы за пределы дерева, ограничение по глубине
                val isWithinBounds = currentBlock.y >= origin.y - 12
                val isValidMaterial = currentBlock.type == woodMaterial

                if (!isWithinBounds || !isValidMaterial) scan = false /*return@forEach*/
            }

            if (scan) {
                scanTree(currentBlock)
                blockDistanceToLastValid = 0
                return@forEach
            }

            if (scanAmount < maxInvalidScans && woods.size > 4 && advancedStemScan && blockDistanceToLastValid < maxInvalidBlockDistance) {
                blockDistanceToLastValid ++
                if (! scannedBlocks.contains(currentBlock)) scanTree(currentBlock)
            }
        }
    }

    /**
     * Восстанавливает блок с эффектами.
     */
    private fun restoreBlockWithEffects(location: Location, particle: Particle, particleCount: Int, sound: Sound, soundVolume: Float, soundPitch: Float, particlesEnabled: Boolean, soundEnabled: Boolean) {
        val block = location.block
        if (block.type != Material.AIR) return
        val material = oldBlocklist.get(location)!!
        block.type = material

        val world = block.world
        if (particlesEnabled) world.spawnParticle(particle, location.add(0.5, 0.5, 0.5), particleCount)
        if (soundEnabled) world.playSound(location, sound, soundVolume, soundPitch)
    }

    private fun getLeaveType(material: Material): Material {
        return when (material) {
            Material.OAK_LOG, Material.MUD_BRICK_WALL, Material.STRIPPED_OAK_LOG -> Material.OAK_LEAVES
            Material.DARK_OAK_LOG, Material.STRIPPED_DARK_OAK_LOG -> Material.DARK_OAK_LEAVES
            Material.JUNGLE_LOG, Material.STRIPPED_JUNGLE_LOG -> Material.JUNGLE_LEAVES
            Material.ACACIA_LOG, Material.STRIPPED_ACACIA_LOG -> Material.ACACIA_LEAVES
            Material.BIRCH_LOG, Material.STRIPPED_BIRCH_LOG -> Material.BIRCH_LEAVES
            Material.SPRUCE_LOG, Material.STRIPPED_SPRUCE_LOG -> Material.SPRUCE_LEAVES
            Material.CHERRY_LOG, Material.STRIPPED_CHERRY_LOG -> Material.CHERRY_LEAVES
            Material.MANGROVE_LOG, Material.STRIPPED_MANGROVE_LOG -> Material.MANGROVE_LEAVES
            Material.WARPED_STEM, Material.NETHER_WART_BLOCK -> Material.WARPED_WART_BLOCK
            Material.CRIMSON_STEM -> Material.NETHER_WART_BLOCK
            else -> Material.AIR
        }
    }

    /**
     * Determines the material of the leaves based on the wood material of the tree.
     *
     * @param material  The wood material of the tree.
     * @return          The material of the leaves.
     */
    /*private fun getLeaveType(material: Material): String {
        return when (material.name) {
            "OAK_LOG", "MUD_BRICK_WALL", "STRIPPED_OAK_LOG" -> "OAK_LEAVES"
            "DARK_OAK_LOG", "STRIPPED_DARK_OAK_LOG" -> "DARK_OAK_LEAVES"
            "JUNGLE_LOG", "STRIPPED_JUNGLE_LOG" -> "JUNGLE_LEAVES"
            "ACACIA_LOG", "STRIPPED_ACACIA_LOG" -> "ACACIA_LEAVES"
            "BIRCH_LOG", "STRIPPED_BIRCH_LOG" -> "BIRCH_LEAVES"
            "SPRUCE_LOG", "STRIPPED_SPRUCE_LOG" -> "SPRUCE_LEAVES"
            "CHERRY_LOG", "STRIPPED_CHERRY_LOG" -> "CHERRY_LEAVES"
            "MANGROVE_LOG", "STRIPPED_MANGROVE_LOG" -> "MANGROVE_LEAVES"
            "WARPED_STEM", "NETHER_WART_BLOCK" -> "WARPED_WART_BLOCK"
            "CRIMSON_STEM" -> "NETHER_WART_BLOCK"
            else -> "AIR"
        }
    }*/
}

fun Material.isWood(): Boolean = this.name.endsWith("LOG") || this.name.endsWith("STEM")

annotation class FancyPhysics(val url: String)