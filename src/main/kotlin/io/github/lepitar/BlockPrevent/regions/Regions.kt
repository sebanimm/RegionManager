package io.github.lepitar.BlockPrevent.regions

import io.github.lepitar.BlockPrevent.plugin.BlockPreventPlugin
import org.bukkit.Location
import org.bukkit.block.Block
import org.bukkit.entity.Entity
import org.bukkit.entity.Player

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