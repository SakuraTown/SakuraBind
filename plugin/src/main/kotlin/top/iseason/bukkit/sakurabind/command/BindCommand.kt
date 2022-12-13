package top.iseason.bukkit.sakurabind.command

import org.bukkit.entity.Player
import org.bukkit.permissions.PermissionDefault
import top.iseason.bukkit.sakurabind.SakuraBindAPI
import top.iseason.bukkit.sakurabind.config.ItemSettings
import top.iseason.bukkit.sakurabind.config.Lang
import top.iseason.bukkit.sakurabind.utils.MessageTool
import top.iseason.bukkittemplate.command.CommandNode
import top.iseason.bukkittemplate.command.CommandNodeExecutor
import top.iseason.bukkittemplate.command.Param
import top.iseason.bukkittemplate.command.ParamSuggestCache
import top.iseason.bukkittemplate.utils.bukkit.EntityUtils.getHeldItem
import top.iseason.bukkittemplate.utils.bukkit.ItemUtils.checkAir
import top.iseason.bukkittemplate.utils.bukkit.MessageUtils.formatBy
import top.iseason.bukkittemplate.utils.bukkit.MessageUtils.sendColorMessages

object BindCommand : CommandNode(
    name = "bind",
    description = "绑定某玩家 手上的物品",
    default = PermissionDefault.OP,
    params = listOf(
        Param("<player>", suggestRuntime = ParamSuggestCache.playerParam),
        Param("[-noLore]", suggest = listOf("-noLore"))
    ),
    async = true
) {
    override var onExecute: CommandNodeExecutor? = CommandNodeExecutor { params, sender ->
        val player = params.next<Player>()
        val showLore = !params.hasParma("-noLore")
        val isSilent = params.hasParma("-silent")
        val itemInMainHand = player.getHeldItem()
        if (itemInMainHand.checkAir()) return@CommandNodeExecutor
        SakuraBindAPI.bind(itemInMainHand!!, player, showLore)
        if (!isSilent) {
            sender.sendColorMessages(Lang.command__bind_item.formatBy(player.name))
            MessageTool.bindMessageCoolDown(
                player,
                Lang.item_bind_hand,
                ItemSettings.getSetting(itemInMainHand),
                itemInMainHand
            )
        }
    }
}