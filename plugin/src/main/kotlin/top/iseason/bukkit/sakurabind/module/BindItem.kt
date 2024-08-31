package top.iseason.bukkit.sakurabind.module

import org.bukkit.Material
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.inventory.InventoryAction
import org.bukkit.event.inventory.InventoryClickEvent
import top.iseason.bukkit.sakurabind.SakuraBindAPI
import top.iseason.bukkit.sakurabind.config.BindItemConfig
import top.iseason.bukkit.sakurabind.config.ItemSettings
import top.iseason.bukkit.sakurabind.config.Lang
import top.iseason.bukkit.sakurabind.utils.BindType
import top.iseason.bukkit.sakurabind.utils.MessageTool
import top.iseason.bukkittemplate.utils.bukkit.ItemUtils.decrease
import top.iseason.bukkittemplate.utils.other.RandomUtils

object BindItem : org.bukkit.event.Listener {

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    fun onInventoryClickEvent(event: InventoryClickEvent) {
        val action = event.action
        if (action != InventoryAction.SWAP_WITH_CURSOR) return
        val cursor = event.cursor ?: return
        val currentItem = event.currentItem ?: return
        val owner = SakuraBindAPI.getOwner(currentItem)
        if (owner != null) {
            if (!SakuraBindAPI.isAutoBind(cursor)) return
            if (event.whoClicked.uniqueId != owner) {
                MessageTool.messageCoolDown(event.whoClicked, Lang.bind_item__not_owner)
                event.isCancelled = true
                return
            }
            if (BindItemConfig.syncAmount) {
                if (cursor.amount >= currentItem.amount) {
                    cursor.decrease(currentItem.amount)
                    if (cursor.type == Material.AIR) {
                        event.cursor = null
                    }
                } else {
                    MessageTool.messageCoolDown(event.whoClicked, Lang.bind_item__no_amount)
                    event.isCancelled = true
                    return
                }
            } else {
                if (cursor.amount == 1) {
                    event.cursor = null
                } else
                    cursor.decrease(1)
            }
            if (!RandomUtils.checkPercentage(BindItemConfig.unbindChance)) {
                SakuraBindAPI.unBind(currentItem, type = BindType.BIND_ITEM_UNBIND_ITEM)
                MessageTool.messageCoolDown(event.whoClicked, Lang.bind_item__unbind_success)
            } else {
                MessageTool.messageCoolDown(event.whoClicked, Lang.bind_item__unbind_failure)
            }
            event.isCancelled = true
        } else {
            val string = BindItemConfig.getSetting(cursor) ?: return
            if (BindItemConfig.syncAmount) {
                if (cursor.amount >= currentItem.amount) {
                    cursor.decrease(currentItem.amount)
                    if (cursor.type == Material.AIR) {
                        event.cursor = null
                    }
                } else {
                    MessageTool.messageCoolDown(event.whoClicked, Lang.bind_item__no_amount)
                    event.isCancelled = true
                    return
                }
            } else {
                if (cursor.amount == 1) {
                    event.cursor = null
                } else
                    cursor.decrease(1)
            }
            if (!RandomUtils.checkPercentage(BindItemConfig.bindChance)) {
                val settingNullable = ItemSettings.getSettingNullable(string)
                SakuraBindAPI.bind(
                    currentItem, event.whoClicked.uniqueId,
                    type = BindType.BIND_ITEM_BIND_ITEM,
                    setting = settingNullable
                )
                MessageTool.messageCoolDown(event.whoClicked, Lang.bind_item__bind_success)
            } else {
                MessageTool.messageCoolDown(event.whoClicked, Lang.bind_item__bind_failure)
            }

            event.isCancelled = true
        }
    }
}