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
import top.iseason.bukkittemplate.utils.bukkit.ItemUtils.checkAir
import top.iseason.bukkittemplate.utils.bukkit.MessageUtils.sendColorMessages

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
            if (itemStack == null) continue
            if (itemStack.checkAir()) continue
            SakuraBindAPI.bind(itemStack, player, showLore)
            if (!hasParma)
                MessageTool.bindMessageCoolDown(
                    player,
                    Lang.item_bind_all,
                    ItemSettings.getSetting(itemStack),
                    itemStack
                )
        }
        player.updateInventory()
        if (!hasParma) {
            sender.sendColorMessages(Lang.command__bindAll)
            MessageTool.messageCoolDown(player, Lang.item_unbind_all)
        }
    }
}