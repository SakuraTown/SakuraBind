package top.iseason.bukkit.sakurabind.listener

import io.github.bananapuncher714.nbteditor.NBTEditor
import org.bukkit.Bukkit
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
import org.bukkit.event.entity.ProjectileLaunchEvent
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.inventory.PrepareItemCraftEvent
import org.bukkit.event.player.*
import org.bukkit.inventory.InventoryHolder
import org.bukkit.inventory.ItemStack
import org.jetbrains.exposed.sql.select
import top.iseason.bukkit.sakurabind.SakuraBindAPI
import top.iseason.bukkit.sakurabind.config.Config
import top.iseason.bukkit.sakurabind.config.Lang
import top.iseason.bukkit.sakurabind.dto.PlayerItems
import top.iseason.bukkittemplate.config.DatabaseConfig
import top.iseason.bukkittemplate.config.dbTransaction
import top.iseason.bukkittemplate.utils.bukkit.EntityUtils.getHeldItem
import top.iseason.bukkittemplate.utils.bukkit.ItemUtils.checkAir
import top.iseason.bukkittemplate.utils.bukkit.MessageUtils.formatBy
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
        if (event.player.isOp) return
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
        if (event.player.isOp) return
        val isItemFrame = event.rightClicked is ItemFrame
        val mainHand = event.player.inventory.itemInMainHand
        val offHand = if (NBTEditor.getMinecraftVersion()
                .greaterThanOrEqualTo(NBTEditor.MinecraftVersion.v1_9)
        ) event.player.inventory.itemInOffHand else null

        fun check(item: ItemStack?): Boolean {
            if (item.checkAir()) return false
            val hasBind = SakuraBindAPI.hasBind(item!!)
            if (isItemFrame && Config.item_deny__item_frame && hasBind) {
                event.player.sendColorMessage(Lang.item__deny_itemFrame)
                return true
            }
            if (Config.item_deny__interact_entity && hasBind && !SakuraBindAPI.isOwner(item, event.player)) {
                event.player.sendColorMessage(Lang.item__deny_entity_interact)
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
        if (event.player.isOp) return
        if (!Config.item_deny__drop) return
        val item = event.itemDrop.itemStack
        if (item.checkAir()) return
        if (SakuraBindAPI.hasBind(item)) {
            event.isCancelled = true
            if (!EasyCoolDown.check(event.player.uniqueId, 1000))
                event.player.sendColorMessage(Lang.item__deny_drop)
        }
    }

    /**
     * 不能捡起
     */
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    fun onPlayerPickupItemEvent(event: PlayerPickupItemEvent) {
        if (event.player.isOp) return
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
            val player1 = Bukkit.getPlayer(owner) ?: Bukkit.getOfflinePlayer(owner)
            event.player.sendColorMessage(Lang.item__deny_pickup.formatBy(player1.name))
        }
    }

    /**
     * 不是你的物品不能点
     */
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    fun onInventoryClickEvent(event: InventoryClickEvent) {
        if (!Config.item_deny__click && !Config.item_deny__inventory) return
        val player = event.whoClicked as? Player ?: return
        if (player.isOp) return
        val item = event.currentItem ?: return
        if (item.checkAir()) return
        val owner = SakuraBindAPI.getOwner(item) ?: return
        val uniqueId = player.uniqueId
        if (Config.item_deny__click && event.clickedInventory == event.view.topInventory && owner != uniqueId) {
            player.sendColorMessage(Lang.item__deny_click)
            event.isCancelled = true
            return
        }
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    fun onInventoryClickEvent2(event: InventoryClickEvent) {
        if (!Config.item_deny__inventory) return
        if (event.whoClicked.isOp) return
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
        if (event.player.isOp) return
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
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    fun onPlayerItemConsumeEvent(event: PlayerItemConsumeEvent) {
        if (!Config.item_deny__consume) return
        if (event.player.isOp) return
        val item = event.item
        if (item.checkAir()) return
        val owner = SakuraBindAPI.getOwner(item) ?: return
        if (Config.item_deny__consume_allow_owner && event.player.uniqueId == owner) return
        if (!EasyCoolDown.check(event.player.uniqueId, 1000))
            event.player.sendColorMessage(Lang.item__deny__consume)
        event.isCancelled = true
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    fun onProjectileLaunchEvent(event: ProjectileLaunchEvent) {
        if (!Config.item_deny__throw) return
        val entity = event.entity
        val player = entity.shooter as? Player ?: return
        if (player.isOp) return
        val heldItem = player.getHeldItem() ?: return
        if (!SakuraBindAPI.hasBind(heldItem)) return
        if (!EasyCoolDown.check(player.uniqueId, 1000))
            player.sendColorMessage(Lang.item__deny_throw)
        event.isCancelled = true
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    fun onBlockBreakEvent(event: BlockBreakEvent) {
        if (!Config.send_when_container_break) return
        if (event.player.isOp) return
        val inventory = (event.block.state as? InventoryHolder)?.inventory ?: return
        val map = mutableMapOf<UUID, MutableList<ItemStack>>()
        val removed = mutableMapOf<Int, ItemStack>()
        inventory.forEachIndexed { index, itemStack ->
            if (itemStack == null || itemStack.checkAir()) return@forEachIndexed
            val owner = SakuraBindAPI.getOwner(itemStack) ?: return@forEachIndexed
            if (Config.item_deny__container_break) {
                event.isCancelled = true
                return
            }
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
                SakuraBindAPI.sendBackItem(uid, items)
            }
        }
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    fun onEntityDamageEvent(event: EntityDamageEvent) {
        if (!Config.send_when_lost) return
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
        if (SakuraBindAPI.hasBind(event.item)) {
            event.isCancelled = true
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    fun autoBindInventoryClickEvent(event: InventoryClickEvent) {
        if (!Config.auto_bind__enable || !Config.auto_bind__onClick) return
        val player = event.whoClicked as? Player ?: return
        if (player.isOp) return
        val item = event.currentItem ?: return
        if (item.checkAir()) return
        if (SakuraBindAPI.hasBind(item)) return
        if (Config.abMaterial.contains(item.type) || NBTEditor.contains(item, Config.auto_bind__nbt)) {
            SakuraBindAPI.bind(item, player)
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    fun autoBindPlayerPickupItemEvent(event: PlayerPickupItemEvent) {
        if (!Config.auto_bind__enable || !Config.auto_bind__onPickup) return
        val player = event.player
        if (player.isOp) return
        val item = event.item.itemStack
        if (item.checkAir()) return
        if (SakuraBindAPI.hasBind(item)) return
        if (Config.abMaterial.contains(item.type) || NBTEditor.contains(item, Config.auto_bind__nbt)) {
            SakuraBindAPI.bind(item, player)
        }
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    fun autoBindPlayerDropItemEvent(event: PlayerDropItemEvent) {
        if (!Config.auto_bind__enable || !Config.auto_bind__onDrop) return
        if (event.player.isOp) return
        val item = event.itemDrop.itemStack
        if (SakuraBindAPI.hasBind(item)) return
        if (Config.abMaterial.contains(item.type) || NBTEditor.contains(item, Config.auto_bind__nbt)) {
            SakuraBindAPI.bind(item, event.player)
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

    fun onLogin(player: Player) {
        if (!DatabaseConfig.isConnected) return
        submit(async = true, delay = 20) {
            val hasItem = dbTransaction {
                val iterator =
                    PlayerItems.slice(PlayerItems.id).select { PlayerItems.uuid eq player.uniqueId }.limit(1).iterator()
                iterator.hasNext()
            }
            if (!hasItem) return@submit
            player.sendColorMessage(Lang.has_lost_item)
        }
    }

}
