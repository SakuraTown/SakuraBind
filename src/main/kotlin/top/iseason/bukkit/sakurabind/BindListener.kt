package top.iseason.bukkit.sakurabind

import org.bukkit.entity.Item
import org.bukkit.entity.ItemFrame
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockDispenseEvent
import org.bukkit.event.block.BlockDropItemEvent
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
import org.bukkit.inventory.ItemStack
import top.iseason.bukkit.bukkittemplate.utils.submit
import java.util.*

object BindListener : Listener {

    /**
     * 不能互动
     */
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    fun onPlayerInteractEvent(event: PlayerInteractEvent) {
        if (!Config.denyInteract) return
        val item = event.item ?: return
        if (item.type.isAir) return
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
            if (item.type.isAir) return false
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
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    fun onPlayerDropItemEvent(event: PlayerDropItemEvent) {
        if (!Config.denyDrop) return
        val item = event.itemDrop.itemStack
        if (item.type.isAir) return
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
            !cursor.type.isAir &&
            SakuraBindAPI.hasBind(cursor) &&
            !SakuraBindAPI.isOwner(item, player)
        ) {
            event.item.pickupDelay = 10
            event.isCancelled = true
            return
        }
        if (item.type.isAir) return
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
        if (item.type.isAir) return
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
            if (type.isAir) return
            if (SakuraBindAPI.hasBind(this)) {
                event.result = null
                return
            }
        }
        item2?.apply {
            if (type.isAir) return
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
            if (matrix.type.isAir) continue
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
        if (item.type.isAir) return
        if (SakuraBindAPI.hasBind(item) &&
            !SakuraBindAPI.isOwner(item, event.player)
        ) {
            event.isCancelled = true
        }
    }

    /**
     *
     */
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    fun onBlockDropItemEvent(event: BlockDropItemEvent) {
        if (!Config.sendLost) return
        if (!SakuraMailHook.hasHook) return
        val map = mutableMapOf<UUID, MutableList<ItemStack>>()
        val iterator = event.items.iterator()
        while (iterator.hasNext()) {
            val next = iterator.next()
            val owner = SakuraBindAPI.getOwner(next.itemStack) ?: continue
            val stacks = map.computeIfAbsent(owner) { mutableListOf() }
            stacks.add(next.itemStack)
            iterator.remove()
        }
        if (map.isEmpty()) return
        submit(async = true) {
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

}