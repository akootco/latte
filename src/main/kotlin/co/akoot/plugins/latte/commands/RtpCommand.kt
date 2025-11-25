package co.akoot.plugins.latte.commands

import co.akoot.plugins.bluefox.api.CatCommand
import co.akoot.plugins.bluefox.api.Kolor
import co.akoot.plugins.bluefox.extensions.sendMessage
import co.akoot.plugins.bluefox.extensions.text
import co.akoot.plugins.bluefox.util.Text
import co.akoot.plugins.latte.Latte
import co.akoot.plugins.latte.extensions.rtp
import com.mojang.brigadier.context.CommandContext
import io.papermc.paper.command.brigadier.CommandSourceStack
import org.bukkit.Location
import org.bukkit.World
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

class RtpCommand(plugin: Latte): CatCommand(plugin, "rtp") {
    init {
        noargs {
            permissionCheck(it) ?:return@noargs false
            val player = getPlayerSender(it) ?: return@noargs false
            rtp(player).second
        }
        then(subcommand("pls") {
            permissionCheck(it) ?:return@subcommand false
            val player = getPlayerSender(it) ?: return@subcommand false
            player.sendMessage(Kolor.TEXT("Only because you asked nicely..."))
            rtp(player).second
        })
        then(int("X radius", 0) {
            permissionCheck(it) ?:return@int false
            val player = getPlayerSender(it) ?: return@int false
            val radiusX = getInt(it, "X radius")
            rtp(player, radiusX).second
        }.then(int("Z radius", 0) {
            permissionCheck(it) ?:return@int false
            val player = getPlayerSender(it) ?: return@int false
            val radiusX = getInt(it, "X radius")
            val radiusZ = getInt(it, "Z radius")
            rtp(player, radiusX, radiusZ).second
        }.then(int("Y radius", 0, 192) {
            permissionCheck(it, "y") ?:return@int false
            val player = getPlayerSender(it) ?: return@int false
            val radiusX = getInt(it, "X radius")
            val radiusZ = getInt(it, "Z radius")
            val radiusY = getInt(it, "Y radius")
            rtp(player, radiusX, radiusZ, radiusY).second
        }.then(world() {
            permissionCheck(it, "world") ?:return@world false
            val player = getPlayerSender(it) ?: return@world false
            val radiusX = getInt(it, "X radius")
            val radiusZ = getInt(it, "Z radius")
            val radiusY = getInt(it, "Y radius")
            val world = getWorld(it)
            rtp(player, radiusX, radiusZ, radiusY, world).second
        }.then(player {
            permissionCheck(it, "other") ?:return@player false
            val player = getPlayer(it)
            val radiusX = getInt(it, "X radius")
            val radiusZ = getInt(it, "Z radius")
            val radiusY = getInt(it, "Y radius")
            val world = getWorld(it)
            val location = rtp(player, radiusX, radiusZ, radiusY, world).first
            getSender(it).sendMessage(Kolor.TEXT("Teleported ") + player.displayName().color(Kolor.PLAYER.get()) + Kolor.TEXT(" to ") + location.text + ".")
            true
        })))))
    }
    
    fun rtp(player: Player, radiusX: Int? = null, radiusZ: Int? = radiusX, radiusY: Int? = null, world: World = player.world): Pair<Location, Boolean> {
        val location = player.rtp(world, radiusX?.toDouble(), radiusZ?.toDouble(), radiusY?.toDouble())
        player.sendMessage(Text("Teleported to ") + location.text + ".")
        return location to true
    }
}