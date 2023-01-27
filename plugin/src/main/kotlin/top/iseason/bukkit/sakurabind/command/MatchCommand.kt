package top.iseason.bukkit.sakurabind.command

import org.bukkit.entity.Player
import org.bukkit.permissions.PermissionDefault
import top.iseason.bukkit.sakurabind.config.ItemSettings
import top.iseason.bukkittemplate.command.CommandNode
import top.iseason.bukkittemplate.command.CommandNodeExecutor
import top.iseason.bukkittemplate.command.ParmaException
import top.iseason.bukkittemplate.utils.bukkit.EntityUtils.getHeldItem
import top.iseason.bukkittemplate.utils.bukkit.MessageUtils.sendColorMessage

object MatchCommand : CommandNode(
    name = "match",
    description = "测试手上的物品匹配到的设置",
    default = PermissionDefault.OP,
    isPlayerOnly = true,
    async = true
) {
    override var onExecute: CommandNodeExecutor? = CommandNodeExecutor { _, sender ->
        val player = sender as Player
        val heldItem = player.getHeldItem() ?: throw ParmaException("请拿着物品")
        val keyPath = ItemSettings.getMatchedSetting(heldItem).keyPath
        sender.sendColorMessage("&a该物品设置为: &6$keyPath")
    }
}