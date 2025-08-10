package top.iseason.bukkit.sakurabind.pickers

import org.bukkit.OfflinePlayer
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import top.iseason.bukkit.sakurabind.config.Lang
import top.iseason.bukkit.sakurabind.config.SendBackLogger
import top.iseason.bukkit.sakurabind.listener.SelectListener
import top.iseason.bukkit.sakurabind.utils.MessageTool
import top.iseason.bukkit.sakurabind.utils.SendBackType
import top.iseason.bukkittemplate.utils.bukkit.MessageUtils.formatBy
import java.util.*

object PlayerInvPicker : BasePicker("player") {

    override fun pickup(uuid: UUID, items: Array<ItemStack>, type: SendBackType, notify: Boolean): Array<ItemStack>? {
        return null
    }

    override fun pickup(
        player: OfflinePlayer,
        items: Array<ItemStack>,
        type: SendBackType,
        notify: Boolean
    ): Array<ItemStack>? {
        if (!player.isOnline) return null
        if (player !is Player) {
            return null
        }
        if (player.isDead) return null
        if (SelectListener.noScanning.contains(player.uniqueId)) return null
        var count = 0
        val add = ArrayList<ItemStack>()
        val remain = ArrayList<ItemStack>()
        val inventory = player.inventory
        for (item in items) {
            val rawAmount = item.amount
            val addItem = inventory.addItem(item)
            if (addItem.isEmpty()) {//全部放下
                count += rawAmount
                val clone = item.clone()
                clone.amount = rawAmount
                add.add(clone)
            } else { //放不下了
                val next = addItem.iterator().next()
                val value = next.value
                if (value.amount == rawAmount) { //全部放不下
                    remain.add(item)
                } else { //放下部分
                    remain.add(value)
                    val addAmount = rawAmount - value.amount
                    val clone = item.clone()
                    clone.amount = addAmount
                    add.add(clone)
                    count += addAmount
                }
            }
        }
        SendBackLogger.log(player.uniqueId, type, name, add)
        if (remain.isEmpty()) {
            if (notify) MessageTool.sendNormal(player, Lang.send_back__player_all.formatBy(count))
            return emptyArray()
        } else if (count > 0 && notify)
            MessageTool.sendNormal(player, Lang.send_back__player_half.formatBy(count))
        return remain.toTypedArray()
    }

}