package top.iseason.bukkit.sakurabind.command

import org.bukkit.Bukkit
import org.bukkit.OfflinePlayer
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.permissions.PermissionDefault
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.statements.api.ExposedBlob
import top.iseason.bukkit.sakurabind.config.Config
import top.iseason.bukkit.sakurabind.config.Lang
import top.iseason.bukkit.sakurabind.dto.PlayerItem
import top.iseason.bukkit.sakurabind.dto.PlayerItems
import top.iseason.bukkittemplate.command.*
import top.iseason.bukkittemplate.config.DatabaseConfig
import top.iseason.bukkittemplate.config.dbTransaction
import top.iseason.bukkittemplate.hook.PlaceHolderHook
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
        Param("<player>|<uuid>", suggestRuntime = ParamSuggestCache.playerParam),
        Param("<page>"),
    )
) {
    override var onExecute: CommandNodeExecutor? = CommandNodeExecutor { params, sender ->
        val player = params.nextOrNull<OfflinePlayer>()
        val uuid = player?.uniqueId ?: params.next()
        val name = player?.name ?: uuid.toString()
        val page = params.nextOrNull<Int>() ?: 1
        val silent = params.hasParma("-silent")
        if (!DatabaseConfig.isConnected) throw ParmaException("数据库异常")
        val items = dbTransaction { PlayerItem.find { PlayerItems.uuid eq uuid }.toList() }
        if (!Config.command_openLost_open_empty && items.isEmpty()) throw ParmaException(Lang.command__openLost_empty)
        val senderPlayer = sender as Player
        val tempChestTitle = PlaceHolderHook.setPlaceHolder(Config.temp_chest_title.formatBy(name), player)
        val inventories = mutableListOf(Bukkit.createInventory(senderPlayer, 54, tempChestTitle))
        var temp: Array<ItemStack>
        var index: Int
        for (item in items) {
            temp = item.getItemStacks().toTypedArray()
            index = 0
            while (temp.isNotEmpty()) {
                var inventory = inventories.getOrNull(index)
                if (inventory == null) {
                    inventory = Bukkit.createInventory(senderPlayer, 54, tempChestTitle)
                    inventories.add(inventory)
                }
                temp = inventory.addItem(*temp).values.toTypedArray()
                index++
            }
        }
        val inv = inventories.getOrNull(page - 1) ?: throw ParmaException(
            Lang.command__openLost_page_not_exist.formatBy(inventories.size)
        )
        if (!silent)
            senderPlayer.sendColorMessage(Lang.command__openLost_open.formatBy(name, page))
        senderPlayer.onItemInput(inv, true) {
            dbTransaction {
                PlayerItems.deleteWhere { this.uuid eq uuid }
                for (inventory in inventories) {
                    val itemStacks = inventory.filter { !it.checkAir() }
                    if (itemStacks.isEmpty()) continue
                    PlayerItem.new {
                        this.uuid = uuid
                        this.item = ExposedBlob(itemStacks.toByteArray())
                    }
                }
            }
            if (!silent)
                senderPlayer.sendColorMessage(Lang.command__openLost_closed.formatBy(name))
        }
    }
}