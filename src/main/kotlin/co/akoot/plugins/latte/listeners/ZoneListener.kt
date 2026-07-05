package co.akoot.plugins.latte.listeners

import co.akoot.plugins.latte.Latte
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityExplodeEvent
import org.bukkit.event.entity.EntitySpawnEvent

class ZoneListener(val plugin: Latte) : Listener {
    @EventHandler
    fun onEntitySpawn(event: EntitySpawnEvent) {
        val location = event.location
        val inSafeZone = Latte.isInZone("safe_zone", location)
        val inServerSafeZone = Latte.isInZone("server_safe_zone", location)
        val inMobZone = Latte.isInZone("mob_zone", location)

        if ((inSafeZone || inServerSafeZone) && !inMobZone) {
            val blacklist = buildSet {
                Latte.entityBlacklist["safe_zone"]?.let { addAll(it) }
                if (inServerSafeZone) {
                    Latte.entityBlacklist["server_safe_zone"]?.let { addAll(it) }
                }
            }

            if (event.entityType in blacklist) {
                event.isCancelled = true
            }
        }
    }

    @EventHandler
    fun onExplosion(event: EntityExplodeEvent) {
        val location = event.entity.location
        if (Latte.isInZone("boom_zone", location)) return
        if (Latte.isInZone("safe_zone", location) || Latte.isInZone("server_safe_zone", location)) event.blockList()
            .clear()
    }

}