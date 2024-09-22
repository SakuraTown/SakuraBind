package top.iseason.bukkit.sakurabind.listener


import org.bukkit.entity.Item
import org.bukkit.entity.Player
import org.bukkit.entity.ThrowableProjectile
import org.bukkit.event.Cancellable
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityPickupItemEvent
import org.bukkit.event.player.PlayerPickupArrowEvent
import org.bukkit.inventory.ItemStack
import top.iseason.bukkit.sakurabind.SakuraBindAPI
import top.iseason.bukkit.sakurabind.command.CallbackCommand
import top.iseason.bukkit.sakurabind.config.Config
import top.iseason.bukkit.sakurabind.config.ItemSettings
import top.iseason.bukkit.sakurabind.config.Lang
import top.iseason.bukkit.sakurabind.utils.BindType
import top.iseason.bukkit.sakurabind.utils.MessageTool
import top.iseason.bukkit.sakurabind.utils.SendBackType
import top.iseason.bukkittemplate.utils.bukkit.MessageUtils.formatBy
import top.iseason.bukkittemplate.utils.bukkit.MessageUtils.sendColorMessage

object PickupItemListener : Listener {

    @EventHandler(ignoreCancelled = true)
    fun onPlayerPickupArrowEvent(event: PlayerPickupArrowEvent) {
        val entity = event.arrow as? ThrowableProjectile ?: return
        val item = event.item
        playerPickupItem(event.player, item, event)
        if (!item.isValid) entity.remove()
    }

    @EventHandler(ignoreCancelled = true)
    fun onEntityPickupItemEvent(event: EntityPickupItemEvent) {
        val player = event.entity as? Player ?: return
        playerPickupItem(player, event.item, event)
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    fun autoBindPlayerPickupArrowEvent(event: PlayerPickupArrowEvent) {
        event.arrow as? ThrowableProjectile ?: return
        val item = event.item
        checkAutoBind(event.player, item.itemStack)
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    fun autoBindEntityPickupItemEvent(event: EntityPickupItemEvent) {
        val player = event.entity as? Player ?: return
        checkAutoBind(player, event.item.itemStack)
    }

    fun playerPickupItem(player: Player, entity: Item, event: Cancellable) {
        if (Config.checkByPass(player)) return
        val item = entity.itemStack
        val owner = SakuraBindAPI.getOwner(item) ?: return
        val uniqueId = player.uniqueId
        if (owner != uniqueId && CallbackCommand.isCallback(owner)) {
            SakuraBindAPI.sendBackItem(owner, listOf(item), type = SendBackType.COMMON_CALLBACK)
            event.isCancelled = true
            entity.pickupDelay = 10
            player.sendColorMessage(Lang.command__callback)
            return
        }
        val itemSetting = ItemSettings.getSetting(item)
        if (itemSetting.getBoolean("item-deny.pickup", owner.toString(), player)) {
            event.isCancelled = true
            if (owner != uniqueId && itemSetting.getBoolean("item.send-back-on-pickup", owner.toString(), player)) {
                SakuraBindAPI.sendBackItem(owner, listOf(item), type = SendBackType.PLAYER_PICKUP)
                entity.remove()
                MessageTool.denyMessageCoolDown(
                    player,
                    Lang.item__deny_pickup.formatBy(SakuraBindAPI.getOwnerName(owner)),
                    ItemSettings.getSetting(item),
                    item
                )
            }
        }
    }

    fun checkAutoBind(player: Player, item: ItemStack) {
        if (Config.checkByPass(player)) return
        val owner = SakuraBindAPI.getOwner(item)?.toString()
        if (owner != null) {
            val setting = ItemSettings.getSetting(item)
            if (setting.getBoolean("auto-unbind.enable", owner, player) &&
                setting.getBoolean("auto-unbind.onPickup", owner, player)
            ) {
                SakuraBindAPI.unBind(item, BindType.PICKUP_UNBIND_ITEM)
                MessageTool.messageCoolDown(player, Lang.auto_unbind__onPickup)
            }
        } else {
            val setting = ItemSettings.getSetting(item)
            if (setting.getBoolean("auto-bind.enable", null, player) &&
                (setting.getBoolean("auto-bind.onPickup", null, player) ||
                        SakuraBindAPI.isAutoBind(item))
            ) {
                SakuraBindAPI.bind(item, player, type = BindType.PICKUP_BIND_ITEM)
                MessageTool.bindMessageCoolDown(player, Lang.auto_bind__onPickup, setting, item)
            }
        }
    }

}