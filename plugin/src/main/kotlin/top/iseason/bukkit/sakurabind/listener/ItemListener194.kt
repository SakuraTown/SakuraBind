package top.iseason.bukkit.sakurabind.listener

import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.inventory.PrepareAnvilEvent
import top.iseason.bukkit.sakurabind.SakuraBindAPI
import top.iseason.bukkit.sakurabind.config.Config
import top.iseason.bukkit.sakurabind.config.ItemSettings
import top.iseason.bukkit.sakurabind.config.Lang
import top.iseason.bukkit.sakurabind.utils.MessageTool

object ItemListener194 : Listener {

    /**
     * 禁止用于铁砧
     */
    @EventHandler(priority = EventPriority.MONITOR)
    fun onPrepareAnvilEvent(event: PrepareAnvilEvent) {
        if (Config.checkByPass(event.view.player)) return
        val item1 = event.inventory.getItem(0)
        val item2 = event.inventory.getItem(1)
        val player = event.view.player
        if (SakuraBindAPI.checkDenyBySetting(item1, player, "item-deny.anvil")) {
            event.result = null
            MessageTool.denyMessageCoolDown(player, Lang.item__deny_anvil, ItemSettings.getSetting(item1!!), item1)
        } else if (SakuraBindAPI.checkDenyBySetting(item2, player, "item-deny.anvil")) {
            event.result = null
            MessageTool.denyMessageCoolDown(player, Lang.item__deny_anvil, ItemSettings.getSetting(item2!!), item2)
        }
    }
}