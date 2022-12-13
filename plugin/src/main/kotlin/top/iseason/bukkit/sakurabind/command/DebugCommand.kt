package top.iseason.bukkit.sakurabind.command

import org.bukkit.permissions.PermissionDefault
import top.iseason.bukkit.sakurabind.config.Lang
import top.iseason.bukkittemplate.command.CommandNode
import top.iseason.bukkittemplate.command.CommandNodeExecutor
import top.iseason.bukkittemplate.debug.SimpleLogger
import top.iseason.bukkittemplate.utils.bukkit.MessageUtils.formatBy
import top.iseason.bukkittemplate.utils.bukkit.MessageUtils.sendColorMessage

object DebugCommand : CommandNode(
    name = "debug",
    description = "切换debug模式",
    default = PermissionDefault.OP,
    async = true
) {
    override var onExecute: CommandNodeExecutor? = CommandNodeExecutor { params, sender ->
        SimpleLogger.isDebug = !SimpleLogger.isDebug
        if (!params.hasParma("-silent"))
            sender.sendColorMessage(Lang.command__debug.formatBy(SimpleLogger.isDebug))
    }
}