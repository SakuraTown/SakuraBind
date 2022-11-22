package top.iseason.bukkit.sakurabind.listener

import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.entity.Item
import org.bukkit.entity.ItemFrame
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.block.*
import org.bukkit.event.entity.EntityDamageEvent
import org.bukkit.event.entity.EntityExplodeEvent
import org.bukkit.event.entity.ItemDespawnEvent
import org.bukkit.event.entity.ItemSpawnEvent
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.inventory.PrepareItemCraftEvent
import org.bukkit.event.player.*
import org.bukkit.inventory.InventoryHolder
import org.bukkit.inventory.ItemStack
import top.iseason.bukkit.sakurabind.BlockCacheManager
import top.iseason.bukkit.sakurabind.Config
import top.iseason.bukkit.sakurabind.SakuraBindAPI
import top.iseason.bukkit.sakurabind.SakuraMailHook
import top.iseason.bukkittemplate.utils.bukkit.EntityUtils.getHeldItem
import top.iseason.bukkittemplate.utils.bukkit.ItemUtils.checkAir
import top.iseason.bukkittemplate.utils.other.submit
import java.util.*

object BindListener : Listener {

    /**
     * 不能互动
     */
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    fun onPlayerInteractEvent(event: PlayerInteractEvent) {
        if (Config.denyInteract) {
            val item = event.item
            if (item != null &&
                !item.checkAir() &&
                SakuraBindAPI.hasBind(item) &&
                !SakuraBindAPI.isOwner(item, event.player)
            ) {
                event.isCancelled = true
                return
            }
        }
        if (Config.block__denyInteract && event.clickedBlock != null) {
            if (!BlockCacheManager.canBreak(event.clickedBlock!!, event.player)) {
                event.isCancelled = true
                return
            }
        }
    }

    /**
     * 不能实体互动
     */
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    fun onPlayerInteractEntityEvent(event: PlayerInteractEntityEvent) {
        val isItemFrame = event.rightClicked is ItemFrame
        val mainHand = event.player.inventory.itemInMainHand
        val offHand = event.player.inventory.itemInOffHand
        fun check(item: ItemStack): Boolean {
            if (item.checkAir()) return false
            val hasBind = SakuraBindAPI.hasBind(item)
            if (isItemFrame && Config.denyItemFrame && hasBind) {
                return true
            }
            if (Config.denyInteractEntity && hasBind && !SakuraBindAPI.isOwner(item, event.player)) {
                return true
            }
            return false
        }
        if (check(mainHand)) {
            event.isCancelled = true
            return
        }
        if (check(offHand)) {
            event.isCancelled = true
            return
        }
    }

    /**
     * 不能丢
     */
    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    fun onPlayerDropItemEvent(event: PlayerDropItemEvent) {
        if (!Config.denyDrop) return
        val item = event.itemDrop.itemStack
        if (item.checkAir()) return
        if (SakuraBindAPI.hasBind(item)) {
            event.isCancelled = true
        }
    }

    /**
     * 不能捡起
     */
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    fun onPlayerPickupItemEvent(event: PlayerPickupItemEvent) {
        if (!Config.denyPickup) return
        val player = event.player
        val item = event.item.itemStack
        val cursor = player.openInventory.cursor
        if (cursor != null &&
            !cursor.checkAir() &&
            SakuraBindAPI.hasBind(cursor) &&
            !SakuraBindAPI.isOwner(item, player)
        ) {
            event.item.pickupDelay = 10
            event.isCancelled = true
            return
        }
        if (item.checkAir()) return
        if (SakuraBindAPI.hasBind(item) && !SakuraBindAPI.isOwner(item, player)) {
            event.isCancelled = true
            event.item.pickupDelay = 10
            val owner = SakuraBindAPI.getOwner(item)!!
            val p = Bukkit.getPlayer(owner)
            if (p != null) {
                val addItem = p.inventory.addItem(item)
                if (addItem.isEmpty()) {
                    event.item.remove()
                    return
                }
            }
            if (SakuraMailHook.hasHook) {
                SakuraMailHook.sendMail(owner, listOf(item))
                event.item.remove()
            }
        }
    }

    /**
     * 不是你的物品不能点
     */
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    fun onInventoryClickEvent(event: InventoryClickEvent) {
        if (!Config.denyClick) return
        val player = event.whoClicked as? Player ?: return
        val item = event.currentItem ?: return
        if (item.checkAir()) return
        if (event.clickedInventory == event.view.topInventory &&
            SakuraBindAPI.hasBind(item) &&
            !SakuraBindAPI.isOwner(item, player)
        ) {
            event.isCancelled = true
        }
    }


    /**
     * 禁止用于合成
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onPrepareItemCraftEvent(event: PrepareItemCraftEvent) {
        if (!Config.denyCraft) return
        val inventory = event.inventory
        if (inventory.result == null) return
        for (matrix in inventory.matrix) {
            if (matrix == null || matrix.checkAir()) continue
            if (SakuraBindAPI.hasBind(matrix))
                inventory.result = null
            break
        }
    }

    /**
     * 禁止用于消耗
     */
    @EventHandler(priority = EventPriority.LOW)
    fun onPlayerItemConsumeEvent(event: PlayerItemConsumeEvent) {
        if (!Config.denyConsume) return
        val item = event.item
        if (item.checkAir()) return
        if (SakuraBindAPI.hasBind(item) &&
            !SakuraBindAPI.isOwner(item, event.player)
        ) {
            event.isCancelled = true
        }
    }

    /**
     *
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    fun onBlockBreakEvent(event: BlockBreakEvent) {
        if (!Config.sendLost) return
        if (!SakuraMailHook.hasHook) return
        val inventory = (event.block.state as? InventoryHolder)?.inventory ?: return
        val map = mutableMapOf<UUID, MutableList<ItemStack>>()
        val removed = mutableMapOf<Int, ItemStack>()
        inventory.forEachIndexed { index, itemStack ->
            if (itemStack == null || itemStack.checkAir()) return@forEachIndexed
            val owner = SakuraBindAPI.getOwner(itemStack) ?: return@forEachIndexed
            val stacks = map.computeIfAbsent(owner) { mutableListOf() }
            inventory.setItem(index, null)
            removed[index] = itemStack
            stacks.add(itemStack)
        }
        if (map.isEmpty()) return
        submit(async = true) {
            //如果最终没有破坏就回滚
            if (event.isCancelled) {
                removed.forEach { (t, u) -> inventory.setItem(t, u) }
                return@submit
            }
            map.forEach { (uid, items) ->
                SakuraMailHook.sendMail(uid, items)
            }
        }
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    fun onEntityDamageEvent(event: EntityDamageEvent) {
        if (!Config.sendLost) return
        if (!SakuraMailHook.hasHook) return
        val item = event.entity as? Item ?: return
        val itemStack = item.itemStack
        val owner = SakuraBindAPI.getOwner(itemStack) ?: return
        val player = Bukkit.getPlayer(owner)
        if (player != null) {
            val addItem = player.inventory.addItem(itemStack)
            if (addItem.isEmpty()) {
                item.remove()
                return
            }
        }
        submit(async = true) {
            SakuraMailHook.sendMail(owner, listOf(itemStack))
        }
        item.remove()
    }


    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    fun onItemDespawnEvent(event: ItemDespawnEvent) {
        if (!Config.sendLost) return
        if (!SakuraMailHook.hasHook) return
        val item = event.entity
        val itemStack = item.itemStack
        val owner = SakuraBindAPI.getOwner(item.itemStack) ?: return
        val player = Bukkit.getPlayer(owner)
        if (player != null) {
            val addItem = player.inventory.addItem(itemStack)
            if (addItem.isEmpty()) {
                item.remove()
                return
            }
        }
        submit(async = true) {
            SakuraMailHook.sendMail(owner, listOf(itemStack))
        }
        item.remove()
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    fun onBlockDispenseEvent(event: BlockDispenseEvent) {
        if (!Config.denyDispense) return
        if (!SakuraMailHook.hasHook) return
        if (SakuraBindAPI.hasBind(event.item)) {
            event.isCancelled = true
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    fun autoBindInventoryClickEvent(event: InventoryClickEvent) {
        if (!Config.auto_bind__enable || !Config.auto_bind__onClick) return
        val player = event.whoClicked as? Player ?: return
        val item = event.currentItem ?: return
        if (item.checkAir()) return
        if (Config.abMaterial.contains(item.type) && !SakuraBindAPI.hasBind(item)) {
            SakuraBindAPI.bind(item, player)
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    fun autoBindPlayerPickupItemEvent(event: PlayerPickupItemEvent) {
        if (!Config.auto_bind__enable || !Config.auto_bind__onPickup) return
        val player = event.player
        val item = event.item.itemStack
        if (item.checkAir()) return
        if (Config.abMaterial.contains(item.type) && !SakuraBindAPI.hasBind(item)) {
            SakuraBindAPI.bind(item, player)
        }
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    fun autoBindPlayerDropItemEvent(event: PlayerDropItemEvent) {
        if (!Config.auto_bind__enable || !Config.auto_bind__onDrop) return
        val itemStack = event.itemDrop.itemStack
        if (Config.abMaterial.contains(itemStack.type) && !SakuraBindAPI.hasBind(itemStack)) {
            SakuraBindAPI.bind(itemStack, event.player)
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onBlockPlaceEvent(event: BlockPlaceEvent) {
        val heldItem = event.player.getHeldItem() ?: return
        if (heldItem.checkAir()) return
        val owner = SakuraBindAPI.getOwner(heldItem) ?: return
        val uniqueId = event.player.uniqueId
        if (owner != uniqueId) return
        BlockCacheManager.addBlock(event.block, event.player)
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    fun onBlockBreakEvent2(event: BlockBreakEvent) {
        val block = event.block
        val player = event.player
        val owner = BlockCacheManager.getOwner(block) ?: return
        // 有主人但可以破坏
        if (!(Config.block__denyBreak && player.uniqueId.toString() != owner)) {
            BlockCacheManager.removeBlock(block)
            val itemStack = player.getHeldItem() ?: ItemStack(Material.AIR)
            val uuid = UUID.fromString(owner)
            block.getDrops(itemStack).forEach {
                SakuraBindAPI.bind(it, uuid)
                block.world.dropItemNaturally(block.location, it)
            }
            block.type = Material.AIR
        }
        event.isCancelled = true
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    fun onBlockExplodeEvent(event: BlockExplodeEvent) {
        val iterator = event.blockList().iterator()
        while (iterator.hasNext()) {
            if (!BlockCacheManager.canBreak(iterator.next(), null)) iterator.remove()
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    fun onEntityExplodeEvent(event: EntityExplodeEvent) {
        val iterator = event.blockList().iterator()
        while (iterator.hasNext()) {
            if (!BlockCacheManager.canBreak(iterator.next(), null)) iterator.remove()
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    fun onBlockPhysicsEvent(event: BlockPhysicsEvent) {
        val owner = BlockCacheManager.getOwner(event.block) ?: return
        BlockCacheManager.addTemp(BlockCacheManager.blockToString(event.block), owner)
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    fun onItemSpawnEvent(event: ItemSpawnEvent) {
        val entityToString = BlockCacheManager.entityToString(event.entity)
        val owner = BlockCacheManager.getTemp(entityToString) ?: BlockCacheManager.getOwner(entityToString) ?: return
        val itemStack = event.entity.itemStack
        SakuraBindAPI.bind(event.entity.itemStack, UUID.fromString(owner))
        BlockCacheManager.removeCache(entityToString)
        BlockCacheManager.removeTemp(entityToString)
        if (!Config.sendLostImmediately) return
        val uuid = UUID.fromString(owner)
        val player = Bukkit.getPlayer(uuid)
        if (player != null) {
            val addItem = player.inventory.addItem(itemStack)
            if (addItem.isEmpty()) {
                event.isCancelled = true
                return
            }
        }
        if (SakuraMailHook.hasHook) {
            submit(async = true) {
                SakuraMailHook.sendMail(uuid, listOf(itemStack))
            }
            event.isCancelled = true
        }
    }
}