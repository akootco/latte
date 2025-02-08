package co.akoot.plugins.latte.commands

import co.akoot.plugins.bluefox.api.FoxCommand
import co.akoot.plugins.bluefox.api.XYZ
import co.akoot.plugins.bluefox.util.Text.Companion.accented
import co.akoot.plugins.bluefox.util.Text.Companion.invoke
import co.akoot.plugins.latte.Latte
import org.bukkit.GameMode
import org.bukkit.World
import org.bukkit.World.Environment
import org.bukkit.WorldType
import org.bukkit.command.CommandSender
import kotlin.random.Random

//TODO this can probably be simplified
class LatteCommand(private val latte: Latte) :
    FoxCommand(latte, "latte", aliases = arrayOf("l", "ltp", "ltpp", "lload", "lunload")) {

    override fun onTabComplete(sender: CommandSender, alias: String, args: Array<out String>): MutableList<String> {
        return when (alias) {
            "latte", "l" -> {
                when (args.size) {
                    1 -> {
                        val suggestions =
                            mutableListOf("load", "unload", "list", "tp", "tpp", "delete", "set", "create")
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
                            "set" -> latte.getWorldFolders().keys.toMutableList()
                            else -> mutableListOf()
                        }
                    }

                    3 -> {
                        return when (args[0]) {
                            "tpp" -> {
                                val player = getPlayer(args[1]).value
                                if (player != null) latte.getWorldFolders().filterValues { !it }.keys.toMutableList()
                                else mutableListOf()
                            }

                            "set" -> mutableListOf("autoload", "parent", "gamemode")
                            "create" -> GameMode.entries.map { it.name.lowercase() }.toMutableList()
                            else -> mutableListOf()
                        }
                    }

                    4 -> {
                        return when (args[0]) {
                            "set" -> {
                                return when (args[2]) {
                                    "autoload" -> mutableListOf("true", "false")
                                    "parent" -> latte.getWorldFolders().keys.toMutableList()
                                    "gamemode" -> GameMode.entries.map { it.name.lowercase() }.toMutableList()
                                    else -> mutableListOf()
                                }
                            }

                            "create" -> World.Environment.entries.map { it.name.lowercase() }.toMutableList()

                            else -> mutableListOf()
                        }
                    }

                    5 -> {
                        return when (args[0]) {
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
            }

            "ltp" -> {
                return when (args.size) {
                    1 -> latte.getWorldFolders().filterValues { it }.keys.toMutableList()
                    else -> mutableListOf()
                }
            }

            "ltpp" -> {
                return when (args.size) {
                    1 -> getOnlinePlayerSuggestions(args)
                    2 -> {
                        val player = getPlayer(args[0]).value
                        if (player != null) latte.getWorldFolders().filterValues { it }.keys.toMutableList()
                        else mutableListOf()
                    }

                    else -> mutableListOf()
                }
            }

            "lload" -> {
                return when (args.size) {
                    1 -> latte.getWorldFolders().filterValues { !it }.keys.toMutableList()
                    else -> mutableListOf()
                }
            }

            "lunload" -> {
                return when (args.size) {
                    1 -> latte.getWorldFolders().filterValues { it }.keys.toMutableList()
                    else -> mutableListOf()
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
            "lload" -> latte.load(args[0]).send(sender).value
            "lunload" -> latte.unload(args[0]).send(sender).value

            "ltp" -> {
                val player = getPlayerSender(sender).send(sender).value ?: return false
                val worldName = args[0]
                if (args.size == 1) {
                    latte.teleport(player, worldName).send(sender).value
                } else if (args.size == 4) {
                    latte.teleport(player, worldName, XYZ(args.drop(1))).send(sender).value
                }
                false
            }

            "ltpp" -> {
                val player = getPlayer(args[0]).send(sender).value ?: return false
                val worldName = args[1]
                if (args.size == 2) {
                    latte.teleport(player, worldName).send(sender).value
                } else if (args.size == 5) {
                    latte.teleport(player, worldName, XYZ(args.drop(2))).send(sender).value
                }
                false
            }

            "latte", "l" -> {
                return when (args[0]) {
                    "load" -> latte.load(args[1]).send(sender).value
                    "unload" -> latte.unload(args[1]).send(sender).value
                    "list" -> latte.list().send(sender).value

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

                    "set" -> {
                        if (args.size < 4) return sendUsage(sender)
                        val worldName = args[1]
                        val config =
                            latte.getWorldConfig(worldName) ?: return Result.fail("No config file found!").value
                        val key = args[2]
                        val value = if (key in setOf("autoload")) args[3].toBoolean() else args[3]
                        config.set(key, value)
                        Result.success(
                            "Set "() + key.accented() + " to " + value.toString()
                                .accented() + " for " + worldName.accented()
                        ).send(sender).value
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

                    "debug" -> {
                        val player = getPlayerSender(sender).send(sender).value ?: return false
                        Result.success("You are in "() + player.world.toString().accented()).send(sender).value
                    }

                    else -> sendUsage(sender)
                }
            }

            else -> {
                sendUsage(sender)
            }
        }
    }
}
