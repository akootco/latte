package co.akoot.plugins.latte.commands

import co.akoot.plugins.bluefox.api.Area
import co.akoot.plugins.bluefox.api.FoxCommand
import co.akoot.plugins.bluefox.api.Kolor
import co.akoot.plugins.bluefox.extensions.getMeta
import co.akoot.plugins.bluefox.extensions.invoke
import co.akoot.plugins.bluefox.util.Text
import co.akoot.plugins.latte.Latte
import org.bukkit.Location
import org.bukkit.command.CommandSender

class SafeZoneCommand(plugin: Latte) : FoxCommand(plugin, "safezone", "sz") {
    override fun onTabComplete(
        sender: CommandSender,
        alias: String,
        args: Array<out String>
    ): MutableList<String> {
        if (args.size == 1) return mutableListOf("cubic", "remove")
        else if (args.size == 2 && args[0] == "remove") {
            return Latte.Companion.safeZones.map { it.toString() }.toMutableList()
        }
        return mutableListOf()
    }

    override fun onCommand(
        sender: CommandSender,
        alias: String,
        args: Array<out String>
    ): Boolean {
        val player = playerCheck(sender) ?: return false
        val cubic = args.getOrNull(0) == "cubic"
        val remove = args.getOrNull(1)
        if (remove != null) {
            if (remove == "all") {
                Latte.Companion.clearSafeZones()
                Text(sender) {
                    Kolor.ACCENT("Removed all safe zones!")
                }
                return true
            }
            val area = Latte.Companion.safeZones.find { it.toString() == remove }
            if (area == null) {
                Text(sender) {
                    Kolor.ERROR("No safe area found!")
                }
                return false
            }
            if (Latte.Companion.removeSafeZone(area)) {
                Text(sender) {
                    Kolor.ACCENT("Removed safe zone!")
                }
                return true
            }
            Text(sender) {
                Kolor.ERROR("Removed safe zone but it will load again on restart! What a fail!")
            }
            return false
        } else if (args.getOrNull(0) == "remove") {
            val location = player.location
            location.world.persistentDataContainer
            val area = Latte.Companion.safeZones.find { it.has(location) }
            if (area == null) {
                Text(sender) {
                    Kolor.ERROR("You are not in a safe zone!")
                }
                return false
            }
            if (Latte.Companion.removeSafeZone(area)) {
                Text(sender) {
                    Kolor.ACCENT("Removed safe zone!")
                }
                return true
            }
            Text(sender) {
                Kolor.ERROR("Removed safe zone but it will load again on restart! What a fail!")
            }
            return false
        }
        val pos1 = player.getMeta<Location>("tool.pos1")
        val pos2 = player.getMeta<Location>("tool.pos2")
        if (pos1 == null || pos2 == null) {
            Text(sender) {
                Kolor.ERROR("You have not selected a safe area yet! Use a ") + Kolor.Companion.ERROR.accent(Latte.Companion.tool.name) + Kolor.ERROR(
                    " to select one."
                )
            }
            return false
        }
        if (pos1.world != pos2.world) {
            Text(sender) {
                Kolor.ERROR("You cannot create a safe area across worlds!")
            }
            return false
        }
        val area = Area(pos1, pos2, cubic)
        if (Latte.Companion.addSafeZone(area)) {
            Text(sender) {
                Kolor.ACCENT("Saved safe zone!")
            }
            return true
        }
        Text(sender) {
            Kolor.ERROR("Failed to save safe zone!")
        }
        return false
    }

}