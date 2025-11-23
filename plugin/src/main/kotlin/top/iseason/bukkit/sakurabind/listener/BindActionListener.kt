package top.iseason.bukkit.sakurabind.listener

import org.bukkit.Bukkit
import org.bukkit.event.EventHandler
import top.iseason.bukkit.sakurabind.event.BlockBoundEvent
import top.iseason.bukkit.sakurabind.event.ItemBoundEvent
import top.iseason.bukkittemplate.utils.bukkit.ItemUtils.getDisplayName
import top.iseason.bukkittemplate.utils.bukkit.MessageUtils.formatBy
import top.iseason.bukkittemplate.utils.bukkit.MessageUtils.sendColorMessages

object BindActionListener : org.bukkit.event.Listener {

    @EventHandler
    fun onItemBoundEvent(event: ItemBoundEvent) {
        val setting = event.setting
        val stringList = setting.getStringList("on-bind.item-msg")
        if (stringList.isEmpty()) return
        val item = event.item
        val type = item.type
        val amount = item.amount
        val subId = item.data?.data ?: 0.toByte()
        val displayName = item.getDisplayName() ?: ""
        val map = stringList.map { it.formatBy(type, displayName, amount, item.type.id, subId) }
        (Bukkit.getPlayer(event.owner) ?: Bukkit.getConsoleSender()).sendColorMessages(map)
    }

    @EventHandler
    fun onBlockBoundEvent(event: BlockBoundEvent) {
        val setting = event.setting
        val stringList = setting.getStringList("on-bind.block-msg")
        if (stringList.isEmpty()) return
        val block = event.block
        val type = block.type
        val world = block.world.name
        val x = block.x
        val y = block.y
        val z = block.z

        val map = stringList.map { it.formatBy(type, world, x, y, z, block.data) }
        (Bukkit.getPlayer(event.owner) ?: Bukkit.getConsoleSender()).sendColorMessages(map)
    }
}