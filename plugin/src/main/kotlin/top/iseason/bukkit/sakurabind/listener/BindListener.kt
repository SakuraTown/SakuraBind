package top.iseason.bukkit.sakurabind.listener

import io.github.bananapuncher714.nbteditor.NBTEditor
import org.bukkit.entity.Item
import org.bukkit.entity.ItemFrame
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.block.BlockDispenseEvent
import org.bukkit.event.entity.EntityDamageEvent
import org.bukkit.event.entity.ItemDespawnEvent
import org.bukkit.event.entity.ItemSpawnEvent
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.inventory.PrepareItemCraftEvent
import org.bukkit.event.player.*
import org.bukkit.inventory.InventoryHolder
import org.bukkit.inventory.ItemStack
import top.iseason.bukkit.sakurabind.SakuraBindAPI
import top.iseason.bukkit.sakurabind.config.Config
import top.iseason.bukkit.sakurabind.config.Lang
import top.iseason.bukkit.sakurabind.hook.SakuraMailHook
import top.iseason.bukkittemplate.utils.bukkit.EntityUtils.getHeldItem
import top.iseason.bukkittemplate.utils.bukkit.ItemUtils.checkAir
import top.iseason.bukkittemplate.utils.bukkit.MessageUtils.sendColorMessage
import top.iseason.bukkittemplate.utils.other.EasyCoolDown
import top.iseason.bukkittemplate.utils.other.submit
import java.util.*

object BindListener : Listener {

    /**
     * 不能互动
     */
    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    fun onPlayerInteractEvent(event: PlayerInteractEvent) {
        if (!Config.item_deny__interact) return
        val item = event.item ?: return
        if (!item.checkAir() &&
            SakuraBindAPI.hasBind(item)
        ) {
            //允许物主使用
            if (Config.item_deny__interact_allow_owner && SakuraBindAPI.isOwner(item, event.player)) {
                return
            }
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
        val offHand = if (NBTEditor.getMinecraftVersion()
                .greaterThanOrEqualTo(NBTEditor.MinecraftVersion.v1_9)
        ) event.player.inventory.itemInOffHand else null

        fun check(item: ItemStack?): Boolean {
            if (item.checkAir()) return false
            val hasBind = SakuraBindAPI.hasBind(item!!)
            if (isItemFrame && Config.item_deny__itemFrame && hasBind) {
                return true
            }
            if (Config.item_deny__interact_entity && hasBind && !SakuraBindAPI.isOwner(item, event.player)) {
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
        if (!Config.item_deny__drop) return
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
        if (!Config.item_deny__pickup) return
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
            val sendBackItem = SakuraBindAPI.sendBackItem(owner, listOf(item))
            if (sendBackItem.isEmpty())
                event.item.remove()
            else event.item.setItemStack(sendBackItem.first())
        }
    }

    /**
     * 不是你的物品不能点
     */
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    fun onInventoryClickEvent(event: InventoryClickEvent) {
        if (!Config.item_deny__click && !Config.item_deny__inventory) return
        val player = event.whoClicked as? Player ?: return
        val item = event.currentItem ?: return
        if (item.checkAir()) return
        val owner = SakuraBindAPI.getOwner(item) ?: return
        val uniqueId = player.uniqueId
        if (Config.item_deny__click && event.clickedInventory == event.view.topInventory && owner != uniqueId) {
            event.isCancelled = true
            return
        }
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    fun onInventoryClickEvent2(event: InventoryClickEvent) {
        if (!Config.item_deny__inventory) return
        val title = event.view.title
        val any = Config.itemDenyInventories.any { it.matcher(title).find() }
        if (!any) return
        val currentItem = event.currentItem
        val cursor = event.cursor
        if (!currentItem.checkAir() && SakuraBindAPI.hasBind(currentItem!!)) {
            event.isCancelled = true
            return
        }
        if (!cursor.checkAir() && SakuraBindAPI.hasBind(cursor!!)) {
            event.isCancelled = true
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    fun onPlayerCommandPreprocessEvent(event: PlayerCommandPreprocessEvent) {
        if (!Config.item_deny__command) return
        val heldItem = event.player.getHeldItem() ?: return
        val owner = SakuraBindAPI.getOwner(heldItem) ?: return
        if (Config.item_deny__command_allow_owner && owner == event.player.uniqueId) return
        val message = event.message
        for (pattern in Config.itemDenyCommands) {
            if (pattern.matcher(message).find()) {
                event.isCancelled = true
                if (!EasyCoolDown.check(event.player.uniqueId, 1000)) {
                    event.player.sendColorMessage(Lang.item__deny_command)
                }
                return
            }
        }
    }

    /**
     * 禁止用于合成
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onPrepareItemCraftEvent(event: PrepareItemCraftEvent) {
        if (!Config.item_deny__craft) return
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
        if (!Config.item_deny__consume) return
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
        if (!Config.send_when_lost) return
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

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    fun onEntityDamageEvent(event: EntityDamageEvent) {
        if (!Config.send_when_lost) return
        if (!SakuraMailHook.hasHook) return
        val item = event.entity as? Item ?: return
        if (item.isDead) return
        val itemStack = item.itemStack
        val owner = SakuraBindAPI.getOwner(itemStack) ?: return
        val sendBackItem = SakuraBindAPI.sendBackItem(owner, listOf(itemStack))
        if (sendBackItem.isEmpty())
            item.remove()
        else item.setItemStack(sendBackItem.first())
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    fun onItemDespawnEvent(event: ItemDespawnEvent) {
        if (!Config.send_when_lost) return
        if (!SakuraMailHook.hasHook) return
        val item = event.entity
        val itemStack = item.itemStack
        val owner = SakuraBindAPI.getOwner(item.itemStack) ?: return
        val sendBackItem = SakuraBindAPI.sendBackItem(owner, listOf(itemStack))
        if (sendBackItem.isEmpty())
            item.remove()
        else item.setItemStack(sendBackItem.first())
        item.remove()
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    fun onBlockDispenseEvent(event: BlockDispenseEvent) {
        if (!Config.item_deny__dispense) return
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
    fun onItemSpawnEvent(event: ItemSpawnEvent) {
        if (!Config.send_immediately) return
        val itemStack = event.entity.itemStack
        val uuid = SakuraBindAPI.getOwner(itemStack) ?: return
        val sendBackItem = SakuraBindAPI.sendBackItem(uuid, listOf(itemStack))
        if (sendBackItem.isEmpty())
            event.isCancelled = true
        else event.entity.setItemStack(sendBackItem.first())
    }

}
