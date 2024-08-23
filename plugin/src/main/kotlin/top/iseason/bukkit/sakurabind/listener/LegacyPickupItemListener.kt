package top.iseason.bukkit.sakurabind.listener

import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerPickupItemEvent
import top.iseason.bukkit.sakurabind.config.Config

/*
* 1.12以下的版本检测物品捡起
* */
object LegacyPickupItemListener : Listener {

    /**
     * 捡起检测
     */
    @EventHandler(ignoreCancelled = true)
    fun onPlayerPickupItemEvent(event: PlayerPickupItemEvent) {
        PickupItemListener.playerPickupItem(event.player, event.item, event)
    }

    /**
     * 自动绑定, 捡起物品时绑定
     */
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    fun autoBindPlayerPickupItemEvent(event: PlayerPickupItemEvent) {
        val player = event.player
        if (Config.checkByPass(player)) return
        val item = event.item.itemStack
        PickupItemListener.checkAutoBind(player, item)
    }
}