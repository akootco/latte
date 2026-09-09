package co.akoot.plugins.latte.commands

import co.akoot.plugins.bluefox.api.CatCommand
import co.akoot.plugins.bluefox.extensions.getMeta
import co.akoot.plugins.bluefox.extensions.getPDC
import co.akoot.plugins.bluefox.extensions.setMeta
import co.akoot.plugins.bluefox.util.Color
import co.akoot.plugins.bluefox.util.Text.Companion.asString
import co.akoot.plugins.bluefox.util.primary
import co.akoot.plugins.bluefox.util.secondary
import co.akoot.plugins.bluefox.util.sendText
import co.akoot.plugins.bluefox.util.sendWarning
import co.akoot.plugins.latte.Latte
import co.akoot.plugins.latte.Latte.Companion.key
import co.akoot.plugins.latte.util.toItemDisplay
import co.akoot.plugins.latte.util.toItemFrame
import io.papermc.paper.event.player.PlayerItemFrameChangeEvent
import org.bukkit.Material
import org.bukkit.entity.EntityType
import org.bukkit.entity.Interaction
import org.bukkit.entity.ItemDisplay
import org.bukkit.entity.ItemFrame
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerInteractEntityEvent
import org.bukkit.inventory.ItemStack
import java.util.UUID

private enum class Mode(val id: String) {
    ITEM_FRAME("itemFrame")
}

object OptimizeCommand: CatCommand(Latte.instance, "optimize", "Optimize the world!", onCommand = {

    fun Player.optimize(frame: ItemFrame, scale: Float = 1f): Boolean {
        if(frame.item.type == Material.AIR) return false
        frame.toItemDisplay(scale)
        return sendText("Optimized this ", frame.item.effectiveName().color(Color.Primary), "!")
    }

    fun Player.unOptimize(interaction: Interaction): Boolean {
        val frame = interaction.toItemFrame() ?: return false
        return sendText("Unoptimized this ", frame.item.effectiveName().color(Color.Primary), ".")
    }

    fun Player.handle(mode: Mode, scale: Float = 1f): Boolean {
        when (mode) {
            Mode.ITEM_FRAME -> {
                val entity = rayTraceEntities(3)?.hitEntity ?: return false
                when (entity.type) {
                    EntityType.ITEM_FRAME -> {
                        val frame = entity as ItemFrame
                        optimize(frame, scale)
                    }
                    EntityType.INTERACTION -> {
                        val interaction = entity as Interaction
                        unOptimize(interaction)

                    }
                    else -> {
                        return sendWarning("Look at an item frame!")
                    }
                }
            }
        }
        return true
    }

    then {
        subcommand(Mode.ITEM_FRAME.id) {
            val player = it.playerSender ?: return@subcommand false
            player.handle(Mode.ITEM_FRAME)
        } then {
            float("scale", -2f, 2f) {
                val player = it.playerSender ?: return@float false
                player.handle(Mode.ITEM_FRAME, it.float("scale"))
            }
        }
    }
}), Listener {

    @EventHandler
    fun PlayerInteractEntityEvent.onInteract() {
        if (isCancelled) return
        if(rightClicked.type != EntityType.INTERACTION) return
        player.inventory.itemInMainHand.takeIf { it.type == Material.SHEARS } ?: return

        val interaction = rightClicked as? Interaction ?: return
        interaction.toItemFrame()
    }

    @EventHandler
    fun PlayerItemFrameChangeEvent.onFrameChange() {
        if (isCancelled) return
        if (action != PlayerItemFrameChangeEvent.ItemFrameChangeAction.REMOVE) return
        val shears = player.inventory.itemInMainHand.takeIf { it.type == Material.SHEARS } ?: return
        val scale = shears.effectiveName().asString().toFloatOrNull()?.coerceIn(-2f, 2f)
        itemFrame.toItemDisplay(scale ?: 1f)
        isCancelled = true
    }
}