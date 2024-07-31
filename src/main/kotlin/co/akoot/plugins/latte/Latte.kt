package co.akoot.plugins.latte

import org.bukkit.plugin.java.JavaPlugin

class Latte : JavaPlugin() {

    override fun onEnable() {
        logger.info("Tell me about it!")
    }

    override fun onDisable() {
        // Plugin shutdown logic
    }
}