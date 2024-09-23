package top.iseason.bukkit.sakurabind.listener

import fr.xephi.authme.api.v3.AuthMeApi
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.entity.Item
import org.bukkit.entity.ItemFrame
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.block.BlockDispenseEvent
import org.bukkit.event.entity.*
import org.bukkit.event.inventory.InventoryAction
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.inventory.InventoryMoveItemEvent
import org.bukkit.event.inventory.InventoryPickupItemEvent
import org.bukkit.event.inventory.PrepareItemCraftEvent
import org.bukkit.event.player.*
import org.bukkit.inventory.InventoryHolder
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.PlayerInventory
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.statements.api.ExposedBlob
import top.iseason.bukkit.sakurabind.SakuraBindAPI
import top.iseason.bukkit.sakurabind.command.CallbackCommand
import top.iseason.bukkit.sakurabind.config.Config
import top.iseason.bukkit.sakurabind.config.ItemSettings
import top.iseason.bukkit.sakurabind.config.Lang
import top.iseason.bukkit.sakurabind.dto.PlayerItem
import top.iseason.bukkit.sakurabind.dto.PlayerItems
import top.iseason.bukkit.sakurabind.hook.AuthMeHook
import top.iseason.bukkit.sakurabind.task.DropItemList
import top.iseason.bukkit.sakurabind.task.EntityRemoveQueue
import top.iseason.bukkit.sakurabind.utils.BindType
import top.iseason.bukkit.sakurabind.utils.MessageTool
import top.iseason.bukkit.sakurabind.utils.PlayerTool
import top.iseason.bukkit.sakurabind.utils.SendBackType
import top.iseason.bukkittemplate.config.DatabaseConfig
import top.iseason.bukkittemplate.config.dbTransaction
import top.iseason.bukkittemplate.utils.bukkit.EntityUtils.getHeldItem
import top.iseason.bukkittemplate.utils.bukkit.ItemUtils.checkAir
import top.iseason.bukkittemplate.utils.bukkit.ItemUtils.toByteArray
import top.iseason.bukkittemplate.utils.bukkit.MessageUtils.sendColorMessage
import top.iseason.bukkittemplate.utils.other.EasyCoolDown
import top.iseason.bukkittemplate.utils.other.runAsync
import top.iseason.bukkittemplate.utils.other.submit
import java.util.*

object ItemListener : Listener {

    /**
     * 互动检查
     */
    @EventHandler(priority = EventPriority.LOW)
    fun onPlayerInteractEvent(event: PlayerInteractEvent) {
        if (Config.checkByPass(event.player)) return
        val action = event.action
        if (action == Action.LEFT_CLICK_AIR || action == Action.LEFT_CLICK_BLOCK) {
            if (SakuraBindAPI.checkDenyBySetting(event.item, event.player, "item-deny.interact-left")) {
                event.isCancelled = true
                MessageTool.denyMessageCoolDown(
                    event.player,
                    Lang.item__deny_interact_left,
                    ItemSettings.getSetting(event.item!!),
                    event.item
                )
            }
        } else if (action == Action.RIGHT_CLICK_AIR || action == Action.RIGHT_CLICK_BLOCK) {
            if (SakuraBindAPI.checkDenyBySetting(event.item, event.player, "item-deny.interact-right")) {
                event.isCancelled = true
                MessageTool.denyMessageCoolDown(
                    event.player,
                    Lang.item__deny_interact_right,
                    ItemSettings.getSetting(event.item!!),
                    event.item
                )
            }
        }
    }

    /**
     * 盔甲架检查
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    fun onPlayerArmorStandManipulateEvent(event: PlayerArmorStandManipulateEvent) {
        if (Config.checkByPass(event.player)) return
        if (SakuraBindAPI.checkDenyBySetting(event.playerItem, event.player, "item-deny.armor-stand")) {
            event.isCancelled = true
            MessageTool.denyMessageCoolDown(
                event.player, Lang.item__deny_armor_stand_set,
                ItemSettings.getSetting(event.playerItem),
                event.playerItem
            )
        } else if (SakuraBindAPI.checkDenyBySetting(event.armorStandItem, event.player, "item-deny.armor-stand")) {
            event.isCancelled = true
            MessageTool.denyMessageCoolDown(
                event.player,
                Lang.item__deny_armor_stand_get,
                ItemSettings.getSetting(event.armorStandItem),
                event.armorStandItem
            )
        }

    }

    /**
     * 不能实体互动
     */
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    fun onPlayerInteractEntityEvent(event: PlayerInteractEntityEvent) {
        val player = event.player
        if (Config.checkByPass(player)) return
        val isItemFrame = event.rightClicked is ItemFrame
        val mainHand = player.getHeldItem()
        val offHand = PlayerTool.getOffHandItem(player)
        //检查展示框
        if (isItemFrame) {
            if (SakuraBindAPI.checkDenyBySetting(mainHand, player, "item-deny.item-frame")
                || (mainHand == null && SakuraBindAPI.checkDenyBySetting(offHand, player, "item-deny.item-frame"))
            ) {
                val item = mainHand ?: offHand!!
                event.isCancelled = true
                MessageTool.denyMessageCoolDown(
                    player, Lang.item__deny_itemFrame,
                    ItemSettings.getSetting(item),
                    item
                )
            }
        } else {
            if (SakuraBindAPI.checkDenyBySetting(mainHand, player, "item-deny.interact-entity")
                || (mainHand == null && SakuraBindAPI.checkDenyBySetting(
                    offHand,
                    player,
                    "item-deny.interact-entity"
                ))
            ) {
                val item = mainHand ?: offHand!!
                event.isCancelled = true
                MessageTool.denyMessageCoolDown(
                    player, Lang.item__deny_entity_interact,
                    ItemSettings.getSetting(item),
                    item
                )
            }
        }
    }


    /**
     * 不能丢
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onPlayerDropItemEvent(event: PlayerDropItemEvent) {
        val player = event.player
        if (Config.checkByPass(player)) return
        val itemDrop = event.itemDrop
        val item = itemDrop.itemStack
        val owner = SakuraBindAPI.getOwner(item) ?: return
        //处理召回
        if (CallbackCommand.isCallback(owner)) {
            SakuraBindAPI.sendBackItem(owner, listOf(item), type = SendBackType.COMMON_CALLBACK)
            MessageTool.messageCoolDown(player, Lang.command__callback)
            return
        }
        if (ItemSettings.getSetting(item).getBoolean("item-deny.drop", owner.toString(), player)) {
            EntityRemoveQueue.syncRemove(itemDrop)
            val openInventory = event.player.openInventory
            val cursor = openInventory.cursor
            val release = event.player.inventory.addItem(item)
            if (cursor != null && cursor != item && !cursor.checkAir()) {
                val releaseCursor = event.player.inventory.addItem(cursor)
                release.putAll(releaseCursor)
                openInventory.cursor = null
            }
            if (release.isNotEmpty()) {
                SakuraBindAPI.sendBackItem(
                    SakuraBindAPI.getOwner(item)!!,
                    release.values.toList(),
                    type = SendBackType.PLAYER_DROP
                )
            }
            MessageTool.denyMessageCoolDown(event.player, Lang.item__deny_drop, ItemSettings.getSetting(item), item)
        }
    }


    /**
     * 物品点击检查，只检查点击上面的物品栏
     */
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    fun onInventoryClickEvent(event: InventoryClickEvent) {
        val player = event.whoClicked as? Player ?: return
        if (Config.checkByPass(player)) return
        val item = event.currentItem ?: return
        if (event.clickedInventory == event.view.topInventory
            && SakuraBindAPI.checkDenyBySetting(item, player, "item-deny.click")
        ) {
            event.isCancelled = true
            MessageTool.denyMessageCoolDown(player, Lang.item__deny_click, ItemSettings.getSetting(item), item)
        }

    }

    /**
     * 上面的物品栏标题符合规则时禁止点击(放入)
     */
    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    fun onInventoryClickEvent2(event: InventoryClickEvent) {
        val whoClicked = event.whoClicked
        if (Config.checkByPass(whoClicked)) return
        val title = event.view.title
        var item: ItemStack?
        if (event.click.name == "SWAP_OFFHAND") {
            item = whoClicked.inventory.itemInOffHand
        } else {
            if (event.hotbarButton >= 0) {
                item = whoClicked.inventory.getItem(event.hotbarButton)
            } else {
                item = event.currentItem
                if (item == null || item.checkAir()) item = event.cursor
            }
        }
        if (item == null || item.checkAir()) return
        val owner = SakuraBindAPI.getOwner(item) ?: return
        val setting = ItemSettings.getSetting(item)
        if (setting.getBoolean("item-deny.inventory", owner.toString(), whoClicked)) {
            val types = setting.getStringList("item-deny.inventory-types")
            val name = event.view.topInventory.type.name
            var any = types.any { name.equals(it, true) }
            if (!any) {
                val namePatterns = setting.getStringList("item-deny.inventory-pattern")
                any = namePatterns.any {
                    title.matches(Regex(it))
                }
            }
            if (any) {
                event.isCancelled = true
                MessageTool.denyMessageCoolDown(whoClicked, Lang.item__deny_inventory, setting, item)
            }
            return
        }

    }

    /**
     * 主手拿着物品时禁止输入符合规则的命令
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    fun onPlayerCommandPreprocessEvent(event: PlayerCommandPreprocessEvent) {
        val player = event.player
        //未登录就不禁止
        if (AuthMeHook.hasHooked && !AuthMeApi.getInstance().isAuthenticated(player)) {
            return
        }
        if (Config.checkByPass(player)) return
        val heldItem = player.getHeldItem() ?: return
        val owner = SakuraBindAPI.getOwner(heldItem) ?: return
        val setting = ItemSettings.getSetting(heldItem)
        if (!setting.getBoolean("item-deny.command", owner.toString(), player))
            return
        val message = event.message
        val any = setting.getStringList("item-deny.command-pattern").any {
            message.matches(Regex(it))
        }
        if (any) {
            event.isCancelled = true
            MessageTool.denyMessageCoolDown(player, Lang.item__deny_command, setting, heldItem)
        }
    }

    /**
     * 禁止用于合成
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    fun onPrepareItemCraftEvent(event: PrepareItemCraftEvent) {
        val inventory = event.inventory
        if (inventory.result == null) return
        val player = event.view.player
        if (Config.checkByPass(player)) return
        for (matrix in inventory.matrix) {
            if (SakuraBindAPI.checkDenyBySetting(matrix, player, "item-deny.craft")) {
                inventory.result = null
                MessageTool.denyMessageCoolDown(player, Lang.item__deny_craft, ItemSettings.getSetting(matrix), matrix)
                break
            }
        }
    }

    /**
     * 禁止用于消耗(吃)
     */
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    fun onPlayerItemConsumeEvent(event: PlayerItemConsumeEvent) {
        val player = event.player
        if (Config.checkByPass(player)) return
        val item = event.item
        if (SakuraBindAPI.checkDenyBySetting(item, player, "item-deny.consume")) {
            event.isCancelled = true
            MessageTool.denyMessageCoolDown(player, Lang.item__deny_consume, ItemSettings.getSetting(item), item)
        } else if (SakuraBindAPI.checkDenyBySetting(PlayerTool.getOffHandItem(player), player, "item-deny.consume")) {
            event.isCancelled = true
            val offHandItem = PlayerTool.getOffHandItem(player)!!
            MessageTool.denyMessageCoolDown(
                player,
                Lang.item__deny_consume,
                ItemSettings.getSetting(offHandItem),
                offHandItem
            )
        }

    }

    /**
     * 物品位于容器中被破坏时的检查
     */
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
                MessageTool.denyMessageCoolDown(event.player, Lang.item__deny_container_break, setting, itemStack)
                event.isCancelled = true
                return
            }
            if (!setting.getBoolean(
                    "item.send-when-container-break",
                    owner.toString(),
                    event.player
                )
            ) return@forEachIndexed
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
                SakuraBindAPI.sendBackItem(uid, items, type = SendBackType.CONTAINER_BREAK)
            }
        }
    }

    /**
     * 检查掉落物被销毁
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    fun onEntityDamageEvent(event: EntityDamageEvent) {
        val item = event.entity as? Item ?: return
        if (item.isDead) return
        val itemStack = item.itemStack
        val filterItem = SakuraBindAPI.filterItem(itemStack) { it.getBoolean("item.send-when-lost", null, null) }
        if (filterItem.isEmpty()) return
        if (itemStack.type == Material.AIR) item.remove()
        for ((uuid, bindItems) in filterItem) {
            SakuraBindAPI.sendBackItem(uuid, bindItems, type = SendBackType.ITEM_DAMAGE)
        }
    }

    /**
     * 检查掉落物由原版的5分钟机制消失
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    fun onItemDespawnEvent(event: ItemDespawnEvent) {
        val item = event.entity
        val itemStack = item.itemStack

        val filterItem = SakuraBindAPI.filterItem(itemStack) { it.getBoolean("item.send-when-lost", null, null) }
        if (filterItem.isEmpty()) return
        if (itemStack.type == Material.AIR) item.remove()
        for ((uuid, bindItems) in filterItem) {
            SakuraBindAPI.sendBackItem(uuid, bindItems, type = SendBackType.ITEM_DE_SPAWN)
        }
    }

    /**
     * 物品由发射器射出,对于 CatServer 1.12.2 似乎无效
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    fun onBlockDispenseEvent(event: BlockDispenseEvent) {
        val item = event.item
        if (SakuraBindAPI.checkDenyBySetting(item, null, "item-deny.dispense")) {
            event.isCancelled = true
        }
    }

    /**
     * 物品被漏斗吸入,对于 CatServer 1.12.2 似乎无效
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    fun onInventoryPickupItemEvent(event: InventoryPickupItemEvent) {
        if (SakuraBindAPI.checkDenyBySetting(event.item.itemStack, null, "item-deny.hopper")) {
            event.isCancelled = true
        }
    }

    /**
     * 物品由一个容器中到另一个容器中去，如漏斗,对于 CatServer 1.12.2 似乎无效
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    fun onInventoryMoveItemEvent(event: InventoryMoveItemEvent) {
        if (SakuraBindAPI.checkDenyBySetting(event.item, null, "item-deny.container-move")) {
            event.isCancelled = true
        }
    }

    /**
     * 自动绑定, 点击时绑定
     */
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    fun autoBindInventoryClickEvent(event: InventoryClickEvent) {
        val player = event.whoClicked
        if (Config.checkByPass(player)) return
        var item: ItemStack? = event.currentItem

        if (item.checkAir() && (event.action == InventoryAction.HOTBAR_SWAP || event.action == InventoryAction.HOTBAR_MOVE_AND_READD)) {
            val bottomInventory = event.view.bottomInventory
            if (event.hotbarButton == -1) {
                val playerInventory = bottomInventory as PlayerInventory
                item = playerInventory.getItem(playerInventory.size - 1)
            } else {
                item = bottomInventory.getItem(event.hotbarButton)
            }
        }

        if (item == null || item.checkAir()) return
        val owner = SakuraBindAPI.getOwner(item)?.toString()
        if (owner != null) {
            val setting = ItemSettings.getSetting(item)
            if (setting.getBoolean("auto-unbind.enable", owner, player) &&
                setting.getBoolean("auto-unbind.onClick", owner, player)
            ) {
                SakuraBindAPI.unBind(item, BindType.CLICK_UNBIND_ITEM)
                MessageTool.messageCoolDown(player, Lang.auto_unbind__onClick)
            }
        } else {
            val setting = ItemSettings.getSetting(item)
            if (setting.getBoolean("auto-bind.enable", null, player) &&
                (setting.getBoolean("auto-bind.onClick", null, player) ||
                        SakuraBindAPI.isAutoBind(item))
            ) {
                SakuraBindAPI.bind(item, player as Player, type = BindType.CLICK_BIND_ITEM)
                MessageTool.bindMessageCoolDown(player, Lang.auto_bind__onClick, setting, item)
            }
        }
    }


    /**
     * 自动绑定, 丢弃物品时绑定
     */
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    fun autoBindPlayerDropItemEvent(event: PlayerDropItemEvent) {
        val player = event.player
        if (Config.checkByPass(player)) return
        val item = event.itemDrop.itemStack
        val owner = SakuraBindAPI.getOwner(item)
        val ownerStr = owner?.toString()
        if (ownerStr != null) {
            val setting = ItemSettings.getSetting(item)
            if (setting.getBoolean("auto-unbind.enable", ownerStr, player) &&
                (setting.getBoolean("auto-unbind.onDrop", ownerStr, player))
            ) {
                SakuraBindAPI.unBind(item, BindType.DROP_UNBIND_ITEM)
                MessageTool.messageCoolDown(player, Lang.auto_unbind__onDrop)
            }
        } else {
            val setting = ItemSettings.getSetting(item)
            if (setting.getBoolean("auto-bind.enable", null, player) &&
                (setting.getBoolean("auto-bind.onDrop", null, player) ||
                        SakuraBindAPI.isAutoBind(item))
            ) {
                SakuraBindAPI.bind(item, player, type = BindType.DROP_BIND_ITEM)
                MessageTool.bindMessageCoolDown(player, Lang.auto_bind__onDrop, setting, item)
            }
        }
    }

    /**
     * 自动绑定/解绑, 左右键绑定
     */
    @EventHandler(priority = EventPriority.LOWEST)
    fun autoBindPlayerInteractEvent(event: PlayerInteractEvent) {
        val player = event.player
        val item = event.item ?: return
        if (Config.checkByPass(player)) return
        if (item.checkAir()) return
        val ownerStr = SakuraBindAPI.getOwner(item)?.toString()
        val action = event.action
        if (ownerStr != null) {
            val setting = ItemSettings.getSetting(item)
            if (!setting.getBoolean("auto-unbind.enable", ownerStr, player)) {
                return
            }
            if ((action == Action.LEFT_CLICK_BLOCK || action == Action.LEFT_CLICK_AIR) &&
                (setting.getBoolean("auto-unbind.onLeft", ownerStr, player))
            ) {
                SakuraBindAPI.unBind(item, BindType.LEFT_UNBIND_ITEM)
                MessageTool.messageCoolDown(player, Lang.auto_unbind__onLeft)
            } else if ((action == Action.RIGHT_CLICK_BLOCK || action == Action.RIGHT_CLICK_AIR) &&
                (setting.getBoolean("auto-unbind.onRight", ownerStr, player))
            ) {
                SakuraBindAPI.unBind(item, BindType.RIGHT_UNBIND_ITEM)
                MessageTool.messageCoolDown(player, Lang.auto_unbind__onRight)
            }
        } else {
            val setting = ItemSettings.getSetting(item)
            if (!setting.getBoolean("auto-bind.enable", null, player)) {
                return
            }
            if ((action == Action.LEFT_CLICK_BLOCK || action == Action.LEFT_CLICK_AIR) &&
                (setting.getBoolean("auto-bind.onLeft", null, player) ||
                        SakuraBindAPI.isAutoBind(item))
            ) {
                SakuraBindAPI.bind(item, player, type = BindType.LEFT_BIND_ITEM)
                MessageTool.bindMessageCoolDown(player, Lang.auto_bind__onLeft, setting, item)
            } else if ((action == Action.RIGHT_CLICK_BLOCK || action == Action.RIGHT_CLICK_AIR) &&
                (setting.getBoolean("auto-bind.onRight", null, player) ||
                        SakuraBindAPI.isAutoBind(item))
            ) {
                SakuraBindAPI.bind(item, player, type = BindType.RIGHT_BIND_ITEM)
                MessageTool.bindMessageCoolDown(player, Lang.auto_bind__onRight, setting, item)
            }
        }
    }

    /**
     * 自动绑定/解绑, 左键实体(其实是攻击动作，但是 PlayerInteractEvent 没有捕获)
     */
    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    fun autoBindEntityDamageByEntityEvent(event: EntityDamageByEntityEvent) {
        val player = event.damager as? Player ?: return
        val item = player.getHeldItem() ?: return
        if (Config.checkByPass(player)) return
        val ownerStr = SakuraBindAPI.getOwner(item)?.toString()
        if (ownerStr != null) {
            val setting = ItemSettings.getSetting(item)
            if (!setting.getBoolean("auto-unbind.enable", ownerStr, player)) {
                return
            }
            if ((setting.getBoolean("auto-unbind.onLeft", ownerStr, player))) {
                SakuraBindAPI.unBind(item, BindType.LEFT_UNBIND_ITEM)
                MessageTool.messageCoolDown(player, Lang.auto_unbind__onLeft)
            }
        } else {
            val setting = ItemSettings.getSetting(item)
            if (!setting.getBoolean("auto-bind.enable", null, player)) {
                return
            }
            if ((setting.getBoolean("auto-bind.onLeft", null, player) ||
                        SakuraBindAPI.isAutoBind(item))
            ) {
                SakuraBindAPI.bind(item, player, type = BindType.LEFT_BIND_ITEM)
                MessageTool.bindMessageCoolDown(player, Lang.auto_bind__onLeft, setting, item)
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    fun autoBindPlayerItemDamageEvent(event: PlayerItemDamageEvent) {
        val player = event.player
        if (Config.checkByPass(player)) return
        val item = event.item
        val owner = SakuraBindAPI.getOwner(item)?.toString()
        if (owner != null) {
            val setting = ItemSettings.getSetting(item)
            if (setting.getBoolean("auto-unbind.enable", owner, player) &&
                (setting.getBoolean("auto-unbind.onUse", owner, player) ||
                        SakuraBindAPI.isAutoBind(item))
            ) {
                SakuraBindAPI.unBind(item, BindType.USE_UNBIND_ITEM)
                MessageTool.messageCoolDown(player, Lang.auto_unbind__onUse)
            }
        } else {
            val setting = ItemSettings.getSetting(item)
            if (setting.getBoolean("auto-bind.enable", null, player) &&
                (setting.getBoolean("auto-bind.onUse", null, player) ||
                        SakuraBindAPI.isAutoBind(item))
            ) {
                SakuraBindAPI.bind(item, player, type = BindType.USE_BIND_ITEM)
                MessageTool.bindMessageCoolDown(player, Lang.auto_bind__onUse, setting, item)
            }
        }
    }

    /**
     * 处理掉落物的监听器
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onItemSpawnEvent(event: ItemSpawnEvent) {
        val entity = event.entity
        if (EntityRemoveQueue.isRemoved(entity)) {
            return
        }
        val itemStack = entity.itemStack
        // 某些mod物品tp时会变成2个的奇怪bug catserver 1.12.2
        if (entity.location.y <= Short.MIN_VALUE.toDouble()) {
            event.isCancelled = true
            return
        }
//        // 处理即刻返还
//        for ((uuid, list) in SakuraBindAPI.filterItem(itemStack) { it.getInt("item.send-back-delay") == 0 }) {
//            SakuraBindAPI.sendBackItem(uuid, list)
//        }
//        if (itemStack.type != Material.AIR) {
//            entity.itemStack = itemStack
//        } else {
//            event.isCancelled = true
//            return
//        }
        val owner = SakuraBindAPI.getOwner(itemStack)
        if (owner != null) {
            val delay = SakuraBindAPI.getItemSetting(itemStack).getInt("item.send-back-delay")
            DropItemList.putDropItem(entity, owner, delay)
        } else {
            DropItemList.putDropInnerItem(entity)
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    fun onPlayerDeathEvent(event: PlayerDeathEvent) {
        //item_deny__drop_on_death
        val entity = event.entity
        if (event.keepInventory || Config.checkByPass(entity)) {
            return
        }
        val iterator = event.drops.iterator()
        val sendBackList = mutableListOf<ItemStack>()
        while (iterator.hasNext()) {
            val next = iterator.next()
            val owner = SakuraBindAPI.getOwner(next) ?: continue
            val setting = ItemSettings.getSetting(next)
            if (
//                !CallbackCommand.isCallback(owner) &&
                !setting.getBoolean("item-deny.drop-on-death", owner.toString(), entity)
            ) continue
            iterator.remove()
            sendBackList.add(next)
        }
        if (sendBackList.isNotEmpty()) {
            submit(async = true) {
                SakuraBindAPI.sendBackItem(entity.uniqueId, sendBackList, type = SendBackType.PLAYER_DEATH)
            }
        }
    }

    /**
     * 玩家登录时检查暂存箱子是否有物品
     */
    fun onLogin(player: Player) {
        if (!DatabaseConfig.isConnected || Config.login_message_delay < 0) return
        if (EasyCoolDown.check("${player.uniqueId}-login_message", 60000)
        ) {
            return
        }
        submit(async = true, delay = Config.login_message_delay) {
            if (!player.isOnline) return@submit
            val hasItem = dbTransaction {
                val iterator =
                    PlayerItems.select(PlayerItems.id).where { PlayerItems.uuid eq player.uniqueId }.limit(1)
                        .iterator()
                iterator.hasNext()
            }
//           debug{"玩家的暂存箱: ${hasItem}"}
            if (!hasItem) return@submit
            player.sendColorMessage(Lang.has_lost_item)
        }
    }

    @EventHandler
    fun onPlayerQuit(event: PlayerQuitEvent) {
        if (!Config.temp_chest_purge_on_quit) return
        val uuid = event.player.uniqueId
        // 每人2小时冷却 5分钟只能一个
        if (EasyCoolDown.check("purge-database", 300000) ||
            EasyCoolDown.check("$uuid-purge-database", 7200000)
        ) {
            return
        }
        runAsync {
            val items = dbTransaction {
                PlayerItem.find { PlayerItems.uuid eq uuid }.toList()
            }
            if (items.size <= 1) return@runAsync
            val inventories = mutableListOf(Bukkit.createInventory(null, 36))
            var temp: Array<ItemStack>
            var index: Int
            for (item in items) {
                temp = item.getItemStacks().toTypedArray()
                index = 0
                while (temp.isNotEmpty()) {
                    var inventory = inventories.getOrNull(index)
                    if (inventory == null) {
                        inventory = Bukkit.createInventory(null, 36)
                        inventories.add(inventory)
                    }
                    temp = inventory.addItem(*temp).values.toTypedArray()
                    index++
                }
            }
            dbTransaction {
                PlayerItems.deleteWhere { PlayerItems.uuid eq uuid }
                for (inventory in inventories) {
                    val itemStacks = inventory.filter { !it.checkAir() }
                    if (itemStacks.isEmpty()) continue
                    PlayerItem.new {
                        this.uuid = uuid
                        this.item = ExposedBlob(itemStacks.toByteArray())
                    }
                }
            }
        }

    }


}
