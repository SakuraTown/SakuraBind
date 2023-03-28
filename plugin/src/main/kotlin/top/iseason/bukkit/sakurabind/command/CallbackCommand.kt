package top.iseason.bukkit.sakurabind.command

import org.bukkit.entity.Player
import org.bukkit.permissions.PermissionDefault
import top.iseason.bukkit.sakurabind.config.Lang
import top.iseason.bukkittemplate.command.CommandNode
import top.iseason.bukkittemplate.command.CommandNodeExecutor
import top.iseason.bukkittemplate.utils.bukkit.MessageUtils.sendColorMessage
import java.util.*

// 召回自己的物品
object CallbackCommand : CommandNode(
    name = "callback",
    description = "切换物品召回模式,将召回属于自己的物品",
    default = PermissionDefault.TRUE,
    isPlayerOnly = true,
    async = true
) {
    private val callbackSet = hashSetOf<String>()
    override var onExecute: CommandNodeExecutor? = CommandNodeExecutor { params, sender ->
        val player = sender as Player
        val isSilent = params.hasParma("-silent")
        val uid = player.uniqueId.toString()
        if (callbackSet.contains(uid)) {
            callbackSet.remove(uid)
            if (!isSilent)
                sender.sendColorMessage(Lang.command__callback_off)
        } else {
            callbackSet.add(uid)
            if (!isSilent)
                sender.sendColorMessage(Lang.command__callback_on)
        }
    }

    /**
     * 该物主是否启用召回模式
     */
    fun isCallback(uuid: UUID?) = if (uuid != null) callbackSet.contains(uuid.toString()) else false

}