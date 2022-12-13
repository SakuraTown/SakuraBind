package top.iseason.bukkit.sakurabind.command

import org.bukkit.entity.Player
import org.bukkit.permissions.PermissionDefault
import top.iseason.bukkit.sakurabind.SakuraBindAPI
import top.iseason.bukkit.sakurabind.config.Lang
import top.iseason.bukkittemplate.command.CommandNode
import top.iseason.bukkittemplate.command.CommandNodeExecutor
import top.iseason.bukkittemplate.command.Param
import top.iseason.bukkittemplate.command.ParamSuggestCache
import top.iseason.bukkittemplate.utils.bukkit.EntityUtils.getHeldItem
import top.iseason.bukkittemplate.utils.bukkit.ItemUtils.checkAir
import top.iseason.bukkittemplate.utils.bukkit.MessageUtils.formatBy
import top.iseason.bukkittemplate.utils.bukkit.MessageUtils.sendColorMessages

object BindToCommand : CommandNode(
    name = "bindTo", description = "绑定手上的物品给某玩家",
    default = PermissionDefault.OP,
    params = listOf(
        Param("<player>", suggestRuntime = ParamSuggestCache.playerParam),
        Param("[-noLore]", suggest = listOf("-noLore"))
    ),
    isPlayerOnly = true,
    async = true
) {
    override var onExecute: CommandNodeExecutor? = CommandNodeExecutor { params, sender ->
        val player = params.getParam<Player>(0)
        val showLore = !params.hasParma("-noLore")
        val itemInMainHand = (sender as Player).getHeldItem()
        if (itemInMainHand.checkAir()) return@CommandNodeExecutor
        SakuraBindAPI.bind(itemInMainHand!!, player, showLore)
        if (!params.hasParma("-silent")) {
            sender.sendColorMessages(Lang.command__bindTo.formatBy(player.name))
        }
    }
}