package top.iseason.bukkit.sakurabind.listener

import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerLoginEvent

object LoginListener : Listener {
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    fun onPlayerLoginEvent(event: PlayerLoginEvent) {
        ItemListener.onLogin(event.player)
    }
}