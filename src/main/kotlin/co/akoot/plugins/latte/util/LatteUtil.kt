package co.akoot.plugins.latte.util

import co.akoot.plugins.bluefox.extensions.getPDC
import co.akoot.plugins.bluefox.extensions.setPDC
import co.akoot.plugins.latte.Latte.Companion.key
import net.kyori.adventure.text.format.TextDecoration
import org.bukkit.block.BlockFace
import org.bukkit.entity.Display
import org.bukkit.entity.EntityType
import org.bukkit.entity.Interaction
import org.bukkit.entity.ItemDisplay
import org.bukkit.entity.ItemFrame
import org.bukkit.entity.TextDisplay
import org.bukkit.util.Transformation
import org.joml.AxisAngle4f
import org.joml.Quaternionf
import org.joml.Vector3f
import java.util.UUID

val Number.radians: Float get() = Math.toRadians(this.toDouble()).toFloat()

fun ItemFrame.toItemDisplay(scale: Float = 1f): Interaction {
    val stack = item.clone()
    val isBlock = stack.type.isBlock
    val displayScale = (if (isBlock) 0.25f else 0.51f) * scale

    val display = world.spawn(location, ItemDisplay::class.java) {
        it.setItemStack(stack)
        it.billboard = Display.Billboard.FIXED
        it.transformation = Transformation(
            Vector3f(),
            Quaternionf().rotateAxis(180.radians, Vector3f(0f, 1f, 0f)),
            Vector3f(displayScale),
            Quaternionf()
        )
    }

    val displayName = stack.effectiveName()
        .takeIf { it.hasDecoration(TextDecoration.ITALIC) }

    val text = displayName?.let { name ->
        world.spawn(location, TextDisplay::class.java) {
            it.viewRange = 2f
            it.text(name)
            it.billboard = Display.Billboard.FIXED
            it.transformation = Transformation(
                Vector3f(
                    0f,
                    displayScale / if (isBlock) 1 else 2,
                    0f
                ),
                Quaternionf(),
                Vector3f(displayScale * if (isBlock) 2 else 1),
                Quaternionf()
            )
        }
    }

    remove()

    return world.spawn(
        location.clone().subtract(0.0, 0.125, 0.0),
        Interaction::class.java
    ) {
        it.interactionHeight = 0.2f
        it.interactionWidth = 0.2f
        it.setPDC(key("display"), display.uniqueId)
        text?.let { textEntity ->
            it.setPDC(key("text"), textEntity.uniqueId)
        }
    }

}

fun Interaction.toItemFrame(): ItemFrame? {
    val display = getPDC<UUID>(key("display"))?.let { world.getEntity(it) } as? ItemDisplay ?: return null
    val text = getPDC<UUID>(key("text"))?.let { world.getEntity(it) }

    remove()
    display.remove()
    text?.remove()

    return world.spawn(location, ItemFrame::class.java) {
        it.setItem(display.itemStack)
    }

}