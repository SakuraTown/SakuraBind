package top.iseason.bukkit.sakurabind

import org.bukkit.entity.Item
import org.bukkit.entity.ItemFrame
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.block.BlockDispenseEvent
import org.bukkit.event.entity.EntityDamageEvent
import org.bukkit.event.entity.EntityPickupItemEvent
import org.bukkit.event.entity.ItemDespawnEvent
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.inventory.PrepareAnvilEvent
import org.bukkit.event.inventory.PrepareItemCraftEvent
import org.bukkit.event.player.PlayerDropItemEvent
import org.bukkit.event.player.PlayerInteractEntityEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.event.player.PlayerItemConsumeEvent
import org.bukkit.inventory.InventoryHolder
import org.bukkit.inventory.ItemStack
import top.iseason.bukkittemplate.utils.bukkit.ItemUtils.checkAir
import top.iseason.bukkittemplate.utils.other.submit
import java.util.*

object BindListener : Listener {

    /**
     * 不能互动
     */
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    fun onPlayerInteractEvent(event: PlayerInteractEvent) {
        if (!Config.denyInteract) return
        val item = event.item ?: return
        if (item.checkAir()) return
        if (SakuraBindAPI.hasBind(item) && !SakuraBindAPI.isOwner(item, event.player)) {
            event.isCancelled = true
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
    fun onPlayerPickupItemEvent(event: EntityPickupItemEvent) {
        if (!Config.denyPickup) return
        val player = event.entity as? Player ?: return
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
            if (SakuraMailHook.hasHook) {
                val owner = SakuraBindAPI.getOwner(item)
                SakuraMailHook.sendMail(owner!!, listOf(item))
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
     * 禁止用于铁砧
     */
    @EventHandler(priority = EventPriority.MONITOR)
    fun onPrepareAnvilEvent(event: PrepareAnvilEvent) {
        if (!Config.denyAnvil) return
        val item1 = event.inventory.getItem(0)
        val item2 = event.inventory.getItem(1)
        item1?.apply {
            if (checkAir()) return
            if (SakuraBindAPI.hasBind(this)) {
                event.result = null
                return
            }
        }
        item2?.apply {
            if (checkAir()) return
            if (SakuraBindAPI.hasBind(this)) {
                event.result = null
                return
            }
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
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
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
        val owner = SakuraBindAPI.getOwner(item.itemStack) ?: return
        submit(async = true) {
            SakuraMailHook.sendMail(owner, listOf(item.itemStack))
        }
        item.remove()
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    fun onItemDespawnEvent(event: ItemDespawnEvent) {
        if (!Config.sendLost) return
        if (!SakuraMailHook.hasHook) return
        val item = event.entity
        val owner = SakuraBindAPI.getOwner(item.itemStack) ?: return
        submit(async = true) {
            SakuraMailHook.sendMail(owner, listOf(item.itemStack))
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
    fun autoBindEntityPickupItemEvent(event: EntityPickupItemEvent) {
        if (!Config.auto_bind__enable || !Config.auto_bind__onPickup) return
        val player = event.entity as? Player ?: return
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
//
//    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
//    fun onBlockPlaceEvent(event: BlockPlaceEvent) {
//        val heldItem = event.player.getHeldItem() ?: return
//        if (heldItem.checkAir()) return
//        val owner = SakuraBindAPI.getOwner(heldItem) ?: return
//        val location = event.block.location
//        submit(async = true) {
//            if (event.isCancelled) return@submit
//            val block = location.block
//            NBTEditor.set(block, owner.toString(), *Config.nbtPathUuid)
//        }
//    }
//
//    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
//    fun onBlockBreakEvent2(event: BlockBreakEvent) {
//        val block = event.block
//        val owner = NBTEditor.getString(block, *Config.nbtPathUuid) ?: return
//        if (event.player.uniqueId.toString() != owner) event.isCancelled = true
//    }

}