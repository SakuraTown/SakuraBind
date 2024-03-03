package top.iseason.bukkit.sakurabind.task

import io.github.bananapuncher714.nbteditor.NBTEditor
import org.bukkit.block.BlockState
import org.bukkit.entity.Item
import org.bukkit.inventory.InventoryHolder
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.BlockStateMeta
import org.bukkit.scheduler.BukkitRunnable
import top.iseason.bukkit.sakurabind.SakuraBindAPI
import top.iseason.bukkit.sakurabind.task.EntityRemoveQueue.syncRemove
import java.util.*
import java.util.concurrent.ConcurrentLinkedQueue

object DropItemList : BukkitRunnable() {
    private val hasMinHeight = NBTEditor.getMinecraftVersion().greaterThanOrEqualTo(NBTEditor.MinecraftVersion.v1_17)
    val drops = ConcurrentLinkedQueue<ItemSender>()

    fun putItem(item: Item, owner: UUID, delay: Int) {
        drops.add(ItemSender(item, owner, delay))
    }

    fun putInnerItem(item: Item) {
        for ((uuid, items) in SakuraBindAPI.filterItem(item.itemStack, remove = false)) {
            for (itemStack in items) {
                val delay = SakuraBindAPI.getItemSetting(itemStack).getInt("item.send-back-delay")
                if (delay == 0) {
                    InnerItemSender(item, itemStack, uuid, delay).sendBack()
                } else {
                    drops.add(InnerItemSender(item, itemStack, uuid, delay))
                }
            }
        }
    }

    override fun run() {
        if (drops.isEmpty()) {
            return
        }
        val iterator = drops.iterator()
        while (iterator.hasNext()) {
            val sender = iterator.next()
            val item = sender.item
            val location = item.location
            if (item.isDead || sender.markAsRemoved) {
                iterator.remove()
                continue
            }
            sender.owner
            val delay = sender.delay
            // 检查掉虚空
            val minHeight = if (hasMinHeight && location.world != null) location.world!!.minHeight else 0
            if (location.y < minHeight) {
                sender.sendBack()
                iterator.remove()
                continue
            }
            // 延迟返还
            if (delay > 0) {
                sender.delay -= 1
            } else if (delay == 0) {// 计时结束
                sender.sendBack()
                iterator.remove()
            }
            // 剩下不计时的
        }
    }

    // 送回物品
    override fun cancel() {
//        println("cancel")
        val hashMap = HashMap<UUID, MutableList<ItemStack>>()
        drops.forEach {
            val item = it.item
            if (item.isDead) return@forEach
            hashMap.computeIfAbsent(it.owner) { mutableListOf() }.add(item.itemStack)
            item.remove()
        }
        hashMap.forEach { (uuid, items) -> SakuraBindAPI.sendBackItem(uuid, items) }
    }

    open class ItemSender(val item: Item, val owner: UUID, var delay: Int) {

        var markAsRemoved = false

        open fun sendBack() {
            SakuraBindAPI.sendBackItem(owner, listOf(item.itemStack))
            syncRemove(item)
        }

        open fun remove() {
            if (item.isDead) return
            markAsRemoved = true
            syncRemove(item)
        }
    }

    // 不删除实体, 从物品容器中删除
    private class InnerItemSender(
        item: Item,
        val itemStack: ItemStack,
        owner: UUID,
        delay: Int
    ) : ItemSender(item, owner, delay) {

        override fun sendBack() {
            SakuraBindAPI.sendBackItem(owner, listOf(itemStack))
            remove()
        }

        override fun remove() {
            markAsRemoved = true
            val rawStack = item.itemStack
            val itemMeta = rawStack.itemMeta as BlockStateMeta
            if (!itemMeta.hasBlockState()) return
            val inventoryHolder = itemMeta.blockState as InventoryHolder
            inventoryHolder.inventory.remove(itemStack)
            itemMeta.blockState = inventoryHolder as BlockState
            rawStack.itemMeta = itemMeta
        }
    }
}