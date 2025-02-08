package co.akoot.plugins.latte.commands

import co.akoot.plugins.bluefox.api.FoxCommand
import co.akoot.plugins.bluefox.api.FoxPlugin
import org.bukkit.command.CommandSender

class HomeCommand(plugin: FoxPlugin) : FoxCommand(
    plugin, "home", "Manage your homes!",
    aliases = arrayOf("h", "homes")
) {

    override fun onTabComplete(sender: CommandSender, alias: String, args: Array<out String>): MutableList<String> {
        return mutableListOf()
    }

    override fun onCommand(sender: CommandSender, alias: String, args: Array<out String>): Boolean {
        if (alias == "homes") {
            val subCommand = args[1]
        } else {

        }
        return false
    }
}