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
import top.iseason.bukkittemplate.utils.bukkit.MessageUtils.formatBy
import top.iseason.bukkittemplate.utils.bukkit.MessageUtils.sendColorMessages

object UnBindAllCommand : CommandNode(
    name = "unBindAll",
    description = "解绑定某玩家背包的物品",
    default = PermissionDefault.OP,
    params = listOf(Param("<player>", suggestRuntime = ParamSuggestCache.playerParam)),
    async = true
) {
    override var onExecute: CommandNodeExecutor? = CommandNodeExecutor { params, sender ->
        val player = params.getParam<Player>(0)
        for (itemStack in player.inventory) {
            if (itemStack == null) continue
            if (itemStack.checkAir()) continue
            SakuraBindAPI.unBind(itemStack)
        }
        player.updateInventory()
        if (!params.hasParma("-silent"))
            sender.sendColorMessages(Lang.command__unbindAll.formatBy(player.name))
        MessageTool.messageCoolDown(player, Lang.item_unbind_all)
    }
}