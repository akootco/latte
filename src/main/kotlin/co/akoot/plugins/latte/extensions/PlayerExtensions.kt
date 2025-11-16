package co.akoot.plugins.latte.extensions

import co.akoot.plugins.bluefox.extensions.*
import co.akoot.plugins.latte.Latte
import org.bukkit.*
import org.bukkit.entity.Player
import java.io.File

private const val LATTE_WORLD = "world"

/**
 * Applies the specified [world] settings on [this] player
 * @param world The world to apply settings from
 */
fun Player.applySettings(world: World) {
    if(hasPermission(Latte.Permission.GAMEMODE_BYPASS) && gameMode in setOf(GameMode.CREATIVE, GameMode.SPECTATOR)) return
    gameMode = world.rootWorld.gameMode
    if(world.rootWorld.flyMode) allowFlight = true
}

/**
 * Get [this] offline player's data file from the specified [world]
 * @param world The world to get the data file from
 * @return The specified world's data file
 */
fun OfflinePlayer.getDataFile(world: World): File {
    return world.getDataFile(this)
}

/**
 * Loads the specified [world]'s data file onto [this] player's current data file
 * @param world The world to load
 */
fun Player.loadData(world: World) {
    val rootWorld = world.rootWorld
    val dataFile = getDataFile(rootWorld)
    if(!dataFile.exists()) createDataFile(rootWorld)
    val pdc = persistentDataContainer.serializeToBytes()
    dataFile.copyTo(getDataFile(), true)
    persistentDataContainer.readFromBytes(pdc)
}

/**
 * Saves [this] player's current data file in the specified [world]'s directory
 * @param world The world to save
 */
fun Player.saveData(world: World) {
    saveData()
    getDataFile().copyTo(getDataFile(world.rootWorld), true)
    setPDC(Latte.key(LATTE_WORLD), world.name)
}

/**
 * The world the player was last teleported to.
 */
val Player.latteWorld: String? get() = getPDC<String>(Latte.key(LATTE_WORLD))

/**
 * Create a new data file for the specified [world]. This data file will copy [this] player's current
 * data file but with a clear inventory & end chest, and 0 xp
 * @param world The world
 */
fun Player.createDataFile(world: World) {
    saveData(this.world)
    inventory.clear()
    enderChest.clear()
    exp = 0f
    totalExperience = 0
    saveData(world)
}

fun Player.rtp(world: World = this.world, radiusX: Double = world.worldBorder.size / 2, radiusZ: Double = radiusX): Location {
    val safeLocation = world.randomSafeLocation(radiusX, radiusZ)
    val randomLocation = safeLocation.facing(yaw, pitch)
    teleport(randomLocation)
    return randomLocation
}