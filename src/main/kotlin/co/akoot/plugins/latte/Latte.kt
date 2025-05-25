package co.akoot.plugins.latte

import co.akoot.plugins.bluefox.BlueFox
import co.akoot.plugins.bluefox.api.Area
import co.akoot.plugins.bluefox.api.FoxCommand.Result
import co.akoot.plugins.bluefox.api.FoxConfig
import co.akoot.plugins.bluefox.api.FoxPlugin
import co.akoot.plugins.bluefox.api.Kolor
import co.akoot.plugins.bluefox.api.XYZ
import co.akoot.plugins.bluefox.extensions.addToPDCList
import co.akoot.plugins.bluefox.extensions.getPDCList
import co.akoot.plugins.bluefox.extensions.invoke
import co.akoot.plugins.bluefox.extensions.removeFromPDCList
import co.akoot.plugins.bluefox.extensions.setMeta
import co.akoot.plugins.bluefox.extensions.setPDC
import co.akoot.plugins.bluefox.extensions.text
import co.akoot.plugins.bluefox.util.Text
import co.akoot.plugins.bluefox.util.Text.Companion.copy
import co.akoot.plugins.bluefox.util.Text.Companion.invoke
import co.akoot.plugins.latte.commands.LatteCommand
import co.akoot.plugins.latte.commands.SafeZoneCommand
import co.akoot.plugins.latte.extensions.*
import org.bukkit.*
import org.bukkit.World.Environment
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.player.PlayerInteractEvent
import java.io.File
import java.io.FileFilter
import kotlin.io.path.exists

class Latte : FoxPlugin("latte"), Listener {

    object Permission {
        const val GAMEMODE_BYPASS = "latte.bypass.gamemode"
    }

    companion object {
        lateinit var instance: Latte

        fun key(key: String): NamespacedKey {
            return instance.key(key)
        }

        val safeZones: MutableSet<Area> = mutableSetOf()

        fun isInSafeZone(location: Location): Boolean {
            for (safeZone in safeZones) {
                if (safeZone.has(location)) return true
            }
            return false
        }

        fun addSafeZone(area: Area): Boolean {
            val s1 = safeZones.add(area)
            val s2 = area.world.addToPDCList(key("safe_zones"), area.serialize())
            return s1 && s2
        }

        fun removeSafeZone(area: Area): Boolean {
            val s1 = safeZones.remove(area)
            val s2 = area.world.removeFromPDCList(key("safe_zones"), area.serialize())
            return s1 && s2
        }

        fun clearSafeZones() {
            for (area in safeZones) {
                area.world.setPDC<List<String>>(key("safe_zones"), null)
            }
            safeZones.clear()
        }

        val tool = Material.BRUSH // lol
        fun toolCheck(player: Player): Boolean {
            return player.gameMode == GameMode.CREATIVE && player.hasPermission("choco.tool") && player.inventory.itemInMainHand.type == tool
        }
    }

    override fun load() {
        instance = this
        loadWorlds()
        loadSafeZones()
        registerEventListener(LatteListener(this))
    }

    override fun unload() {
        for (player in server.onlinePlayers) {
            player.saveData(player.world)
        }
    }

    override fun registerCommands() {
        registerCommand(LatteCommand(this))
        registerCommand(SafeZoneCommand(this))
    }

    override fun registerEvents() {
        registerEventListener(this)
    }

    private fun loadWorlds() {
        for ((world, _) in getWorldFolders()) {
            val config = getWorldConfig(world) ?: continue
            if (config.getBoolean(WorldKeys.AUTO_LOAD) == true) {
                if (load(world).value) logger.info("Loading world: $world")
            }
        }
    }

    fun loadSafeZones() {
        for (world in server.worlds) {
            val safeZoneValue = world.getPDCList<String>(key("safe_zones")) ?: continue
            for (entry in safeZoneValue) {
                val safeZone = Area.deserialize(world, entry) ?: continue
                safeZones.add(safeZone)
            }
        }
    }

    fun list(): Result<Boolean> {
        val worlds = getWorldFolders()
        val loaded = worlds.filter { it.value }.map { it.key }
        val unloaded = worlds.filterNot { it.value }.map { it.key }
        val message = Text.list(unloaded, itemKolor = Kolor.WARNING, postfix = "\n") + Text.list(loaded)
        return Result.success(message.component)
    }

    fun delete(name: String): Result<Boolean> {
        val world = server.getWorld(name) ?: return Result.fail(Kolor.ERROR("World ") + Kolor.ERROR.accent(name) + Kolor.ERROR(" does not exist!"))
        server.unloadWorld(world, false)
        world.config.file.delete()
        world.worldFolder.deleteRecursively()
        return Result.success(Kolor.TEXT("World ") + Kolor.ACCENT(name) + Kolor.TEXT(" deleted successfully! RIP"))
    }

    fun load(name: String, environment: Environment? = null): Result<Boolean> {
        server.getWorld(name)?.let { return Result.fail(Kolor.ERROR("World ") + Kolor.ERROR.accent(it.name) + Kolor.ERROR(" already loaded!")) }
        val worldCreator = WorldCreator(name)
        val config = getWorldConfig(name)
        val env = environment ?: config?.getString("environment")?.let { Environment.valueOf(it) } ?: Environment.NORMAL
        worldCreator.environment(env)
        val world =
            worldCreator.createWorld() ?: return Result.fail(Kolor.ERROR("World ") + Kolor.ERROR.accent(name) + Kolor.ERROR(" failed to load!"))
        config?.apply {
            set("environment", world.environment.name)
        }
        return Result.success(Kolor.TEXT("World ") + Kolor.ACCENT(name) + Kolor.TEXT(" has been loaded!"))
    }

    fun unload(name: String): Result<Boolean> {
        val world = server.getWorld(name) ?: return Result.fail(Kolor.ERROR("World ") + Kolor.ERROR.accent(name) + Kolor.ERROR(" could not be unloaded!"))
        for (player in world.players) {
            player.teleport(player.respawnLocation ?: BlueFox.spawnLocation)
        }
        val environment = world.environment.name
        if (!server.unloadWorld(world, true))
            return Result.fail(Kolor.ERROR("World ") + Kolor.ERROR.accent(name) + Kolor.ERROR(" could not be unloaded!"))
        world.config.set("environment", environment)
        return Result.success(Kolor.TEXT("World ") + Kolor.ACCENT(name) + Kolor.TEXT(" has been unloaded!"))
    }

    fun teleport(player: Player, name: String, pos: XYZ? = null): Result<Boolean> {
        val world = server.getWorld(name) ?: return Result.fail(Kolor.ERROR("World ") + Kolor.ERROR.accent(name) + Kolor.ERROR(" is not loaded!"))
        var location = world.spawnLocation
        if (pos != null) {
            location = Location(world, pos.x, pos.y, pos.z)
        }
        player.teleport(location)
        return Result.success(Kolor.TEXT("Teleporting to ") + XYZ(location) + " in " + Kolor.ACCENT(world.name))//Result.success("Teleporting to "() + XYZ(location) + " in " + world.name.accented() + ".")
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
            Kolor.TEXT("Created world ") +
                    Kolor.ACCENT(world.name) +
                    Kolor.TEXT(" (") +
                    Kolor.ACCENT((gameMode ?: GameMode.SURVIVAL).name.lowercase() +
                    Kolor.TEXT("/") +
                    Kolor.ALT((environment ?: Environment.NORMAL).name.lowercase()) +
                    Kolor.TEXT("/") +
                    Kolor.ACCENT((type ?: WorldType.NORMAL).name.lowercase()) +
                    Kolor.TEXT(") with seed ") +
                    (seed ?: creator.seed()).copy()
                    )
        )
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    fun onInteract(event: PlayerInteractEvent) {
        if (!toolCheck(event.player)) return
        val player = event.player
        val action = event.action
        if(action == Action.LEFT_CLICK_BLOCK) {
            event.clickedBlock?.location?.let { loc ->
                player.setMeta("tool.pos1", loc)
                Text(player) {
                    Kolor.ACCENT("Pos1") + Kolor.ALT(" set to ") + loc.text
                }
                event.isCancelled = true
            }
        } else if(action == Action.RIGHT_CLICK_BLOCK) {
            event.clickedBlock?.location?.let { loc ->
                player.setMeta("tool.pos2", loc)
                Text(player) {
                    Kolor.ACCENT("Pos2") + Kolor.ALT(" set to ") + loc.text
                }
                event.isCancelled = true
            }
        }
    }
}