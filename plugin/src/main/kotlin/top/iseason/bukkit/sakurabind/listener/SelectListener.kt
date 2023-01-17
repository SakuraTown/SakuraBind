package top.iseason.bukkit.sakurabind.listener

import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerInteractAtEntityEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.event.player.PlayerQuitEvent
import top.iseason.bukkit.sakurabind.SakuraBindAPI
import top.iseason.bukkit.sakurabind.config.Lang
import top.iseason.bukkittemplate.utils.bukkit.ItemUtils.checkAir
import top.iseason.bukkittemplate.utils.bukkit.MessageUtils.formatBy
import top.iseason.bukkittemplate.utils.bukkit.MessageUtils.sendColorMessage
import top.iseason.bukkittemplate.utils.other.WeakCoolDown
import java.util.concurrent.ConcurrentHashMap

object SelectListener : Listener {
    val selecting = ConcurrentHashMap<Player, Any>()
    private val coolDown = WeakCoolDown<Player>()

    @EventHandler(priority = EventPriority.LOWEST)
    fun onPlayerInteractEvent(event: PlayerInteractEvent) {
        val player = event.player
        if (!selecting.containsKey(event.player)) return
        val item = event.item
        if (item.checkAir()) return
        if (coolDown.check(player, 1000)) {
            return
        }
        if (SakuraBindAPI.hasBind(item!!)) {
            player.sendColorMessage(Lang.command__select_has_bind)
            return
        }
        selecting[player] = item
        val name =
            if (item.itemMeta != null && item.itemMeta!!.hasDisplayName()) item.itemMeta!!.displayName else item.type.name
        player.sendColorMessage(Lang.command__select_select_item.formatBy(name, item.amount))
        event.isCancelled = true
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    fun onPlayerInteractEventBlock(event: PlayerInteractEvent) {
        val player = event.player
        if (!selecting.containsKey(event.player)) return
        val block = event.clickedBlock
        if (block == null || block.isEmpty) return
        if (coolDown.check(player, 1000)) {
            return
        }
        if (SakuraBindAPI.getBlockOwner(block) != null) {
            player.sendColorMessage(Lang.command__select_has_bind)
            return
        }
        selecting[player] = block
        player.sendColorMessage(Lang.command__select_select_block.formatBy(block.type, block.x, block.y, block.z))
        event.isCancelled = true
    }

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

    @EventHandler
    fun onPlayerQuit(event: PlayerQuitEvent) {
        selecting.remove(event.player)
    }
}