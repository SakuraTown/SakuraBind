package top.iseason.bukkit.sakurabind.listener

import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockPhysicsEvent
import top.iseason.bukkit.sakurabind.cache.CacheManager

object BlockListener1132 : Listener {

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    fun onBlockPhysicsEvent(event: BlockPhysicsEvent) {
        val owner = CacheManager.getBlockOwner(event.sourceBlock)?.first ?: return
        CacheManager.removeBlock(event.sourceBlock)
        CacheManager.addBlockTemp(CacheManager.blockToString(event.sourceBlock), owner)
    }
}