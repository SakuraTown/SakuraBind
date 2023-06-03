package top.iseason.bukkit.sakurabind.hook

import cc.bukkitPlugin.pds.events.PlayerDataLoadCompleteEvent
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import top.iseason.bukkit.sakurabind.listener.SelectListener
import top.iseason.bukkittemplate.hook.BaseHook

object PlayerDataSQLHook : BaseHook("PlayerDataSQL"), org.bukkit.event.Listener {

    @EventHandler(priority = EventPriority.HIGHEST)
    fun onDataSync(event: PlayerDataLoadCompleteEvent) {
        SelectListener.noScanning.remove(event.player.uniqueId)
    }
}