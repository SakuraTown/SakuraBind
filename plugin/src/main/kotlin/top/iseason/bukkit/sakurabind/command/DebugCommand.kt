package top.iseason.bukkit.sakurabind.command

import org.bukkit.entity.Player
import org.bukkit.permissions.PermissionDefault
import top.iseason.bukkit.sakurabind.config.Lang
import top.iseason.bukkittemplate.command.CommandNode
import top.iseason.bukkittemplate.command.CommandNodeExecutor
import top.iseason.bukkittemplate.command.Param
import top.iseason.bukkittemplate.command.ParamSuggestCache
import top.iseason.bukkittemplate.debug.SimpleLogger
import top.iseason.bukkittemplate.utils.bukkit.MessageUtils.formatBy
import top.iseason.bukkittemplate.utils.bukkit.MessageUtils.sendColorMessage
import java.util.*
import java.util.concurrent.ConcurrentHashMap

object DebugCommand : CommandNode(
    name = "debug",
    description = "切换debug模式",
    default = PermissionDefault.OP,
    async = true,
    params = listOf(Param("[player]", suggestRuntime = ParamSuggestCache.playerParam))
) {
    val debugPlayer = ConcurrentHashMap.newKeySet<UUID>()
    override var onExecute: CommandNodeExecutor? = CommandNodeExecutor { params, sender ->
        val player = params.nextOrNull<Player>()
        if (player != null) {
            val uid = player.uniqueId
            if (debugPlayer.remove(uid)) {
                if (!params.hasParma("-silent"))
                    sender.sendColorMessage(Lang.command__debug_player_close.formatBy(player.name))
            } else {
                debugPlayer.add(player.uniqueId)
                if (!params.hasParma("-silent"))
                    sender.sendColorMessage(Lang.command__debug_player_open.formatBy(player.name))
            }
        } else {
            SimpleLogger.isDebug = !SimpleLogger.isDebug
            if (!params.hasParma("-silent"))
                sender.sendColorMessage(Lang.command__debug.formatBy(SimpleLogger.isDebug))
        }

    }
}