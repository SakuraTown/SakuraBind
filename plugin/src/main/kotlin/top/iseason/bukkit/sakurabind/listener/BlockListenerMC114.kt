package top.iseason.bukkit.sakurabind.listener

import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockPhysicsEvent
import top.iseason.bukkit.sakurabind.SakuraBindAPI
import top.iseason.bukkit.sakurabind.cache.BlockCache
import top.iseason.bukkit.sakurabind.utils.BindType
import top.iseason.bukkittemplate.debug.debug

object BlockListenerMC114 : Listener {

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    fun onBlockPhysicsEvent(event: BlockPhysicsEvent) {
        val sourceBlock = event.sourceBlock
        if (!sourceBlock.isEmpty) return
        val blockToString = BlockCache.blockToString(sourceBlock)
        val blockInfo = BlockCache.getBlockInfo(blockToString) ?: return
        SakuraBindAPI.unbindBlock(sourceBlock, type = BindType.BLOCK_TO_ITEM_UNBIND)
        BlockCache.addBreakingCache(blockToString, blockInfo)
        debug { "2 add temp $blockToString to ${blockInfo.setting.keyPath} for ${blockInfo.owner}" }
    }
}