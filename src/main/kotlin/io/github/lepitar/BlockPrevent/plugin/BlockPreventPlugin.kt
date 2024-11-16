package io.github.lepitar.BlockPrevent.plugin

import dev.jorel.commandapi.CommandAPICommand
import dev.jorel.commandapi.arguments.ArgumentSuggestions
import dev.jorel.commandapi.arguments.StringArgument
import dev.jorel.commandapi.executors.CommandExecutor
import io.github.lepitar.BlockPrevent.regions.Region
import io.github.lepitar.BlockPrevent.regions.RegionSize
import io.github.lepitar.BlockPrevent.regions.Regions
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration
import org.bukkit.Material
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.entity.Player
import org.bukkit.plugin.java.JavaPlugin
import java.io.File

class BlockPreventPlugin : JavaPlugin() {

    companion object {
        val log_head = Component.text("[ 건차 ] ", NamedTextColor.GOLD)
            .decoration(TextDecoration.ITALIC, false)
    }

    override fun onEnable() {
        println("건축차단 Enabled!")
        Regions.initialize(this)

        server.apply {
            pluginManager.registerEvents(EventListener(), this@BlockPreventPlugin)
        }

        CommandAPICommand("건차")
            .withArguments(listOf(StringArgument("size").replaceSuggestions(
                ArgumentSuggestions.strings("LARGE", "MEDIUM", "SMALL")
            )))
            .executes(CommandExecutor { sender, args ->
                val player = sender as Player
                val loc = player.location

                val region = Region(loc, RegionSize.valueOf(args["size"].toString()), player)
                region.initialize()
            })
            .register()

        loadRegions()
    }

    override fun onDisable() {
        saveRegions()
        Regions.manager.unload()
    }

    private fun loadRegions() {
        val regionsFile = File(dataFolder, "regions.yml")
        if (!regionsFile.exists()) {
            saveResource("regions.yml", false)
        }
        val regionsConfig = YamlConfiguration.loadConfiguration(regionsFile)
        Regions.manager.loadAllRegions(regionsConfig)
    }

    private fun saveRegions() {
        val regionsFile = File(dataFolder, "regions.yml")
        val regionsConfig = YamlConfiguration()
        Regions.manager.saveAllRegions(regionsConfig)
        regionsConfig.save(regionsFile)
    }
}