package top.iseason.bukkit.sakurabind.listener

import io.github.bananapuncher714.nbteditor.NBTEditor
import org.bukkit.GameMode
import org.bukkit.Material
import org.bukkit.block.PistonMoveReaction
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
import top.iseason.bukkit.sakurabind.config.Config
import top.iseason.bukkit.sakurabind.config.ItemSettings
import top.iseason.bukkit.sakurabind.config.Lang
import top.iseason.bukkit.sakurabind.logger.BindType
import top.iseason.bukkit.sakurabind.task.FallingList
import top.iseason.bukkit.sakurabind.utils.MessageTool
import top.iseason.bukkit.sakurabind.utils.PlayerTool
import top.iseason.bukkittemplate.utils.bukkit.EntityUtils.getHeldItem
import top.iseason.bukkittemplate.utils.bukkit.ItemUtils.checkAir
import top.iseason.bukkittemplate.utils.bukkit.MessageUtils.formatBy
import java.util.*

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
            val pair = SakuraBindAPI.getBlockInfo(event.clickedBlock!!) ?: return
            if (pair.second.getBoolean("block-deny.interact", pair.first, player)) {
                event.isCancelled = true
                MessageTool.denyMessageCoolDown(
                    player,
                    Lang.block__deny_interact.formatBy(SakuraBindAPI.getOwnerName(UUID.fromString(pair.first))),
                    pair.second,
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
//        if (Config.checkByPass(player)) return
        //覆盖检查，比如草被覆盖，但是不实用
//        if (BlockCache.getOwner(event.block) != null) {
//            MessageTool.sendCoolDown(event.player, Lang.block__deny_place_exist)
//            event.isCancelled = true
//            return
//        }
        val heldItem = player.getHeldItem()
        var owner: UUID? = null
        if (!heldItem.checkAir()) {
            if (!Config.checkByPass(player) && SakuraBindAPI.checkDenyBySetting(heldItem, player, "block-deny.place")) {
                MessageTool.denyMessageCoolDown(
                    event.player, Lang.block__deny_place,
                    ItemSettings.getSetting(heldItem!!),
                    heldItem
                )
                event.isCancelled = true
                return
            } else {
                owner = SakuraBindAPI.getOwner(heldItem!!)
            }
        } else {
            val offHandItem = PlayerTool.getOffHandItem(player)
            if (offHandItem.checkAir()) {
                owner = SakuraBindAPI.getOwner(offHandItem!!)
            } else if (!Config.checkByPass(player) && SakuraBindAPI.checkDenyBySetting(
                    offHandItem,
                    player,
                    "block-deny.place"
                )
            ) {
                MessageTool.denyMessageCoolDown(
                    event.player, Lang.block__deny_place, ItemSettings.getSetting(offHandItem!!),
                    offHandItem
                )
                event.isCancelled = true
                return
            }
        }
        if (owner != null) {
            SakuraBindAPI.bindBlock(
                event.block,
                owner,
                ItemSettings.getSetting(NBTEditor.getString(heldItem, *ItemSettings.nbtPath)),
                type = BindType.ITEM_TO_BLOCK_BIND
            )
        }
    }

    /**
     * 多方快放置
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onBlockMultiPlaceEvent(event: BlockMultiPlaceEvent) {
        val player = event.player
        if (Config.checkByPass(player)) return
        val heldItem = player.getHeldItem()
        var owner: UUID? = null
        if (!heldItem.checkAir()) {
            if (!Config.checkByPass(player) && SakuraBindAPI.checkDenyBySetting(heldItem, player, "block-deny.place")) {
                MessageTool.denyMessageCoolDown(
                    event.player,
                    Lang.block__deny_place,
                    ItemSettings.getSetting(heldItem!!),
                    heldItem
                )
                event.isCancelled = true
                return
            } else {
                owner = SakuraBindAPI.getOwner(heldItem!!)
            }
        } else {
            val offHandItem = PlayerTool.getOffHandItem(player)
            if (offHandItem.checkAir()) {
                owner = SakuraBindAPI.getOwner(offHandItem!!)
            } else if (!Config.checkByPass(player) && SakuraBindAPI.checkDenyBySetting(
                    offHandItem,
                    player,
                    "block-deny.place"
                )
            ) {
                MessageTool.denyMessageCoolDown(
                    event.player, Lang.block__deny_place,
                    ItemSettings.getSetting(offHandItem!!),
                    offHandItem
                )
                event.isCancelled = true
                return
            }
        }
        if (owner != null) {
            //覆盖检查，比如草，但是不实用
//            if (event.replacedBlockStates.any { BlockCache.getOwner(it) != null }) {
//                event.isCancelled = true
//                MessageTool.sendCoolDown(player, Lang.block__deny_place_exist)
//                return
//            }
            val key = NBTEditor.getString(heldItem, *ItemSettings.nbtPath)
            val setting = ItemSettings.getSetting(key)

            for (state in event.replacedBlockStates) {
                SakuraBindAPI.bindBlock(state.block, owner, setting, type = BindType.ITEM_TO_BLOCK_BIND)
            }

        }

    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    fun onBlockBreakEvent2(event: BlockBreakEvent) {
        val block = event.block
        val player = event.player
        val (owner, setting) = SakuraBindAPI.getBlockInfo(block) ?: return
        val uuid = UUID.fromString(owner)
        // 有主人但可以破坏
        val deny = setting.getBoolean("block-deny.break", owner, event.player)
        //可以破坏
        if (Config.checkByPass(event.player) || !deny) {
            BlockCache.addBlockTemp(BlockCache.blockToString(block), owner)
            //没有掉落物直接删除
            if (event.player.gameMode != GameMode.SURVIVAL || block.getDrops(
                    player.getHeldItem() ?: ItemStack(Material.AIR)
                ).isEmpty()
            ) SakuraBindAPI.unbindBlock(block, type = BindType.BLOCK_TO_ITEM_UNBIND)
            return
        } else {
            event.isCancelled = true
            MessageTool.denyMessageCoolDown(
                player, Lang.block__deny_break.formatBy(SakuraBindAPI.getOwnerName(uuid)),
                setting, block = block
            )
        }

    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    fun onBlockExplodeEvent(event: BlockExplodeEvent) {
        val iterator = event.blockList().iterator()
        while (iterator.hasNext()) {
            val next = iterator.next()
            val pair = SakuraBindAPI.getBlockInfo(next) ?: continue
            if (pair.second.getBoolean("block-deny.explode", null, null)) iterator.remove()
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    fun onEntityExplodeEvent(event: EntityExplodeEvent) {
        val iterator = event.blockList().iterator()
        while (iterator.hasNext()) {
            val next = iterator.next()
            val pair = SakuraBindAPI.getBlockInfo(next) ?: continue
            if (pair.second.getBoolean("block-deny.explode", null, null)) iterator.remove()
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    fun onBlockPhysicsEvent(event: BlockPhysicsEvent) {
        val block = event.block
        //只检查变成空气的
        if (!block.isEmpty) return
        val pair = SakuraBindAPI.getBlockInfo(block) ?: return
        SakuraBindAPI.unbindBlock(block, BindType.BLOCK_TO_ITEM_UNBIND)
//        println("${event.block.type} -> ${event.changedType} ${event.block.location}")
//        if(event.changedType==Material.AIR)
        BlockCache.addBlockTemp(BlockCache.blockToString(block), pair.first)
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    fun onBlockFrom(event: BlockFromToEvent) {
        val toBlock = event.toBlock
        val (owner, setting) = SakuraBindAPI.getBlockInfo(toBlock) ?: return
        if (!setting.getBoolean("block-deny.flow", null, null)) {
            SakuraBindAPI.unbindBlock(toBlock, BindType.BLOCK_TO_ITEM_UNBIND)
            val first = toBlock.drops.firstOrNull()
            if (first != null) {
                val uuid = UUID.fromString(owner)
                SakuraBindAPI.bind(first, uuid, type = BindType.BLOCK_TO_ITEM_BIND)
                SakuraBindAPI.sendBackItem(uuid, listOf(first))
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
            val owner = SakuraBindAPI.getBlockInfo(it) ?: return@any false
            val denyMove = owner.second.getBoolean("block-deny.piston", null, null)
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
                    UUID.fromString(owner.first),
                    owner.second,
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
            val owner = SakuraBindAPI.getBlockInfo(it) ?: return@any false
            val denyMove = owner.second.getBoolean("block-deny.piston", null, null)
            //可以移动
            if (!denyMove) {
                SakuraBindAPI.unbindBlock(it, BindType.BLOCK_MOVE_UNBIND)
                SakuraBindAPI.bindBlock(
                    it.getRelative(direction, 1),
                    UUID.fromString(owner.first),
                    owner.second,
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
        val itemStack = entity.itemStack
        // 处理下落方块变成掉落物
        kotlin.run {
            val location = entity.location
            val findEntity = FallingList.findFalling(FallingList.locationToString(location))
            FallingList.check()
            if (findEntity == null) return@run
            val entityOwner = FallingBlockCache.getEntityOwner(findEntity)?.first ?: return
            FallingBlockCache.removeEntity(findEntity)
            SakuraBindAPI.bind(itemStack, UUID.fromString(entityOwner), type = BindType.ENTITY_TO_ITEM_BIND)
            return
        }
        //处理方块变成掉落物
        val itemMeta = itemStack.itemMeta
        if (itemMeta is BlockStateMeta && itemMeta.blockState is InventoryHolder && itemStack.amount != 1) return
        val entityToString = BlockCache.dropItemToString(entity)
        val owner = BlockCache.getBlockTemp(entityToString) ?: BlockCache.getBlockInfo(entityToString)?.first ?: return
        SakuraBindAPI.bind(itemStack, UUID.fromString(owner), type = BindType.BLOCK_TO_ITEM_BIND)
        BlockCache.removeCache(entityToString)
        BlockCache.removeBlockTemp(entityToString)
        return

    }

    /**
     * 方块变实体，实体变方块
     */
    @EventHandler
    fun onEntityChangeBlockEvent(event: EntityChangeBlockEvent) {
        if (!Config.entity_listener) return
        val entity = event.entity
        val block = event.block
        //处理实体变方块
        val entityOwner = FallingBlockCache.getEntityOwner(entity)
        if (entityOwner != null) {
            SakuraBindAPI.bindBlock(
                block,
                UUID.fromString(entityOwner.first),
                entityOwner.second,
                BindType.ENTITY_TO_BLOCK_BIND
            )
            FallingBlockCache.removeEntity(entity)
            return
        }
        //处理方块变实体
        val pair = SakuraBindAPI.getBlockInfo(event.block) ?: return
        if (pair.second.getBoolean("block-deny.change-to-entity", null, null)) {
            event.isCancelled = true
        } else {
            SakuraBindAPI.unbindBlock(block, BindType.BLOCK_TO_ENTITY_UNBIND)
            FallingBlockCache.addEntity(entity, pair.first, pair.second.keyPath)
            FallingList.addFalling(entity)
            FallingList.check()
        }
    }

}