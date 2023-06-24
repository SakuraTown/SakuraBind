package top.iseason.bukkit.sakurabind.command

import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.permissions.PermissionDefault
import org.jetbrains.exposed.sql.statements.api.ExposedBlob
import top.iseason.bukkit.sakurabind.config.Lang
import top.iseason.bukkit.sakurabind.dto.PlayerItem
import top.iseason.bukkit.sakurabind.dto.PlayerItems
import top.iseason.bukkittemplate.command.CommandNode
import top.iseason.bukkittemplate.command.CommandNodeExecutor
import top.iseason.bukkittemplate.config.dbTransaction
import top.iseason.bukkittemplate.utils.bukkit.ItemUtils.toByteArray
import top.iseason.bukkittemplate.utils.bukkit.MessageUtils.formatBy
import top.iseason.bukkittemplate.utils.bukkit.MessageUtils.sendColorMessage
import top.iseason.bukkittemplate.utils.other.EasyCoolDown

object GetLostCommand : CommandNode(
    name = "getLost",
    description = "获取遗失物品",
    default = PermissionDefault.TRUE,
    async = true,
    isPlayerOnly = true
) {
    override var onExecute: CommandNodeExecutor? = CommandNodeExecutor { params, sender ->
        val player = sender as Player
        var page = 0
        var isEmpty = true
        if (EasyCoolDown.check(player, 1000)) {
            sender.sendColorMessage(Lang.command__getLost_coolDown)
            return@CommandNodeExecutor
        }
        var totalCount = 0
        dbTransaction {
            while (true) {
                val items =
                    PlayerItem.find { PlayerItems.uuid eq player.uniqueId }
                        .limit(10, (page * 10).toLong())
                        .toList()
                if (items.isEmpty()) break
                for (item in items) {
                    val itemStacks = item.getItemStacks()
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
                        item.delete()
                    } else {
                        item.item = ExposedBlob(release.toByteArray())
                        break
                    }
                }
                page++
            }
        }
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