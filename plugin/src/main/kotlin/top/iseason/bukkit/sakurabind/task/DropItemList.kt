package top.iseason.bukkit.sakurabind.task

import io.github.bananapuncher714.nbteditor.NBTEditor
import org.bukkit.entity.Item
import org.bukkit.scheduler.BukkitRunnable
import top.iseason.bukkit.sakurabind.SakuraBindAPI
import java.util.*

object DropItemList : BukkitRunnable() {
    val hasMinHeight = NBTEditor.getMinecraftVersion().greaterThanOrEqualTo(NBTEditor.MinecraftVersion.v1_17)
    private val entities = LinkedList<ItemSender>()
    fun putItem(item: Item, owner: UUID, delay: Int) {
//        ConcurrentLinkedDeque
        entities.add(ItemSender(item, owner, delay))
    }

    override fun run() {
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
//            println(item.lastDamageCause?.cause)
            //非自然死亡
//            val location = item.location
//            println(delay)
//            println(location.y)
            if (delay < 0) {
                val location = item.location
                val minHeight = if (hasMinHeight && location.world != null) location.world!!.minHeight else 0
                if (item.location.y < minHeight) {
//                    println("void")
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

    data class ItemSender(val item: Item, val owner: UUID, var delay: Int)
}