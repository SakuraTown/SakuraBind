package top.iseason.bukkit.sakurabind.pickers

import org.bukkit.OfflinePlayer
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import top.iseason.bukkit.sakurabind.config.Config
import top.iseason.bukkit.sakurabind.config.Lang
import top.iseason.bukkit.sakurabind.config.SendBackLogger
import top.iseason.bukkit.sakurabind.task.DelaySender
import top.iseason.bukkit.sakurabind.utils.MessageTool
import top.iseason.bukkit.sakurabind.utils.SendBackType
import top.iseason.bukkittemplate.config.DatabaseConfig
import top.iseason.bukkittemplate.utils.bukkit.MessageUtils.formatBy
import java.util.*

object DataBasePicker : BasePicker("database") {

    override fun pickup(uuid: UUID, items: Array<ItemStack>, type: SendBackType, notify: Boolean): Array<ItemStack>? {
        if (!Config.send_back_database || !DatabaseConfig.isConnected) return null
        DelaySender.sendItem(uuid, items)
        SendBackLogger.log(uuid, type, name, items)
        return emptyArray()
    }

    override fun pickup(
        player: OfflinePlayer,
        items: Array<ItemStack>,
        type: SendBackType,
        notify: Boolean
    ): Array<ItemStack>? {
        val pickup = pickup(player.uniqueId, items, type, notify)

        if (pickup != null && notify && player is Player) {
            val count = items.sumOf { it.amount }
            MessageTool.sendNormal(player, Lang.send_back__database_all.formatBy(count))
        }
        return pickup
    }
}