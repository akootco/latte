package co.akoot.plugins.latte.extensions

import org.bukkit.Location
import kotlin.random.Random

val Location.lowestFloor: Location get() {
    var below = this.subtract(0.0, 1.0, 0.0)
    while (!below.block.isSolid) {
        if (below.y <= below.world.minHeight) return this.toHighestLocation()
        below = below.subtract(0.0, 1.0, 0.0)
    }
    return below.add(0.0,1.0,0.0)
}

val Location.ascendUntilSafe: Location get() {
    var loc = this
    var above = loc.add(0.0, 1.0, 0.0)
    while (above.block.isSolid) {
        if (loc.y >= loc.world.maxHeight) return loc.toHighestLocation()
        loc = above
        above = loc.add(0.0, 1.0, 0.0)
    }
    return loc
}

val Location.lowestSafeLocation: Location get() = lowestFloor.ascendUntilSafe

fun Location.around(radius: Double = 10.0): Location {
    val randomX = x - Random.nextDouble(radius) + Random.nextDouble(radius)
    val randomY = y - Random.nextDouble(radius) + Random.nextDouble(radius)
    val randomZ = z - Random.nextDouble(radius) + Random.nextDouble(radius)
    return Location(world, randomX, randomY, randomZ, yaw, pitch)
}

fun Location.facing(yaw: Float, pitch: Float): Location {
    return Location(world, x, y, z, yaw, pitch)
}