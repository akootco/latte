package co.akoot.plugins.latte.listeners

import co.akoot.plugins.bluefox.util.async
import co.akoot.plugins.bluefox.util.sync
import co.akoot.plugins.latte.Latte
import co.akoot.plugins.latte.extensions.advancementsAllowed
import co.akoot.plugins.latte.extensions.applySettings
import co.akoot.plugins.latte.extensions.getRelatedWorld
import co.akoot.plugins.latte.extensions.isRelated
import co.akoot.plugins.latte.extensions.latteWorld
import co.akoot.plugins.latte.extensions.loadData
import co.akoot.plugins.latte.extensions.rootWorld
import co.akoot.plugins.latte.extensions.saveData
import co.akoot.plugins.latte.extensions.statsAllowed
import com.destroystokyo.paper.event.player.PlayerAdvancementCriterionGrantEvent
import org.bukkit.Location
import org.bukkit.World
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityTeleportEvent
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerPortalEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.event.player.PlayerRespawnEvent
import org.bukkit.event.player.PlayerStatisticIncrementEvent
import org.bukkit.event.player.PlayerTeleportEvent

class MultiWorldListener(private val latte: Latte) : Listener {

    @EventHandler(priority = EventPriority.HIGHEST)
    fun onEntityTeleport(event: EntityTeleportEvent) {
        if (event.isCancelled) return

        val toLocation = event.to ?: return
        val fromWorld = event.from.world
        val toWorld = toLocation.world ?: return

        // If worlds are the same, no need to check further
        if (fromWorld == toWorld) return

        // Cancel teleport if worlds are unrelated
        if (!fromWorld.isRelated(toWorld)) {
            event.isCancelled = true
        }
    }


    @EventHandler
    fun onPlayerPortal(event: PlayerPortalEvent) {
        if (event.isCancelled) return
        var targetEnvironment = when (event.cause) {
            PlayerTeleportEvent.TeleportCause.NETHER_PORTAL -> World.Environment.NETHER
            PlayerTeleportEvent.TeleportCause.END_PORTAL -> World.Environment.THE_END
            else -> return
        }

        val fromWorld = event.from.world
        if (fromWorld.environment == World.Environment.NETHER || fromWorld.environment == World.Environment.THE_END) {
            targetEnvironment = World.Environment.NORMAL
        }

        // Set new destination
        event.to.world = fromWorld.getRelatedWorld(targetEnvironment) ?: run {
            event.isCancelled = true
            return
        }

        // Allow portal creation only for Nether portals
        if (event.cause == PlayerTeleportEvent.TeleportCause.NETHER_PORTAL) {
            event.canCreatePortal = true
        }
    }


    @EventHandler(priority = EventPriority.HIGHEST)
    fun onPlayerTeleport(event: PlayerTeleportEvent) {
        if (event.isCancelled) return

        // Ignore these teleport causes
        if (event.cause in setOf(
                PlayerTeleportEvent.TeleportCause.EXIT_BED,
                PlayerTeleportEvent.TeleportCause.DISMOUNT,
                PlayerTeleportEvent.TeleportCause.CHORUS_FRUIT,
                PlayerTeleportEvent.TeleportCause.END_GATEWAY
            )
        ) return

        val fromWorld = event.from.world
        val toLocation = event.to
        val toWorld = toLocation.world ?: return

        if (fromWorld == toWorld || fromWorld.isRelated(toWorld)) return

        val player = event.player

        // Allow Nether & End portal teleports while applying world settings
        if (event.cause in setOf(
                PlayerTeleportEvent.TeleportCause.NETHER_PORTAL,
                PlayerTeleportEvent.TeleportCause.END_PORTAL
            )
        ) {
            player.applySettings(toWorld)
            return
        }

        // Cancel & manually handle teleportation for unrelated worlds
        event.isCancelled = true
        teleport(player, fromWorld, toWorld, toLocation)
    }


    @EventHandler(priority = EventPriority.LOW)
    fun onPlayerRespawn(event: PlayerRespawnEvent) {
        val player = event.player
        val fromWorld = player.world
        val respawnLocation = event.respawnLocation
        val toWorld = respawnLocation.world ?: return

        // No need to reassign event.respawnLocation, it's already set
        if (fromWorld == toWorld || fromWorld.isRelated(toWorld)) return

        // Ensure teleportation happens smoothly
        teleport(player, fromWorld, toWorld, respawnLocation)
    }


    private fun teleport(player: Player, fromWorld: World, toWorld: World, location: Location) {
        async {
            player.saveData(fromWorld)
            player.loadData(toWorld)
            sync {
                player.loadData()
                player.applySettings(toWorld)

                // Do not handle teleport event again
                player.teleport(location, PlayerTeleportEvent.TeleportCause.EXIT_BED)
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    fun onPlayerJoin(event: PlayerJoinEvent) {
        val player = event.player

        val latteWorld = player.latteWorld
        if (latteWorld != null && player.server.getWorld(latteWorld) == null) {
            async {
                player.loadData(player.world) // Load data asynchronously
                sync {
                    player.loadData()
                    player.applySettings(player.world)
                }
            }
            return
        }
        player.applySettings(player.world)
    }


    @EventHandler
    fun onPlayerQuit(event: PlayerQuitEvent) {
        event.player.apply { saveData(world) }
    }
}