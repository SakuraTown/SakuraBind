package top.iseason.bukkittemplate.command

import org.bukkit.command.CommandSender

fun interface CommandNodeExecutor {
    fun onExecute(params: Params, sender: CommandSender)
}