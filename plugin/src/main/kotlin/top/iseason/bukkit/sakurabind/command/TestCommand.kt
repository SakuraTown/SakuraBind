package top.iseason.bukkit.sakurabind.command

import org.bukkit.permissions.PermissionDefault
import top.iseason.bukkittemplate.command.CommandNode

object TestCommand : CommandNode(
    name = "test",
    description = "测试命令，用于调试以及性能检测",
    default = PermissionDefault.OP,
    async = true
)