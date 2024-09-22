package top.iseason.bukkit.sakurabind.listener

import org.bukkit.GameMode
import org.bukkit.Material
import org.bukkit.block.PistonMoveReaction
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.block.*
import org.bukkit.event.entity.EntityChangeBlockEvent
import org.bukkit.event.entity.EntityExplodeEvent
import org.bukkit.event.entity.ItemSpawnEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.inventory.InventoryHolder
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.BlockStateMeta
import top.iseason.bukkit.sakurabind.SakuraBindAPI
import top.iseason.bukkit.sakurabind.cache.BlockCache
import top.iseason.bukkit.sakurabind.cache.FallingBlockCache
import top.iseason.bukkit.sakurabind.config.BaseSetting
import top.iseason.bukkit.sakurabind.config.Config
import top.iseason.bukkit.sakurabind.config.ItemSettings
import top.iseason.bukkit.sakurabind.config.Lang
import top.iseason.bukkit.sakurabind.task.EntityRemoveQueue
import top.iseason.bukkit.sakurabind.task.FallingList
import top.iseason.bukkit.sakurabind.utils.BindType
import top.iseason.bukkit.sakurabind.utils.MessageTool
import top.iseason.bukkit.sakurabind.utils.SendBackType
import top.iseason.bukkittemplate.utils.bukkit.ItemUtils.checkAir
import top.iseason.bukkittemplate.utils.bukkit.MessageUtils.formatBy
import top.iseason.bukkittemplate.utils.other.runSync

/**
 * 方块物品监听器
 */
object BlockListener : Listener {
    /**
     * 玩家与方块互动
     */
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    fun onPlayerInteractEvent(event: PlayerInteractEvent) {
        if (Config.checkByPass(event.player)) return
        val player = event.player
        if (event.clickedBlock != null) {
            val blockInfo = SakuraBindAPI.getBlockInfo(event.clickedBlock!!) ?: return
            if (blockInfo.setting.getBoolean("block-deny.interact", blockInfo.owner, player)) {
                event.isCancelled = true
                MessageTool.denyMessageCoolDown(
                    player,
                    Lang.block__deny_interact.formatBy(
                        SakuraBindAPI.getOwnerName(blockInfo.ownerUUID)
                    ),
                    blockInfo.setting,
                    block = event.clickedBlock
                )
                return
            }
        }
    }

    /**
     * 方块放置
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onBlockPlaceEvent(event: BlockPlaceEvent) {
        val player = event.player
        val block = event.block
//        if (Config.checkByPass(player)) return
        //覆盖检查，比如草被覆盖，但是不实用
//        if (BlockCache.getOwner(event.block) != null) {
//            MessageTool.sendCoolDown(event.player, Lang.block__deny_place_exist)
//            event.isCancelled = true
//            return
//        }
        val itemInHand = event.itemInHand
        if (itemInHand.checkAir()) return
        val owner = SakuraBindAPI.getOwner(itemInHand) ?: return
        val setting = SakuraBindAPI.getItemSetting(itemInHand)
        val deny = checkDenyBlockPlace(player, itemInHand, setting, owner.toString()) ?: return
        if (deny) {
            event.isCancelled = true
            return
        }
        SakuraBindAPI.bindBlock(player, itemInHand, block, owner, setting, false)
    }

    /**
     * 多方快放置
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onBlockMultiPlaceEvent(event: BlockMultiPlaceEvent) {
        val player = event.player
//        if (Config.checkByPass(player)) return
        val heldItem = event.itemInHand
        val owner = SakuraBindAPI.getOwner(heldItem) ?: return
        val setting = SakuraBindAPI.getItemSetting(heldItem)
//        if (!heldItem.checkAir()) { //主手放置物品
//            owner = SakuraBindAPI.getOwner(heldItem!!) ?: return
//            setting = SakuraBindAPI.getItemSetting(heldItem)
//        } else {
//            heldItem = PlayerTool.getOffHandItem(player)
//            if (!heldItem.checkAir()) { // 副手放置物品
//                owner = SakuraBindAPI.getOwner(heldItem!!) ?: return
//                setting = SakuraBindAPI.getItemSetting(heldItem)
//            }
//        }
        val deny = checkDenyBlockPlace(player, heldItem, setting, owner.toString()) ?: return
        if (deny) {
            event.isCancelled = true
            return
        }
        val rawLoc = event.block.location
        val x = rawLoc.x
        val y = rawLoc.y
        val z = rawLoc.z
        for (state in event.replacedBlockStates) {
            val loc = state.location
            if (x == loc.x && y == loc.y && z == loc.z) continue
            SakuraBindAPI.bindBlock(player, heldItem, state.block, owner, setting, true)
        }
    }

    private fun checkDenyBlockPlace(player: Player, item: ItemStack, setting: BaseSetting, owner: String): Boolean? {
        if (setting.getBoolean("block-deny.bind-from-item", owner, player)) return null
        if (!Config.checkByPass(player) && setting.getBoolean("block-deny.place", owner, player)) {
            MessageTool.denyMessageCoolDown(
                player, Lang.block__deny_place,
                setting,
                item
            )
            return true
        }
        return false
    }


    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    fun onBlockBreakEvent(event: BlockBreakEvent) {
        val block = event.block
        val player = event.player
        val blockInfo = SakuraBindAPI.getBlockInfo(block) ?: return
        val owner = blockInfo.owner
        // 有主人但可以破坏
        val deny = blockInfo.setting.getBoolean("block-deny.break", owner, event.player)
        //可以破坏
        if (Config.checkByPass(event.player) || !deny) {
            val blockToString = BlockCache.blockToString(block)
            BlockCache.addBreakingCache(blockToString, blockInfo)
            //没有掉落物直接删除
            if (event.player.gameMode == GameMode.CREATIVE) {
                SakuraBindAPI.unbindBlock(block, type = BindType.BLOCK_TO_ITEM_UNBIND)
            }
            val state = block.state
            if (state is InventoryHolder && state.inventory.size > 0) {
                val containerCache = BlockCache.containerCache
                containerCache.put(blockToString, block.type)
                runSync {
                    containerCache.remove(blockToString)
                }
            }
        } else {
            event.isCancelled = true
            MessageTool.denyMessageCoolDown(
                player, Lang.block__deny_break.formatBy(SakuraBindAPI.getOwnerName(blockInfo.ownerUUID)),
                blockInfo.setting, block = block
            )
        }
    }

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
    fun onBlockPhysicsEvent(event: BlockPhysicsEvent) {
        val block = event.block
        //只检查变成空气的
        if (!block.isEmpty) return
        val blockToString = BlockCache.blockToString(block)
        val blockInfo = BlockCache.getBlockInfo(blockToString) ?: return
        SakuraBindAPI.unbindBlock(block, BindType.BLOCK_TO_ITEM_UNBIND)
//        println("${event.block.type} -> ${event.changedType} ${event.block.location}")
//        if(event.changedType==Material.AIR)
        BlockCache.addBreakingCache(blockToString, blockInfo)
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    fun onBlockFrom(event: BlockFromToEvent) {
        val toBlock = event.toBlock
        val blockInfo = SakuraBindAPI.getBlockInfo(toBlock) ?: return
        if (!blockInfo.setting.getBoolean("block-deny.flow", null, null)) {
            SakuraBindAPI.unbindBlock(toBlock, BindType.BLOCK_TO_ITEM_UNBIND)
            val first = toBlock.drops.firstOrNull()
            if (first != null) {
                val uuid = blockInfo.ownerUUID
                SakuraBindAPI.bind(first, uuid, type = BindType.BLOCK_TO_ITEM_BIND)
                SakuraBindAPI.sendBackItem(uuid, listOf(first), type = SendBackType.BLOCK_FROM_TO)
            }
            event.toBlock.type = Material.AIR
        }
        event.isCancelled = true
        // 由BlockPhysicEvent 处理方块绑定
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    fun onBlockPistonExtendEvent(event: BlockPistonExtendEvent) {
        val direction = event.direction
        val cancel = event.blocks.reversed().any {
            if (it.pistonMoveReaction == PistonMoveReaction.BREAK) return@any false
            val blockInfo = SakuraBindAPI.getBlockInfo(it) ?: return@any false
            val denyMove = blockInfo.setting.getBoolean("block-deny.piston", null, null)
            //可以移动
            if (!denyMove) {
                val relative = it.getRelative(direction, 1)
                if (SakuraBindAPI.getBlockOwner(relative) != null) {
                    event.isCancelled = true
                    return
                }
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

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    fun onItemSpawnEvent(event: ItemSpawnEvent) {
        val entity = event.entity
        if (EntityRemoveQueue.isRemoved(entity)) {
            return
        }
        val itemStack = entity.itemStack
        entity.location
        // 处理下落方块变成掉落物
        kotlin.run {
            val findEntity = FallingList.findFalling(entity.location)
            FallingList.check()
            if (findEntity == null) return@run
            val fallingInfo = FallingBlockCache.getFallingInfo(findEntity) ?: return
            FallingBlockCache.removeEntity(findEntity)
            SakuraBindAPI.bind(itemStack, fallingInfo, BindType.ENTITY_TO_ITEM_BIND)
            return
        }
        //处理方块变成掉落物
        val itemMeta = itemStack.itemMeta
        if (itemMeta is BlockStateMeta && itemMeta.hasBlockState() && itemMeta.blockState is InventoryHolder && itemStack.amount != 1) return
        val entityToString = BlockCache.dropItemToString(entity)
        val ifPresent = BlockCache.containerCache[entityToString]
        if (ifPresent != null && (itemStack.type != ifPresent || itemStack.amount != 1)) return
        val blockInfo = BlockCache.getBreakingCache(entityToString) ?: return
        SakuraBindAPI.bind(itemStack, blockInfo)
        BlockCache.removeBreakingCache(entityToString)
        return
    }

    /**
     * 方块变实体，实体变方块
     */
    @EventHandler
    fun onEntityChangeBlockEvent(event: EntityChangeBlockEvent) {
        val entity = event.entity
        val block = event.block
        //处理实体变方块
        val entityInfo = FallingBlockCache.getFallingInfo(entity)
        if (entityInfo != null) {
            SakuraBindAPI.bindBlock(
                block,
                entityInfo.ownerUUID,
                entityInfo.setting,
                BindType.ENTITY_TO_BLOCK_BIND,
                entityInfo.extraData
            )
            FallingBlockCache.removeEntity(entity)
            return
        }
        //处理方块变实体
        val blockInfo = SakuraBindAPI.getBlockInfo(event.block) ?: return
        if (blockInfo.setting.getBoolean("block-deny.change-to-entity", null, null)) {
            event.isCancelled = true
        } else {
            SakuraBindAPI.unbindBlock(block, BindType.BLOCK_TO_ENTITY_UNBIND)
            FallingBlockCache.addFalling(entity, blockInfo)
            FallingList.addFalling(entity)
            FallingList.check()
        }
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    fun onLeftClick(event: PlayerInteractEvent) {
        val action = event.action
        if (action != Action.LEFT_CLICK_BLOCK && action != Action.RIGHT_CLICK_BLOCK) return
        val clickedBlock = event.clickedBlock ?: return
        val item = event.item ?: return
        val player = event.player
        if (Config.checkByPass(player)) return
        if (item.checkAir()) return
        val ownerStr = SakuraBindAPI.getOwner(item)?.toString()
        if (ownerStr != null) {
            val setting = ItemSettings.getSetting(item)
            if (action == Action.LEFT_CLICK_BLOCK &&
                setting.getBoolean("item-deny.left-click-at-bind-block", ownerStr, player)
                && SakuraBindAPI.hasBind(clickedBlock)
            ) {
                event.isCancelled = true
                MessageTool.messageCoolDown(player, Lang.item__deny_left_click_at_bind_block)
            }
            if (action == Action.RIGHT_CLICK_BLOCK &&
                setting.getBoolean("item-deny.right-click-at-bind-block", ownerStr, player)
                && SakuraBindAPI.hasBind(clickedBlock)
            ) {
                event.isCancelled = true
                MessageTool.messageCoolDown(player, Lang.item__deny_right_click_at_bind_block)
            }

        }
    }

}