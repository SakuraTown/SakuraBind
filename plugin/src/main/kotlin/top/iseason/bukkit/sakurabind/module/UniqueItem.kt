package top.iseason.bukkit.sakurabind.module


import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.inventory.ItemStack
import org.bukkit.scheduler.BukkitRunnable
import top.iseason.bukkit.sakurabind.SakuraBindAPI
import top.iseason.bukkit.sakurabind.config.UniqueItemConfig
import top.iseason.bukkit.sakurabind.event.BlockBindFromItemEvent
import top.iseason.bukkit.sakurabind.event.ItemBindEvent
import top.iseason.bukkit.sakurabind.event.ItemBindFromBlockEvent
import top.iseason.bukkit.sakurabind.listener.SelectListener
import top.iseason.bukkit.sakurabind.task.DropItemList
import top.iseason.bukkittemplate.utils.bukkit.ItemUtils.checkAir
import java.util.*
import java.util.function.BiPredicate
import kotlin.math.min

/**
 * 唯一物品模块，用于防刷
 */
object UniqueItem : org.bukkit.event.Listener {

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    fun onItemBindEvent(event: ItemBindEvent) {
        val maxNum = event.setting.getInt("module.unique-item")
        if (maxNum < 0) return
        val item = event.item
        if (UniqueItemConfig.isUnique(item)) {
            return
        }
        val amount = item.amount
        if (maxNum == 0) {
            item.amount = 0
            return
        }
        val max = min(maxNum, item.maxStackSize)
        val firstNum = min(amount, max)
        item.amount = firstNum
        UniqueItemConfig.setUnique(item, firstNum)
        var last = amount - firstNum
        if (last <= 0) return
        var cost: Int
        val arrayListOf = ArrayList<ItemStack>(last)
        while (last > 0) {
            cost = min(last, max)
            val repeatItem = item.clone()
            repeatItem.amount = cost
            UniqueItemConfig.setUnique(repeatItem, cost)
            SakuraBindAPI.bind(repeatItem, event.owner, type = event.bindType, silent = true)
            arrayListOf.add(repeatItem)
            last -= cost
        }
        SakuraBindAPI.sendBackItem(event.owner, arrayListOf, false)
    }

    class Scanner : BukkitRunnable() {

        override fun run() {
            val uniqueFilter = UniqueFilter
            UniqueFilter.reset()
            UniqueFilter.remover = InventoryRemover
            // 鼠标、查看的界面、背包
            for (player in Bukkit.getOnlinePlayers()) {
                if (!player.isOnline || SelectListener.noScanning.contains(player.uniqueId)) continue
                UniqueFilter.player = player
                val openInventory = player.openInventory ?: continue
                val cursor = player.itemOnCursor
                if (!cursor.checkAir()) {
                    CursorRemover.setPlayer(player)
                    val cur = CursorRemover.setCache(cursor)
                    UniqueFilter.remover = CursorRemover
                    SakuraBindAPI.filterItems(cur, remove = false, deep = true, uniqueFilter)
                    UniqueFilter.remover = InventoryRemover
                }
                SakuraBindAPI.filterInventory(openInventory.topInventory, remove = true, deep = true, uniqueFilter)
                SakuraBindAPI.filterInventory(
                    openInventory.bottomInventory,
                    remove = true,
                    deep = true,
                    uniqueFilter
                )
            }
            // 掉落物
            UniqueFilter.remover = DroppedRemover
            UniqueFilter.player = null
            checkDrops(DropItemList.drops.iterator(), uniqueFilter)
        }

    }

    @EventHandler
    fun onBlockBindFromItem(event: BlockBindFromItemEvent) {
        val handItem = event.handItem
        val value = UniqueItemConfig.getUniqueId(handItem) ?: return
        event.extraData.add("U^${value}")
    }

    @EventHandler
    fun onItemBindFromBlock(event: ItemBindFromBlockEvent) {
        for (str in event.blockInfo.extraData) {
            if (!str.startsWith("U^")) continue
            val key = str.substring(2, str.length)
            runCatching { parseUniqueKey(key) }.getOrNull() ?: return
            val item = event.item
            UniqueItemConfig.setUnique(item, key)
            break
        }
    }

    fun getUniqueKey(amount: Int): String {
        if (!UniqueItemConfig.random_template__enable) return "${UUID.randomUUID()},${amount}"
        val char = UniqueItemConfig.random_template__char
        val chars = UniqueItemConfig.random_template__chars

        val uid = UniqueItemConfig.random_template__template
            .fold(StringBuilder()) { sb, c ->
                if (c == char) sb.append(chars.random()) else sb.append(c)
            }.toString()
        return "${uid},${amount}"
    }

    fun parseUniqueKey(key: String): Pair<String, Int> {
        val indexOf = key.lastIndexOf(',')
        val uid = key.substring(0, indexOf)
        val num = key.substring(indexOf + 1, key.length).toInt()
        return uid to num
    }

    fun checkDrops(
        iterator: Iterator<DropItemList.ItemSender>,
        predicate: BiPredicate<UUID, ItemStack>
    ) {
        while (iterator.hasNext()) {
            val next = iterator.next()
            val item = next.getItemStack()
            val filterItem = SakuraBindAPI.filterItem(item, remove = true, deep = false, predicate)
            if (filterItem.isEmpty()) continue
            if (item.type == Material.AIR) {
                if (iterator is MutableIterator) {
                    iterator.remove()
                }
                next.remove()
            }
        }
    }
}

object UniqueFilter : BiPredicate<UUID, ItemStack> {

    val map = hashMapOf<String, Int>() //操作都是单线程的
    lateinit var remover: Removable
    var player: Player? = null

    fun reset() {
        map.clear()
    }

    override fun test(t: UUID, item: ItemStack): Boolean {
        val value = UniqueItemConfig.getUniqueId(item) ?: return false
        val (uid, num) = runCatching { UniqueItem.parseUniqueKey(value) }.getOrNull() ?: return false
        val raw = map[uid] ?: 0
        if (raw >= num) { // 超过限制
            if (player != null)
                UniqueItemConfig.log(uid, num, item.clone(), item.amount, remover.getName(), player!!.name)
            else
                UniqueItemConfig.log(uid, num, item.clone(), item.amount, remover.getName())
            return remover.remove(item)
        }
        val amount = item.amount
        val new = raw + amount
        if (new > num) { //部分超过
            if (player != null)
                UniqueItemConfig.log(uid, num, item.clone(), item.amount - num + raw, remover.getName(), player!!.name)
            else
                UniqueItemConfig.log(uid, num, item.clone(), item.amount - num + raw, remover.getName())
            item.amount = num - raw
            map[uid] = num
        } else map[uid] = new
        return false
    }

}

fun interface Removable {
    fun getName() = ""
    fun remove(item: ItemStack): Boolean
}

object InventoryRemover : Removable {
    override fun getName(): String = "背包容器"
    override fun remove(item: ItemStack): Boolean = true
}

object CursorRemover : Removable {
    private val cache = mutableListOf(ItemStack(Material.AIR))
    private lateinit var player: Player
    override fun getName(): String = "鼠标指针"
    fun setCache(item: ItemStack): MutableList<ItemStack> {
        cache[0] = item
        return cache
    }

    fun setPlayer(player: Player) {
        CursorRemover.player = player
    }

    override fun remove(item: ItemStack): Boolean {
        player.setItemOnCursor(null)
        return false
    }
}

object DroppedRemover : Removable {
    override fun getName(): String = "掉落物"
    override fun remove(item: ItemStack): Boolean = true
}

