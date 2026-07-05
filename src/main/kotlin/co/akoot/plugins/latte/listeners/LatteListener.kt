package co.akoot.plugins.latte.listeners

import co.akoot.plugins.bluefox.api.Kolor
import co.akoot.plugins.bluefox.extensions.invoke
import co.akoot.plugins.bluefox.extensions.setMeta
import co.akoot.plugins.bluefox.extensions.text
import co.akoot.plugins.bluefox.util.Text
import co.akoot.plugins.latte.Latte
import co.akoot.plugins.latte.extensions.*
import com.destroystokyo.paper.event.player.PlayerAdvancementCriterionGrantEvent
import org.bukkit.Location
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.player.AsyncPlayerChatEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.event.player.PlayerRespawnEvent
import org.bukkit.event.player.PlayerStatisticIncrementEvent

class LatteListener(plugin: Latte) : Listener {

    //todo: per-world chat?
    @EventHandler(priority = EventPriority.HIGHEST)
    fun onAsyncChat(event: AsyncPlayerChatEvent) {
        if (event.isCancelled) return

        val world = event.player.world
        event.recipients.removeAll {
            val sameWorld = world == it.world
            val relatedWorld = it.world.isRelated(world)
            !it.globalChatEnabled && (world.isSeparateChat || it.world.isSeparateChat) && !sameWorld && !relatedWorld
        }
    }

    @EventHandler
    fun onPlayerAdvancementCriterionGrant(event: PlayerAdvancementCriterionGrantEvent) {
        if (!event.player.world.rootWorld.advancementsAllowed) {
            event.isCancelled = true
        }
    }

    @EventHandler
    fun onPlayerStatisticIncrement(event: PlayerStatisticIncrementEvent) {
        if (!event.player.world.rootWorld.statsAllowed) {
            event.isCancelled = true
        }
    }

    @EventHandler
    fun onPlayerRespawn(event: PlayerRespawnEvent) {
        val world = event.respawnLocation.world
        if (!world.isAnarchy) return
        if (!event.isMissingRespawnBlock) return
        event.respawnLocation = world.randomSafeLocation()
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    fun onInteract(event: PlayerInteractEvent) {
        if (!Latte.toolCheck(event.player)) return
        val player = event.player
        val action = event.action
        if (action == Action.LEFT_CLICK_BLOCK) {
            event.clickedBlock?.location?.let { loc ->
                if (player.isSneaking) {
                    sendZoneInfo(player, loc)
                } else {
                    player.setMeta("tool.pos1", loc)
                    Text(player) {
                        Kolor.ACCENT("Pos1") + Kolor.ALT(" set to ") + loc.text
                    }
                }
                event.isCancelled = true
            }
        } else if (action == Action.RIGHT_CLICK_BLOCK) {
            event.clickedBlock?.location?.let { loc ->
                if (player.isSneaking) {
                    sendZoneInfo(player, loc)
                } else {
                    player.setMeta("tool.pos2", loc)
                    Text(player) {
                        Kolor.ACCENT("Pos2") + Kolor.ALT(" set to ") + loc.text
                    }
                }
                event.isCancelled = true
            }
        }
    }

    fun sendZoneInfo(player: Player, loc: Location) {
        val zones = mutableListOf<String>()
        for (zoneName in Latte.zones.keys) {
            if (Latte.isInZone(zoneName, loc)) {
                zones.add(zoneName)
            }
        }
        if (!zones.isEmpty()) {
            Text(player) {
                Kolor.ACCENT("Zones: ") + Kolor.TEXT("[") + Text.list(
                    zones,
                    ", ",
                    Kolor.ALT
                ) + Kolor.TEXT("]")
            }
        } else {
            Text(player) {
                Kolor.ACCENT.number("Free real estate!")
            }
        }
    }
}