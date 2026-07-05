package top.iseason.bukkit.sakurabind.task


import de.tr7zw.nbtapi.utils.MinecraftVersion
import org.bukkit.entity.Entity
import org.bukkit.entity.Item
import org.bukkit.entity.ThrowableProjectile
import org.bukkit.inventory.ItemStack
import org.bukkit.scheduler.BukkitRunnable
import top.iseason.bukkit.sakurabind.SakuraBindAPI
import top.iseason.bukkit.sakurabind.task.EntityRemoveQueue.syncRemove
import top.iseason.bukkit.sakurabind.utils.SendBackType
import java.util.*
import java.util.concurrent.ConcurrentLinkedQueue

object DropItemList : BukkitRunnable() {
    private val hasMinHeight = MinecraftVersion.isAtLeastVersion(MinecraftVersion.MC1_17_R1)
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
        val rawStack = item.itemStack
        val filterItem = SakuraBindAPI.filterItem(rawStack, remove = true) {
            it.getInt("item.send-back-delay") >= 0
        }
        if (filterItem.isEmpty()) return
        item.itemStack = rawStack
        for ((uuid, items) in filterItem) {
            for (itemStack in items) {
                val delay = SakuraBindAPI.getItemSetting(itemStack).getInt("item.send-back-delay")
                if (delay == 0) {
                    CachedInnerItemSender(item, itemStack, uuid, delay).sendBack()
                } else {
                    drops.add(CachedInnerItemSender(item, itemStack, uuid, delay))
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
            val item = sender.entity
            val useEntityState = sender.useEntityState()
            val invalidEntity = useEntityState && (item.isDead || sender.markAsRemoved || !item.isValid)
            if (invalidEntity) {
                iterator.remove()
                continue
            }
            val delay = sender.delay
            // 检查掉虚空
            if (useEntityState && sender.checkVoid()) {
                val location = item.location
                val minHeight = if (hasMinHeight && location.world != null) location.world!!.minHeight else 0
                if (location.y < minHeight) {
                    sender.sendBack()
                    iterator.remove()
                    continue
                }
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
        val hashMap = HashMap<Pair<UUID, SendBackType>, MutableList<ItemStack>>()
        drops.forEach {
            if (!it.shouldReturnOnCancel()) return@forEach
            hashMap.computeIfAbsent(it.owner to it.getSendBackType()) { mutableListOf() }.add(it.getItemStack())
            it.removeOnCancel()
        }
        drops.clear()
        hashMap.forEach { (key, items) -> SakuraBindAPI.sendBackItem(key.first, items, type = key.second) }
    }

    abstract class ItemSender(val entity: Entity, val owner: UUID, var delay: Int) {

        var markAsRemoved = false

        open fun sendBack() {
            SakuraBindAPI.sendBackItem(owner, listOf(getItemStack()), type = SendBackType.DROP)
            syncRemove(entity)
        }

        abstract fun getItemStack(): ItemStack

        open fun getSendBackType(): SendBackType = SendBackType.DROP

        open fun shouldReturnOnCancel(): Boolean = entity.isValid

        open fun removeOnCancel() {
            if (entity.isValid) entity.remove()
        }

        open fun remove() {
            if (entity.isDead) return
            markAsRemoved = true
            syncRemove(entity)
        }

        open fun keepWhenEntityInvalid(): Boolean = false

        open fun checkVoid(): Boolean = true

        open fun useEntityState(): Boolean = !keepWhenEntityInvalid()
    }

    open class DropItemSender(val item: Item, owner: UUID, delay: Int) :
        ItemSender(item, owner, delay) {
        override fun getItemStack(): ItemStack = item.itemStack
    }

    class ThrowableItemSender(val item: ThrowableProjectile, owner: UUID, delay: Int) :
        ItemSender(item, owner, delay) {
        override fun getItemStack(): ItemStack = item.item
    }

    // 内部绑定物在掉落时已经从容器物品中剥离，这里只保留待返还的克隆。
    private class CachedInnerItemSender(
        item: Item,
        val slotItem: ItemStack,
        owner: UUID,
        delay: Int
    ) : DropItemSender(item, owner, delay) {

        override fun sendBack() {
            SakuraBindAPI.sendBackItem(owner, listOf(slotItem), type = SendBackType.CONTAINER_DROP)
        }

        override fun getItemStack(): ItemStack = slotItem

        override fun getSendBackType(): SendBackType = SendBackType.CONTAINER_DROP

        override fun shouldReturnOnCancel(): Boolean = true

        override fun removeOnCancel() {
        }

        override fun remove() {
            markAsRemoved = true
        }

        override fun keepWhenEntityInvalid(): Boolean = true

        override fun checkVoid(): Boolean = false
    }
}
