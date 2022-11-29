package top.iseason.bukkit.sakurabind.listener

import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.inventory.PrepareAnvilEvent
import top.iseason.bukkit.sakurabind.SakuraBindAPI
import top.iseason.bukkit.sakurabind.config.Config
import top.iseason.bukkit.sakurabind.config.ItemSettings
import top.iseason.bukkit.sakurabind.config.Lang
import top.iseason.bukkittemplate.utils.bukkit.ItemUtils.checkAir
import top.iseason.bukkittemplate.utils.bukkit.MessageUtils.sendColorMessage
import top.iseason.bukkittemplate.utils.other.EasyCoolDown

object BindListener194 : Listener {

    /**
     * 禁止用于铁砧
     */
    @EventHandler(priority = EventPriority.MONITOR)
    fun onPrepareAnvilEvent(event: PrepareAnvilEvent) {
//        if (!Config.item_deny__anvil) return
        if (Config.checkByPass(event.view.player)) return
        val item1 = event.inventory.getItem(0)
        val item2 = event.inventory.getItem(1)
        val uniqueId = event.view.player.uniqueId
        item1?.apply {
            if (checkAir()) return
            val owner = SakuraBindAPI.getOwner(this) ?: return@apply
            if (ItemSettings.getSetting(this).getBoolean("item-deny.anvil", owner.toString(), event.view.player)) {
                event.result = null
                if (!EasyCoolDown.check(uniqueId, 1000))
                    event.view.player.sendColorMessage(Lang.item__deny__anvil)
                return
            }
        }
        item2?.apply {
            if (checkAir()) return
            val owner = SakuraBindAPI.getOwner(this) ?: return@apply
            if (ItemSettings.getSetting(this).getBoolean("item-deny.anvil", owner.toString(), event.view.player)) {
                event.result = null
                if (!EasyCoolDown.check(uniqueId, 1000))
                    event.view.player.sendColorMessage(Lang.item__deny__anvil)
                return
            }
        }
    }
}