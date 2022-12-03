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
import org.bukkit.event.inventory.InventoryMoveItemEvent
import org.bukkit.event.inventory.InventoryPickupItemEvent
import org.bukkit.event.inventory.PrepareItemCraftEvent
import org.bukkit.event.player.*
import org.bukkit.inventory.InventoryHolder
import org.bukkit.inventory.ItemStack
import org.jetbrains.exposed.sql.select
import top.iseason.bukkit.sakurabind.SakuraBindAPI
import top.iseason.bukkit.sakurabind.config.Config
import top.iseason.bukkit.sakurabind.config.ItemSettings
import top.iseason.bukkit.sakurabind.config.Lang
import top.iseason.bukkit.sakurabind.dto.PlayerItems
import top.iseason.bukkit.sakurabind.task.DropItemList
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
//        if (!Config.item_deny__interact) return
        val item = event.item ?: return
        if (Config.checkByPass(event.player)) return
        val owner = SakuraBindAPI.getOwner(item)
        if (!item.checkAir() &&
            owner != null
        ) {
            val boolean =
                ItemSettings.getSetting(item)
                    .getBoolean("item-deny.interact", owner.toString(), event.player)

            if (boolean) {
                event.isCancelled = true
                if (!EasyCoolDown.check(event.player.uniqueId, 1000))
                    event.player.sendColorMessage(Lang.item__deny_interact)

            }
        }
    }

    /**
     * 不能实体互动
     */
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    fun onPlayerInteractEntityEvent(event: PlayerInteractEntityEvent) {
        if (Config.checkByPass(event.player)) return
        val isItemFrame = event.rightClicked is ItemFrame
        val mainHand = event.player.inventory.itemInMainHand
        val offHand = if (NBTEditor.getMinecraftVersion()
                .greaterThanOrEqualTo(NBTEditor.MinecraftVersion.v1_9)
        ) event.player.inventory.itemInOffHand else null
        val uniqueId = event.player.uniqueId
        fun check(item: ItemStack?): Boolean {
            if (item.checkAir()) return false
            val setting = ItemSettings.getSetting(item!!)
            val owner = SakuraBindAPI.getOwner(item)
            if (isItemFrame
                && owner != null
                && setting.getBoolean("item-deny.item-frame", owner.toString(), event.player)
            ) {
                if (!EasyCoolDown.check(event.player.uniqueId, 1000))
                    event.player.sendColorMessage(Lang.item__deny_itemFrame)
                return true
            }

            if (owner != null
                && setting.getBoolean("item-deny.interact-entity", owner.toString(), event.player)
            ) {
                if (!EasyCoolDown.check(event.player.uniqueId, 1000))
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
        if (Config.checkByPass(event.player)) return
//        if (!Config.item_deny__drop) return
        val item = event.itemDrop.itemStack
        if (item.checkAir()) return
        val owner = SakuraBindAPI.getOwner(item) ?: return
        if (ItemSettings.getSetting(item)
                .getBoolean("item-deny.drop", owner.toString(), event.player)
        ) {
            event.itemDrop.remove()
            val openInventory = event.player.openInventory
            val cursor = openInventory.cursor
            val release = event.player.inventory.addItem(item)
            if (!cursor.checkAir()) {
                val releaseCursor = event.player.inventory.addItem(cursor!!)
                release.putAll(releaseCursor)
                openInventory.cursor = null
            }
            if (release.isNotEmpty()) {
//                DelaySender.sendItem(owner, release.values)
                SakuraBindAPI.sendBackItem(owner, release.values.toList())
            }
            if (!EasyCoolDown.check(event.player.uniqueId, 1000))
                event.player.sendColorMessage(Lang.item__deny_drop)
        }
    }

    /**
     * 不能捡起
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    fun onPlayerPickupItemEvent(event: PlayerPickupItemEvent) {
        if (Config.checkByPass(event.player)) return
//        if (!Config.item_deny__pickup) return
        val player = event.player
//        val cursor = player.openInventory.cursor
//        if (!cursor.checkAir() && SakuraBindAPI.hasBind(cursor!!)) {
//            event.isCancelled = true
//            event.item.pickupDelay = 10
//            return
//        }
        val item = event.item.itemStack
        if (item.checkAir()) return
        val owner = SakuraBindAPI.getOwner(item) ?: return
        val setting = ItemSettings.getSetting(item)
        val deny = setting.getBoolean("item-deny.pickup", owner.toString(), player)
        if (!deny) return
        event.isCancelled = true
        event.item.pickupDelay = 10
        val sendBackItem = SakuraBindAPI.sendBackItem(owner, listOf(item))
        if (sendBackItem.isEmpty())
            event.item.remove()
        else event.item.itemStack = sendBackItem.first()
        val player1 = Bukkit.getPlayer(owner) ?: Bukkit.getOfflinePlayer(owner)
        if (!EasyCoolDown.check(player.uniqueId, 1000))
            player.sendColorMessage(Lang.item__deny_pickup.formatBy(player1.name))

    }

    /**
     * 不是你的物品不能点
     */
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    fun onInventoryClickEvent(event: InventoryClickEvent) {
//        if (!Config.item_deny__click && !Config.item_deny__inventory) return
        val player = event.whoClicked as? Player ?: return
        if (Config.checkByPass(player)) return
        val item = event.currentItem ?: return
        if (item.checkAir()) return
        val owner = SakuraBindAPI.getOwner(item) ?: return
        val setting = ItemSettings.getSetting(item)
        if (event.clickedInventory == event.view.topInventory &&
            setting.getBoolean("item-deny.click", owner.toString(), player)
        ) {
            if (!EasyCoolDown.check(player.uniqueId, 1000))
                player.sendColorMessage(Lang.item__deny_click)
            event.isCancelled = true
            return
        }
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    fun onInventoryClickEvent2(event: InventoryClickEvent) {
        val whoClicked = event.whoClicked
        if (Config.checkByPass(whoClicked)) return
        val title = event.view.title
        val item = event.currentItem ?: event.cursor ?: return
        val owner = SakuraBindAPI.getOwner(item) ?: return
        val setting = ItemSettings.getSetting(item)
        val boolean = setting.getBoolean("item-deny.inventory", owner.toString(), whoClicked)
        if (!boolean) return
        val stringList = setting.getStringList("item-deny.inventory-pattern")
        val any = stringList.any { title.matches(Regex(it)) }
        if (any) {
            event.isCancelled = true
            if (!EasyCoolDown.check(whoClicked.uniqueId, 1000))
                whoClicked.sendColorMessage(Lang.item__deny_inventory)
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    fun onPlayerCommandPreprocessEvent(event: PlayerCommandPreprocessEvent) {
        if (Config.checkByPass(event.player)) return
        val heldItem = event.player.getHeldItem() ?: return
        val owner = SakuraBindAPI.getOwner(heldItem) ?: return
        val player = event.player
        val setting = ItemSettings.getSetting(heldItem)
        if (!setting.getBoolean("item-deny.command", owner.toString(), player))
            return
        val message = event.message
        val any = setting.getStringList("item-deny.command-pattern").any {
            message.matches(Regex(it))
        }
        if (any) {
            event.isCancelled = true
            if (!EasyCoolDown.check(event.player.uniqueId, 1000)) {
                event.player.sendColorMessage(Lang.item__deny_command)
            }
        }
    }

    /**
     * 禁止用于合成
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onPrepareItemCraftEvent(event: PrepareItemCraftEvent) {
        val inventory = event.inventory
        if (inventory.result == null) return
        val uuid = event.view.player.uniqueId
        for (matrix in inventory.matrix) {
            if (matrix == null || matrix.checkAir()) continue
            val owner = SakuraBindAPI.getOwner(matrix) ?: continue
            if (!ItemSettings.getSetting(matrix)
                    .getBoolean("item-deny.craft", owner.toString(), event.view.player)
            ) continue
            inventory.result = null
            if (!EasyCoolDown.check(uuid, 1000))
                event.view.player.sendColorMessage(Lang.item__deny__craft)
            break
        }
    }

    /**
     * 禁止用于消耗
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    fun onPlayerItemConsumeEvent(event: PlayerItemConsumeEvent) {
        if (Config.checkByPass(event.player)) return
        val item = event.item
        if (item.checkAir()) return
        val owner = SakuraBindAPI.getOwner(item) ?: return
        val setting = ItemSettings.getSetting(item)
        if (!setting.getBoolean("item-deny.consume", owner.toString(), event.player)) return
        if (!EasyCoolDown.check(event.player.uniqueId, 1000))
            event.player.sendColorMessage(Lang.item__deny__consume)
        event.isCancelled = true
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    fun onProjectileLaunchEvent(event: ProjectileLaunchEvent) {
//        if (!Config.item_deny__throw) return
        val entity = event.entity
        val player = entity.shooter as? Player ?: return
        if (Config.checkByPass(player)) return
        val heldItem = player.getHeldItem() ?: return
        val owner = SakuraBindAPI.getOwner(heldItem) ?: return
        if (!ItemSettings.getSetting(heldItem).getBoolean("item-deny.throw", owner.toString(), player)) {
            return
        }
        if (!EasyCoolDown.check(player.uniqueId, 1000))
            player.sendColorMessage(Lang.item__deny_throw)
        event.isCancelled = true
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onBlockBreakEvent(event: BlockBreakEvent) {
        if (Config.checkByPass(event.player)) return
        val inventory = (event.block.state as? InventoryHolder)?.inventory ?: return
        val map = mutableMapOf<UUID, MutableList<ItemStack>>()
        val removed = mutableMapOf<Int, ItemStack>()
        inventory.forEachIndexed { index, itemStack ->
            if (itemStack.checkAir()) return@forEachIndexed
            val owner = SakuraBindAPI.getOwner(itemStack) ?: return@forEachIndexed
            val setting = ItemSettings.getSetting(itemStack)
            if (setting.getBoolean("item-deny.container-break", owner.toString(), event.player)) {
                if (!EasyCoolDown.check(event.player.uniqueId, 1000))
                    event.player.sendColorMessage(Lang.item__deny_container_break)
                event.isCancelled = true
                return
            }
            if (!setting.getBoolean("send-when-container-break", owner.toString(), event.player)) return@forEachIndexed
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

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    fun onEntityDamageEvent(event: EntityDamageEvent) {
//        if (!Config.send_when_lost) return
        val item = event.entity as? Item ?: return
        if (item.isDead) return
        val itemStack = item.itemStack
        val owner = SakuraBindAPI.getOwner(itemStack) ?: return
        if (!ItemSettings.getSetting(itemStack).getBoolean("send-when-lost")) {
            return
        }
        val sendBackItem = SakuraBindAPI.sendBackItem(owner, listOf(itemStack))
        if (sendBackItem.isEmpty())
            item.remove()
        else item.itemStack = sendBackItem.first()
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    fun onItemDespawnEvent(event: ItemDespawnEvent) {
//        if (!Config.send_when_lost) return
        val item = event.entity
        val itemStack = item.itemStack
        val owner = SakuraBindAPI.getOwner(item.itemStack) ?: return
        if (!ItemSettings.getSetting(itemStack).getBoolean("send-when-lost")) {
            return
        }
        val sendBackItem = SakuraBindAPI.sendBackItem(owner, listOf(itemStack))
        if (sendBackItem.isEmpty())
            item.remove()
        else item.itemStack = sendBackItem.first()
        item.remove()
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    fun onBlockDispenseEvent(event: BlockDispenseEvent) {
//        if (!Config.item_deny__dispense) return
        val item = event.item
        if (!SakuraBindAPI.hasBind(item)) return
        if (ItemSettings.getSetting(item).getBoolean("item-deny.dispense")) {
            event.isCancelled = true
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    fun onInventoryPickupItemEvent(event: InventoryPickupItemEvent) {
//        if (!Config.item_deny__hopper) return
        val itemStack = event.item.itemStack
        if (!SakuraBindAPI.hasBind(itemStack)) return
        if (ItemSettings.getSetting(itemStack).getBoolean("item-deny.hopper")) {
            event.isCancelled = true
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    fun onInventoryMoveItemEvent(event: InventoryMoveItemEvent) {
//        if (!Config.item_deny__container_move) return
        val item = event.item
        if (!SakuraBindAPI.hasBind(item)) return
        if (ItemSettings.getSetting(item).getBoolean("item-deny.container-move")) {
            event.isCancelled = true
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    fun autoBindInventoryClickEvent(event: InventoryClickEvent) {
        val player = event.whoClicked
        if (Config.checkByPass(player)) return
        val item = event.currentItem ?: return
        if (item.checkAir()) return
        if (SakuraBindAPI.hasBind(item)) return
        val setting = ItemSettings.getSetting(item, false)
        if (!setting.getBoolean("auto-bind.enable", player = player)) return
        if (setting.getBoolean("auto-bind.onClick", player = player)
            || NBTEditor.contains(item, Config.auto_bind_nbt)
        ) {
            SakuraBindAPI.bind(item, player as Player)
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    fun autoBindPlayerPickupItemEvent(event: PlayerPickupItemEvent) {
        val player = event.player
        if (Config.checkByPass(player)) return
        val item = event.item.itemStack
        if (item.checkAir()) return
        if (SakuraBindAPI.hasBind(item)) return
        val setting = ItemSettings.getSetting(item, false)
        if (!setting.getBoolean("auto-bind.enable", player = player)) return
        if (setting.getBoolean("auto-bind.onPickup", player = player) || NBTEditor.contains(
                item,
                Config.auto_bind_nbt
            )
        ) {
            SakuraBindAPI.bind(item, player)
        }
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    fun autoBindPlayerDropItemEvent(event: PlayerDropItemEvent) {
        if (Config.checkByPass(event.player)) return
        val item = event.itemDrop.itemStack
        if (SakuraBindAPI.hasBind(item)) return
        val setting = ItemSettings.getSetting(item, false)
        if (!setting.getBoolean("auto-bind.enable", player = event.player)) return
        if (setting.getBoolean("auto-bind.onDrop", player = event.player)
            || NBTEditor.contains(item, Config.auto_bind_nbt)
        ) {
            SakuraBindAPI.bind(item, event.player)
        }
    }


    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onItemSpawnEvent(event: ItemSpawnEvent) {
        val entity = event.entity
        val itemStack = entity.itemStack
        val uuid = SakuraBindAPI.getOwner(itemStack) ?: return
        val setting = ItemSettings.getSetting(itemStack)
        val delay = setting.getLong("send-back-delay")
        if (delay == 0L) {
            val sendBackItem = SakuraBindAPI.sendBackItem(uuid, listOf(itemStack))
            if (sendBackItem.isEmpty())
                event.isCancelled = true
            else event.entity.itemStack = sendBackItem.first()
        } else
            DropItemList.putItem(entity, uuid, delay.toInt())
//        if (delay > 0) {
//            submit(delay = delay) {
//                if (event.isCancelled || entity.isDead) return@submit
//                val sendBackItem = SakuraBindAPI.sendBackItem(uuid, listOf(entity.itemStack))
//                if (sendBackItem.isEmpty())
//                    entity.remove()
//            }
//        } else {
//            val sendBackItem = SakuraBindAPI.sendBackItem(uuid, listOf(itemStack))
//            if (sendBackItem.isEmpty())
//                event.isCancelled = true
//            else event.entity.setItemStack(sendBackItem.first())
//        }
    }

    fun onLogin(player: Player) {

        if (!DatabaseConfig.isConnected || Config.login_message_delay < 0) return
        submit(async = true, delay = Config.login_message_delay) {
            if (!player.isOnline) return@submit
            val hasItem = dbTransaction {
                val iterator =
                    PlayerItems.slice(PlayerItems.id).select { PlayerItems.uuid eq player.uniqueId }.limit(1).iterator()
                iterator.hasNext()
            }
//            debug("玩家的暂存箱: ${hasItem}")
            if (!hasItem) return@submit
            player.sendColorMessage(Lang.has_lost_item)
        }
    }

}
