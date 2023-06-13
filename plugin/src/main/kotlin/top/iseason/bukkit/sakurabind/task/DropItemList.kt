package top.iseason.bukkit.sakurabind.task

import io.github.bananapuncher714.nbteditor.NBTEditor
import org.bukkit.Bukkit
import org.bukkit.block.BlockState
import org.bukkit.entity.Entity
import org.bukkit.entity.Item
import org.bukkit.inventory.InventoryHolder
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.BlockStateMeta
import org.bukkit.scheduler.BukkitRunnable
import top.iseason.bukkit.sakurabind.SakuraBindAPI
import top.iseason.bukkittemplate.BukkitTemplate
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedQueue

object DropItemList : BukkitRunnable() {
    private val hasMinHeight = NBTEditor.getMinecraftVersion().greaterThanOrEqualTo(NBTEditor.MinecraftVersion.v1_17)
    private val entities = ConcurrentLinkedQueue<ItemSender>()
    private val removed = ConcurrentLinkedQueue<Entity>()
    private val isRemoving = ConcurrentHashMap.newKeySet<Entity>()

    init {
        Collections.synchronizedList(LinkedList<ItemSender>())
        Bukkit.getScheduler().runTaskTimer(
            BukkitTemplate.getPlugin(),
            Runnable {
                if (entities.isEmpty()) {
                    return@Runnable
                }
                for (item in removed) {
                    item.remove()
                }
                removed.clear()
            },
            0L, 1L
        )
    }

    /**
     * 同步删除实体，存在一定延迟
     */
    fun syncRemove(item: Entity) {
        if (item.isDead) return
        isRemoving.add(item)
        // 为了兼容尽可能多的mod服务端和游戏版本
        // 只能让它传送到玩家碰不到的地方，再在事件结束后删除
        val location = item.location
        location.y = Int.MIN_VALUE.toDouble()
        item.teleport(location)
        removed.add(item)
    }

    fun isRemoving(entity: Entity) = isRemoving.contains(entity)
    fun cleanRemoving(entity: Entity) = isRemoving.remove(entity)

    fun putItem(item: Item, owner: UUID, delay: Int) {
        entities.add(ItemSender(item, owner, delay))
    }

    fun putInnerItem(item: Item) {
        for ((uuid, items) in SakuraBindAPI.filterItem(item.itemStack, remove = false)) {
            for (itemStack in items) {
                val delay = SakuraBindAPI.getItemSetting(itemStack).getInt("item.send-back-delay")
                entities.add(InnerItemSender(item, itemStack, uuid, delay))
            }
        }
    }

    override fun run() {
        if (entities.isEmpty()) {
            return
        }
        val iterator = entities.iterator()
        while (iterator.hasNext()) {
            val sender = iterator.next()
            val item = sender.item
            val dead = item.isDead
            val location = item.location
            if (dead) {
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
        entities.forEach {
            val item = it.item
            if (item.isDead) return@forEach
            hashMap.computeIfAbsent(it.owner) { mutableListOf() }.add(item.itemStack)
            item.remove()
        }
        hashMap.forEach { (uuid, items) -> SakuraBindAPI.sendBackItem(uuid, items) }
    }

    private open class ItemSender(val item: Item, val owner: UUID, var delay: Int) {
        open fun sendBack() {
            SakuraBindAPI.sendBackItem(owner, listOf(item.itemStack))
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
            val rawStack = item.itemStack
            val itemMeta = rawStack.itemMeta as BlockStateMeta
            val inventoryHolder = itemMeta.blockState as InventoryHolder
            inventoryHolder.inventory.remove(itemStack)
            itemMeta.blockState = inventoryHolder as BlockState
            rawStack.itemMeta = itemMeta
        }
    }
}