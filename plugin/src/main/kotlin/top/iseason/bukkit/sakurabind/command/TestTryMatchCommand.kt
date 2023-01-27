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

object TestTryMatchCommand : CommandNode(
    name = "tryMatch",
    description = "尝试以某个配置匹配手上的物品, 输出信息",
    default = PermissionDefault.OP,
    isPlayerOnly = true,
    async = true,
    params = listOf(
        Param("<setting>", suggestRuntime = { ItemSettings.getSettingKeys() })
    )
) {
    override var onExecute: CommandNodeExecutor? = CommandNodeExecutor { params, sender ->
        val key = params.next<String>()
        if (key !in ItemSettings.getSettingKeys()) throw ParmaException(Lang.command__test__try_match_not_found)
        val setting = ItemSettings.getSetting(key)
        val player = sender as Player
        val itemStack = player.getHeldItem() ?: throw ParmaException("请拿着物品")
        player.sendColorMessage(Lang.command__test__try_match_header.formatBy(key))
        val match = setting.match(itemStack, player)
        player.sendColorMessage(Lang.command__test__try_match_result.formatBy(match))
    }
}