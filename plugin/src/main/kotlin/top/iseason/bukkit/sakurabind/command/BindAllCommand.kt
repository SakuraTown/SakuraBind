package top.iseason.bukkit.sakurabind.command

import org.bukkit.entity.Player
import org.bukkit.permissions.PermissionDefault
import top.iseason.bukkit.sakurabind.SakuraBindAPI
import top.iseason.bukkit.sakurabind.config.Lang
import top.iseason.bukkit.sakurabind.logger.BindType
import top.iseason.bukkit.sakurabind.utils.MessageTool
import top.iseason.bukkittemplate.command.CommandNode
import top.iseason.bukkittemplate.command.CommandNodeExecutor
import top.iseason.bukkittemplate.command.Param
import top.iseason.bukkittemplate.command.ParamSuggestCache
import top.iseason.bukkittemplate.utils.bukkit.ItemUtils.checkAir
import top.iseason.bukkittemplate.utils.bukkit.MessageUtils.formatBy
import top.iseason.bukkittemplate.utils.bukkit.MessageUtils.sendColorMessage

object BindAllCommand : CommandNode(
    name = "bindAll",
    description = "绑定某玩家背包里的所有物品",
    default = PermissionDefault.OP,
    params = listOf(
        Param("<player>", suggestRuntime = ParamSuggestCache.playerParam),
        Param("[-noLore]", suggest = listOf("-noLore"))
    ),
    async = true
) {
    override var onExecute: CommandNodeExecutor? = CommandNodeExecutor { params, sender ->
        val player = params.getParam<Player>(0)
        val showLore = !params.hasParma("-noLore")
        val hasParma = params.hasParma("-silent")
        for (itemStack in player.inventory) {
            if (itemStack.checkAir()) continue
            SakuraBindAPI.bind(itemStack, player, showLore, BindType.COMMAND_BIND_ITEM)
        }
        player.updateInventory()
        if (!hasParma) {
            sender.sendColorMessage(Lang.command__bindAll.formatBy(player.name))
            MessageTool.messageCoolDown(player, Lang.item_bind_all)
        }
    }
}