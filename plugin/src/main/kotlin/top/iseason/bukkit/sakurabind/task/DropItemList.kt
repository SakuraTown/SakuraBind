package top.iseason.bukkit.sakurabind.task

import io.github.bananapuncher714.nbteditor.NBTEditor
import org.bukkit.block.BlockState
import org.bukkit.entity.Entity
import org.bukkit.entity.Item
import org.bukkit.entity.ThrowableProjectile
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

    fun putDropItem(item: Item, owner: UUID, delay: Int) {
        if (delay == 0) EntityRemoveQueue.hide(item)
        drops.add(DropItemSender(item, owner, delay))
    }

    fun putThrowableItem(item: ThrowableProjectile, owner: UUID, delay: Int) {
        if (delay == 0) EntityRemoveQueue.hide(item)
        drops.add(ThrowableItemSender(item, owner, delay))
    }

    fun putDropInnerItem(item: Item) {
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
        if (drops.isEmpty) {
            return
        }
        val iterator = drops.iterator()
        while (iterator.hasNext()) {
            val sender = iterator.next()
            val item = sender.entity
            val location = item.location
            if (item.isDead || sender.markAsRemoved || !item.isValid) {
                iterator.remove()
                continue
            }
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
            val entity = it.entity
            if (!entity.isValid) return@forEach
            hashMap.computeIfAbsent(it.owner) { mutableListOf() }.add(it.getItemStack())
            entity.remove()
        }
        hashMap.forEach { (uuid, items) -> SakuraBindAPI.sendBackItem(uuid, items) }
    }

    abstract class ItemSender(val entity: Entity, val owner: UUID, var delay: Int) {

        var markAsRemoved = false

        open fun sendBack() {
            SakuraBindAPI.sendBackItem(owner, listOf(getItemStack()))
            syncRemove(entity)
        }

        abstract fun getItemStack(): ItemStack

        open fun remove() {
            if (entity.isDead) return
            markAsRemoved = true
            syncRemove(entity)
        }
    }

    open class DropItemSender(val item: Item, owner: UUID, delay: Int) :
        ItemSender(item, owner, delay) {
        override fun getItemStack(): ItemStack = item.itemStack
    }

    class ThrowableItemSender(val item: ThrowableProjectile, owner: UUID, delay: Int) :
        ItemSender(item, owner, delay) {
        override fun getItemStack(): ItemStack = item.item
    }

    // 不删除实体, 从物品容器中删除
    private class InnerItemSender(
        item: Item,
        val slotItem: ItemStack,
        owner: UUID,
        delay: Int
    ) : DropItemSender(item, owner, delay) {

        override fun sendBack() {
            SakuraBindAPI.sendBackItem(owner, listOf(slotItem))
            remove()
        }

        override fun remove() {
            markAsRemoved = true
            val rawStack = super.getItemStack()
            val itemMeta = rawStack.itemMeta as BlockStateMeta
            if (!itemMeta.hasBlockState()) return
            val inventoryHolder = itemMeta.blockState as InventoryHolder
            inventoryHolder.inventory.remove(slotItem)
            itemMeta.blockState = inventoryHolder as BlockState
            rawStack.itemMeta = itemMeta
        }
    }
}