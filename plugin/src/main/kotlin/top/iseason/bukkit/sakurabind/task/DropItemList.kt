package top.iseason.bukkit.sakurabind.task

import io.github.bananapuncher714.nbteditor.NBTEditor
import org.bukkit.entity.Item
import org.bukkit.inventory.ItemStack
import org.bukkit.scheduler.BukkitRunnable
import top.iseason.bukkit.sakurabind.SakuraBindAPI
import java.util.*
import java.util.concurrent.ConcurrentLinkedQueue

object DropItemList : BukkitRunnable() {
    private val hasMinHeight = NBTEditor.getMinecraftVersion().greaterThanOrEqualTo(NBTEditor.MinecraftVersion.v1_17)
    private val entities = ConcurrentLinkedQueue<ItemSender>()
    fun putItem(item: Item, owner: UUID, delay: Int) {
        entities.add(ItemSender(item, owner, delay))
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
            if (dead) {
                iterator.remove()
                continue
            }
            val owner = sender.owner
            val delay = sender.delay
            if (delay < 0) {
                val location = item.location
                val minHeight = if (hasMinHeight && location.world != null) location.world!!.minHeight else 0
                if (item.location.y < minHeight) {
                    SakuraBindAPI.sendBackItem(owner, listOf(item.itemStack))
                    item.remove()
                    iterator.remove()
                }
                continue
            }
            if (delay > 0) {
                sender.delay -= 1
            } else {
                SakuraBindAPI.sendBackItem(owner, listOf(item.itemStack))
                iterator.remove()
                item.remove()
            }
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

    data class ItemSender(val item: Item, val owner: UUID, var delay: Int)
}