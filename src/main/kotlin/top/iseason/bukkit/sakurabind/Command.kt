package top.iseason.bukkit.sakurabind

import org.bukkit.entity.Player
import org.bukkit.permissions.PermissionDefault
import top.iseason.bukkit.bukkittemplate.command.CommandBuilder
import top.iseason.bukkit.bukkittemplate.command.Param
import top.iseason.bukkit.bukkittemplate.command.ParamSuggestCache
import top.iseason.bukkit.bukkittemplate.command.commandRoot
import top.iseason.bukkit.bukkittemplate.utils.sendColorMessages

fun command() {
    commandRoot(
        "sakuraBind",
        description = "樱花绑定根节点",
        alias = arrayOf("sBind", "sb"),
        default = PermissionDefault.OP
    ) {
        node(
            "bind",
            description = "绑定某玩家手上的物品",
            default = PermissionDefault.OP,
            params = arrayOf(Param("<player>", suggestRuntime = ParamSuggestCache.playerParam))
        ) {
            onExecute {
                val player = getParam<Player>(0)
                val itemInMainHand = player.inventory.itemInMainHand
                if (itemInMainHand.type.isAir) return@onExecute true
                SakuraBindAPI.bind(itemInMainHand, player)
                it.sendColorMessages("&a绑定成功!")
                true
            }
        }
        node(
            "unBind",
            description = "解绑定某玩家手上的物品",
            default = PermissionDefault.OP,
            params = arrayOf(Param("<player>", suggestRuntime = ParamSuggestCache.playerParam))
        ) {
            onExecute {
                val player = getParam<Player>(0)
                val itemInMainHand = player.inventory.itemInMainHand
                if (itemInMainHand.type.isAir) return@onExecute true
                SakuraBindAPI.unBind(itemInMainHand)
                it.sendColorMessages("&a解绑成功!")
                true
            }
        }
        node(
            "bindAll",
            description = "绑定某玩家背包的物品",
            default = PermissionDefault.OP,
            params = arrayOf(Param("<player>", suggestRuntime = ParamSuggestCache.playerParam))
        ) {
            onExecute {
                val player = getParam<Player>(0)
                for (itemStack in player.inventory) {
                    if (itemStack == null) continue
                    if (itemStack.type.isAir) continue
                    SakuraBindAPI.bind(itemStack, player)
                }
                player.updateInventory()
                it.sendColorMessages("&a绑定成功!")
                true
            }
        }
        node(
            "unBindAll",
            description = "解绑定某玩家背包的物品",
            default = PermissionDefault.OP,
            params = arrayOf(Param("<player>", suggestRuntime = ParamSuggestCache.playerParam))
        ) {
            onExecute {
                val player = getParam<Player>(0)
                for (itemStack in player.inventory) {
                    if (itemStack == null) continue
                    if (itemStack.type.isAir) continue
                    SakuraBindAPI.unBind(itemStack)
                }
                player.updateInventory()
                it.sendColorMessages("&a解绑成功!")
                true
            }
        }
    }
    CommandBuilder.updateCommands()
}