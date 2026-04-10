package co.akoot.plugins.latte.listeners

import co.akoot.plugins.bluefox.api.Kolor
import co.akoot.plugins.bluefox.extensions.getPDC
import co.akoot.plugins.bluefox.extensions.invoke
import co.akoot.plugins.bluefox.extensions.setMeta
import co.akoot.plugins.bluefox.extensions.setPDC
import co.akoot.plugins.bluefox.extensions.text
import co.akoot.plugins.bluefox.util.Text
import co.akoot.plugins.latte.Latte
import co.akoot.plugins.latte.Latte.Companion.key
import co.akoot.plugins.latte.extensions.advancementsAllowed
import co.akoot.plugins.latte.extensions.globalChatEnabled
import co.akoot.plugins.latte.extensions.isAnarchy
import co.akoot.plugins.latte.extensions.isRelated
import co.akoot.plugins.latte.extensions.isSeparateChat
import co.akoot.plugins.latte.extensions.randomSafeLocation
import co.akoot.plugins.latte.extensions.rootWorld
import co.akoot.plugins.latte.extensions.statsAllowed
import com.destroystokyo.paper.event.player.PlayerAdvancementCriterionGrantEvent
import io.papermc.paper.event.player.PlayerItemFrameChangeEvent
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.entity.EntityType
import org.bukkit.entity.Interaction
import org.bukkit.entity.ItemDisplay
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.player.AsyncPlayerChatEvent
import org.bukkit.event.player.PlayerInteractEntityEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.event.player.PlayerRespawnEvent
import org.bukkit.event.player.PlayerStatisticIncrementEvent
import java.util.UUID

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
        if(!world.isAnarchy) return
        if(!event.isMissingRespawnBlock) return
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

    @EventHandler
    fun PlayerInteractEntityEvent.interact() {
        if (isCancelled) return
        val owner = rightClicked.getPDC<UUID>(key("frame_display")) ?: return
        if (!player.hasPermission("latte.frame.cleanup") || owner != player.uniqueId) return
        if (player.inventory.itemInMainHand.type != Material.SHEARS) return

        rightClicked.apply {
            passengers
            .filterIsInstance<ItemDisplay>()
            .forEach { it.remove() }
            remove()
        }
    }

    @EventHandler
    fun PlayerItemFrameChangeEvent.onFrameChange() {
        if (isCancelled) return
        if (action != PlayerItemFrameChangeEvent.ItemFrameChangeAction.REMOVE) return

        val loc = itemFrame.location

        if (!Latte.isInZone("server_safe_zone", loc)) return
        if (player.inventory.itemInMainHand.type != Material.SHEARS) return

        val item = itemFrame.item
        val display = loc.world.spawnEntity(loc, EntityType.ITEM_DISPLAY) as ItemDisplay
        val scale = if (item.type.isBlock) 0.25f else 0.6f
        val interaction = loc.world.spawnEntity(loc.add(0.0,-.15,0.0), EntityType.INTERACTION) as Interaction

        interaction.apply {
            interactionHeight = 0.2f
            interactionWidth = 0.2f
            setPDC(key("frame_display"), player.uniqueId)
            addPassenger(display.apply {
                setItemStack(item)
                transformation = transformation.apply { this.scale.set(scale) }
            })
        }

        itemFrame.remove()
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