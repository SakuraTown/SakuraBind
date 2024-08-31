package top.iseason.bukkit.sakurabind.command

import org.bukkit.entity.Player
import org.bukkit.permissions.PermissionDefault
import top.iseason.bukkit.sakurabind.config.ItemSettings
import top.iseason.bukkit.sakurabind.config.Lang
import top.iseason.bukkittemplate.command.CommandNode
import top.iseason.bukkittemplate.command.CommandNodeExecutor
import top.iseason.bukkittemplate.command.Param
import top.iseason.bukkittemplate.command.ParmaException
import top.iseason.bukkittemplate.utils.bukkit.EntityUtils.getHeldItem
import top.iseason.bukkittemplate.utils.bukkit.MessageUtils.formatBy
import top.iseason.bukkittemplate.utils.bukkit.MessageUtils.sendColorMessage

object TestMatchCommand : CommandNode(
    name = "match",
    description = "匹配手上的物品 [count] 次并输出信息",
    default = PermissionDefault.OP,
    isPlayerOnly = true,
    async = true,
    params = listOf(Param("[count]", listOf("1", "5", "100")))
) {
    override var onExecute: CommandNodeExecutor? = CommandNodeExecutor { params, sender ->
        val count = params.nextOrDefault(1)
        val player = sender as Player
        val heldItem = player.getHeldItem() ?: throw ParmaException("请拿着物品")
        val m1 = System.currentTimeMillis()
        val n1 = System.nanoTime()
        var keyPath = "global-setting"
        sender.sendColorMessage(Lang.command__test__match_start.formatBy(count))
        repeat(count) {
            keyPath = ItemSettings.getMatchedSetting(heldItem).keyPath
        }
        val m2 = System.currentTimeMillis()
        val n2 = System.nanoTime()
        sender.sendColorMessage(
            Lang.command__test__match.formatBy(
                count,
                keyPath,
                m2 - m1,
                n2 - n1
            )
        )
    }
}