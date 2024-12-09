package io.github.lepitar.blockPrevent.regions

import com.google.common.collect.ImmutableSet
import io.github.lepitar.blockPrevent.plugin.BlockPreventPlugin
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.World
import org.bukkit.block.Block
import org.bukkit.entity.BlockDisplay
import org.bukkit.entity.Interaction
import org.bukkit.entity.Player
import java.util.*
import kotlin.math.max
import kotlin.math.min

class Region(val loc: Location, val type: RegionSize, val owner: Player) {
    private val startX = loc.blockX + type.size
    private val endX = loc.blockX - type.size
    private val startZ = loc.blockZ + type.size
    private val endZ = loc.blockZ - type.size

    private val minX = min(startX, endX)
    private val maxX = max(startX, endX)
    private val minZ = min(startZ, endZ)
    private val maxZ = max(startZ, endZ)
    private val minY = loc.blockY - 1
    private val maxY = minY + 30

    private val _permissionUser = mutableSetOf(owner.uniqueId)

    val permissionUser: ImmutableSet<UUID> get() = ImmutableSet.copyOf(_permissionUser)
    val world: World = loc.world
    lateinit var regionCore: BlockDisplay
    lateinit var regionInteraction: Interaction

    fun initialize() {
        val success = Regions.manager.registerNewRegion(this)
        if (!success) {
            owner.sendMessage("여긴 겹쳐용")
            return
        }
        for (x in minX..maxX) {
            for (z in minZ..maxZ) {
                // 사각형의 테두리 부분
                if (x == minX || x == maxX || z == minZ || z == maxZ) {
                    val block = loc.world.getBlockAt(x, minY, z)
                    block.type = Material.YELLOW_CONCRETE
                    //울타리 가운데 제외
                    if (x in loc.blockX - 1..loc.blockX + 1 || z in loc.blockZ - 1..loc.blockZ + 1) continue
                    val fence = loc.world.getBlockAt(x, minY + 1, z)
                    fence.type = Material.OAK_FENCE
                } else {
                    val block = loc.world.getBlockAt(x, minY, z)
                    block.type = Material.GRASS_BLOCK
                }
            }
        }

        val center = Location(world, loc.blockX + 0.5, loc.blockY + 0.5, loc.blockZ + 0.5, 0f, 0f)
        regionCore = world.spawn(center, BlockDisplay::class.java).apply {
            setGravity(false)
            isInvulnerable = true
            block = Material.BEACON.createBlockData()
            val transformation = this.transformation
            transformation.translation.set(-0.3, 0.0, -0.3)
            transformation.scale.set(0.6)
            this.transformation = transformation
        }
        regionInteraction = world.spawn(center, Interaction::class.java).apply {
            setGravity(false)
            isInvulnerable = true
            interactionHeight = 0.6f
            interactionWidth = 0.6f
            isResponsive = true
        }
    }

    fun isOutline(block: Block): Boolean {
        val x = block.x
        val y = block.y
        val z = block.z

        // y 좌표가 minY와 같은지 확인
        if (y != minY) return false

        // x 또는 z 좌표가 테두리에 있는지 확인
        return (x == minX || x == maxX || z == minZ || z == maxZ) &&
                x in minX..maxX && z in minZ..maxZ
    }

    private fun overlaps(
        minX: Int,
        minY: Int,
        minZ: Int,
        maxX: Int,
        maxY: Int,
        maxZ: Int,
    ): Boolean {
        return this.minX <= maxX && this.maxX >= minX && this.minY <= maxY && this.maxY >= minY && this.minZ <= maxZ && this.maxZ >= minZ
    }

    fun overlaps(box: Region): Boolean {
        return box.run {
            this@Region.overlaps(minX, minY, minZ, maxX, maxY, maxZ)
        }
    }

    fun hasPermission(player: Player?): Boolean {
        return _permissionUser.contains(player?.uniqueId)
    }

    fun addPermission(player: Player) {
        _permissionUser.add(player.uniqueId)
    }

    fun removePermission(player: Player) {
        _permissionUser.remove(player.uniqueId)
    }

    fun contains(x: Int, y: Int, z: Int): Boolean {
        return x in minX..maxX && y in minY..maxY && z in minZ..maxZ
    }

    fun contains(loc: Location): Boolean {
        return contains(loc.blockX, loc.blockY, loc.blockZ)
    }

    fun rejectAction(player: Player) {
        player.sendMessage(
            BlockPreventPlugin.log_head
                .append(Component.text("${owner.name}님의", NamedTextColor.WHITE))
                .append(Component.text(" 건설 차단 ", NamedTextColor.RED))
                .append(Component.text("구역입니다.", NamedTextColor.WHITE))
        )
    }

    fun getPlayersInRegion(): List<Player> {
        val playersInRegion = mutableListOf<Player>()

        // 모든 온라인 플레이어를 순회
        for (player in Bukkit.getOnlinePlayers()) {
            val loc = player.location
            val x = loc.blockX
            val z = loc.blockZ

            // 플레이어가 영역 내에 있는지 확인
            if (x in minX..maxX && z in minZ..maxZ) {
                if (!_permissionUser.contains(player.uniqueId)) {
                    playersInRegion.add(player)
                }
            }
        }

        return playersInRegion
    }

    fun remove() {
        for (x in minX..maxX) {
            for (z in minZ..maxZ) {
                // 사각형의 테두리 삭제
                if (x == minX || x == maxX || z == minZ || z == maxZ) {
                    val block = loc.world.getBlockAt(x, minY, z)
                    block.type = Material.AIR
                }
            }
        }
        regionInteraction.remove()
        regionCore.remove()
        Regions.manager.removeRegion(this)
    }
}