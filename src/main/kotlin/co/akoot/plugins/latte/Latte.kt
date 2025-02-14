package co.akoot.plugins.latte

import co.akoot.plugins.bluefox.BlueFox
import co.akoot.plugins.bluefox.api.FoxCommand.Result
import co.akoot.plugins.bluefox.api.FoxConfig
import co.akoot.plugins.bluefox.api.FoxPlugin
import co.akoot.plugins.bluefox.api.XYZ
import co.akoot.plugins.bluefox.util.Text
import co.akoot.plugins.bluefox.util.Text.Companion.accented
import co.akoot.plugins.bluefox.util.Text.Companion.copy
import co.akoot.plugins.bluefox.util.Text.Companion.errorAccented
import co.akoot.plugins.bluefox.util.Text.Companion.invoke
import co.akoot.plugins.latte.commands.LatteCommand
import co.akoot.plugins.latte.extensions.*
import org.bukkit.*
import org.bukkit.World.Environment
import org.bukkit.entity.Player
import java.io.File
import java.io.FileFilter
import kotlin.io.path.exists

class Latte : FoxPlugin("latte") {

    object Permission {
        const val GAMEMODE_BYPASS = "latte.bypass.gamemode"
    }

    companion object {
        lateinit var instance: Latte

        fun key(key: String): NamespacedKey {
            return instance.key(key)
        }
    }

    override fun load() {
        instance = this
        loadWorlds()
        registerEventListener(LatteListener(this))
    }

    override fun unload() {
        for (player in server.onlinePlayers) {
            player.saveData(player.world)
        }
    }

    override fun registerCommands() {
        registerCommand(LatteCommand(this))
    }

    private fun loadWorlds() {
        for ((world, _) in getWorldFolders()) {
            val config = getWorldConfig(world) ?: continue
            if (config.getBoolean(WorldKeys.AUTO_LOAD) == true) {
                if (load(world).value) logger.info("Loading world: $world")
            }
        }
    }

    fun list(): Result<Boolean> {
        val worlds = getWorldFolders()
        val loaded = worlds.filter { it.value }.map { it.key }
        val unloaded = worlds.filterNot { it.value }.map { it.key }
        val message = Text.list(unloaded, itemColor = "player", postfix = "\n") + Text.list(loaded)
        return Result.success(message.component)
    }

    fun delete(name: String): Result<Boolean> {
        val world = server.getWorld(name) ?: return Result.fail("World "() + name.errorAccented() + " does not exist!")
        server.unloadWorld(world, false)
        world.config.file.delete()
        world.worldFolder.deleteRecursively()
        return Result.success("World "() + name.accented() + " deleted successfully! RIP")
    }

    fun load(name: String, environment: Environment? = null): Result<Boolean> {
        server.getWorld(name)?.let { return Result.fail("World "() + it.name.errorAccented() + " already loaded!") }
        val worldCreator = WorldCreator(name)
        val config = getWorldConfig(name)
        val env = environment ?: config?.getString("environment")?.let { Environment.valueOf(it) } ?: Environment.NORMAL
        worldCreator.environment(env)
        val world =
            worldCreator.createWorld() ?: return Result.fail("World "() + name.errorAccented() + " failed to load!")
        config?.apply {
            set("environment", world.environment.name)
        }
        return Result.success("World "() + world.name.accented() + " has been loaded!")
    }

    fun unload(name: String): Result<Boolean> {
        val world = server.getWorld(name) ?: return Result.fail("World "() + name.errorAccented() + "could not be unloaded!")
        for (player in world.players) {
            player.teleport(player.respawnLocation ?: BlueFox.spawnLocation)
        }
        val environment = world.environment.name
        if (!server.unloadWorld(world, true))
            return Result.fail("World "() + world.name.errorAccented() + " could not be unloaded!")
        world.config.set("environment", environment)
        return Result.success("World "() + world.name.accented() + " has been unloaded!")
    }

    fun teleport(player: Player, name: String, pos: XYZ? = null): Result<Boolean> {
        val world = server.getWorld(name) ?: return Result.fail("World " + name.errorAccented() + " is not loaded!")
        var location = world.spawnLocation
        if (pos != null) {
            location = Location(world, pos.x, pos.y, pos.z)
        }
        player.teleport(location)
        return Result.success("Teleporting to "() + XYZ(location) + " in " + world.name.accented() + ".")
    }

    fun getWorldFolders(): Map<String, Boolean> {
        return File(".").listFiles(FileFilter { it.isDirectory && it.toPath().resolve("uid.dat").exists() })
            ?.associateBy({ it.name }, { server.getWorld(it.name) != null }) ?: return mapOf()
    }

    fun getWorldConfig(name: String): FoxConfig? {
        val folder = File(name)
        if (!(folder.isDirectory && folder.exists())) return null
        val file = folder.resolve("latte.conf")
        return FoxConfig(file)
    }

    fun createWorld(
        name: String,
        gameMode: GameMode? = GameMode.SURVIVAL,
        environment: Environment? = Environment.NORMAL,
        seed: Long? = 0,
        type: WorldType? = WorldType.NORMAL
    ): Result<World?> {
        var creator = WorldCreator(name)
        environment?.let { creator = creator.environment(it) }
        seed?.let { creator = creator.seed(it) }
        type?.let { creator = creator.type(it) }
        val world = creator.createWorld()
        world?.config?.apply {
            set(WorldKeys.ENVIRONMENT, environment?.name)
            set(WorldKeys.GAME_MODE, gameMode?.name)
            set(WorldKeys.TYPE, type?.name)
        }
        if (world == null) return Result(null, "There was some kinda error trying to create that world...")
        return Result(
            world,
            "Created world "() + world.name.accented() + " (" + (gameMode ?: GameMode.SURVIVAL).name.lowercase()
                .accented() + "/" + (environment ?: Environment.NORMAL).name.lowercase().accented() + "/" + (type
                ?: WorldType.NORMAL).name.lowercase().accented() + ") with seed " + (seed ?: creator.seed()).copy()
        )
    }
}