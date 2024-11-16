package io.github.lepitar.BlockPrevent.plugin

import com.github.stefvanschie.inventoryframework.gui.GuiItem
import com.github.stefvanschie.inventoryframework.gui.type.ChestGui
import com.github.stefvanschie.inventoryframework.pane.OutlinePane
import com.github.stefvanschie.inventoryframework.pane.Pane
import com.github.stefvanschie.inventoryframework.pane.PatternPane
import com.github.stefvanschie.inventoryframework.pane.StaticPane
import com.github.stefvanschie.inventoryframework.pane.util.Pattern
import io.github.lepitar.BlockPrevent.regions.areaAt
import io.github.lepitar.BlockPrevent.regions.isBreakable
import io.github.lepitar.BlockPrevent.regions.regionArea
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextColor
import net.kyori.adventure.text.format.TextDecoration
import net.kyori.adventure.util.RGBLike
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.block.Container
import org.bukkit.block.Dispenser
import org.bukkit.entity.Entity
import org.bukkit.entity.EntityType
import org.bukkit.entity.Interaction
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.block.*
import org.bukkit.event.entity.EntityExplodeEvent
import org.bukkit.event.inventory.InventoryClickEvent
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
    fun onBlockPlace(e: BlockPlaceEvent) {
        val player = e.player
        val region = e.block.regionArea
        if (region == null || region.hasPermission(player)) return

        region.rejectAction(player)
        e.isCancelled = true
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    fun onBlockBreak(event: BlockBreakEvent) {
        val player = event.player
        val region = event.block.regionArea
        if (region == null || region.hasPermission(player) && event.block.isBreakable) return

        if (!region.hasPermission(player)) {
            region.rejectAction(player)
        }
        event.isCancelled = true
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    fun onEntityExplode(event: EntityExplodeEvent) {
        if (event.entity.areaAt != null) {
            event.isCancelled = true
        } else {
            event.blockList().removeIf { block ->
                block.regionArea != null
            }
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    fun onPlayerBucketFill(event: PlayerBucketFillEvent) {
        val player = event.player
        val region = event.blockClicked.regionArea
        if (region == null || region.hasPermission(player) || player.isOp) return

        event.isCancelled = true
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    fun onPlayerBucketEmpty(event: PlayerBucketEmptyEvent) {
        val player = event.player
        val region = event.blockClicked.getRelative(event.blockFace).regionArea
        if (region == null || region.hasPermission(player) || player.isOp) return

        event.isCancelled = true
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    fun onBlockExplode(event: BlockExplodeEvent) {
        event.blockList().removeIf { it.regionArea != null }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    fun onBlockCanBuild(e: BlockCanBuildEvent) {
        val player = e.player
        val region = e.block.regionArea
        if (player == null || region == null || region.hasPermission(player)) return

        region.rejectAction(player)
        e.isBuildable = false
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    fun onPlayerInteract(e: PlayerInteractEvent) {
        val player = e.player
        e.clickedBlock?.let { clickedBlock ->
            val region = clickedBlock.regionArea
            if (region== null || region.hasPermission(player)) return

            region.rejectAction(player)
            e.isCancelled = true

            return
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    fun onBlockDispense(event: BlockDispenseEvent) {
        val block = event.block
        val state = block.state

        if (state is Dispenser) {
            val data = block.blockData
            if (data is Directional) {
                val from = block.regionArea
                val to = block.getRelative(data.facing).regionArea

                if (from !== to &&
                    (from != null || to != null)
                ) {
                    event.isCancelled = true
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    fun onBlockIgnite(event: BlockIgniteEvent) {
        val area = event.block.regionArea
        val player = event.player
        //먼가안되는듯?
        if (player == null) {
            if (area != null) {
                event.isCancelled = true
            }
        } else {
            if (area == null || area.hasPermission(player) || player.isOp) return

            event.isCancelled = true
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    fun onInventoryMoveItem(event: InventoryMoveItemEvent) {
        val sourceHolder = event.source.holder
        val destinationHolder = event.destination.holder

        if (sourceHolder is Container && destinationHolder is Container) {
            val sourceArea = sourceHolder.block.regionArea
            val destinationArea = destinationHolder.block.regionArea

            if (sourceArea !== destinationArea &&
                    (sourceArea != null || destinationArea != null)
            ) {
                event.isCancelled = true
            }
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    fun onBlockPistonRetract(event: BlockPistonRetractEvent) {
        val blocks = event.blocks

        if (blocks.isEmpty())
            return

        val direction = event.direction
        val piston = event.block
        val pistonArea = piston.regionArea
        val pistonProtection = pistonArea != null


        if (pistonProtection) {
            for (block in event.blocks) {
                if (block.regionArea !== pistonArea || block.getRelative(direction).regionArea !== pistonArea) {
                    event.isCancelled = true
                    break
                }
            }
        } else {
            for (block in event.blocks) {
                val blockArea = block.regionArea

                if (blockArea !== pistonArea && blockArea != null) {
                    event.isCancelled = true
                    break
                }

                val toBlockArea = block.getRelative(direction).regionArea

                if (toBlockArea !== pistonArea && toBlockArea != null) {
                    event.isCancelled = true
                    break
                }
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
        val pistonProtection = pistonArea != null

        if (pistonArea !== headArea
            && (pistonProtection || headArea != null)
        ) {
            event.isCancelled = true
            return
        }

        if (pistonProtection) {
            for (block in event.blocks) {
                if (block.regionArea !== pistonArea || block.getRelative(direction).regionArea !== pistonArea) {
                    event.isCancelled = true
                    break
                }
            }
        } else {
            for (block in event.blocks) {
                val blockArea = block.regionArea

                if (blockArea !== pistonArea && blockArea != null) {
                    event.isCancelled = true
                    break
                }

                val toBlockArea = block.getRelative(direction).regionArea

                if (toBlockArea !== pistonArea && toBlockArea != null) {
                    event.isCancelled = true
                    break
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    fun onPlayerInteractEntity(event: PlayerInteractEntityEvent) {
        val player = event.player

        val region = event.rightClicked.areaAt
        if (region == null || region.hasPermission(player) || player.isOp) return

        event.isCancelled = true
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    fun onVehicleDestroy(event: VehicleDestroyEvent) {
        val vehicle = event.vehicle
        val area = vehicle.areaAt
        val attacker = event.attacker

        if (attacker is Player) {
            if (!attacker.isOp && area != null) {
                area.rejectAction(attacker)
                event.isCancelled = true
            }
        } else {
            if (area != null) {
                event.isCancelled = true
            }
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    fun onBlockFromTo(event: BlockFromToEvent) {
        val from = event.block.regionArea
        val to = event.toBlock.regionArea

        if (from !== to) {
            if (from != null
                || to != null
            ) {
                event.isCancelled = true
            }
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    fun onStructureGrow(event: StructureGrowEvent) {
        val area = event.location.regionArea

        if (area == null) {
            val blocks = event.blocks

            blocks.removeIf { it.location.regionArea != null }
        }
    }

    @EventHandler(ignoreCancelled = true)
    fun onPlayerInteractAtEntity(e: PlayerInteractEntityEvent) {
        val player = e.player
        val entity = e.rightClicked
        val region = entity.areaAt ?: return

        // 플레이어가 Interaction 엔티티와 상호작용할 때
        if (entity.type == EntityType.INTERACTION && entity is Interaction && region.owner.uniqueId == player.uniqueId) {
            val gui = ChestGui(4, "${region.owner.name}의 소유")
            gui.setOnGlobalClick { e -> e.isCancelled = true }
            val pattern = Pattern(
                "111121111",
                "000000000",
                "000000000",
                "111131114"
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
                    displayName(Component.text("권한 부여")
                        .color(TextColor.fromHexString("#66FF66"))
                        .decorate(TextDecoration.BOLD)
                        .decoration(TextDecoration.ITALIC, false))
                }
            }) { e ->
                val manageUser = ChestGui(3, "권한 부여").apply {
                    setOnGlobalClick { it.isCancelled = true }
                }
                val user_pane = OutlinePane(0,1,9,2)

                fun refreshUserPane() {
                    user_pane.clear() // Clear old items before refreshing

                    region.getPlayersInRegion().forEach { regionPlayer ->
                        user_pane.addItem(GuiItem(ItemStack(Material.PLAYER_HEAD).apply {
                            itemMeta = (itemMeta as SkullMeta).apply {
                                playerProfile = regionPlayer.playerProfile
                                displayName(Component.text("${regionPlayer.name}님")
                                    .decoration(TextDecoration.ITALIC, false))
                            }
                        }) { e ->
                            region.addPermission(regionPlayer)
                            refreshUserPane() // Recursively refresh the pane after adding permission
                            manageUser.update() // Update the whole GUI
                        })
                    }
                }
                val closePane = StaticPane(0,0,9,1)
                closePane.addItem(GuiItem(ItemStack(Material.BARRIER).apply {
                    //닫기
                    itemMeta = itemMeta.apply {
                        displayName(Component.text("닫기"))
                    }
                }) {
                    gui.show(player)
                },8,0)

                refreshUserPane()
                manageUser.addPane(closePane)
                manageUser.addPane(user_pane)
                manageUser.show(player)
            })
            pane.bindItem('4', GuiItem(ItemStack(Material.BARRIER).apply {
                itemMeta = itemMeta.apply {
                    displayName(Component.text("회수하기")
                        .color(NamedTextColor.RED)
                        .decorate(TextDecoration.BOLD)
                        .decoration(TextDecoration.ITALIC, false))
                }
            }) { e ->
                val player = e.whoClicked
                if (region.owner.uniqueId != player.uniqueId) return@GuiItem

                val withdraw = ChestGui(1, "회수하시겠습니까?").apply {
                    setOnGlobalClick { it.isCancelled = true }
                }
                val agreePane = StaticPane(0,0,4,1, Pane.Priority.LOWEST)
                agreePane.fillWith(ItemStack(Material.GREEN_STAINED_GLASS_PANE).apply {
                    itemMeta = itemMeta.apply {
                        displayName(
                            Component.text('예', NamedTextColor.GREEN)
                                .decoration(TextDecoration.ITALIC, false)
                        )
                    }
                }) { agree ->
                    val player = agree.whoClicked
                    region.remove()
                    player.closeInventory()
                }

                val disagree = StaticPane(5, 0, 4,1, Pane.Priority.LOWEST)
                disagree.fillWith(ItemStack(Material.RED_STAINED_GLASS_PANE).apply {
                    itemMeta = itemMeta.apply {
                        displayName(
                            Component.text("아니요", NamedTextColor.RED)
                                .decoration(TextDecoration.ITALIC, false)
                        )
                    }
                }) { disagree ->
                    val player = disagree.whoClicked
                    player.closeInventory()
                }
                withdraw.addPane(agreePane)
                withdraw.addPane(disagree)
                withdraw.show(player)
            })

            val manager_pane = OutlinePane(0,1,9,2)
            manager_pane.priority = Pane.Priority.HIGHEST
            fun refreshPermissionUser() {
                manager_pane.clear()

                region.permissionUser.forEach {
                    if (region.owner.uniqueId == it) return@forEach

                    val manager = Bukkit.getPlayer(it)!!
                    manager_pane.addItem(GuiItem(ItemStack(Material.PLAYER_HEAD).apply {
                        itemMeta = (itemMeta as SkullMeta).apply {
                            playerProfile = manager.playerProfile
                            displayName(Component.text(manager.name).decoration(TextDecoration.ITALIC, false))
                            lore(mutableListOf(Component.text(""), Component.text("쉬프트 좌클릭시 제거됩니다.", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false)))
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
            gui.addPane(manager_pane)
            gui.addPane(pane)
            gui.show(player)
        }
    }
}