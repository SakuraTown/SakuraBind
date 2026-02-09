package top.iseason.bukkit.sakurabind.command

import org.bukkit.entity.Player
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.update
import org.bukkit.inventory.ItemStack
import org.bukkit.permissions.PermissionDefault
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.statements.api.ExposedBlob
import org.jetbrains.exposed.sql.update
import top.iseason.bukkit.sakurabind.config.Lang
import top.iseason.bukkit.sakurabind.dto.PlayerItems
import top.iseason.bukkit.sakurabind.listener.SelectListener
import top.iseason.bukkittemplate.command.CommandNode
import top.iseason.bukkittemplate.command.CommandNodeExecutor
import top.iseason.bukkittemplate.command.ParmaException
import top.iseason.bukkittemplate.config.DatabaseConfig
import top.iseason.bukkittemplate.config.dbTransaction
import top.iseason.bukkittemplate.utils.bukkit.ItemUtils
import top.iseason.bukkittemplate.utils.bukkit.ItemUtils.toByteArray
import top.iseason.bukkittemplate.utils.bukkit.MessageUtils.formatBy
import top.iseason.bukkittemplate.utils.bukkit.MessageUtils.sendColorMessage
import top.iseason.bukkittemplate.utils.other.EasyCoolDown
import java.util.*
import java.util.concurrent.ConcurrentHashMap

object GetLostCommand : CommandNode(
    name = "getLost",
    description = "获取遗失物品",
    default = PermissionDefault.TRUE,
    async = true,
    isPlayerOnly = true
) {
    val syncing: MutableSet<UUID> = ConcurrentHashMap.newKeySet()

    override var onExecute: CommandNodeExecutor? = CommandNodeExecutor { params, sender ->
        val player = sender as Player
        var page = 0
        var isEmpty = true
        if (EasyCoolDown.check(player, 1000)) {
            sender.sendColorMessage(Lang.command__getLost_coolDown)
            return@CommandNodeExecutor
        }
        if (!DatabaseConfig.isConnected) throw ParmaException("数据库异常")
        val uniqueId = player.uniqueId
        if (SelectListener.noScanning.contains(uniqueId)) throw ParmaException("数据同步中，请稍后")
        if (syncing.contains(uniqueId)) throw ParmaException("请等待上一个操作完成")

        var totalCount = 0
        try {
            dbTransaction {
                syncing.add(uniqueId)
                while (true) {
                    val results = PlayerItems
                        .select(PlayerItems.id, PlayerItems.item)
                        .where { PlayerItems.uuid eq uniqueId }
                        .limit(10)
                        .offset((page * 10).toLong())
                        .toList()

                    if (results.isEmpty()) break
                    for (result in results) {
                        val itemStacks = ItemUtils.fromByteArrays(result[PlayerItems.item].bytes)
                        val release = mutableListOf<ItemStack>()
                        for (itemStack in itemStacks) {
                            val amount = itemStack.amount
                            val addItem = player.inventory.addItem(itemStack)
//                                println("size = ${addItem.size}")
                            //放不下了
                            if (addItem.isNotEmpty()) {
                                val first = addItem.values.first()
                                release.add(first)
                                isEmpty = false
//                                    println("not empty ${amount} - ${first.amount}")
                                totalCount += (amount - first.amount)
                            } else {
//                                    println("empty ${amount}")
                                totalCount += amount
                            }
                        }
                        if (release.isEmpty()) {
                            PlayerItems.deleteWhere { PlayerItems.id eq result[PlayerItems.id] }
                        } else {
                            PlayerItems
                                .update({ PlayerItems.id eq result[PlayerItems.id] })
                                {
                                    it[PlayerItems.item] = ExposedBlob(release.toByteArray())
                                }
                            break
                        }
                    }
                    page++
                }
            }
        } catch (e: Exception) {
            syncing.remove(uniqueId)
            e.printStackTrace()
        }
        syncing.remove(uniqueId)
        if (params.hasParma("-silent")) return@CommandNodeExecutor
//                println(totalCount)
        if (totalCount == 0 && !isEmpty) {
            sender.sendColorMessage(Lang.command__getLost_full)
        } else if (totalCount == 0) {
            sender.sendColorMessage(Lang.command__getLost_empty)
        } else if (isEmpty) {
            sender.sendColorMessage(Lang.command__getLost_all.formatBy(totalCount))
        } else {
            sender.sendColorMessage(Lang.command__getLost_item.formatBy(totalCount))
        }
    }
}