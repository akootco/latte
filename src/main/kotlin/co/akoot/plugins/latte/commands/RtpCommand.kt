package co.akoot.plugins.latte.commands

import co.akoot.plugins.bluefox.api.CatCommand
import co.akoot.plugins.bluefox.api.Kolor
import co.akoot.plugins.bluefox.extensions.invoke
import co.akoot.plugins.bluefox.extensions.sendActionBar
import co.akoot.plugins.bluefox.extensions.sendMessage
import co.akoot.plugins.bluefox.extensions.text
import co.akoot.plugins.bluefox.util.Text
import co.akoot.plugins.latte.Latte
import co.akoot.plugins.latte.extensions.globalChatEnabled
import co.akoot.plugins.latte.extensions.rtp
import co.akoot.plugins.latte.extensions.rtpCooldown
import com.mojang.brigadier.context.CommandContext
import io.papermc.paper.command.brigadier.CommandSourceStack
import net.kyori.adventure.text.Component
import org.bukkit.GameMode
import org.bukkit.Location
import org.bukkit.World
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

class RtpCommand(plugin: Latte): CatCommand(plugin, "rtp") {
    init {
        noargs {
            permissionCheck(it) ?:return@noargs false
            val player = getPlayerSender(it) ?: return@noargs false
            cooldownCheck(player) ?: return@noargs false
            rtp(player).second
        }
        then(subcommand("pls") {
            permissionCheck(it) ?:return@subcommand false
            val player = getPlayerSender(it) ?: return@subcommand false
            cooldownCheck(player) ?: return@subcommand false
            rtp(player)
            player.sendActionBar(Kolor.ACCENT("Only because you asked nicely..."))
            true
        })
        then(int("X radius", 0) {
            permissionCheck(it) ?:return@int false
            val player = getPlayerSender(it) ?: return@int false
            cooldownCheck(player) ?: return@int false
            val radiusX = getInt(it, "X radius")
            rtp(player, radiusX).second
        }.then(int("Z radius", 0) {
            permissionCheck(it) ?:return@int false
            val player = getPlayerSender(it) ?: return@int false
            cooldownCheck(player) ?: return@int false
            val radiusX = getInt(it, "X radius")
            val radiusZ = getInt(it, "Z radius")
            rtp(player, radiusX, radiusZ).second
        }.then(int("Y radius", 0, 192) {
            permissionCheck(it, "y") ?:return@int false
            val player = getPlayerSender(it) ?: return@int false
            cooldownCheck(player) ?: return@int false
            val radiusX = getInt(it, "X radius")
            val radiusZ = getInt(it, "Z radius")
            val radiusY = getInt(it, "Y radius")
            rtp(player, radiusX, radiusZ, radiusY).second
        }.then(world() {
            permissionCheck(it, "world") ?:return@world false
            val player = getPlayerSender(it) ?: return@world false
            cooldownCheck(player) ?: return@world false
            val radiusX = getInt(it, "X radius")
            val radiusZ = getInt(it, "Z radius")
            val radiusY = getInt(it, "Y radius")
            val world = getWorld(it)
            rtp(player, radiusX, radiusZ, radiusY, world).second
        }.then(player {
            permissionCheck(it, "other") ?:return@player false
            val player = getPlayer(it)
            cooldownCheck(player) ?: return@player false
            val radiusX = getInt(it, "X radius")
            val radiusZ = getInt(it, "Z radius")
            val radiusY = getInt(it, "Y radius")
            val world = getWorld(it)
            val location = rtp(player, radiusX, radiusZ, radiusY, world).first
            getSender(it).sendMessage(Kolor.TEXT("Teleported ") + player.displayName().color(Kolor.PLAYER.get()) + Kolor.TEXT(" to ") + location.text + ".")
            true
        })))))
    }

    fun cooldownCheck(player: Player): Boolean? {
        val cooldown = player.rtpCooldown
        val now = System.currentTimeMillis()
        if(cooldown > now) {
            val timeLeft = cooldown - now
            player.sendMessage(Kolor.WARNING("You must wait ") + Kolor.WARNING.number(String.format("%.2f seconds", timeLeft / 1000.0)) + Kolor.WARNING(" before using /$id again."))
            return null
        }
        return true
    }
    
    fun rtp(player: Player, radiusX: Int? = null, radiusZ: Int? = radiusX, radiusY: Int? = null, world: World = player.world): Pair<Location, Boolean> {
        player.sendActionBar(Text("Finding a ") + Kolor.ACCENT("random location") + " just for you...")
        val location = player.rtp(world, radiusX?.toDouble(), radiusZ?.toDouble(), radiusY?.toDouble())
        player.sendMessage(Text("Teleported to ") + location.text + ".")
        player.sendActionBar(Component.empty())
        player.rtpCooldown = System.currentTimeMillis() + (plugin.settings.getInt("rtp.cooldown.min") ?: 10000) + (player.server.onlinePlayers.size * (plugin.settings.getInt("rtp.cooldown.per_player") ?: 1000))
        return location to true
    }
}