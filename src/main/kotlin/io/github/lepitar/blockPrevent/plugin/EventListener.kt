package io.github.lepitar.blockPrevent.plugin

import com.github.stefvanschie.inventoryframework.gui.GuiItem
import com.github.stefvanschie.inventoryframework.gui.type.ChestGui
import com.github.stefvanschie.inventoryframework.pane.OutlinePane
import com.github.stefvanschie.inventoryframework.pane.Pane
import com.github.stefvanschie.inventoryframework.pane.PatternPane
import com.github.stefvanschie.inventoryframework.pane.StaticPane
import com.github.stefvanschie.inventoryframework.pane.util.Pattern
import io.github.lepitar.blockPrevent.regions.areaAt
import io.github.lepitar.blockPrevent.regions.isBreakable
import io.github.lepitar.blockPrevent.regions.regionArea
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextColor
import net.kyori.adventure.text.format.TextDecoration
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.block.Container
import org.bukkit.block.Dispenser
import org.bukkit.entity.EntityType
import org.bukkit.entity.Interaction
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.block.*
import org.bukkit.event.entity.EntityExplodeEvent
import org.bukkit.event.inventory.InventoryMoveItemEvent
import org.bukkit.event.player.PlayerBucketEmptyEvent
import org.bukkit.event.player.PlayerBucketFillEvent
import org.bukkit.event.player.PlayerInteractEntityEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.event.vehicle.VehicleDestroyEvent
import org.bukkit.event.world.StructureGrowEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.SkullMeta
import org.bukkit.material.Directional

class EventListener : Listener {
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    fun onBlockPlace(event: BlockPlaceEvent) {
        val player = event.player
        val region = event.block.regionArea

        if (region == null || region.hasPermission(player)) {
            return
        }

        region.rejectAction(player)
        event.isCancelled = true
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    fun onBlockBreak(event: BlockBreakEvent) {
        val player = event.player
        val region = event.block.regionArea

        if (region == null || (region.hasPermission(player) && event.block.isBreakable)) {
            return
        }

        if (!region.hasPermission(player)) {
            region.rejectAction(player)
        }

        event.isCancelled = true
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    fun onEntityExplode(event: EntityExplodeEvent) {
        if (event.entity.areaAt == null) {
            event.blockList().removeIf { block -> block.regionArea != null }
            return
        }

        event.isCancelled = true
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    fun onPlayerBucketFill(event: PlayerBucketFillEvent) {
        val player = event.player
        val region = event.blockClicked.regionArea

        if (region == null || region.hasPermission(player) || player.isOp) {
            return
        }

        event.isCancelled = true
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    fun onPlayerBucketEmpty(event: PlayerBucketEmptyEvent) {
        val player = event.player
        val region = event.blockClicked.getRelative(event.blockFace).regionArea

        if (region == null || region.hasPermission(player) || player.isOp) {
            return
        }

        event.isCancelled = true
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    fun onBlockExplode(event: BlockExplodeEvent) {
        event.blockList().removeIf { it.regionArea != null }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    fun onBlockCanBuild(event: BlockCanBuildEvent) {
        val player = event.player
        val region = event.block.regionArea

        if (player == null || region == null || region.hasPermission(player)) {
            return
        }

        region.rejectAction(player)
        event.isBuildable = false
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    fun onPlayerInteract(event: PlayerInteractEvent) {
        val player = event.player
        event.clickedBlock?.let { clickedBlock ->
            val region = clickedBlock.regionArea

            if (region == null || region.hasPermission(player)) {
                return
            }

            region.rejectAction(player)
            event.isCancelled = true
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    fun onBlockDispense(event: BlockDispenseEvent) {
        val block = event.block
        val state = block.state

        if (state !is Dispenser) {
            return
        }

        val data = block.blockData

        if (data !is Directional) {
            return
        }

        val from = block.regionArea
        val to = block.getRelative(data.facing).regionArea

        if (from == null && to == null || from == to) {
            return
        }

        event.isCancelled = true
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    fun onBlockIgnite(event: BlockIgniteEvent) {
        val area = event.block.regionArea
        val player = event.player

        if (player == null || area == null || !area.hasPermission(player) && !player.isOp) {
            return
        }

        event.isCancelled = true
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    fun onInventoryMoveItem(event: InventoryMoveItemEvent) {
        val sourceHolder = event.source.holder
        val destinationHolder = event.destination.holder

        if (sourceHolder !is Container || destinationHolder !is Container) {
            return
        }

        val sourceArea = sourceHolder.block.regionArea
        val destinationArea = destinationHolder.block.regionArea

        if (sourceArea == null && destinationArea == null || sourceArea === destinationArea) {
            return
        }

        event.isCancelled = true
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    fun onBlockPistonRetract(event: BlockPistonRetractEvent) {
        val blocks = event.blocks

        if (blocks.isEmpty()) {
            return
        }

        val direction = event.direction
        val piston = event.block
        val pistonArea = piston.regionArea

        for (block in event.blocks) {
            val blockArea = block.regionArea
            val toBlockArea = block.getRelative(direction).regionArea

            if (blockArea !== pistonArea && blockArea != null ||
                toBlockArea !== pistonArea && toBlockArea != null
            ) {
                event.isCancelled = true
                return
            }
        }
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    fun onBlockPistonExtend(event: BlockPistonExtendEvent) {
        val direction = event.direction
        val piston = event.block
        val head = piston.getRelative(direction)
        val pistonArea = piston.regionArea
        val headArea = head.regionArea

        if (pistonArea !== headArea && pistonArea != null) {
            event.isCancelled = true
            return
        }

        for (block in event.blocks) {
            val blockArea = block.regionArea
            val toBlockArea = block.getRelative(direction).regionArea

            if (blockArea !== pistonArea && blockArea != null ||
                toBlockArea !== pistonArea && toBlockArea != null
            ) {
                event.isCancelled = true
                return
            }
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    fun onPlayerInteractEntity(event: PlayerInteractEntityEvent) {
        val player = event.player
        val region = event.rightClicked.areaAt

        if (region == null || region.hasPermission(player) || player.isOp) {
            return
        }

        event.isCancelled = true
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    fun onVehicleDestroy(event: VehicleDestroyEvent) {
        val vehicle = event.vehicle
        val area = vehicle.areaAt
        val attacker = event.attacker

        if (area == null) {
            return
        }

        if (attacker is Player && !attacker.isOp) {
            area.rejectAction(attacker)
        }

        event.isCancelled = true
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    fun onBlockFromTo(event: BlockFromToEvent) {
        val from = event.block.regionArea
        val to = event.toBlock.regionArea

        if (from == to || from == null) {
            return
        }

        event.isCancelled = true
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    fun onStructureGrow(event: StructureGrowEvent) {
        val area = event.location.regionArea

        if (area != null) {
            return
        }

        val blocks = event.blocks
        blocks.removeIf { it.location.regionArea != null }
    }

    @EventHandler(ignoreCancelled = true)
    fun onPlayerInteractAtEntity(e: PlayerInteractEntityEvent) {
        val player = e.player
        val entity = e.rightClicked
        val region = entity.areaAt ?: return

        if (entity.type != EntityType.INTERACTION || entity !is Interaction || region.owner.uniqueId != player.uniqueId) {
            return
        }

        // 플레이어가 Interaction 엔티티와 상호작용할 때
        val gui = ChestGui(4, "${region.owner.name}의 소유")
        gui.setOnGlobalClick { event -> event.isCancelled = true }
        val pattern = Pattern(
            "111121111",
            "000000000",
            "000000000",
            "111131114",
        )
        val pane = PatternPane(0, 0, 9, 4, pattern)
        pane.priority = Pane.Priority.LOWEST
        pane.bindItem('1', GuiItem(ItemStack(Material.GRAY_STAINED_GLASS_PANE)))
        pane.bindItem('2', GuiItem(ItemStack(Material.PLAYER_HEAD).apply {
            itemMeta = (itemMeta as SkullMeta).apply {
                displayName(Component.text("${region.owner.name}의 소유").decoration(TextDecoration.ITALIC, false))
                playerProfile = region.owner.playerProfile
            }
        }))
        pane.bindItem('3', GuiItem(ItemStack(Material.GREEN_STAINED_GLASS_PANE).apply {
            itemMeta = itemMeta.apply {
                displayName(
                    Component.text("권한 부여")
                        .color(TextColor.fromHexString("#66FF66"))
                        .decorate(TextDecoration.BOLD)
                        .decoration(TextDecoration.ITALIC, false)
                )
            }
        }) {
            val manageUser = ChestGui(3, "권한 부여").apply {
                setOnGlobalClick { it.isCancelled = true }
            }
            val userPane = OutlinePane(0, 1, 9, 2)

            fun refreshUserPane() {
                userPane.clear() // Clear old items before refreshing
                region.getPlayersInRegion().forEach { regionPlayer ->
                    userPane.addItem(GuiItem(ItemStack(Material.PLAYER_HEAD).apply {
                        itemMeta = (itemMeta as SkullMeta).apply {
                            playerProfile = regionPlayer.playerProfile
                            displayName(
                                Component.text("${regionPlayer.name}님")
                                    .decoration(TextDecoration.ITALIC, false)
                            )
                        }
                    }) { _ ->
                        region.addPermission(regionPlayer)
                        refreshUserPane() // Recursively refresh the pane after adding permission
                        manageUser.update() // Update the whole GUI
                    })
                }
            }

            val closePane = StaticPane(0, 0, 9, 1)
            closePane.addItem(GuiItem(ItemStack(Material.BARRIER).apply {
                //닫기
                itemMeta = itemMeta.apply {
                    displayName(Component.text("닫기"))
                }
            }) {
                gui.show(player)
            }, 8, 0)

            refreshUserPane()
            manageUser.addPane(closePane)
            manageUser.addPane(userPane)
            manageUser.show(player)
        })
        pane.bindItem('4', GuiItem(ItemStack(Material.BARRIER).apply {
            itemMeta = itemMeta.apply {
                displayName(
                    Component.text("회수하기")
                        .color(NamedTextColor.RED)
                        .decorate(TextDecoration.BOLD)
                        .decoration(TextDecoration.ITALIC, false)
                )
            }
        }) { event ->
            val clickedPlayer = event.whoClicked
            if (region.owner.uniqueId != clickedPlayer.uniqueId) return@GuiItem
            val withdraw = ChestGui(1, "회수하시겠습니까?").apply {
                setOnGlobalClick { it.isCancelled = true }
            }
            val agreePane = StaticPane(0, 0, 4, 1, Pane.Priority.LOWEST)
            agreePane.fillWith(ItemStack(Material.GREEN_STAINED_GLASS_PANE).apply {
                itemMeta = itemMeta.apply {
                    displayName(
                        Component.text('예', NamedTextColor.GREEN)
                            .decoration(TextDecoration.ITALIC, false)
                    )
                }
            }) { agree ->
                val agreedPlayer = agree.whoClicked
                region.remove()
                agreedPlayer.closeInventory()
            }
            val disagree = StaticPane(5, 0, 4, 1, Pane.Priority.LOWEST)
            disagree.fillWith(ItemStack(Material.RED_STAINED_GLASS_PANE).apply {
                itemMeta = itemMeta.apply {
                    displayName(
                        Component.text("아니요", NamedTextColor.RED)
                            .decoration(TextDecoration.ITALIC, false)
                    )
                }
            }) { disagreeButton ->
                val disagreedPlayer = disagreeButton.whoClicked
                disagreedPlayer.closeInventory()
            }
            withdraw.addPane(agreePane)
            withdraw.addPane(disagree)
            withdraw.show(player)
        })
        val managerPane = OutlinePane(0, 1, 9, 2)
        managerPane.priority = Pane.Priority.HIGHEST
        fun refreshPermissionUser() {
            managerPane.clear()

            region.permissionUser.forEach {
                if (region.owner.uniqueId == it) return@forEach
                val manager = Bukkit.getPlayer(it)!!
                managerPane.addItem(GuiItem(ItemStack(Material.PLAYER_HEAD).apply {
                    itemMeta = (itemMeta as SkullMeta).apply {
                        playerProfile = manager.playerProfile
                        displayName(Component.text(manager.name).decoration(TextDecoration.ITALIC, false))
                        lore(
                            mutableListOf(
                                Component.text(""),
                                Component.text("쉬프트 좌클릭시 제거됩니다.", NamedTextColor.GRAY)
                                    .decoration(TextDecoration.ITALIC, false)
                            )
                        )
                    }
                }) { e ->
                    if (e.click.isShiftClick && e.click.isLeftClick) {
                        region.removePermission(manager)
                        refreshPermissionUser()
                        gui.update()
                    }
                })
            }
        }
        refreshPermissionUser()
        gui.addPane(managerPane)
        gui.addPane(pane)
        gui.show(player)
    }
}