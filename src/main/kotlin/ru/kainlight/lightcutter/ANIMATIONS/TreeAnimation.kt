package ru.kainlight.lightcutter.ANIMATIONS

import org.bukkit.*
import org.bukkit.block.Block
import org.bukkit.block.BlockFace
import org.bukkit.block.data.BlockData
import org.bukkit.block.data.type.Leaves
import org.bukkit.entity.BlockDisplay
import org.bukkit.entity.Display
import org.bukkit.entity.FallingBlock
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.util.Transformation
import org.joml.Quaternionf
import org.joml.Vector3f
import ru.kainlight.lightcutter.Main
import ru.kainlight.lightcutter.UTILS.Debug
import ru.kainlight.lightlibrary.LightCommon
import java.util.logging.Level
import kotlin.math.abs

// Thanks max1mde — https://github.com/max1mde/FancyPhysics

class TreeAnimation(private val plugin: Main, private val origin: Block) {

    private val lower_1_19_4: Boolean = LightCommon.lower(plugin.server.version, "1.19.3")

    /** Returns true if the tree's properties are characteristic of a naturally generated tree. */
    private var isNatural: Boolean

    /** The material of the tree's stem. */
    private val woodMaterial: Material

    /** The material of the tree's leaves. */
    private val leaveMaterial: Material

    private val woods: MutableList<Block> = mutableListOf()
    private val leaves: MutableList<Block> = mutableListOf()

    private val scannedBlocks: MutableList<Block> = mutableListOf()
    private val oldBlocklist: MutableMap<Location, BlockData> = mutableMapOf()

    init {
        val aboveOrigin: Block = origin.location.clone().add(0.0, 1.0, 0.0).block
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
            if (! block.getRelative(BlockFace.UP).type.isWood()) return

            val tree = TreeAnimation(plugin, block)

            event.player.apply {
                if (tree.woods.isNotEmpty()) incrementStatistic(Statistic.MINE_BLOCK,
                                                                tree.woodMaterial,
                                                                tree.woods.size)
                if (tree.leaves.isNotEmpty()) incrementStatistic(Statistic.MINE_BLOCK,
                                                                 tree.leaveMaterial,
                                                                 tree.leaves.size)
            }

            tree.breakAndFall()

            val regenerationEnabled = plugin.config.getBoolean("region-settings.regeneration.enable", true)
            val delayBeforeRestore: Int? = plugin.config.getInt("region-settings.regeneration.delay")
            if (regenerationEnabled && delayBeforeRestore != null && delayBeforeRestore > 0) {
                val animationEnabled = plugin.config.getBoolean("region-settings.regeneration.animation.enable", false)

                if (animationEnabled) tree.restoreAnimated(delayBeforeRestore) else tree.restore(delayBeforeRestore)
            }
        }

        //internal fun getStripedLog(blockName: String): Material = Material.valueOf("STRIPPED_" + blockName.uppercase())
    }

    /** Breaks the tree with a falling animation if the tree is natural. */
    private fun breakAndFall() {
        if (! isNatural) return
        val allowDrop: Boolean = plugin.config.getBoolean("woodcutter-settings.allow-drop", true)
        val isAnimated: Boolean = plugin.config.getBoolean("woodcutter-settings.animation", false)

        Debug.log("Animation is [$isAnimated]")

        if (! isAnimated) this.treeCapitator(allowDrop)
        else {
            if (lower_1_19_4) {
                Debug.log("Animation «spawnFallingBlocks» started")
                this.woods.forEach { spawnFallingBlocks(it, allowDrop) }
                this.leaves.forEach { spawnFallingBlocks(it, allowDrop) }
            } else {
                Debug.log("Animation «spawnDisplay» started")
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
        val blockDisplay: Any = location.world.spawn(location, BlockDisplay::class.java)

        if(blockDisplay is BlockDisplay) {
            displayList.add(blockDisplay)
            val blockData = block.type.createBlockData()

            blockDisplay.block = blockData
            blockDisplay.addScoreboardTag("lightcutter_tree")
            block.type = Material.AIR

            // Ищется нижний блок дерева
            val lowestBlock = woods.minByOrNull { it.location.y } ?: origin
            val baseY = lowestBlock.y

            val transformationY = - 1 + (baseY - block.y).toInt()
            val transformationZ = (baseY - block.y + (baseY - block.y) / 0.9).toFloat()

            /** Transform display (Falling animation) */
            plugin.scheduleSyncDelayedTask(Runnable {
                val yPlus = (baseY - (block.y + 0.7)).toFloat()
                val loc = blockDisplay.location.add(0.0, yPlus + 1.5, (transformationY - 0.5f).toDouble())

                var impactLocation = loc.block
                if (impactLocation.type.isSolid) {
                    var tries = 0
                    while (impactLocation.type.isSolid && tries < 5) {
                        impactLocation = impactLocation.getRelative(BlockFace.UP)
                        tries ++
                    }
                }

                val translation = Vector3f(0f, transformationY + (baseY - (block.y + 0.6)).toFloat() / 2, transformationZ)
                val leftRotation = Quaternionf(- 1.0f + loc.distance(impactLocation.location).toFloat() / 10, 0f, 0f, 0.1f)
                val scale = Vector3f(1f, 1f, 1f)
                val rightRotation = blockDisplay.transformation.rightRotation

                val transformation = Transformation(translation, leftRotation, scale, rightRotation)
                blockDisplay.interpolationDuration = 30
                blockDisplay.interpolationDelay = - 1
                blockDisplay.transformation = transformation

                /** Break tree */
                val finalImpactLocation = impactLocation
                val dist = (loc.distance(impactLocation.location) * 2).toInt()

                plugin.scheduleSyncDelayedTask(Runnable {
                    blockDisplay.location.world.spawnParticle(Particle.BLOCK_CRACK,
                                                              finalImpactLocation.location,
                                                              50,
                                                              blockData)
                    removeTree(blockDisplay, transformationY.toFloat(), blockData, allowDrop)
                }, 12L - minOf(11, dist).toLong())

            }, 2L)
        }

        /*location.world.spawn(location, BlockDisplay::class.java) { blockDisplay -> }*/
    }

    /**
     * Removes the tree after the falling animation is completed.
     *
     * @param blockDisplay     The block display to remove.
     * @param transformationY  The Y transformation value.
     * @param blockData        The block data of the block display to get the material.
     */
    private fun removeTree(blockDisplay: BlockDisplay, transformationY: Float, blockData: BlockData, allowDrop: Boolean) {
        plugin.scheduleSyncDelayedTask(Runnable {
            val block = blockDisplay.location.add(0.0, (transformationY + 2).toDouble(), transformationY.toDouble()).block
            if (block.type == Material.AIR) {
                block.type = blockData.material

                if (allowDrop) block.breakNaturally()
                else block.type = Material.AIR
            }

            displayList.remove(blockDisplay)
            blockDisplay.remove()
        }, 4L)
    }

    private fun restore(delayBeforeRestore: Int) {
        val particlesEnabled: Boolean = plugin.config.getBoolean("region-settings.regeneration.particle.enable", false)
        val particleArgs: List<String> = plugin.config.getStringList("region-settings.regeneration.particle.types").random().split(":")

        val particleName: String = particleArgs.getOrNull(0) ?: "DOLPHIN"
        val particleCount: Int = particleArgs.getOrNull(1)?.toIntOrNull() ?: plugin.config.getInt("region-settings.regeneration.particle.default-count", 1)

        val soundEnabled = plugin.config.getBoolean("region-settings.regeneration.sound.enable", false)
        val soundArgs = plugin.config.getStringList("region-settings.regeneration.sound.types").random().split(":")

        val soundName: String = soundArgs.getOrNull(0) ?: "BLOCK_WOOD_PLACE"
        val soundVolume: Float = soundArgs.getOrNull(1)?.toFloatOrNull() ?: plugin.config.getDouble("region-settings.regeneration.sound.default-volume", 1.0).toFloat()
        val soundPitch: Float = soundArgs.getOrNull(2)?.toFloatOrNull() ?: plugin.config.getDouble("region-settings.regeneration.sound.default-pitch", 0.8).toFloat()

        plugin.runTaskLater(Runnable {
            for (location in oldBlocklist.keys) {
                handleRestoreBlocks(location)
                if (particlesEnabled) location.particle(particleName, particleCount)
            }

            val effectLocation = origin.location
            if (soundEnabled) effectLocation.sound(soundName, soundVolume, soundPitch)

            origin.type = woodMaterial
        }, 20L * delayBeforeRestore)
    }

    private fun restoreAnimated(delayBeforeRestore: Int) {
        val particlesEnabled: Boolean = plugin.config.getBoolean("region-settings.regeneration.particle.enable", false)
        val particleArgs: List<String> = plugin.config.getStringList("region-settings.regeneration.particle.types").random().split(":")

        val particleName: String = particleArgs.getOrNull(0) ?: "DOLPHIN"
        val particleCount: Int = particleArgs.getOrNull(1)?.toIntOrNull() ?: plugin.config.getInt("region-settings.regeneration.particle.default-count", 1)

        val soundEnabled = plugin.config.getBoolean("region-settings.regeneration.sound.enable", false)
        val soundArgs = plugin.config.getStringList("region-settings.regeneration.sound.types").random().split(":")

        val soundName: String = soundArgs.getOrNull(0) ?: "BLOCK_WOOD_PLACE"
        val soundVolume: Float = soundArgs.getOrNull(1)?.toFloatOrNull() ?: plugin.config.getDouble("region-settings.regeneration.sound.default-volume", 1.0).toFloat()
        val soundPitch: Float = soundArgs.getOrNull(2)?.toFloatOrNull() ?: plugin.config.getDouble("region-settings.regeneration.sound.default-pitch", 0.7).toFloat()

        var delayPerBlock: Long = plugin.config.getLong("region-settings.regeneration.animation.delayPerBlock", 1L)
        if (delayPerBlock <= 0) delayPerBlock = 1L
        var currentDelay = 0L

        // Ожидание перед запуском восстановления
        plugin.runTaskLaterAsync(Runnable {
            // Разделение блоков дерева и листвы
            val leafBlocks =
                oldBlocklist.filterValues { it.material == leaveMaterial }.keys.sortedByDescending { loc -> loc.y }
            val woodBlocks =
                oldBlocklist.filterValues { it.material == woodMaterial }.keys.sortedByDescending { loc -> loc.y }

            // Восстанавливаем блоки листвы
            for (location in leafBlocks) {
                plugin.runTaskLater(Runnable {
                    handleRestoreBlocks(location)

                    if (particlesEnabled) location.particle(particleName, particleCount)
                    if (soundEnabled) location.sound(soundName, soundVolume, soundPitch)
                }, currentDelay)
                currentDelay += delayPerBlock
            }

            // Восстанавливаем блоки дерева
            for (location in woodBlocks) {
                plugin.runTaskLater(Runnable {
                    handleRestoreBlocks(location)

                    if (particlesEnabled) location.particle(particleName, particleCount)
                    if (soundEnabled) location.sound(soundName, soundVolume, soundPitch)
                }, currentDelay)
                currentDelay += delayPerBlock
            }

            // Завершающий блок (если есть `origin`)
            plugin.runTaskLater(Runnable { origin.type = woodMaterial }, currentDelay)
        }, 20L * delayBeforeRestore)
    }

    /** Recursively scans the tree structure, populating the stem and leaves lists. */
    private fun scanTree(scannedBlock: Block) {
        var scanAmount = 0

        scannedBlocks.add(scannedBlock)
        scanAmount ++
        if (abs(scannedBlock.x - origin.x) > 10 || abs(scannedBlock.z - origin.z) > 10) return

        val scanMaxStemSize = 200
        val scanMaxLeavesSize = 260

        if (scannedBlock.type == woodMaterial) {
            if (woods.size < scanMaxStemSize) {
                if (this.woods.contains(scannedBlock)) return

                this.woods.add(scannedBlock)
                this.oldBlocklist.put(scannedBlock.location, scannedBlock.blockData.clone())
            } else {
                isNatural = false
                return
            }
        } else if (scannedBlock.type == leaveMaterial) {
            if (leaves.size < scanMaxLeavesSize) {
                if (this.leaves.contains(scannedBlock)) return

                (scannedBlock.blockData as? Leaves)?.apply {
                    isPersistent = true
                    scannedBlock.blockData = this
                }

                this.leaves.add(scannedBlock)
                this.oldBlocklist.put(scannedBlock.location, scannedBlock.blockData.clone())
            } else {
                isNatural = false
                return
            }
        }

        var blockDistanceToLastValid = 0
        val advancedStemScan = false
        val maxInvalidScans = 2700
        val maxInvalidBlockDistance = 2

        if (listOf(Material.COCOA_BEANS,
                   Material.VINE,
                   Material.SNOW).contains(scannedBlock.type) && advancedStemScan) {
            scannedBlock.breakNaturally()
        }

        listOf(BlockFace.DOWN,
               BlockFace.UP,
               BlockFace.SOUTH,
               BlockFace.NORTH,
               BlockFace.WEST,
               BlockFace.EAST).forEach { blockFace ->
            val currentBlock = scannedBlock.getRelative(blockFace)

            var scan = (currentBlock.type == this.woodMaterial || currentBlock.type == this.leaveMaterial)
            ///if (blockFace == BlockFace.DOWN && currentBlock.y <= origin.y + 12) scan = false

            if (blockFace == BlockFace.DOWN) {
                // Проверяем, не вышли ли мы за пределы дерева, ограничение по глубине
                val isWithinBounds = currentBlock.y >= origin.y - 12
                val isValidMaterial = currentBlock.type == woodMaterial

                if (! isWithinBounds || ! isValidMaterial) scan = false /*return@forEach*/
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
    private fun handleRestoreBlocks(location: Location) {
        val block = location.block
        val blockType = block.type
        if (blockType != Material.AIR) return
        block.blockData = oldBlocklist.get(location)!!

        Debug.log("Restored ${blockType.name} at $location location")
    }

    private fun Location.particle(name: String?, count: Int?) {
        if(name == null || count == null) return

        try {
            this.world.spawnParticle(Particle.valueOf(name), this.add(0.5, 0.5, 0.5), count)
            Debug.log("Spawned $name particle with count $count")
        } catch (e: IllegalArgumentException) {
            Debug.log("An attempt to spawn an unsupported particle $name at $this: ${e.message}", Level.SEVERE)
        }
    }

    private fun Location.sound(soundName: String?, volume: Float?, pitch: Float?) {
        if(soundName == null || volume == null || pitch == null) return

        this.world.playSound(this, Sound.valueOf(soundName), volume, pitch)
        Debug.log("Played $soundName sound with volume $volume and pitch $pitch")
    }

    private fun getLeaveType(material: Material): Material {
        return when (material) {
            Material.OAK_LOG, Material.STRIPPED_OAK_LOG -> Material.OAK_LEAVES
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
}

fun Material.isWood(): Boolean = this.name.endsWith("LOG") || this.name.endsWith("STEM")