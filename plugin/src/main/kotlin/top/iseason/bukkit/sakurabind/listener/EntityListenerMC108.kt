package top.iseason.bukkit.sakurabind.listener

import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerInteractAtEntityEvent
import top.iseason.bukkit.sakurabind.SakuraBindAPI
import top.iseason.bukkit.sakurabind.config.Config
import top.iseason.bukkit.sakurabind.config.Lang
import top.iseason.bukkit.sakurabind.utils.MessageTool
import top.iseason.bukkittemplate.utils.bukkit.MessageUtils.formatBy
import java.util.UUID

object EntityListenerMC108 : Listener {
    /**
     * 禁止交互
     */
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    fun onPlayerInteractAtEntityEvent(event: PlayerInteractAtEntityEvent) {
        if (Config.checkByPass(event.player)) {
            return
        }
        val rightClicked = event.rightClicked
        val entityOwner = SakuraBindAPI.getEntityInfo(rightClicked) ?: return
        if (entityOwner.second.getBoolean("entity-deny.interact", entityOwner.first, event.player)) {
            MessageTool.denyMessageCoolDown(
                event.player,
                Lang.entity__deny_interact.formatBy(SakuraBindAPI.getOwnerName(UUID.fromString(entityOwner.first))),
                entityOwner.second,
                entity = rightClicked
            )
            event.isCancelled = true
        }
    }
}