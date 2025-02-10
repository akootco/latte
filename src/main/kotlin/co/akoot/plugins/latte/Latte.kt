package co.akoot.plugins.latte

import co.akoot.plugins.bluefox.BlueFox
import co.akoot.plugins.bluefox.api.FoxCommand.Result
import co.akoot.plugins.bluefox.api.FoxConfig
import co.akoot.plugins.bluefox.api.FoxPlugin
import co.akoot.plugins.bluefox.api.XYZ
import co.akoot.plugins.bluefox.extensions.getPDC
import co.akoot.plugins.bluefox.util.Text
import co.akoot.plugins.bluefox.util.Text.Companion.accented
import co.akoot.plugins.bluefox.util.Text.Companion.copy
import co.akoot.plugins.bluefox.util.Text.Companion.errorAccented
import co.akoot.plugins.bluefox.util.Text.Companion.invoke
import co.akoot.plugins.latte.commands.LatteCommand
import org.bukkit.*
import org.bukkit.World.Environment
import org.bukkit.entity.Player
import java.io.File
import java.io.FileFilter
import kotlin.io.path.exists

class Latte : FoxPlugin("latte") {

    companion object {
        fun World.getConfig(): FoxConfig {
            return FoxConfig(worldFolder.resolve("latte.conf"))
        }

        fun World.getParentWorldName(): String? {
            return if (name.endsWith("_nether") || name.endsWith("_the_end")) name.substringBefore("_") else getConfig().getString(
                "parent"
            )
        }

        fun World.getRelatedWorld(environment: Environment?): World? {
            val relatedName = when (environment) {
                Environment.NETHER -> "${name}_nether"
                Environment.THE_END -> "${name}_the_end"
                Environment.NORMAL -> name.substringBefore("_")
                else -> return null
            }
            return Bukkit.getWorlds().find { it.name == relatedName }
        }

        fun World.isRelated(world: World): Boolean {
            return getParentWorldName() == world.name || world.getParentWorldName() == name
        }

        lateinit var instance: Latte

        fun key(key: String): NamespacedKey {
            return instance.key(key)
        }
    }

    val tps get() = server.tps.getOrElse(0) { 20.0 }

    override fun load() {
        instance = this
        for ((world, loaded) in getWorldFolders()) {
            val config = getWorldConfig(world) ?: continue
            if (config.getBoolean("autoload") == true) {
                if (load(world).value) logger.info("Loading $world...")
            }
        }
        registerEventListener(LatteListener(this))
    }

    override fun unload() {
        for (player in server.onlinePlayers) {
            saveDataFile(player, player.world)
        }
    }

    override fun registerCommands() {
        registerCommand(LatteCommand(this))
    }

    fun getWorld(name: String): World? {
        return server.getWorld(name)
    }

    fun list(): Result<Boolean> {
        val worlds = getWorldFolders()
        val loaded = worlds.filter { it.value }.map { it.key }
        val unloaded = worlds.filterNot { it.value }.map { it.key }
        val message = Text.list(unloaded, itemColor = "player", postfix = "\n") + Text.list(loaded)
        return Result.success(message.component)
    }

    fun delete(name: String): Result<Boolean> {
        val world = getWorld(name) ?: return Result.fail("World "() + name.errorAccented() + " does not exist!")
        server.unloadWorld(world, false)
        world.getConfig().file.delete()
        world.worldFolder.deleteRecursively()
        return Result.success("World "() + name.accented() + " deleted successfully! RIP")
    }

    fun load(name: String): Result<Boolean> {
        getWorld(name)?.let { return Result.fail("World "() + it.name.errorAccented() + " already loaded!") }
        val worldCreator = WorldCreator(name)
        val config = getWorldConfig(name)
        val environment = config?.getString("environment")?.let { Environment.valueOf(it) } ?: Environment.NORMAL
        worldCreator.environment(environment)
        val world =
            worldCreator.createWorld() ?: return Result.fail("World "() + name.errorAccented() + " failed to load!")
        config?.apply {
            set("environment", world.environment.name)
        }
        return Result.success("World "() + world.name.accented() + " has been loaded!")
    }

    fun unload(name: String): Result<Boolean> {
        val world = getWorld(name) ?: return Result.fail("World "() + name.errorAccented() + "could not be unloaded!")
        for (player in world.players) {
            player.teleport(player.respawnLocation ?: BlueFox.spawnLocation)
        }
        val environment = world.environment.name
        if (!server.unloadWorld(world, true))
            return Result.fail("World "() + world.name.errorAccented() + " could not be unloaded!")
        world.getConfig().set("environment", environment)
        return Result.success("World "() + world.name.accented() + " has been unloaded!")
    }

    fun teleport(player: Player, name: String, pos: XYZ? = null): Result<Boolean> {
        val world = getWorld(name) ?: return Result.fail("World " + name.errorAccented() + " is not loaded!")
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

    fun setupNewDataFile(player: Player) {
        player.inventory.clear()
        player.enderChest.clear()
        player.saveData()
    }

    fun loadDataFile(offlinePlayer: OfflinePlayer, fromWorld: World, toWorld: World, op: () -> Unit = {}) {
        if (fromWorld.isRelated(toWorld)) {
            runLater((tps / 10).toLong()) {
                op()
            }
            return
        }
        val fromWorldDataFile = getDataFile(offlinePlayer, fromWorld)
        val toWorldDataFile = getDataFile(offlinePlayer, toWorld)
        val currentDataFile = getDataFile(offlinePlayer)
        offlinePlayer.player?.saveData()
        currentDataFile.copyTo(fromWorldDataFile, true)
        if (toWorldDataFile.exists()) {
            toWorldDataFile.copyTo(currentDataFile, true)
        } else {
            offlinePlayer.player?.let { setupNewDataFile(it) }
            currentDataFile.copyTo(toWorldDataFile, true)
            runLater((tps / 10).toLong()) {
                op()
            }
            return
        }
        runLater((tps / 10).toLong()) {
            offlinePlayer.player?.apply {
                loadData()
                updateInventory()
                gameMode = toWorld.getConfig().getString("gamemode")?.uppercase()?.let { GameMode.valueOf(it) }
                    ?: GameMode.SURVIVAL
            }
            op()
        }
    }

    fun loadDataFile(offlinePlayer: OfflinePlayer, world: World) {
        getDataFile(offlinePlayer, world).copyTo(getDataFile(offlinePlayer), true)
    }

    fun saveDataFile(offlinePlayer: OfflinePlayer, world: World) {
        offlinePlayer.player?.saveData()
        getDataFile(offlinePlayer).copyTo(getDataFile(offlinePlayer, world), true)
    }

    fun setDefaultGamemode(player: Player, world: World, default: GameMode = GameMode.SURVIVAL) {
        player.gameMode = world.getConfig().getString("gamemode")?.uppercase()?.let { GameMode.valueOf(it) } ?: default
    }

    fun update(player: Player) {
        player.apply {
            loadData()
            updateInventory()
        }
    }

    fun runLater(ticks: Long = 1, runnable: Runnable) {
        server.scheduler.runTaskLater(this, runnable, ticks)
    }

    fun getDataFile(offlinePlayer: OfflinePlayer): File {
        return File("world/playerdata/${offlinePlayer.uniqueId}.dat")
    }

    fun getDataFile(offlinePlayer: OfflinePlayer, world: World): File {
        val folder = dataFolder.resolve("playerdata/${world.run { getParentWorldName() ?: name }}")
        folder.mkdirs()
        return folder.resolve("${offlinePlayer.uniqueId}.dat")
    }

    fun getWorldConfig(name: String): FoxConfig? {
        val folder = File(name)
        if (!(folder.isDirectory && folder.exists())) return null
        val file = folder.resolve("latte.conf")
        return FoxConfig(file)
    }

    fun key(key: String): NamespacedKey {
        return NamespacedKey(this, key)
    }

    fun runAsync(runnable: Runnable) {
        server.scheduler.runTaskAsynchronously(this, runnable)
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
        world?.getConfig()?.apply {
            set("environment", environment?.name)
            set("gamemode", gameMode?.name)
            set("type", type?.name)
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