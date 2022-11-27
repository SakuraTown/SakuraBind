package top.iseason.bukkit.sakurabind.listener

import io.github.bananapuncher714.nbteditor.NBTEditor
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.block.*
import org.bukkit.event.entity.EntityExplodeEvent
import org.bukkit.event.entity.ItemSpawnEvent
import org.bukkit.event.player.PlayerInteractEvent
import top.iseason.bukkit.sakurabind.SakuraBindAPI
import top.iseason.bukkit.sakurabind.cache.BlockCacheManager
import top.iseason.bukkit.sakurabind.config.ItemSettings
import top.iseason.bukkit.sakurabind.config.Lang
import top.iseason.bukkittemplate.utils.bukkit.EntityUtils.getHeldItem
import top.iseason.bukkittemplate.utils.bukkit.ItemUtils.checkAir
import top.iseason.bukkittemplate.utils.bukkit.MessageUtils.formatBy
import top.iseason.bukkittemplate.utils.bukkit.MessageUtils.sendColorMessage
import top.iseason.bukkittemplate.utils.other.EasyCoolDown
import java.util.*

/**
 * 方块物品监听器
 */
object BlockListener : Listener {

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    fun onPlayerInteractEvent(event: PlayerInteractEvent) {
        if (event.player.isOp) return
        val player = event.player
        if (event.clickedBlock != null) {
            val (owner, setting) = BlockCacheManager.getOwner(event.clickedBlock!!) ?: return
            if (setting.getBoolean("block-deny.interact", player.uniqueId.toString() == owner)) {
                event.isCancelled = true
                if (!EasyCoolDown.check(event.player.uniqueId, 1000)) {
                    val uuid = UUID.fromString(owner)
                    val ownerPlayer = Bukkit.getPlayer(uuid) ?: Bukkit.getOfflinePlayer(uuid)
                    event.player.sendColorMessage(Lang.block__deny_interact.formatBy(ownerPlayer.name))
                }
                return
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onBlockPlaceEvent(event: BlockPlaceEvent) {
        val heldItem = event.player.getHeldItem() ?: return
        if (heldItem.checkAir()) return
        val owner = SakuraBindAPI.getOwner(heldItem) ?: return
        val setting = ItemSettings.getSetting(heldItem)
        if (setting.getBoolean("block-deny.place", event.player.uniqueId == owner)) {
            event.isCancelled = true
            return
        }
        val key = NBTEditor.getString(heldItem, *ItemSettings.nbtPath)
        BlockCacheManager.addBlock(event.block, owner, key)
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    fun onBlockBreakEvent2(event: BlockBreakEvent) {
        val block = event.block
        val player = event.player
        val (owner, setting) = BlockCacheManager.getOwner(block) ?: return
        val uuid = UUID.fromString(owner)
        // 有主人但可以破坏
        val deny = setting.getBoolean("block-deny.break", player.uniqueId.toString() == owner)
        if (player.isOp || !deny) {
            if (!event.isDropItems)
                BlockCacheManager.removeBlock(block)
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
            if (!EasyCoolDown.check(uuid, 1000)) {
                val ownerPlayer = Bukkit.getPlayer(uuid) ?: Bukkit.getOfflinePlayer(uuid)
                player.sendColorMessage(Lang.block__deny_break.formatBy(ownerPlayer.name))
            }
        }

    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    fun onBlockExplodeEvent(event: BlockExplodeEvent) {
//        if (!Config.block__deny_explode) return
        val iterator = event.blockList().iterator()
        while (iterator.hasNext()) {
            val next = iterator.next()
            val (_, setting) = BlockCacheManager.getOwner(next) ?: continue
            if (setting.getBoolean("block-deny.explode")) iterator.remove()
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    fun onEntityExplodeEvent(event: EntityExplodeEvent) {
//        if (!Config.block__deny_explode) return
        val iterator = event.blockList().iterator()
        while (iterator.hasNext()) {
            val next = iterator.next()
            val (_, setting) = BlockCacheManager.getOwner(next) ?: continue
            if (setting.getBoolean("block-deny.explode")) iterator.remove()
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    fun onBlockPhysicsEvent(event: BlockPhysicsEvent) {
        val pair = BlockCacheManager.getOwner(event.block) ?: return
        BlockCacheManager.addTemp(BlockCacheManager.blockToString(event.block), pair.first)
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    fun onBlockFrom(event: BlockFromToEvent) {
        val toBlock = event.toBlock
        val (owner, setting) = BlockCacheManager.getOwner(toBlock) ?: return
        if (!setting.getBoolean("block-deny.flow")) {
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
//        BlockCacheManager.addTemp(BlockCacheManager.blockToString(event.block), owner)
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    fun onBlockPistonExtendEvent(event: BlockPistonExtendEvent) {
//        if (!Config.block__deny_piston) return
        for (block in event.blocks) {
            val boolean = BlockCacheManager.getOwner(block)?.second?.getBoolean("block-deny.pistion") ?: continue
            if (boolean) {
                event.isCancelled = true
                return
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    fun onBlockPistonRetractEvent(event: BlockPistonRetractEvent) {
//        if (!Config.block__deny_piston) return
        for (block in event.blocks) {
            val boolean = BlockCacheManager.getOwner(block)?.second?.getBoolean("block-deny.pistion") ?: continue
            if (boolean) {
                event.isCancelled = true
                return
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    fun onItemSpawnEvent(event: ItemSpawnEvent) {
        val itemStack = event.entity.itemStack
        val entityToString = BlockCacheManager.entityToString(event.entity)
//        println(entityToString)
        val owner =
            BlockCacheManager.getTemp(entityToString) ?: BlockCacheManager.getOwner(entityToString)?.first ?: return
        SakuraBindAPI.bind(itemStack, UUID.fromString(owner))
        BlockCacheManager.removeCache(entityToString)
        BlockCacheManager.removeTemp(entityToString)

    }


}