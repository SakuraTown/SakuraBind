package top.iseason.bukkit.sakurabind.command

import top.iseason.bukkittemplate.command.CommandNode

object RootCommand : CommandNode(
    name = "sakurabind",
    alias = arrayOf("sbind", "sb", "sab", "bind"),
    description = "樱花绑定根节点"
)
