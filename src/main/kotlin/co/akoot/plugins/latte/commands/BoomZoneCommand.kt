package co.akoot.plugins.latte.commands

import co.akoot.plugins.latte.Latte

class BoomZoneCommand(plugin: Latte): AbstractZoneCommand(plugin, "boomzone", "boom_zone", aliases = arrayOf("tntzone", "explosionzone"))