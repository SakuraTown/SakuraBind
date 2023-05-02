package top.iseason.bukkit.sakurabind.command

import io.github.bananapuncher714.nbteditor.NBTEditor
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.entity.Item
import org.bukkit.entity.Player
import org.bukkit.inventory.InventoryHolder
import org.bukkit.inventory.ItemStack
import org.bukkit.permissions.PermissionDefault
import top.iseason.bukkit.sakurabind.SakuraBindAPI
import top.iseason.bukkit.sakurabind.SakuraBindAPI.filterInventory
import top.iseason.bukkit.sakurabind.SakuraBindAPI.filterItem
import top.iseason.bukkit.sakurabind.config.Lang
import top.iseason.bukkittemplate.BukkitTemplate
import top.iseason.bukkittemplate.command.CommandNode
import top.iseason.bukkittemplate.command.CommandNodeExecutor
import top.iseason.bukkittemplate.command.Param
import top.iseason.bukkittemplate.command.ParamSuggestCache
import top.iseason.bukkittemplate.utils.bukkit.MessageUtils.formatBy
import top.iseason.bukkittemplate.utils.bukkit.MessageUtils.sendColorMessage
import java.util.*
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicInteger
import java.util.stream.Collectors

// 召回某个玩家的所有物品
object SuperCallbackCommand : CommandNode(
    name = "superCallback",
    description = "一键将玩家的所有绑定物品召回(扫描箱子、玩家背包、掉落物)",
    default = PermissionDefault.OP,
    isPlayerOnly = false,
    async = true,
    params = listOf(Param("<player>", suggestRuntime = ParamSuggestCache.playerParam))
) {
    override var onExecute: CommandNodeExecutor? = CommandNodeExecutor { params, sender ->
        val player = params.next<Player>()
        val isSilent = params.hasParma("-silent")
        val uniqueId = player.uniqueId
        val linkedList = ConcurrentLinkedQueue<ItemStack>()
        if (!isSilent) sender.sendColorMessage(Lang.command__super_callback_start.formatBy(player.name))
        val startMills = System.currentTimeMillis()
        // 搜索背包
        val onlinePlayers = Bukkit.getOnlinePlayers()
        for (onlinePlayer in onlinePlayers) {
            if (player == onlinePlayer) continue
            linkedList.addAll(filterInventory(onlinePlayer.inventory, uniqueId).flatMap { it.value })
            linkedList.addAll(filterInventory(onlinePlayer.enderChest, uniqueId).flatMap { it.value })
        }
        val backpackMills = System.currentTimeMillis()
        val backpacksSize = linkedList.size
        if (!isSilent) sender.sendColorMessage(
            Lang.command__super_callback_backpacks.formatBy(
                onlinePlayers.size - 1,
                backpacksSize,
                backpackMills - startMills
            )
        )
        // 同步获取所有 掉落物 和 方块实体
        val count = AtomicInteger(0)
        val (drops, containers) = Bukkit.getScheduler().callSyncMethod(BukkitTemplate.getPlugin()) {
            val drops = Bukkit.getWorlds()
                .stream()
                .flatMap { it.entities.stream() }
                .filter { it is Item }
                .collect(Collectors.toList())
            val containers = Bukkit.getWorlds()
                .stream()
                .flatMap { world ->
                    Arrays.stream(world.loadedChunks)
                        .flatMap { chunk -> Arrays.stream(chunk.tileEntities) }
                        .filter {
                            if (it !is InventoryHolder) return@filter false
                            // 防止被 proguard 优化成 Translatable 导致低版本无法使用, 所以强转
                            if (NBTEditor.contains(it.block as Any, "LootTableSeed")) return@filter false
                            true
                        }
                }.collect(Collectors.toList())
            drops to containers
        }.get()
        val syncMills = System.currentTimeMillis()
        if (!isSilent) sender.sendColorMessage(Lang.command__super_callback_sync.formatBy(syncMills - backpackMills))
        val itemList = drops
            .parallelStream()
            .filter {
                count.getAndIncrement()
                val itemStack = (it as Item).itemStack
                val stacks = filterItem(itemStack, uniqueId)
                if (stacks.isNotEmpty()) linkedList.addAll(stacks.flatMap { entry -> entry.value })
                itemStack.type == Material.AIR
            }
            .collect(Collectors.toList())
        // 同步删除实体, 问就是 paper不允许异步
        Bukkit.getScheduler().callSyncMethod(BukkitTemplate.getPlugin()) { itemList.forEach { it.remove() } }
        val dropsMills = System.currentTimeMillis()
        val dropsSize = linkedList.size
        if (!isSilent) sender.sendColorMessage(
            Lang.command__super_callback_drops.formatBy(
                count.get(),
                dropsSize - backpacksSize,
                dropsMills - syncMills
            )
        )
        containers.parallelStream()
            .forEach { state ->
                val filterInventory = filterInventory((state as InventoryHolder).inventory, uniqueId)
                if (filterInventory.isEmpty()) return@forEach
                linkedList.addAll(filterInventory.flatMap { it.value })
            }
        val containersMills = System.currentTimeMillis()
        val containersSize = linkedList.size
        if (!isSilent) sender.sendColorMessage(
            Lang.command__super_callback_containers.formatBy(
                containers.size,
                containersSize - dropsSize,
                containersMills - dropsMills
            )
        )
        val sumOf = linkedList.sumOf { it.amount }
        if (!isSilent) {
            sender.sendColorMessage(
                Lang.command__super_callback_end.formatBy(
                    linkedList.size,
                    sumOf,
                    System.currentTimeMillis() - startMills
                )
            )
        }
        SakuraBindAPI.sendBackItem(uniqueId, linkedList)
        player.sendColorMessage(Lang.command__super_callback_player.formatBy(linkedList.size, sumOf))
    }


}