package co.akoot.plugins.latte

import co.akoot.plugins.latte.Latte.Companion.isRelated
import org.bukkit.World
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityTeleportEvent
import org.bukkit.event.player.PlayerLoginEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.event.player.PlayerRespawnEvent
import org.bukkit.event.player.PlayerTeleportEvent

class LatteListener(private val latte: Latte) : Listener {

    @EventHandler
    fun onTeleportEntity(event: EntityTeleportEvent) {
        val toWorld = event.to?.world ?: return
        val fromWorld = event.from.world
        if (!fromWorld.isRelated(toWorld)) {
            event.isCancelled = true
        }
    }

    @EventHandler
    fun onTeleport(event: PlayerTeleportEvent) {
        if (event.cause in setOf(
                PlayerTeleportEvent.TeleportCause.EXIT_BED,
                PlayerTeleportEvent.TeleportCause.DISMOUNT,
                PlayerTeleportEvent.TeleportCause.CHORUS_FRUIT,
                PlayerTeleportEvent.TeleportCause.END_GATEWAY,
            )
        ) return
        val fromWorld = event.from.world
        val toWorld: World = event.to.world
        if (fromWorld == toWorld) return
        if (event.cause in setOf(
                PlayerTeleportEvent.TeleportCause.NETHER_PORTAL,
                PlayerTeleportEvent.TeleportCause.END_PORTAL
            )
        ) {
            if (!fromWorld.isRelated(toWorld)) {
                event.isCancelled = true
                return
            }
        }
        val player = event.player
        latte.loadDataFile(player, fromWorld, toWorld) {
            player.teleport(event.to, PlayerTeleportEvent.TeleportCause.EXIT_BED)
        }
    }

    @EventHandler
    fun onRespawn(event: PlayerRespawnEvent) {
        val player = event.player
        val fromWorld = player.world
        val respawnLocation = player.respawnLocation ?: fromWorld.spawnLocation
        event.respawnLocation = respawnLocation
        val toWorld = respawnLocation.world
        if (fromWorld == toWorld) return
        latte.runLater {
            latte.loadDataFile(player, fromWorld, toWorld) {
                player.teleport(respawnLocation, PlayerTeleportEvent.TeleportCause.EXIT_BED)
            }
        }
    }

    @EventHandler
    fun onLogin(event: PlayerLoginEvent) {
        latte.runLater((latte.tps / 10).toLong()) {
            val player = event.player
            val world = player.world
            latte.loadDataFile(player, world)
            latte.runLater((latte.tps / 10).toLong()) {
                latte.update(player)
                latte.setDefaultGamemode(player, world)
            }
        }
    }

    @EventHandler
    fun onQuit(event: PlayerQuitEvent) {
        val player = event.player
        latte.saveDataFile(player, player.world)
    }
}