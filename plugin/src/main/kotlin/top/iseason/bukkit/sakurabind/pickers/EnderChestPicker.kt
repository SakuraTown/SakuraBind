package top.iseason.bukkit.sakurabind.pickers

import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import top.iseason.bukkit.sakurabind.config.Lang
import top.iseason.bukkit.sakurabind.utils.MessageTool
import top.iseason.bukkittemplate.utils.bukkit.MessageUtils.formatBy
import java.util.*

object EnderChestPicker : BasePicker("ender-chest") {

    override fun pickup(uuid: UUID, items: Array<ItemStack>, notify: Boolean): Array<ItemStack>? {
        return null
    }

    override fun pickup(player: Player, items: Array<ItemStack>, notify: Boolean): Array<ItemStack> {
        val count = items.sumOf { it.amount }
        val addItem = player.enderChest.addItem(*items)
        if (addItem.isEmpty) {
            if (notify) MessageTool.sendNormal(player, Lang.send_back__ender_chest_all.formatBy(count))
            return emptyArray()
        }
        val toTypedArray = addItem.values.toTypedArray()
        val releaseCount = toTypedArray.sumOf { it.amount }
        if (notify && releaseCount != count)
            MessageTool.sendNormal(player, Lang.send_back__ender_chest_half.formatBy(count - releaseCount))
        return toTypedArray
    }

}