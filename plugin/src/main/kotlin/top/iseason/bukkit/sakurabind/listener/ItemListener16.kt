package top.iseason.bukkit.sakurabind.listener

import org.bukkit.entity.Player
import org.bukkit.entity.ThrowableProjectile
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.entity.ProjectileLaunchEvent
import top.iseason.bukkit.sakurabind.SakuraBindAPI
import top.iseason.bukkit.sakurabind.config.Config
import top.iseason.bukkit.sakurabind.config.ItemSettings
import top.iseason.bukkit.sakurabind.config.Lang
import top.iseason.bukkit.sakurabind.task.DropItemList
import top.iseason.bukkit.sakurabind.utils.MessageTool

object ItemListener16 : Listener {
    /**
     * 禁止弹射物射出
     */
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    fun onProjectileLaunchEvent(event: ProjectileLaunchEvent) {
        val entity = event.entity
        val player = entity.shooter as? Player ?: return
        if (Config.checkByPass(player)) return
        val throwable = entity as? ThrowableProjectile ?: return
        val item = throwable.item
        val owner = SakuraBindAPI.getOwner(item) ?: return
        val setting = ItemSettings.getSetting(item)
        if (setting.getBoolean("item-deny.throw", owner.toString(), player)) {
            event.isCancelled = true
            MessageTool.denyMessageCoolDown(
                player,
                Lang.item__deny_throw,
                ItemSettings.getSetting(item),
                item
            )
        } else {
            val delay = setting.getInt("item.send-back-delay")
            DropItemList.putThrowableItem(entity, owner, delay)
        }
    }
}