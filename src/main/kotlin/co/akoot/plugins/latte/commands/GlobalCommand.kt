package co.akoot.plugins.latte.commands

import co.akoot.plugins.bluefox.api.CatCommand
import co.akoot.plugins.bluefox.api.Kolor
import co.akoot.plugins.bluefox.extensions.sendMessage
import co.akoot.plugins.latte.Latte
import co.akoot.plugins.latte.extensions.globalChatEnabled
import com.mojang.brigadier.context.CommandContext
import io.papermc.paper.command.brigadier.CommandSourceStack
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

class GlobalCommand(plugin: Latte): CatCommand(plugin, "global") {

    init {
        noargs {
            permissionCheck(it, "set") ?: return@noargs false
            val player = getPlayerSender(it) ?: return@noargs false
            setGlobalChat(player, player, !player.globalChatEnabled)
        }
        then(boolean {
            permissionCheck(it, "set") ?: return@boolean false
            val player = getPlayerSender(it) ?: return@boolean false
            setGlobalChat(player, player, getBoolean(it))
        }.then(player {
            permissionCheck(it, "set.other") ?: return@player false
            val sender = getSender(it)
            val player = getPlayer(it)
            setGlobalChat(sender, player, getBoolean(it))
        }))
        then(player {
            permissionCheck(it, "get.other") ?: return@player false
            val sender = getSender(it)
            val player = getPlayer(it)
            setGlobalChat(sender, player, player.globalChatEnabled)
        }.then(boolean {
            permissionCheck(it, "set.other") ?: return@boolean false
            val sender = getSender(it)
            val player = getPlayer(it)
            setGlobalChat(sender, player, getBoolean(it))
        }))
    }

    fun setGlobalChat(sender: CommandSender, target: Player, enable: Boolean): Boolean {
        if(target.globalChatEnabled != enable) target.globalChatEnabled = enable
        target.sendMessage(Kolor.TEXT("Global chat ") + Kolor.ACCENT(if(enable) "enabled" else "disabled") + Kolor.TEXT("."))
        if(sender == target) return true
        sender.sendMessage(Kolor.TEXT("Global chat ") + Kolor.ACCENT(if(enable) "enabled" else "disabled") + Kolor.TEXT(" for ") + target.displayName().color(Kolor.PLAYER.get()))
        return true
    }
}