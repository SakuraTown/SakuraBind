package top.iseason.bukkit.sakurabind.command

import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.permissions.PermissionDefault
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.statements.api.ExposedBlob
import top.iseason.bukkit.sakurabind.config.Lang
import top.iseason.bukkit.sakurabind.dto.PlayerItem
import top.iseason.bukkit.sakurabind.dto.PlayerItems
import top.iseason.bukkittemplate.command.*
import top.iseason.bukkittemplate.config.dbTransaction
import top.iseason.bukkittemplate.utils.bukkit.IOUtils.onItemInput
import top.iseason.bukkittemplate.utils.bukkit.ItemUtils.checkAir
import top.iseason.bukkittemplate.utils.bukkit.ItemUtils.toByteArray
import top.iseason.bukkittemplate.utils.bukkit.MessageUtils.formatBy
import top.iseason.bukkittemplate.utils.bukkit.MessageUtils.sendColorMessage

object OpenLostCommand : CommandNode(
    name = "openLost",
    description = "打开某玩家的暂存箱子",
    default = PermissionDefault.OP,
    async = true,
    isPlayerOnly = true,
    params = listOf(
        Param("<player>", suggestRuntime = ParamSuggestCache.playerParam),
        Param("<page>"),
    )
) {
    override var onExecute: CommandNodeExecutor? = CommandNodeExecutor { params, sender ->
        val player = params.next<Player>()
        val page = params.nextOrNull<Int>() ?: 1
        val silent = params.hasParma("-silent")
//        var inventory = Bukkit.createInventory(player, 36)
        val items = dbTransaction { PlayerItem.find { PlayerItems.uuid eq player.uniqueId }.toList() }
        if (items.isEmpty()) throw ParmaException(Lang.command__openLost_empty)
        val inventories = mutableListOf(Bukkit.createInventory(player, 36))
        var temp: Array<ItemStack>
        var index: Int
        for (item in items) {
            temp = item.getItemStacks().toTypedArray()
            index = 0
            while (temp.isNotEmpty()) {
                var inventory = inventories.getOrNull(index)
                if (inventory == null) {
                    inventory = Bukkit.createInventory(player, 36)
                    inventories.add(inventory)
                }
                temp = inventory.addItem(*temp).values.toTypedArray()
                index++
            }
        }
        val inv = inventories.getOrNull(page - 1) ?: throw ParmaException(
            Lang.command__openLost_page_not_exist.formatBy(inventories.size)
        )
        val senderPlayer = sender as Player
        if (!silent)
            senderPlayer.sendColorMessage(Lang.command__openLost_open.formatBy(player.name, page))
        senderPlayer.onItemInput(inv, true) {
            dbTransaction {
                PlayerItems.deleteWhere { uuid eq player.uniqueId }
                for (inventory in inventories) {
                    PlayerItem.new {
                        this.uuid = player.uniqueId
                        this.item = ExposedBlob(inventory.filter { !it.checkAir() }.toByteArray())
                    }
                }
            }
            if (!silent)
                senderPlayer.sendColorMessage(Lang.command__openLost_closed.formatBy(player.name))
        }
    }
}