package top.iseason.bukkit.sakurabind.pickers

import org.bukkit.OfflinePlayer
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import top.iseason.bukkit.sakurabind.config.Lang
import top.iseason.bukkit.sakurabind.utils.MessageTool
import top.iseason.bukkittemplate.utils.bukkit.MessageUtils.formatBy
import java.util.*

object PlayerInvPicker : BasePicker("player") {

    override fun pickup(uuid: UUID, items: Array<ItemStack>, notify: Boolean): Array<ItemStack>? {
        return null
    }

    override fun pickup(player: OfflinePlayer, items: Array<ItemStack>, notify: Boolean): Array<ItemStack>? {
        if (!player.isOnline) return null
        if (player !is Player) {
            return null
        }
        if (player.isDead) return null
        val count = items.sumOf { it.amount }
        val addItem = player.inventory.addItem(*items)
        if (addItem.isEmpty) {
            if (notify) MessageTool.sendNormal(player, Lang.send_back__player_all.formatBy(count))
            return emptyArray()
        }
        val toTypedArray = addItem.values.toTypedArray()
        val releaseCount = toTypedArray.sumOf { it.amount }
        if (notify && releaseCount != count)
            MessageTool.sendNormal(player, Lang.send_back__player_half.formatBy(count - releaseCount))
        return toTypedArray
    }

}