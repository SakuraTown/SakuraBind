package top.iseason.bukkit.sakurabind.listener

import fr.xephi.authme.api.v3.AuthMeApi
import io.github.bananapuncher714.nbteditor.NBTEditor
import org.bukkit.Bukkit
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
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.inventory.InventoryMoveItemEvent
import org.bukkit.event.inventory.InventoryPickupItemEvent
import org.bukkit.event.inventory.PrepareItemCraftEvent
import org.bukkit.event.player.*
import org.bukkit.inventory.InventoryHolder
import org.bukkit.inventory.ItemStack
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.select
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
import top.iseason.bukkit.sakurabind.utils.BindType
import top.iseason.bukkit.sakurabind.utils.MessageTool
import top.iseason.bukkit.sakurabind.utils.PlayerTool
import top.iseason.bukkittemplate.config.DatabaseConfig
import top.iseason.bukkittemplate.config.dbTransaction
import top.iseason.bukkittemplate.utils.bukkit.EntityUtils.getHeldItem
import top.iseason.bukkittemplate.utils.bukkit.ItemUtils.checkAir
import top.iseason.bukkittemplate.utils.bukkit.ItemUtils.toByteArray
import top.iseason.bukkittemplate.utils.bukkit.MessageUtils.formatBy
import top.iseason.bukkittemplate.utils.bukkit.MessageUtils.sendColorMessage
import top.iseason.bukkittemplate.utils.other.EasyCoolDown
import top.iseason.bukkittemplate.utils.other.runAsync
import top.iseason.bukkittemplate.utils.other.submit
import java.util.*

object ItemListener : Listener {

    /**
     * 互动检查
     */
    @EventHandler(priority = EventPriority.LOWEST)
    fun onPlayerInteractEvent(event: PlayerInteractEvent) {
        if (Config.checkByPass(event.player)) return
        val action = event.action
        if (action == Action.LEFT_CLICK_AIR || action == Action.LEFT_CLICK_BLOCK) {
            if (SakuraBindAPI.checkDenyBySetting(event.item, event.player, "item-deny.interact-left")) {
                event.isCancelled = true
                MessageTool.denyMessageCoolDown(
                    event.player,
                    Lang.item__deny_interact,
                    ItemSettings.getSetting(event.item!!),
                    event.item
                )
            }
        } else if (action == Action.RIGHT_CLICK_AIR || action == Action.RIGHT_CLICK_BLOCK) {
            if (SakuraBindAPI.checkDenyBySetting(event.item, event.player, "item-deny.interact-right")) {
                event.isCancelled = true
                MessageTool.denyMessageCoolDown(
                    event.player,
                    Lang.item__deny_interact,
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
        if (Config.checkByPass(event.player)) return
        val item = event.itemDrop.itemStack
        if (SakuraBindAPI.checkDenyBySetting(item, event.player, "item-deny.drop")) {
            event.itemDrop.remove()
            val openInventory = event.player.openInventory
            val cursor = openInventory.cursor
            val release = event.player.inventory.addItem(item)
            if (cursor != null && cursor != item && !cursor.checkAir()) {
                val releaseCursor = event.player.inventory.addItem(cursor)
                release.putAll(releaseCursor)
                openInventory.cursor = null
            }
            if (release.isNotEmpty()) {
                SakuraBindAPI.sendBackItem(SakuraBindAPI.getOwner(item)!!, release.values.toList())
            }
            MessageTool.denyMessageCoolDown(event.player, Lang.item__deny_drop, ItemSettings.getSetting(item), item)
        }
    }

    /**
     * 不能捡起
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    fun onPlayerPickupItemEvent(event: PlayerPickupItemEvent) {
        if (Config.checkByPass(event.player)) return
        val player = event.player
        val item = event.item.itemStack
        val owner = SakuraBindAPI.getOwner(item) ?: return
        if (CallbackCommand.isCallback(owner)) {
            val sendBackItem = SakuraBindAPI.sendBackItem(owner, listOf(item))
            if (sendBackItem.isEmpty())
                event.item.remove()
            else event.item.itemStack = sendBackItem.first()
            event.isCancelled = true
            event.item.pickupDelay = 10
            player.sendColorMessage(Lang.command__callback)
            return
        }
        val itemSetting = ItemSettings.getSetting(item)
        if (itemSetting.getBoolean("item-deny.pickup", owner.toString(), player)) {
            event.isCancelled = true
            event.item.pickupDelay = 10
            if (itemSetting.getBoolean("item.send-back-on-pickup", owner.toString(), player)) {
                val sendBackItem = SakuraBindAPI.sendBackItem(owner, listOf(item))
                if (sendBackItem.isEmpty())
                    event.item.remove()
                else event.item.itemStack = sendBackItem.first()
                MessageTool.denyMessageCoolDown(
                    player,
                    Lang.item__deny_pickup.formatBy(SakuraBindAPI.getOwnerName(owner)),
                    ItemSettings.getSetting(item),
                    item
                )
            }
        }
    }

    /**
     * 物品点击检查，只检查点击上面的物品栏
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
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
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    fun onInventoryClickEvent2(event: InventoryClickEvent) {
        val whoClicked = event.whoClicked
        if (Config.checkByPass(whoClicked)) return

        val title = event.view.title
        var item: ItemStack?
        if (event.hotbarButton >= 0) {
            item = whoClicked.inventory.getItem(event.hotbarButton)
        } else {
            item = event.currentItem
            if (item == null || item.checkAir()) item = event.cursor
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
        for (matrix in inventory.matrix) {
            if (!SakuraBindAPI.checkDenyBySetting(matrix, player, "item-deny.craft")) continue
            inventory.result = null
            MessageTool.denyMessageCoolDown(player, Lang.item__deny_craft, ItemSettings.getSetting(matrix), matrix)
            break
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
     * 禁止弹射物射出
     */
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    fun onProjectileLaunchEvent(event: ProjectileLaunchEvent) {
        val entity = event.entity
        val player = entity.shooter as? Player ?: return
        if (Config.checkByPass(player)) return
        val heldItem = player.getHeldItem() ?: return
        if (SakuraBindAPI.checkDenyBySetting(heldItem, player, "item-deny.throw")) {
            event.isCancelled = true
            MessageTool.denyMessageCoolDown(
                player,
                Lang.item__deny_throw,
                ItemSettings.getSetting(heldItem),
                heldItem
            )
        } else {
            val offHandItem = PlayerTool.getOffHandItem(player)
            if (offHandItem.checkAir()) return
            if (SakuraBindAPI.checkDenyBySetting(offHandItem, player, "item-deny.throw")) {
                event.isCancelled = true
                MessageTool.denyMessageCoolDown(
                    player,
                    Lang.item__deny_throw,
                    ItemSettings.getSetting(offHandItem!!),
                    offHandItem
                )
            }
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
                SakuraBindAPI.sendBackItem(uid, items)
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
        val owner = SakuraBindAPI.getOwner(itemStack) ?: return
        if (CallbackCommand.isCallback(owner) ||
            ItemSettings.getSetting(itemStack).getBoolean("item.send-when-lost", null, null)
        ) {
            val sendBackItem = SakuraBindAPI.sendBackItem(owner, listOf(itemStack))
            if (sendBackItem.isEmpty())
                item.remove()
            else item.itemStack = sendBackItem.first()
        }
    }

    /**
     * 检查掉落物由原版的5分钟机制消失
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    fun onItemDespawnEvent(event: ItemDespawnEvent) {
        val item = event.entity
        val itemStack = item.itemStack
        val owner = SakuraBindAPI.getOwner(item.itemStack) ?: return
        if (!CallbackCommand.isCallback(owner)
            && !ItemSettings.getSetting(itemStack).getBoolean("item.send-when-lost", null, null)
        ) {
            return
        }
        val sendBackItem = SakuraBindAPI.sendBackItem(owner, listOf(itemStack))
        if (sendBackItem.isEmpty())
            item.remove()
        else item.itemStack = sendBackItem.first()
        item.remove()
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
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    fun autoBindInventoryClickEvent(event: InventoryClickEvent) {
        val player = event.whoClicked
        if (Config.checkByPass(player)) return
        val item = event.currentItem ?: return
        if (item.checkAir()) return
        val owner = SakuraBindAPI.getOwner(item)?.toString()
        if (owner != null) {
            val setting = ItemSettings.getSetting(item)
            if (setting.getBoolean("auto-unbind.enable", owner, player) &&
                (setting.getBoolean("auto-unbind.onClick", owner, player) ||
                        NBTEditor.contains(item, Config.auto_bind_nbt))
            ) {
                SakuraBindAPI.unBind(item, BindType.CLICK_UNBIND_ITEM)
                MessageTool.messageCoolDown(player, Lang.auto_unbind__onClick)
            }
        } else {
            val setting = ItemSettings.getSetting(item, false)
            if (setting.getBoolean("auto-bind.enable", null, player) &&
                (setting.getBoolean("auto-bind.onClick", null, player) ||
                        NBTEditor.contains(item, Config.auto_bind_nbt))
            ) {
                SakuraBindAPI.bind(item, player as Player, type = BindType.CLICK_BIND_ITEM)
                MessageTool.bindMessageCoolDown(player, Lang.auto_bind__onClick, setting, item)
            }
        }
    }


    /**
     * 自动绑定, 捡起物品时绑定
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    fun autoBindPlayerPickupItemEvent(event: PlayerPickupItemEvent) {
        val player = event.player
        if (Config.checkByPass(player)) return
        val item = event.item.itemStack
        if (item.checkAir()) return
        val owner = SakuraBindAPI.getOwner(item)?.toString()
        if (owner != null) {
            val setting = ItemSettings.getSetting(item)
            if (setting.getBoolean("auto-unbind.enable", owner, player) &&
                (setting.getBoolean("auto-unbind.onPickup", owner, player) ||
                        NBTEditor.contains(item, Config.auto_bind_nbt))
            ) {
                SakuraBindAPI.unBind(item, BindType.PICKUP_UNBIND_ITEM)
                MessageTool.messageCoolDown(player, Lang.auto_unbind__onPickup)
            }
        } else {
            val setting = ItemSettings.getSetting(item, false)
            if (setting.getBoolean("auto-bind.enable", null, player) &&
                (setting.getBoolean("auto-bind.onPickup", null, player) ||
                        NBTEditor.contains(item, Config.auto_bind_nbt))
            ) {
                SakuraBindAPI.bind(item, player, type = BindType.PICKUP_BIND_ITEM)
                MessageTool.bindMessageCoolDown(player, Lang.auto_bind__onPickup, setting, item)
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
        //处理召回
        if (CallbackCommand.isCallback(owner)) {
            event.itemDrop.itemStack
            val sendBackItem = SakuraBindAPI.sendBackItem(owner!!, listOf(item))
            if (sendBackItem.isEmpty())
                event.itemDrop.remove()
            else event.itemDrop.itemStack = sendBackItem.first()
            player.sendColorMessage(Lang.command__callback)
            return
        }
        val ownerStr = owner?.toString()
        if (ownerStr != null) {
            val setting = ItemSettings.getSetting(item)
            if (setting.getBoolean("auto-unbind.enable", ownerStr, player) &&
                (setting.getBoolean("auto-unbind.onDrop", ownerStr, player) ||
                        NBTEditor.contains(item, Config.auto_bind_nbt))
            ) {
                SakuraBindAPI.unBind(item, BindType.DROP_UNBIND_ITEM)
                MessageTool.messageCoolDown(player, Lang.auto_unbind__onDrop)
            }
        } else {
            val setting = ItemSettings.getSetting(item, false)
            if (setting.getBoolean("auto-bind.enable", null, player) &&
                (setting.getBoolean("auto-bind.onDrop", null, player) ||
                        NBTEditor.contains(item, Config.auto_bind_nbt))
            ) {
                SakuraBindAPI.bind(item, player, type = BindType.DROP_BIND_ITEM)
                MessageTool.bindMessageCoolDown(player, Lang.auto_bind__onDrop, setting, item)
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
                        NBTEditor.contains(item, Config.auto_bind_nbt))
            ) {
                SakuraBindAPI.unBind(item, BindType.USE_UNBIND_ITEM)
                MessageTool.messageCoolDown(player, Lang.auto_unbind__onUse)
            }
        } else {
            val setting = ItemSettings.getSetting(item, false)
            if (setting.getBoolean("auto-bind.enable", null, player) &&
                (setting.getBoolean("auto-bind.onUse", null, player) ||
                        NBTEditor.contains(item, Config.auto_bind_nbt))
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
        val itemStack = entity.itemStack
        val uuid = SakuraBindAPI.getOwner(itemStack) ?: return
        val setting = ItemSettings.getSetting(itemStack)
        val delay = setting.getLong("item.send-back-delay")
        if (delay == 0L) {
            val sendBackItem = SakuraBindAPI.sendBackItem(uuid, listOf(itemStack))
            if (sendBackItem.isEmpty())
                event.isCancelled = true
            else event.entity.itemStack = sendBackItem.first()
        } else
            DropItemList.putItem(entity, uuid, delay.toInt())
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
            if (!CallbackCommand.isCallback(owner)
                && !setting.getBoolean("item-deny.drop-on-death", owner.toString(), entity)
            ) continue
            iterator.remove()
            sendBackList.add(next)
        }
        if (sendBackList.isNotEmpty()) {
            submit(async = true) {
                SakuraBindAPI.sendBackItem(entity.uniqueId, sendBackList)
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
                    PlayerItems.slice(PlayerItems.id).select { PlayerItems.uuid eq player.uniqueId }.limit(1)
                        .iterator()
                iterator.hasNext()
            }
//            debug("玩家的暂存箱: ${hasItem}")
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
