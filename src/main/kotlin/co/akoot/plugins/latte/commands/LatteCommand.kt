package co.akoot.plugins.latte.commands

import co.akoot.plugins.bluefox.BlueFox
import co.akoot.plugins.bluefox.api.FoxCommand
import co.akoot.plugins.bluefox.api.FoxPlugin
import co.akoot.plugins.bluefox.api.XYZ
import co.akoot.plugins.bluefox.util.ColorUtil
import co.akoot.plugins.bluefox.util.Txt
import org.bukkit.Location
import org.bukkit.World
import org.bukkit.WorldCreator
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import java.io.File
import kotlin.io.path.exists

class LatteCommand(plugin: FoxPlugin): FoxCommand(plugin, "latte") {

    override fun onTabComplete(
        sender: CommandSender,
        args: Array<out String>?
    ): MutableList<String> {
        if (args?.size == 1) {
            return mutableListOf("load", "unload", "list", "tp")
        }
        else if (args?.size == 2) {
            return when(args[0]) {
                "load" -> getWorldFolders().filterValues { it == false }.keys.toMutableList()
                "unload", "teleport", "tp" -> getWorldFolders().filterValues { it == true }.keys.toMutableList()
                "teleportPlayer", "tpp" -> getOnlinePlayerSuggestions(args)
                else -> mutableListOf()
            }
        }
        else if (args?.size == 3) {
            if (args[0] in setOf("tpPlayer", "tpp")) {
                val player = getPlayer(args[1]).value
                if (player != null) return getWorldFolders().filterValues { it == true }.keys.toMutableList()
            }
        }
        return mutableListOf()
    }

    override fun onCommand(
        sender: CommandSender,
        alias: String,
        args: Array<out String>?
    ): Boolean {
        if (args.isNullOrEmpty()) return sendUsage(sender)
        return when(args[0]) {
            "load" -> load(args[1]).send(sender).value
            "unload" -> unload(args[1]).send(sender).value
            "list" -> list().send(sender).value
            "teleport", "tp" -> {
                val sender = getPlayerSender(sender).send(sender).value ?: return false
                val worldName = args[1]
                if (args.size == 2) {
                    teleport(sender, worldName).send(sender).value
                } else if(args.size == 5) {
                    teleport(sender, worldName, XYZ(args.drop(1))).send(sender).value
                }
                false
            }
            "teleportPlayer", "tpp" -> {
                val player = getPlayer(args[1]).send(sender).value ?: return false
                val worldName = args[2]
                if (args.size == 3) {
                    teleport(player, worldName).send(sender).value
                } else if(args.size == 6) {
                    teleport(player, worldName, XYZ(args.drop(2))).send(sender).value
                }
                false
            }
            else -> sendUsage(sender)
        }
    }

    private fun getWorld(name: String): World? {
        return plugin.server.getWorld(name)
    }

    private fun list(): Result<Boolean> {
        val worlds = getWorldFolders()
        val message = Txt()
        for ((name, loaded) in worlds.entries) {
            val nameComponent = Txt(name)
            if (loaded) nameComponent.color("accent")
            message += Txt.CR + nameComponent
        }
        return Result.success(message.c)
    }

    private fun load(name: String): Result<Boolean> {
        getWorld(name)?.let { return Result.fail("World \$WORLD already loaded!", "WORLD" to it.name) }
        val world = WorldCreator(name).createWorld() ?: return Result.fail("World \$WORLD failed to load!", "WORLD" to name)
        return Result.success("World \$WORLD has been loaded!", "WORLD" to world.name)
    }

    private fun unload(name: String): Result<Boolean> {
        val world = getWorld(name) ?: return Result.fail("World \$WORLD not loaded!", "WORLD" to name)
        for(player in world.players) {
            player.teleport(player.respawnLocation ?: BlueFox.spawnLocation)
        }
        if (!plugin.server.unloadWorld(world, true)) return Result.fail("World \$WORLD could not be unloaded!", "WORLD" to name)
        return Result.success("World \$WORLD has been unloaded!", "WORLD" to world.name)
    }

    private fun teleport(player: Player, name: String, pos: XYZ? = null): Result<Boolean> {
        val world = getWorld(name) ?: return Result.fail("World \$WORLD is not loaded!", "WORLD" to name)
        var location = world.spawnLocation
        if (pos != null) {
            location = Location(world, pos.x, pos.y, pos.z)
        }
        player.teleport(location)
        return Result.success("Teleporting to \$LOCATION in \$WORLD", "X" to location)
    }

    private fun getWorldFolders(): Map<String, Boolean> {
        return File(".").listFiles { it.isDirectory && it.toPath().resolve("level.dat").exists() }
            ?.associateBy({ it.name }, { plugin.server.getWorld(it.name) != null }) ?: return mapOf()
    }
}