package co.akoot.plugins.latte.extensions

import co.akoot.plugins.bluefox.api.FoxConfig
import co.akoot.plugins.bluefox.extensions.mkdirp
import co.akoot.plugins.latte.extensions.WorldKeys.ALLOW_ADVANCEMENTS
import co.akoot.plugins.latte.extensions.WorldKeys.ALLOW_FLIGHT
import co.akoot.plugins.latte.extensions.WorldKeys.ALLOW_STATS
import co.akoot.plugins.latte.extensions.WorldKeys.GAME_MODE
import co.akoot.plugins.latte.extensions.WorldKeys.IS_ANARCHY
import co.akoot.plugins.latte.extensions.WorldKeys.PARENT_WORLD
import co.akoot.plugins.latte.extensions.WorldKeys.SEPARATE_CHAT
import org.bukkit.*
import org.bukkit.World.Environment
import java.io.File
import kotlin.random.Random


object WorldKeys {
    const val ENVIRONMENT = "environment"
    const val TYPE = "type"
    const val GAME_MODE = "gameMode"
    const val ALLOW_FLIGHT = "allowFlight"
    const val PARENT_WORLD = "parentWorld"
    const val AUTO_LOAD = "autoload"
    const val ALLOW_ADVANCEMENTS = "allowAdvancements"
    const val ALLOW_STATS = "allowStats"
    const val IS_ANARCHY = "isAnarchy"

    const val SEPARATE_CHAT = "separateChat"

    val booleans = setOf(AUTO_LOAD, ALLOW_FLIGHT, ALLOW_ADVANCEMENTS, ALLOW_STATS, SEPARATE_CHAT, IS_ANARCHY)
    val enums = setOf(GAME_MODE, ENVIRONMENT, TYPE)
    val strings = setOf(PARENT_WORLD)
    val all = strings + enums + booleans

    fun from(world: World): Map<String, Any> {
        val map = mutableMapOf<String, Any>()
        for (key in booleans) {
            map += key to (world.config.getBoolean(key) ?: false)
        }
        for (key in enums + strings) {
            map += key to (world.config.getString(key) ?: "DEFAULT")
        }
        return map
    }
}

val World.config: FoxConfig get() = FoxConfig(worldFolder.resolve("latte.conf"))
val World.parentWorld: World? get() = config.getString(PARENT_WORLD)?.let { Bukkit.getWorld(it) }
val World.rootWorld: World get() = parentWorld ?: getRelatedWorld(Environment.NORMAL) ?: this
val World.gameMode: GameMode
    get() = config.getString(GAME_MODE)?.let { GameMode.valueOf(it.uppercase()) } ?: GameMode.SURVIVAL
val World.flyMode: Boolean get() = config.getBoolean(ALLOW_FLIGHT) ?: false
val World.statsAllowed: Boolean get() = config.getBoolean(ALLOW_STATS) ?: true
val World.advancementsAllowed: Boolean get() = config.getBoolean(ALLOW_ADVANCEMENTS) ?: true

val World.isAnarchy: Boolean get() = config.getBoolean(IS_ANARCHY) ?: false

val World.isSeparateChat: Boolean get() = config.getBoolean(SEPARATE_CHAT) ?: false


/**
 * Get the related world based on the specified [environment].
 * If [this] world's name is "pepe", and [environment] is [Environment.THE_END], then the related world
 * would be "pepe_the_end".
 *
 * @param environment The environment to check for
 * @return The related world, if found, null otherwise
 */
fun World.getRelatedWorld(environment: Environment?): World? {
    if (environment == null) return null
    if (environment == this.environment) return null

    val worldName = name
    val relatedName = when (environment) {
        Environment.NETHER -> if (worldName.endsWith("_nether")) worldName else "${worldName}_nether"
        Environment.THE_END -> if (worldName.endsWith("_the_end")) worldName else "${worldName}_the_end"
        Environment.NORMAL -> worldName.removeSuffix("_nether").removeSuffix("_the_end")
        else -> return null
    }

    return Bukkit.getWorld(relatedName)
}

/**
 * Check if either [this] world's parent world is [world], or [world]'s parent world is [this] world, OR they are related
 * based on their environments
 *
 * @param world The world to check
 * @return Whether [this] world is related to [world] in any way
 */
fun World.isRelated(world: World): Boolean {
    return parentWorld == world || world.parentWorld == this || isRelatedEnvironment(world)
}

/**
 * Check if [this] world is related to [world] based on their environments.
 * If [this] world is of [Environment.NORMAL] and [world]'s environment is of [Environment.NETHER] AND [world]'s
 * name ends with "_nether", then they are related.
 *
 * @param world The world to check
 * @return Whether [this] world is related to [world] based on their environments
 */
fun World.isRelatedEnvironment(world: World): Boolean {
    return getRelatedWorld(world.environment) == world
}

/**
 * Get the [offlinePlayer]'s data file from [this] world
 * @param offlinePlayer The offline player
 * @return The data file for the offline player from [this] world
 */
fun World.getDataFile(offlinePlayer: OfflinePlayer): File {
    return worldFolder.resolve("latte/playerdata").mkdirp()
        .resolve("${offlinePlayer.uniqueId}.dat")
}

fun World.randomSafeLocation(
    radiusX: Double = this.worldBorder.size,
    radiusZ: Double = radiusX,
    radiusY: Double = this.maxHeight.toDouble()
): Location {
    val border = worldBorder.size
    val randomLocation = Location(
        this,
        Random.nextDouble(-radiusX, radiusX).coerceIn(-border, border),
        Random.nextDouble(this.minHeight.toDouble(), radiusY).coerceAtMost(maxHeight.toDouble()),
        Random.nextDouble(-radiusZ, radiusZ).coerceIn(-border, border)
    )

    return randomLocation.lowestSafeLocation
}

