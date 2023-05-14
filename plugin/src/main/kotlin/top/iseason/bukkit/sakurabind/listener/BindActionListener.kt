package top.iseason.bukkit.sakurabind.listener

import org.bukkit.Bukkit
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import top.iseason.bukkit.sakurabind.event.BlockBindEvent
import top.iseason.bukkit.sakurabind.event.ItemBindEvent
import top.iseason.bukkittemplate.utils.bukkit.ItemUtils.getDisplayName
import top.iseason.bukkittemplate.utils.bukkit.MessageUtils.formatBy
import top.iseason.bukkittemplate.utils.bukkit.MessageUtils.sendColorMessages

object BindActionListener : org.bukkit.event.Listener {

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    fun onItemBindEvent(event: ItemBindEvent) {
        val setting = event.setting
        val stringList = setting.getStringList("on-bind.item-msg")
        if (stringList.isEmpty()) return
        val item = event.item
        val type = item.type
        val amount = item.amount
        val displayName = item.getDisplayName() ?: ""
        val map = stringList.map { it.formatBy(type, displayName, amount) }
        (Bukkit.getPlayer(event.owner) ?: Bukkit.getConsoleSender()).sendColorMessages(map)
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    fun onBlockBindEvent(event: BlockBindEvent) {
        val setting = event.setting
        val stringList = setting.getStringList("on-bind.block-msg")
        if (stringList.isEmpty()) return
        val block = event.block
        val type = block.type
        val world = block.world.name
        val x = block.x
        val y = block.y
        val z = block.z
        val map = stringList.map { it.formatBy(type, world, x, y, z) }
        (Bukkit.getPlayer(event.owner) ?: Bukkit.getConsoleSender()).sendColorMessages(map)
    }
}