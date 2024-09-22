package top.iseason.bukkit.sakurabind.module

import org.bukkit.Material
import org.bukkit.entity.Player
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
import top.iseason.bukkittemplate.utils.bukkit.EntityUtils.giveItem
import top.iseason.bukkittemplate.utils.bukkit.ItemUtils.decrease
import top.iseason.bukkittemplate.utils.bukkit.MessageUtils.formatBy
import top.iseason.bukkittemplate.utils.other.RandomUtils

object BindItem : org.bukkit.event.Listener {

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    fun onInventoryClickEvent(event: InventoryClickEvent) {
        val action = event.action
        if (action != InventoryAction.SWAP_WITH_CURSOR) return
        val player = event.whoClicked as Player? ?: return
        val cursor = event.cursor ?: return
        val currentItem = event.currentItem ?: return
        val owner = SakuraBindAPI.getOwner(currentItem)
        val amount = currentItem.amount

        if (owner != null) {
            val (key, rate) = BindItemConfig.getUnBind(cursor) ?: return
            if (player.uniqueId != owner) {
                MessageTool.messageCoolDown(player, Lang.bind_item__not_owner)
                event.isCancelled = true
                return
            }
            if (key.isNotEmpty() && !Regex(key).matches(ItemSettings.getSetting(currentItem).keyPath)) {
                MessageTool.messageCoolDown(player, Lang.bind_item__not_match)
                event.isCancelled = true
                return
            }
            if (BindItemConfig.syncAmount) {
                if (cursor.amount >= amount) {
                    cursor.decrease(amount)
                    if (cursor.type == Material.AIR) {
                        event.cursor = null
                    }
                } else {
                    MessageTool.messageCoolDown(player, Lang.bind_item__no_amount)
                    event.isCancelled = true
                    return
                }
            } else {
                if (cursor.amount == 1) {
                    event.cursor = null
                } else
                    cursor.decrease(1)
            }
            var success = 0
            repeat(amount) {
                if (!RandomUtils.checkPercentage(rate)) {
                    success++
                }
            }
            if (success > 0) {
                if (success == amount) { // 全部解绑
                    SakuraBindAPI.unBind(currentItem, type = BindType.BIND_ITEM_UNBIND_ITEM)
                    MessageTool.messageCoolDown(event.whoClicked, Lang.bind_item__unbind_success)
                } else { //部分解绑
                    currentItem.amount = amount - success
                    val clone = currentItem.clone()
                    clone.amount = success
                    SakuraBindAPI.unBind(clone, type = BindType.BIND_ITEM_UNBIND_ITEM)
                    player.giveItem(clone)
                    MessageTool.messageCoolDown(
                        event.whoClicked,
                        Lang.bind_item__unbind_success_remain.formatBy(success)
                    )
                }
            } else { //没解绑
                MessageTool.messageCoolDown(event.whoClicked, Lang.bind_item__unbind_failure)
            }
            event.isCancelled = true
        } else {
            val (key, rate) = BindItemConfig.getBind(cursor) ?: return
            if (BindItemConfig.syncAmount) {
                if (cursor.amount >= amount) {
                    cursor.decrease(amount)
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
            var success = 0
            repeat(amount) {
                if (!RandomUtils.checkPercentage(rate)) {
                    success++
                }
            }
            if (success > 0) {
                val settingNullable = ItemSettings.getSettingNullable(key)
                if (success == amount) { // 全部绑定
                    SakuraBindAPI.bind(
                        currentItem, event.whoClicked.uniqueId,
                        type = BindType.BIND_ITEM_BIND_ITEM,
                        setting = settingNullable
                    )
                    MessageTool.messageCoolDown(event.whoClicked, Lang.bind_item__bind_success)
                } else { //部分绑定
                    currentItem.amount = amount - success
                    val clone = currentItem.clone()
                    clone.amount = success
                    SakuraBindAPI.bind(
                        clone, event.whoClicked.uniqueId,
                        type = BindType.BIND_ITEM_BIND_ITEM,
                        setting = settingNullable
                    )
                    player.giveItem(clone)
                    MessageTool.messageCoolDown(
                        event.whoClicked,
                        Lang.bind_item__bind_success_remain.formatBy(success)
                    )
                }
            } else { //没绑定
                MessageTool.messageCoolDown(event.whoClicked, Lang.bind_item__bind_failure)
            }
            event.isCancelled = true
        }
    }
}