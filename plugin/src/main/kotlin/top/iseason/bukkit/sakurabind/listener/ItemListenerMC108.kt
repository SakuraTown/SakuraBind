package top.iseason.bukkit.sakurabind.listener

import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerArmorStandManipulateEvent
import top.iseason.bukkit.sakurabind.SakuraBindAPI
import top.iseason.bukkit.sakurabind.config.Config
import top.iseason.bukkit.sakurabind.config.ItemSettings
import top.iseason.bukkit.sakurabind.config.Lang
import top.iseason.bukkit.sakurabind.utils.MessageTool

object ItemListenerMC108 : Listener {
    /**
     * 盔甲架检查
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    fun onPlayerArmorStandManipulateEvent(event: PlayerArmorStandManipulateEvent) {
        if (Config.checkByPass(event.player)) return
        if (SakuraBindAPI.checkDenyBySetting(event.playerItem, event.player, "item-deny.armor-stand")) {
            event.isCancelled = true
            MessageTool.denyMessageCoolDown(
                event.player, Lang.item__deny_armor_stand_set,
                ItemSettings.getSetting(event.playerItem),
                event.playerItem
            )
        } else if (SakuraBindAPI.checkDenyBySetting(event.armorStandItem, event.player, "item-deny.armor-stand")) {
            event.isCancelled = true
            MessageTool.denyMessageCoolDown(
                event.player,
                Lang.item__deny_armor_stand_get,
                ItemSettings.getSetting(event.armorStandItem),
                event.armorStandItem
            )
        }
    }
}