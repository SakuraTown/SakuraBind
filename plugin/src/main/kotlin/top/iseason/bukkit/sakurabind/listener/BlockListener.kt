package top.iseason.bukkit.sakurabind.listener

import io.github.bananapuncher714.nbteditor.NBTEditor
import org.bukkit.GameMode
import org.bukkit.Material
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.block.*
import org.bukkit.event.entity.EntityExplodeEvent
import org.bukkit.event.entity.ItemSpawnEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.inventory.InventoryHolder
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.BlockStateMeta
import top.iseason.bukkit.sakurabind.SakuraBindAPI
import top.iseason.bukkit.sakurabind.cache.BlockCacheManager
import top.iseason.bukkit.sakurabind.config.Config
import top.iseason.bukkit.sakurabind.config.ItemSettings
import top.iseason.bukkit.sakurabind.config.Lang
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
            val (owner, setting) = BlockCacheManager.getOwner(event.clickedBlock!!) ?: return
            if (setting.getBoolean("block-deny.interact", owner, player)) {
                event.isCancelled = true
                MessageTool.denyMessageCoolDown(
                    player,
                    Lang.block__deny_interact.formatBy(SakuraBindAPI.getOwnerName(UUID.fromString(owner))),
                    setting,
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
        if (Config.checkByPass(player)) return
        //覆盖检查，比如草被覆盖，但是不实用
//        if (BlockCacheManager.getOwner(event.block) != null) {
//            MessageTool.sendCoolDown(event.player, Lang.block__deny_place_exist)
//            event.isCancelled = true
//            return
//        }
        val heldItem = player.getHeldItem()
        var owner: UUID? = null
        if (!heldItem.checkAir()) {
            if (SakuraBindAPI.checkDenyBySetting(heldItem, player, "block-deny.place")) {
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
            } else if (SakuraBindAPI.checkDenyBySetting(offHandItem, player, "block-deny.place")) {
                MessageTool.denyMessageCoolDown(
                    event.player, Lang.block__deny_place, ItemSettings.getSetting(offHandItem!!),
                    offHandItem
                )
                event.isCancelled = true
                return
            }
        }
        if (owner != null) {
            BlockCacheManager.addBlock(event.block, owner, NBTEditor.getString(heldItem, *ItemSettings.nbtPath))
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
            if (SakuraBindAPI.checkDenyBySetting(heldItem, player, "block-deny.place")) {
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
            } else if (SakuraBindAPI.checkDenyBySetting(offHandItem, player, "block-deny.place")) {
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
//            if (event.replacedBlockStates.any { BlockCacheManager.getOwner(it) != null }) {
//                event.isCancelled = true
//                MessageTool.sendCoolDown(player, Lang.block__deny_place_exist)
//                return
//            }
            val key = NBTEditor.getString(heldItem, *ItemSettings.nbtPath)
            for (state in event.replacedBlockStates) {
                BlockCacheManager.addBlock(state, owner, key)
            }
        }

    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    fun onBlockBreakEvent2(event: BlockBreakEvent) {
        val block = event.block
        val player = event.player
        val (owner, setting) = BlockCacheManager.getOwner(block) ?: return
        val uuid = UUID.fromString(owner)
        // 有主人但可以破坏
        val deny = setting.getBoolean("block-deny.break", owner, event.player)
        //可以破坏
        if (Config.checkByPass(event.player) || !deny) {
            //没有掉落物直接删除
            if (event.player.gameMode != GameMode.SURVIVAL || block.getDrops(
                    player.getHeldItem() ?: ItemStack(Material.AIR)
                ).isEmpty()
            ) BlockCacheManager.removeBlock(block)
            return
//
//            val itemStack = player.getHeldItem() ?: ItemStack(Material.AIR)
//            block.getDrops(itemStack).forEach {
//                SakuraBindAPI.bind(it, uuid)
//                block.world.dropItemNaturally(block.location, it)
//            }
//            block.type = Material.AIR
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
            val pair = BlockCacheManager.getOwner(next) ?: continue
            if (pair.second.getBoolean("block-deny.explode", null, null)) iterator.remove()
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    fun onEntityExplodeEvent(event: EntityExplodeEvent) {
        val iterator = event.blockList().iterator()
        while (iterator.hasNext()) {
            val next = iterator.next()
            val pair = BlockCacheManager.getOwner(next) ?: continue
            if (pair.second.getBoolean("block-deny.explode", null, null)) iterator.remove()
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    fun onBlockPhysicsEvent(event: BlockPhysicsEvent) {
        val block = event.block
        //只检查变成空气的
        if (block.type != Material.AIR) return
        val pair = BlockCacheManager.getOwner(block) ?: return
        BlockCacheManager.removeBlock(block)
//        println("${event.block.type} -> ${event.changedType} ${event.block.location}")
//        if(event.changedType==Material.AIR)
        BlockCacheManager.addTemp(BlockCacheManager.blockToString(block), pair.first)
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    fun onBlockFrom(event: BlockFromToEvent) {
        val toBlock = event.toBlock
        val (owner, setting) = BlockCacheManager.getOwner(toBlock) ?: return
        if (!setting.getBoolean("block-deny.flow", null, null)) {
            BlockCacheManager.removeBlock(toBlock)
            val first = toBlock.drops.firstOrNull()
            if (first != null) {
                val uuid = UUID.fromString(owner)
                SakuraBindAPI.bind(first, uuid)
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
            val owner = BlockCacheManager.getOwner(it) ?: return@any false
            val denyMove = owner.second.getBoolean("block-deny.piston", null, null)
            //可以移动
            if (!denyMove) {
                BlockCacheManager.removeBlock(it)
                BlockCacheManager.addBlock(
                    it.getRelative(direction, 1),
                    owner.first,
                    owner.second.keyPath
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
            val owner = BlockCacheManager.getOwner(it) ?: return@any false
            val denyMove = owner.second.getBoolean("block-deny.piston", null, null)
            //可以移动
            if (!denyMove) {
                BlockCacheManager.removeBlock(it)
                BlockCacheManager.addBlock(
                    it.getRelative(direction, 1),
                    owner.first,
                    owner.second.keyPath
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
        val itemStack = event.entity.itemStack
        val itemMeta = itemStack.itemMeta
        if (itemMeta is BlockStateMeta && itemMeta.blockState is InventoryHolder && itemStack.amount != 1) return
        val entityToString = BlockCacheManager.entityToString(event.entity)
        val owner =
            BlockCacheManager.getTemp(entityToString) ?: BlockCacheManager.getOwner(entityToString)?.first ?: return
        SakuraBindAPI.bind(itemStack, UUID.fromString(owner))
        BlockCacheManager.removeCache(entityToString)
        BlockCacheManager.removeTemp(entityToString)

    }

}