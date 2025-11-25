package co.akoot.plugins.latte.commands

import co.akoot.plugins.bluefox.api.FoxCommand
import co.akoot.plugins.bluefox.api.Kolor
import co.akoot.plugins.bluefox.api.XYZ
import co.akoot.plugins.bluefox.extensions.sendMessage
import co.akoot.plugins.bluefox.util.Text
import co.akoot.plugins.bluefox.util.Text.Companion.invoke
import co.akoot.plugins.latte.Latte
import co.akoot.plugins.latte.extensions.WorldKeys
import org.bukkit.GameMode
import org.bukkit.World
import org.bukkit.World.Environment
import org.bukkit.WorldType
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import kotlin.random.Random

//TODO this can probably be simplified
class LatteCommand(private val latte: Latte) :
    FoxCommand(latte, "latte", aliases = arrayOf("ltp", "ltpp", "lload", "lunload")) {

    override fun onTabComplete(sender: CommandSender, alias: String, args: Array<out String>): MutableList<String> {
        return when (alias) {
            "ltp" -> onTabComplete(sender, id, arrayOf("tp") + args)
            "ltpp" -> onTabComplete(sender, id, arrayOf("tpp") + args)
            "lload" -> onTabComplete(sender, id, arrayOf("load") + args)
            "lunload" -> onTabComplete(sender, id, arrayOf("unload") + args)
            id -> when (args.size) {
                1 -> {
                    val suggestions =
                        mutableListOf("load", "unload", "list", "tp", "tpp", "delete", "config", "create", "reload", "info")
                    val arg = args[0]
                    if (arg.startsWith("delete") || arg.startsWith("load")) {
                        suggestions += "$arg-all"
                    }
                    return suggestions.permissionCheck(sender)
                }

                2 -> {
                    return when (args[0]) {
                        "load" -> latte.getWorldFolders().filterValues { !it }.keys.toMutableList()
                        "unload", "tp" -> latte.getWorldFolders().filterValues { it }.keys.toMutableList()
                        "tpp" -> getOnlinePlayerSuggestions(args)
                        "config" -> latte.getWorldFolders().keys.toMutableList()
                        else -> mutableListOf()
                    }
                }

                3 -> {
                    return when (args[0]) {
                        "load" -> Environment.entries.map { it.name.lowercase() }.toMutableList()
                        "tpp" -> {
                            val player = getPlayer(args[1]).value
                            if (player != null) latte.getWorldFolders().filterValues { !it }.keys.toMutableList()
                            else mutableListOf()
                        }
                        "config" -> {
                            if(!latte.getWorldFolders().containsKey(args[1])) return mutableListOf()
                            mutableListOf("set")
                        }
                        "create" -> GameMode.entries.map { it.name.lowercase() }.toMutableList()
                        else -> mutableListOf()
                    }
                }

                4 -> {
                    return when (args[0]) {
                        "config" -> WorldKeys.all.toMutableList()
                        "create" -> World.Environment.entries.map { it.name.lowercase() }.toMutableList()
                        else -> mutableListOf()
                    }
                }

                5 -> {
                    return when (args[0]) {
                        "config" -> {
                            return when (args[3]) {
                                in WorldKeys.booleans -> mutableListOf("true", "false")
                                WorldKeys.PARENT_WORLD -> latte.getWorldFolders().keys.toMutableList()
                                WorldKeys.GAME_MODE -> GameMode.entries.map { it.name.lowercase() }.toMutableList()
                                else -> mutableListOf()
                            }
                        }
                        "create" -> WorldType.entries.map { it.name.lowercase() }.toMutableList()
                        else -> mutableListOf()
                    }
                }

                6 -> {
                    return when (args[0]) {
                        "create" -> mutableListOf(Random.nextLong().toString())
                        else -> mutableListOf()
                    }
                }

                else -> {
                    return when (args[0]) {
                        "load-all", "delete-all" -> latte.getWorldFolders()
                            .filterKeys { it !in args }.keys.toMutableList()

                        else -> mutableListOf()
                    }
                }
            }
            else -> mutableListOf()
        }
    }

    override fun onCommand(
        sender: CommandSender,
        alias: String,
        args: Array<out String>
    ): Boolean {
        if (args.isEmpty()) return sendUsage(sender)
        return when (alias) {
            "ltp" -> onCommand(sender, id, arrayOf("tp") + args)
            "ltpp" -> onCommand(sender, id, arrayOf("tpp") + args)
            "lload" -> onCommand(sender, id, arrayOf("load") + args)
            "lunload" -> onCommand(sender, id, arrayOf("unload") + args)
            id -> {
                return when (args[0]) {
                    "info" -> {
                        val player = getPlayerSender(sender).value ?: return false
                        var message = Kolor.TEXT("You are in ") + Kolor.ACCENT(player.world.name) + "."
                        val map = WorldKeys.from(player.world).map { (key, value) -> Kolor.ACCENT(key) + Kolor.TEXT(" = ") + Kolor.ALT(value.toString()) }
                        for(entry in map) {
                            message = message + Text.newline + entry
                        }
                        sender.sendMessage(message)
                        return true
                    }
                    "load" -> latte.load(args[1], args.getOrNull(2)?.let { Environment.valueOf(it.uppercase()) }).send(sender).value
                    "unload" -> latte.unload(args[1]).send(sender).value
                    "list" -> latte.list().send(sender).value
                    "reload" -> {
                        latte.settings.reload()
                        return Result.success("Reloaded config!").send(sender).value
                    }
                    "tp" -> {
                        val player = getPlayerSender(sender).send(sender).value ?: return false
                        val worldName = args[1]
                        if (args.size == 2) {
                            latte.teleport(player, worldName).send(sender).value
                        } else if (args.size == 5) {
                            latte.teleport(player, worldName, XYZ(args.drop(1))).send(sender).value
                        }
                        false
                    }

                    "tpp" -> {
                        val player = getPlayer(args[1]).send(sender).value ?: return false
                        val worldName = args[2]
                        if (args.size == 3) {
                            latte.teleport(player, worldName).send(sender).value
                        } else if (args.size == 6) {
                            latte.teleport(player, worldName, XYZ(args.drop(2))).send(sender).value
                        }
                        false
                    }

                    "delete" -> {
                        latte.delete(args[1]).send(sender).value
                    }

                    "load-all" -> {
                        var success = true
                        for (name in args.drop(1)) {
                            if (!latte.load(name).send(sender).value) success = false
                        }
                        success
                    }

                    "delete-all" -> {
                        var success = true
                        for (name in args.drop(1)) {
                            if (!latte.delete(name).send(sender).value) success = false
                        }
                        success
                    }

                    "config" -> {
                        if (args.size < 4) return sendUsage(sender)
                        val worldName = args[1]
                        val config =
                            latte.getWorldConfig(worldName) ?: return Result.fail("No config file found!").value
                        val key = args[3]
                        when(args[2]) {
                            "set" -> {
                                if (args.size < 5) return sendUsage(sender)
                                val desiredValue = args[4]
                                val value = when (key) {
                                    in WorldKeys.booleans -> desiredValue.toBoolean()
                                    in WorldKeys.enums -> desiredValue.uppercase()
                                    else -> desiredValue
                                }
                                config.set(key, value)
                                Result.success(
                                    Kolor.TEXT("Set ") +
                                            Kolor.ACCENT(key) +
                                            Kolor.TEXT(" to ") +
                                            Kolor.ALT(value.toString()) +
                                            Kolor.TEXT(" for ") +
                                            Kolor.ACCENT(worldName)
                                ).send(sender).value
                            }
                            else -> sendUsage(sender)
                        }

                    }

                    "create" -> {
                        if (args.size < 2) return sendUsage(sender)
                        val name = args[1]
                        val gameMode = args.getOrNull(2)?.let { GameMode.valueOf(it.uppercase()) } ?: GameMode.SURVIVAL
                        val environment =
                            args.getOrNull(3)?.let { Environment.valueOf(it.uppercase()) } ?: Environment.NORMAL
                        val type = args.getOrNull(4)?.let { WorldType.valueOf(it.uppercase()) } ?: WorldType.NORMAL
                        val seed = args.getOrNull(5)?.toLong()
                        latte.createWorld(name, gameMode, environment, seed, type).send(sender).value != null
                    }

                    else -> sendUsage(sender)
                }
            }
            else -> sendUsage(sender)
        }
    }
}
