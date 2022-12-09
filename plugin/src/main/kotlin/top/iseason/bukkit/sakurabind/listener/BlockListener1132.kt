package top.iseason.bukkit.sakurabind.listener

import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockPhysicsEvent
import top.iseason.bukkit.sakurabind.SakuraBindAPI
import top.iseason.bukkit.sakurabind.cache.BlockCache

object BlockListener1132 : Listener {

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    fun onBlockPhysicsEvent(event: BlockPhysicsEvent) {
        val sourceBlock = event.sourceBlock
        if (!sourceBlock.isEmpty) return
        val owner = BlockCache.getBlockOwner(sourceBlock)?.first ?: return
        SakuraBindAPI.unbindBlock(sourceBlock)
        BlockCache.addBlockTemp(BlockCache.blockToString(sourceBlock), owner)
    }
}