package io.github.lepitar.blockPrevent.regions

import io.github.lepitar.blockPrevent.plugin.BlockPreventPlugin
import org.bukkit.Location
import org.bukkit.block.Block
import org.bukkit.entity.Entity

object Regions {
    lateinit var manager: RegionManager
        private set

    internal fun initialize(plugin: BlockPreventPlugin) {
        manager = RegionManager(plugin)
    }
}

val Block.isBreakable: Boolean
    get() = regionArea?.let { !it.isOutline(this) } ?: true

val Block.regionArea: Region?
    get() = Regions.manager.regionAt(world, x, y, z)

val Entity.areaAt: Region?
    get() = Regions.manager.regionAt(location)

val Location.regionArea: Region?
    get() = Regions.manager.regionAt(this)