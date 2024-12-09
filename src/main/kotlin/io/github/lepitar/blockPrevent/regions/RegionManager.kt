package io.github.lepitar.blockPrevent.regions

import com.google.common.collect.ImmutableList
import io.github.lepitar.blockPrevent.plugin.BlockPreventPlugin
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.World
import org.bukkit.configuration.ConfigurationSection
import java.util.*

class RegionManager(plugin: BlockPreventPlugin) {
    private val regions: List<Region>
        get() = ImmutableList.copyOf(_regions)

    private val _regions = mutableListOf<Region>()

    fun registerNewRegion(region: Region): Boolean {
        val overlap = checkOverlaps(region)

        if (overlap) {
            return false
        }

        _regions.add(region)

        return true
    }

    fun removeRegion(region: Region) {
        _regions.remove(region)
    }

    fun regionAt(bukkitWorld: World, x: Int, y: Int, z: Int): Region? {
        return _regions.find { it.world == bukkitWorld && it.contains(x, y, z) }
    }

    fun regionAt(loc: Location): Region? {
        return _regions.find { it.world == loc.world && it.contains(loc) }
    }

    private fun checkOverlaps(region: Region): Boolean {
        var isOverlap = false
        _regions.forEach {
            val _isOverlap = it.overlaps(region)
            if (_isOverlap) {
                isOverlap = true
                return@forEach
            }
        }
        return isOverlap
    }

    fun unload() {
        _regions.forEach { region ->
            region.regionCore.remove()
            region.regionInteraction.remove()
        }
    }

    fun saveAllRegions(config: ConfigurationSection) {
        regions.forEachIndexed { index, region ->
            val regionConfig = config.createSection("region_$index")
            saveRegion(regionConfig, region)
        }
    }

    fun loadAllRegions(config: ConfigurationSection) {
        _regions.clear()
        config.getKeys(false).forEach { key ->
            config.getConfigurationSection(key)?.let { sectionConfig ->
                loadRegion(sectionConfig)?.let { region ->
                    _regions.add(region)
                }
            }
        }
    }

    private fun saveRegion(config: ConfigurationSection, region: Region) {
        config.set("location.world", region.world.name)
        config.set("location.x", region.loc.x)
        config.set("location.y", region.loc.y)
        config.set("location.z", region.loc.z)

        config.set("type", region.type.name)
        config.set("owner", region.owner.uniqueId.toString())

        val permissionList = region.permissionUser.map { it.toString() }
        config.set("permissionUsers", permissionList)
    }

    private fun loadRegion(config: ConfigurationSection): Region? {
        val worldName = config.getString("location.world") ?: return null
        val world = Bukkit.getWorld(worldName) ?: return null

        val x = config.getDouble("location.x")
        val y = config.getDouble("location.y")
        val z = config.getDouble("location.z")
        val location = Location(world, x, y, z)

        val typeString = config.getString("type") ?: return null
        val type = RegionSize.valueOf(typeString)

        val ownerUUID = UUID.fromString(config.getString("owner")) ?: return null
        val owner = Bukkit.getPlayer(ownerUUID) ?: return null

        val region = Region(location, type, owner)

        val permissionUsers = config.getStringList("permissionUsers")
        permissionUsers.mapNotNull { UUID.fromString(it) }.forEach { region.permissionUser += it }

        return region
    }
}