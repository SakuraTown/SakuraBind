package top.iseason.bukkit.sakurabind.command

import org.bukkit.entity.Player
import org.bukkit.permissions.PermissionDefault
import top.iseason.bukkit.sakurabind.SakuraBindAPI
import top.iseason.bukkit.sakurabind.config.Lang
import top.iseason.bukkit.sakurabind.utils.MessageTool
import top.iseason.bukkittemplate.command.CommandNode
import top.iseason.bukkittemplate.command.CommandNodeExecutor
import top.iseason.bukkittemplate.command.Param
import top.iseason.bukkittemplate.command.ParamSuggestCache
import top.iseason.bukkittemplate.utils.bukkit.ItemUtils.checkAir
import top.iseason.bukkittemplate.utils.bukkit.MessageUtils.sendColorMessages

object UnBindCommand : CommandNode(
    name = "unBind",
    description = "解绑定某玩家手上的物品",
    default = PermissionDefault.OP,
    params = listOf(Param("<player>", suggestRuntime = ParamSuggestCache.playerParam)),
    async = true
) {
    override var onExecute: CommandNodeExecutor? = CommandNodeExecutor { params, sender ->
        val player = params.getParam<Player>(0)
        val itemInMainHand = player.inventory.itemInMainHand
        if (itemInMainHand.checkAir()) return@CommandNodeExecutor
        SakuraBindAPI.unBind(itemInMainHand)
        if (!params.hasParma("-silent")) {
            sender.sendColorMessages(Lang.command__unbind)
            if (!SakuraBindAPI.hasBind(itemInMainHand)) {
                MessageTool.messageCoolDown(player, Lang.item_unbind_hand)
            }
        }
    }
}