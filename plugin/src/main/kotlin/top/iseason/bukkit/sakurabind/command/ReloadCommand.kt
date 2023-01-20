package top.iseason.bukkit.sakurabind.command

import org.bukkit.permissions.PermissionDefault
import top.iseason.bukkit.sakurabind.SakuraBind
import top.iseason.bukkittemplate.command.CommandNode
import top.iseason.bukkittemplate.command.CommandNodeExecutor
import top.iseason.bukkittemplate.config.DatabaseConfig

object ReloadCommand : CommandNode(
    name = "reload",
    description = "重载配置",
    default = PermissionDefault.OP,
    async = true
) {
    override var onExecute: CommandNodeExecutor? = CommandNodeExecutor { _, _ ->
        DatabaseConfig.closeDB()
        SakuraBind.initConfig()
    }

}