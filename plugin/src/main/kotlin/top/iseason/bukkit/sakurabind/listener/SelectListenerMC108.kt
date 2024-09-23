package top.iseason.bukkit.sakurabind.listener

import org.bukkit.entity.LivingEntity
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerInteractAtEntityEvent
import top.iseason.bukkit.sakurabind.SakuraBindAPI
import top.iseason.bukkit.sakurabind.config.Lang
import top.iseason.bukkit.sakurabind.listener.SelectListener.coolDown
import top.iseason.bukkit.sakurabind.listener.SelectListener.selecting
import top.iseason.bukkittemplate.utils.bukkit.MessageUtils.formatBy
import top.iseason.bukkittemplate.utils.bukkit.MessageUtils.sendColorMessage
import kotlin.collections.set

object SelectListenerMC108 : Listener {

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    fun onPlayerInteractAtEntityEvent(event: PlayerInteractAtEntityEvent) {
        val player = event.player
        if (!selecting.containsKey(event.player)) return
        val rightClicked = event.rightClicked
        if (rightClicked !is LivingEntity) return
        if (coolDown.check(player, 1000)) {
            return
        }
        if (SakuraBindAPI.getEntityOwner(rightClicked) != null)
            player.sendColorMessage(Lang.command__select_has_bind)
        selecting[player] = rightClicked
        player.sendColorMessage(
            Lang.command__select_select_entity.formatBy(
                rightClicked.customName ?: rightClicked.type.name,
                rightClicked.location.blockX, rightClicked.location.blockY, rightClicked.location.blockZ
            )
        )
        event.isCancelled = true
    }

}