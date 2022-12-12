package top.iseason.bukkit.sakurabind.listener

import fr.xephi.authme.events.LoginEvent
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener

object LoginAuthMeListener : Listener {

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    fun onPlayerLoginEvent(event: LoginEvent) {
        BindListener.onLogin(event.player)
    }
}