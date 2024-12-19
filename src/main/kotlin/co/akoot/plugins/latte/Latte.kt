package co.akoot.plugins.latte

import co.akoot.plugins.bluefox.api.FoxPlugin
import co.akoot.plugins.latte.commands.LatteCommand

class Latte : FoxPlugin("latte") {

    override fun load() {
    }

    override fun unload() {
    }

    override fun registerCommands() {
        registerCommand(LatteCommand(this))
    }
}