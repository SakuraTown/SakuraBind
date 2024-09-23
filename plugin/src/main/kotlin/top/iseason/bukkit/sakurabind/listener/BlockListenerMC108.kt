package top.iseason.bukkit.sakurabind.listener

import org.bukkit.block.PistonMoveReaction
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockExplodeEvent
import org.bukkit.event.block.BlockPistonRetractEvent
import org.bukkit.event.entity.EntityExplodeEvent
import top.iseason.bukkit.sakurabind.SakuraBindAPI
import top.iseason.bukkit.sakurabind.utils.BindType

object BlockListenerMC108 : Listener {

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    fun onBlockExplodeEvent(event: BlockExplodeEvent) {
        val iterator = event.blockList().iterator()
        while (iterator.hasNext()) {
            val next = iterator.next()
            val blockInfo = SakuraBindAPI.getBlockInfo(next) ?: continue
            if (blockInfo.setting.getBoolean("block-deny.explode", null, null)) iterator.remove()
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    fun onEntityExplodeEvent(event: EntityExplodeEvent) {
        val iterator = event.blockList().iterator()
        while (iterator.hasNext()) {
            val next = iterator.next()
            val blockInfo = SakuraBindAPI.getBlockInfo(next) ?: continue
            if (blockInfo.setting.getBoolean("block-deny.explode", null, null)) iterator.remove()
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    fun onBlockPistonRetractEvent(event: BlockPistonRetractEvent) {
        val direction = event.direction
        val cancel = event.blocks.reversed().any {
            if (it.pistonMoveReaction == PistonMoveReaction.BREAK) return@any false
            val blockInfo = SakuraBindAPI.getBlockInfo(it) ?: return@any false
            val denyMove = blockInfo.setting.getBoolean("block-deny.piston", null, null)
            //可以移动
            if (!denyMove) {
                SakuraBindAPI.unbindBlock(it, BindType.BLOCK_MOVE_UNBIND)
                SakuraBindAPI.bindBlock(
                    it.getRelative(direction, 1),
                    blockInfo.ownerUUID,
                    blockInfo.setting,
                    BindType.BLOCK_MOVE_BIND
                )
            }
            denyMove
        }
        if (cancel) {
            event.isCancelled = true
            return
        }
    }
}