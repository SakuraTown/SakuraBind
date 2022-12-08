package top.iseason.bukkit.sakurabind.listener

import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockPhysicsEvent
import top.iseason.bukkit.sakurabind.cache.BlockCache

object BlockListener1132 : Listener {

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    fun onBlockPhysicsEvent(event: BlockPhysicsEvent) {
        val owner = BlockCache.getBlockOwner(event.sourceBlock)?.first ?: return
        BlockCache.removeBlock(event.sourceBlock)
        BlockCache.addBlockTemp(BlockCache.blockToString(event.sourceBlock), owner)
    }
}