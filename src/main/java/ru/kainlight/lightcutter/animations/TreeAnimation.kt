package ru.kainlight.lightcutter.animations

import org.bukkit.*
import org.bukkit.block.Block
import org.bukkit.block.BlockFace
import org.bukkit.block.data.BlockData
import org.bukkit.block.data.type.Leaves
import org.bukkit.enchantments.Enchantment
import org.bukkit.entity.BlockDisplay
import org.bukkit.entity.Display
import org.bukkit.entity.FallingBlock
import org.bukkit.entity.Player
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.Damageable
import org.bukkit.util.Transformation
import org.jetbrains.annotations.ApiStatus.ScheduledForRemoval
import org.joml.Quaternionf
import org.joml.Vector3f
import ru.kainlight.lightcutter.Main
import ru.kainlight.lightlibrary.LightCommon
import ru.kainlight.lightlibrary.UTILS.DebugBukkit
import ru.kainlight.lightlibrary.multiMessage
import kotlin.math.abs
import kotlin.random.Random

// Thanks max1mde — https://github.com/max1mde/FancyPhysics

internal class TreeAnimation(private val plugin: Main, private val origin: Block) {

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

    private val advancedStemScanMaterials = listOf(Material.COCOA_BEANS, Material.VINE, Material.SNOW)
    private val blockFaceList =
        listOf(BlockFace.DOWN, BlockFace.UP, BlockFace.SOUTH, BlockFace.NORTH, BlockFace.WEST, BlockFace.EAST)

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
            val player = event.player

            val isFullItemDamage = plugin.config.getBoolean("woodcutter-settings.full-item-damage", true)
            tree.damageItem(player.inventory.itemInMainHand, isFullItemDamage)

            val isIncrementStatistics = plugin.config.getBoolean("woodcutter-settings.increment-statistics", true)
            if (isIncrementStatistics) tree.incrementStatistics(player)

            tree.breakAndFall()

            val regenerationEnabled = plugin.config.getBoolean("region-settings.regeneration.enable", true)
            val delayBeforeRestore: Int? = plugin.config.getInt("region-settings.regeneration.delay", 5)
            if (regenerationEnabled && delayBeforeRestore != null && delayBeforeRestore > 0) {
                tree.startAnimation(delayBeforeRestore)
            }
        }

        //internal fun getStripedLog(blockName: String): Material = Material.valueOf("STRIPPED_" + blockName.uppercase())
    }

    /** Breaks the tree with a falling animation if the tree is natural. */
    private fun breakAndFall() {
        if (! isNatural) return
        val isAnimated: Boolean = plugin.config.getBoolean("woodcutter-settings.animation", false)
        DebugBukkit.info("Animation is [$isAnimated]")

        if (! isAnimated) this.treeCapitator() else {
            if (lower_1_19_4) {
                DebugBukkit.info("Animation «spawnFallingBlocks» started")
                this.woods.forEach { spawnFallingBlocks(it) }
                this.leaves.forEach { spawnFallingBlocks(it) }
            } else {
                DebugBukkit.info("Animation «spawnDisplay» started")
                this.woods.forEach { spawnDisplay(it) }
                this.leaves.forEach { spawnDisplay(it) }
            }
        }
    }

    @Deprecated("Removed after switching to JDK21")
    @ScheduledForRemoval
    private fun spawnFallingBlocks(block: Block) {
        val fallingBlock = block.world.spawnFallingBlock(block.location, block.blockData)

        fallingBlock.setHurtEntities(false)
        fallingBlock.dropItem = false
        fallingBlock.isInvulnerable = true

        drop(block)
        fallingBlocks.add(fallingBlock)
    }

    /** Breaks the tree instantly without any animation if the tree is natural. */
    private fun treeCapitator() {
        this.woods.forEach { drop(it) }
        this.leaves.forEach { drop(it) }
    }

    private fun spawnDisplay(block: Block) {
        val location = block.location
        val blockDisplay: Any = location.world.spawn(location, BlockDisplay::class.java)

        if (blockDisplay is BlockDisplay) {
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
            plugin.lightScheduler.scheduleSyncDelayedTask(2L) {
                val newY = (baseY - (block.y + 0.7)).toFloat()
                val blockDisplayLocation =
                    blockDisplay.location.add(0.0, newY + 1.5, (transformationY - 0.5f).toDouble())

                var impactLocation = blockDisplayLocation.block
                if (impactLocation.type.isSolid) {
                    var tries = 0
                    while (impactLocation.type.isSolid && tries < 5) {
                        impactLocation = impactLocation.getRelative(BlockFace.UP)
                        tries ++
                    }
                }

                val translation =
                    Vector3f(0f, transformationY + (baseY - (block.y + 0.6)).toFloat() / 2, transformationZ)
                val leftRotation =
                    Quaternionf(- 1.0f + blockDisplayLocation.distance(impactLocation.location).toFloat() / 10,
                                0f,
                                0f,
                                0.1f)
                val scale = Vector3f(1f, 1f, 1f)
                val rightRotation = blockDisplay.transformation.rightRotation

                val transformation = Transformation(translation, leftRotation, scale, rightRotation)
                blockDisplay.interpolationDuration = 30
                blockDisplay.interpolationDelay = - 1
                blockDisplay.transformation = transformation

                /** Break tree */
                val finalImpactLocation = impactLocation.location
                val dist = (blockDisplayLocation.distance(impactLocation.location) * 2).toInt()

                val delayForTask = 12L - minOf(11, dist).toLong()
                plugin.lightScheduler.scheduleSyncDelayedTask(delayForTask) {
                    blockDisplay.location.world.spawnParticle(
                        Particle.BLOCK_CRACK,
                        finalImpactLocation,
                        50,
                        blockData)
                    removeTree(blockDisplay, transformationY.toFloat(), blockData)
                }
            }
        }
    }

    /**
     * Removes the tree after the falling animation is completed.
     *
     * @param blockDisplay     The block display to remove.
     * @param transformationY  The Y transformation value.
     * @param blockData        The block data of the block display to get the material.
     */
    private fun removeTree(blockDisplay: BlockDisplay, transformationY: Float, blockData: BlockData) {
        plugin.lightScheduler.scheduleSyncDelayedTask(4L) {
            val block =
                blockDisplay.location.add(0.0, (transformationY + 2).toDouble(), transformationY.toDouble()).block
            if (block.type == Material.AIR) {
                block.type = blockData.material
                drop(block)
            }

            displayList.remove(blockDisplay)
            blockDisplay.remove()
        }
    }

    private fun restore(
        particlesEnabled: Boolean,
        particleName: String,
        particleCount: Int,
        soundEnabled: Boolean,
        soundName: String,
        soundVolume: Float,
        soundPitch: Float,
        delayBeforeRestore: Int
    ) {
        plugin.lightScheduler.runTaskLater(20L * delayBeforeRestore) {
            for (location in oldBlocklist.keys) {
                location.handleRestoreBlocks()
                if (particlesEnabled) location.particle(particleName, particleCount)
            }

            if (soundEnabled) origin.location.sound(soundName, soundVolume, soundPitch)

            origin.type = woodMaterial
        }
    }

    private fun restoreAnimated(
        particlesEnabled: Boolean,
        particleName: String,
        particleCount: Int,
        soundEnabled: Boolean,
        soundName: String,
        soundVolume: Float,
        soundPitch: Float,
        delayBeforeRestore: Int
    ) {
        var delayPerBlock: Long = plugin.config.getLong("region-settings.regeneration.animation.delayPerBlock", 1L)
        if (delayPerBlock <= 0) delayPerBlock = 1L
        var currentDelay = 0L

        val sch = plugin.lightScheduler

        // Ожидание перед запуском восстановления
        sch.runTaskLaterAsynchronously(20L * delayBeforeRestore) {
            // Разделение блоков дерева и листвы
            val leafBlocks =
                oldBlocklist.filterValues { it.material == leaveMaterial }.keys.sortedByDescending { loc -> loc.y }
            val woodBlocks =
                oldBlocklist.filterValues { it.material == woodMaterial }.keys.sortedByDescending { loc -> loc.y }

            // Восстанавливаем блоки листвы
            for (location in leafBlocks) {
                sch.runTaskLater(currentDelay) {
                    location.handleRestoreBlocks()

                    if (particlesEnabled) location.particle(particleName, particleCount)
                    if (soundEnabled) location.sound(soundName, soundVolume, soundPitch)
                }
                currentDelay += delayPerBlock
            }

            // Восстанавливаем блоки дерева
            for (location in woodBlocks) {
                sch.runTaskLater(currentDelay) {
                    location.handleRestoreBlocks()

                    if (particlesEnabled) location.particle(particleName, particleCount)
                    if (soundEnabled) location.sound(soundName, soundVolume, soundPitch)
                }
                currentDelay += delayPerBlock
            }

            // Завершающий блок (если есть `origin`)
            sch.runTaskLater(currentDelay) { origin.type = woodMaterial }
        }
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

        if (advancedStemScanMaterials.contains(scannedBlock.type) && advancedStemScan) {
            scannedBlock.breakNaturally()
        }

        blockFaceList.forEach { blockFace ->
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

    private fun drop(block: Block) {
        val drops = plugin.config.getConfigurationSection("woodcutter-settings.drops") !!
        if (block.type.isWood()) {
            if (drops.getBoolean("logs")) block.breakNaturally()
            else block.type = Material.AIR
        }
        if (block.type.isLeave()) {
            if (drops.getBoolean("leaves")) block.breakNaturally()
            else block.type = Material.AIR
        }
    }

    private fun isDroppableLogs(): Boolean {
        val drops = plugin.config.getConfigurationSection("woodcutter-settings.drops") !!
        return drops.getBoolean("logs")
    }

    private fun isDroppableLeaves(): Boolean {
        val drops = plugin.config.getConfigurationSection("woodcutter-settings.drops") !!
        return drops.getBoolean("leaves")
    }

    private fun damageItem(mainHand: ItemStack, full: Boolean = false) {
        val itemMeta = mainHand.itemMeta
        if (itemMeta !is Damageable) return

        val baseDamage = if (full) {
            val logDamage = if (isDroppableLogs()) this.getLogsSize() else 1
            val leafDamage = if (isDroppableLeaves()) this.getLeavesSize() else 0
            logDamage + leafDamage
        } else 1
        val unbreakingLevel = mainHand.getEnchantmentLevel(Enchantment.DURABILITY)
        var finalDamage = 0
        for (i in 0 until baseDamage) {
            if (unbreakingLevel > 0) {
                if (Random.nextInt(unbreakingLevel + 1) == 0) {
                    finalDamage ++
                }
            } else {
                finalDamage ++
            }
        }
        itemMeta.damage += finalDamage
        mainHand.itemMeta = itemMeta
    }

    private fun incrementStatistics(player: Player) {
        if (this.woods.isNotEmpty() && this.isDroppableLogs()) {
            val woodSize = this.getLogsSize()
            if (woodSize > 1) {
                player.incrementStatistic(Statistic.MINE_BLOCK, this.woodMaterial, woodSize)
            }
        }

        if (this.leaves.isNotEmpty() && this.isDroppableLeaves()) {
            val leavesSize = this.getLeavesSize()
            if (leavesSize > 1) {
                player.incrementStatistic(Statistic.MINE_BLOCK, this.leaveMaterial, leavesSize)
            }
        }
    }

    private fun startAnimation(delayBeforeRestore: Int) {
        val animationEnabled = plugin.config.getBoolean("region-settings.regeneration.animation.enable", false)

        val particlesEnabled: Boolean = plugin.config.getBoolean("region-settings.regeneration.particle.enable", false)
        val particleArgs: List<String> =
            plugin.config.getStringList("region-settings.regeneration.particle.types").random().split(":")

        val particleName: String = particleArgs.getOrNull(0) ?: "DOLPHIN"
        val particleCount: Int = particleArgs.getOrNull(1)?.toIntOrNull()
            ?: plugin.config.getInt("region-settings.regeneration.particle.default-count", 1)

        val soundEnabled = plugin.config.getBoolean("region-settings.regeneration.sound.enable", false)
        val soundArgs = plugin.config.getStringList("region-settings.regeneration.sound.types").random().split(":")

        val soundName = soundArgs.getOrNull(0) ?: "BLOCK_WOOD_PLACE"
        val soundVolume: Float = soundArgs.getOrNull(1)?.toFloatOrNull()
            ?: plugin.config.getDouble("region-settings.regeneration.sound.default-volume", 1.0).toFloat()
        val soundPitch: Float = soundArgs.getOrNull(2)?.toFloatOrNull()
            ?: plugin.config.getDouble("region-settings.regeneration.sound.default-pitch", 0.8).toFloat()

        if (animationEnabled) {
            restoreAnimated(particlesEnabled,
                            particleName,
                            particleCount,
                            soundEnabled,
                            soundName,
                            soundVolume,
                            soundPitch,
                            delayBeforeRestore)
        } else {
            restore(particlesEnabled,
                    particleName,
                    particleCount,
                    soundEnabled,
                    soundName,
                    soundVolume,
                    soundPitch,
                    delayBeforeRestore)
        }
    }

    /**
     * Восстанавливает блок с эффектами.
     */
    private fun Location.handleRestoreBlocks() {
        val block = this.block
        val blockType = block.type
        if (blockType != Material.AIR) return
        block.blockData = oldBlocklist.get(this) !!

        DebugBukkit.info("Restored ${blockType.name} at $this location")
    }

    private fun Location.particle(name: String?, count: Int?) {
        if (name == null || count == null) return

        try {
            val particle = Particle.valueOf(name)
            val changedLocation = this.add(0.5, 0.5, 0.5)

            this.world.spawnParticle(particle, changedLocation, count)
            DebugBukkit.info("Spawned $name particle with count $count")
        } catch (e: IllegalArgumentException) {
            DebugBukkit.error("An attempt to spawn an unsupported particle $name at $this", e)
        }
    }

    private fun Location.sound(soundName: String?, volume: Float?, pitch: Float?) {
        if (soundName == null || volume == null || pitch == null) return
        val name = soundName.uppercase()

        this.world.playSound(this, Sound.valueOf(name), volume, pitch)

        DebugBukkit.info("Played ${name.uppercase()} sound with volume $volume and pitch $pitch")
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

    private fun getLogsSize() = this.woods.size - 1
    private fun getLeavesSize() = this.leaves.size
}

fun Material.isWood(): Boolean = this.name.endsWith("LOG") || this.name.endsWith("STEM")
fun Material.isLeave(): Boolean = this.name.endsWith("LEAVES")