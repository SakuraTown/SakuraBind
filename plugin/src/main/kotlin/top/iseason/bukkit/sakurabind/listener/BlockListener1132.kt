package top.iseason.bukkit.sakurabind.listener

import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockPhysicsEvent
import top.iseason.bukkit.sakurabind.SakuraBindAPI
import top.iseason.bukkit.sakurabind.cache.BlockCache
import top.iseason.bukkit.sakurabind.utils.BindType

object BlockListener1132 : Listener {

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    fun onBlockPhysicsEvent(event: BlockPhysicsEvent) {
        val sourceBlock = event.sourceBlock
        if (!sourceBlock.isEmpty) return
        val owner = SakuraBindAPI.getBlockInfo(sourceBlock)?.first ?: return
        SakuraBindAPI.unbindBlock(sourceBlock, type = BindType.BLOCK_TO_ITEM_UNBIND)
        BlockCache.addBlockTemp(BlockCache.blockToString(sourceBlock), owner)
    }
}