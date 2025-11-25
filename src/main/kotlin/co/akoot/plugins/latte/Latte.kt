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
import co.akoot.plugins.bluefox.extensions.setPDC
import co.akoot.plugins.bluefox.util.Text
import co.akoot.plugins.bluefox.util.Text.Companion.copy
import co.akoot.plugins.latte.commands.GlobalCommand
import co.akoot.plugins.latte.commands.LatteCommand
import co.akoot.plugins.latte.commands.MobZoneCommand
import co.akoot.plugins.latte.commands.RtpCommand
import co.akoot.plugins.latte.commands.SafeZoneCommand
import co.akoot.plugins.latte.commands.ServerSafeZoneCommand
import co.akoot.plugins.latte.extensions.*
import co.akoot.plugins.latte.listeners.LatteListener
import co.akoot.plugins.latte.listeners.MultiWorldListener
import co.akoot.plugins.latte.listeners.ZoneListener
import org.bukkit.*
import org.bukkit.World.Environment
import org.bukkit.entity.EntityType
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

        val zones: MutableMap<String, MutableSet<Area>> = mutableMapOf()
        val entityBlacklist: MutableMap<String, MutableSet<EntityType>> = mutableMapOf()

        fun isInZone(zoneName: String, location: Location): Boolean {
            for (zone in zones[zoneName] ?: return false) {
                if (zone.has(location)) return true
            }
            return false
        }

        fun addZone(zoneName: String, area: Area): Boolean {
            val s1 = zones.getOrPut(zoneName) { mutableSetOf() }.add(area) //zones[zoneName]?.add(area) ?: return false
            val s2 =  area.world.addToPDCList(key("zones.$zoneName"), area.serialize())
            return s1 && s2
        }

        fun removeZone(zoneName: String, area: Area): Boolean {
            val s1 = zones[zoneName]?.remove(area) ?: return false
            val s2 = area.world.removeFromPDCList(key("zones.$zoneName"), area.serialize())
            return s1 && s2
        }

        fun clearZones(zoneName: String) {
            val namedZone = zones[zoneName] ?: return
            for (area in namedZone) {
                area.world.setPDC<List<String>>(key("zones.$zoneName"), null)
            }
            namedZone.clear()
        }

        val tool = Material.BRUSH // lol
        fun toolCheck(player: Player): Boolean {
            return player.gameMode == GameMode.CREATIVE && player.hasPermission("choco.tool") && player.inventory.itemInMainHand.type == tool
        }
    }

    override fun load() {
        instance = this
        loadWorlds()
        loadZones()
        settings.onLoad = { loadZones() }
    }

    override fun unload() {
        for (player in server.onlinePlayers) {
            player.saveData(player.world)
        }
    }

    override fun registerCommands() {
        registerCommand(LatteCommand(this))
        registerCommand(ServerSafeZoneCommand(this))
        registerCommand(SafeZoneCommand(this))
        registerCommand(MobZoneCommand(this))
        registerCommand(RtpCommand(this))
        registerCommand(GlobalCommand(this))
    }

    override fun registerEvents() {
        if(settings.getBoolean("world_management") == true) {
            registerEventListener(MultiWorldListener(this))
        }
        registerEventListener(LatteListener(this))
        registerEventListener(ZoneListener(this))
    }

    private fun loadWorlds() {
        for ((world, _) in getWorldFolders()) {
            val config = getWorldConfig(world) ?: continue
            if (config.getBoolean(WorldKeys.AUTO_LOAD) == true) {
                if (load(world).value) logger.info("Loading world: $world")
            }
        }
    }

    fun loadZones() {
        zones.clear()
        entityBlacklist.clear()
        logger.info("Loading zones...")
        for (world in server.worlds) {
            loadZone(world, "server_safe_zone")
            loadZone(world, "safe_zone")
            loadZone(world, "mob_zone")
        }
    }

    fun loadZone(world: World, zoneName: String) {
        val loadedZones = zones.getOrPut(zoneName) { mutableSetOf() }
        val loadedBlacklist = entityBlacklist.getOrPut(zoneName) { mutableSetOf() }

        world.getPDCList<String>(key("zones.$zoneName"))
            ?.mapNotNull { Area.deserialize(world, it) }
            ?.forEach { loadedZones.add(it) }

        settings.getStringList("zones.$zoneName.blacklisted_entities")
            .mapNotNull { value ->
                EntityType.entries.find { it.name.equals(value, ignoreCase = true) }
            }
            .forEach { loadedBlacklist.add(it) }
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
            (Kolor.TEXT("Created world ") +
                    Kolor.ACCENT(world.name) +
                    Kolor.TEXT(" (") +
                    Kolor.ACCENT((gameMode ?: GameMode.SURVIVAL).name.lowercase() +
                    Kolor.TEXT("/") +
                    Kolor.ALT((environment ?: Environment.NORMAL).name.lowercase()) +
                    Kolor.TEXT("/") +
                    Kolor.ACCENT((type ?: WorldType.NORMAL).name.lowercase()) +
                    Kolor.TEXT(") with seed ") +
                    (seed ?: creator.seed()).copy()
                    )).component
        )
    }
}