package co.akoot.plugins.latte.commands

import co.akoot.plugins.bluefox.api.Area
import co.akoot.plugins.bluefox.api.FoxCommand
import co.akoot.plugins.bluefox.api.Kolor
import co.akoot.plugins.bluefox.extensions.getMeta
import co.akoot.plugins.bluefox.extensions.invoke
import co.akoot.plugins.bluefox.util.Text
import co.akoot.plugins.bluefox.util.Text.Companion.titleCase
import co.akoot.plugins.latte.Latte
import org.bukkit.Location
import org.bukkit.command.CommandSender

abstract class AbstractZoneCommand(
    plugin: Latte,
    id: String,
    val zoneName: String,
    val zoneDisplayName: String = zoneName.titleCase("_").replace("_", " "),
    description: String = "",
    defaultUsage: String = "/$id",
    vararg aliases: String
) : FoxCommand(plugin, id, description, defaultUsage, *aliases) {

    override fun onTabComplete(sender: CommandSender, alias: String, args: Array<out String>): MutableList<String> {
        return when {
            args.size == 1 -> mutableListOf("cubic", "remove")
            args.size == 2 && args[0] == "remove" -> Latte.zones[zoneName]?.map { it.toString() }?.toMutableList() ?: nothing
            else -> nothing
        }
    }

    override fun onCommand(sender: CommandSender, alias: String, args: Array<out String>): Boolean {
        val player = playerCheck(sender) ?: return false

        when {
            args.getOrNull(0) == "remove" -> {
                return handleRemove(player, sender, args.getOrNull(1))
            }

            else -> {
                val pos1 = player.getMeta<Location>("tool.pos1")
                val pos2 = player.getMeta<Location>("tool.pos2")

                if (pos1 == null || pos2 == null) {
                    Text(sender) {
                        Kolor.ERROR("You have not selected a $zoneDisplayName yet! Use a ") +
                                Kolor.ERROR.accent(Latte.tool.name) +
                                Kolor.ERROR(" to select one.")
                    }
                    return false
                }

                if (pos1.world != pos2.world) {
                    Text(sender) {
                        Kolor.ERROR("You cannot create a $zoneDisplayName across worlds!")
                    }
                    return false
                }

                val area = Area(pos1, pos2, args.getOrNull(0) == "cubic")
                return if (Latte.addZone(zoneName, area)) {
                    Text(sender) { Kolor.ACCENT("Saved $zoneDisplayName!") }
                    true
                } else {
                    Text(sender) { Kolor.ERROR("Failed to save $zoneDisplayName!") }
                    false
                }
            }
        }
    }

    private fun handleRemove(player: org.bukkit.entity.Player, sender: CommandSender, removeArg: String?): Boolean {
        when (removeArg) {
            "all" -> {
                Latte.clearZones(zoneName)
                Text(sender) { Kolor.ACCENT("Removed all ${zoneDisplayName}s!") }
                return true
            }

            null -> {
                val location = player.location
                val area = Latte.zones[zoneName]?.find { it.has(location) }

                if (area == null) {
                    Text(sender) { Kolor.ERROR("You are not in a $zoneDisplayName!") }
                    return false
                }

                return finishRemoval(sender, area)
            }

            else -> {
                val area = Latte.zones[zoneName]?.find { it.toString() == removeArg }

                if (area == null) {
                    Text(sender) { Kolor.ERROR("No $zoneDisplayName found!") }
                    return false
                }

                return finishRemoval(sender, area)
            }
        }
    }

    private fun finishRemoval(sender: CommandSender, area: Area): Boolean {
        return if (Latte.removeZone(zoneName, area)) {
            Text(sender) { Kolor.ACCENT("Removed $zoneDisplayName.") }
            true
        } else {
            Text(sender) {
                Kolor.ERROR("Removed $zoneDisplayName but it will load again on restart! What a fail!")
            }
            false
        }
    }
}
