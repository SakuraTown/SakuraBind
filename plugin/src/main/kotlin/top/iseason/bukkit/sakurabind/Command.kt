package top.iseason.bukkit.sakurabind

import org.bukkit.entity.Player
import org.bukkit.permissions.PermissionDefault
import top.iseason.bukkittemplate.command.*
import top.iseason.bukkittemplate.utils.bukkit.ItemUtils.checkAir
import top.iseason.bukkittemplate.utils.bukkit.MessageUtils.sendColorMessages

fun mainCommand() {
    command("sakuraBind") {
        description = "樱花绑定根节点"
        alias = arrayOf("sBind", "sb")
        default = PermissionDefault.OP
        node(
            "bind"
        ) {
            description = "绑定某玩家手上的物品"
            default = PermissionDefault.OP
            params = listOf(
                Param("<player>", suggestRuntime = ParamSuggestCache.playerParam),
                Param("[-noLore]", suggest = listOf("-noLore"))
            )
            executor {
                val player = getParam<Player>(0)
                val showLore = !"-noLore".equals(getOptionalParam<String>(1), true)
                val itemInMainHand = player.inventory.itemInMainHand
                if (itemInMainHand.checkAir()) return@executor
                SakuraBindAPI.bind(itemInMainHand, player, showLore)
                it.sendColorMessages("&a绑定成功!")
            }
        }
        node(
            "unBind"
        ) {
            description = "解绑定某玩家手上的物品"
            default = PermissionDefault.OP
            params = listOf(Param("<player>", suggestRuntime = ParamSuggestCache.playerParam))
            executor {
                val player = getParam<Player>(0)
                val itemInMainHand = player.inventory.itemInMainHand
                if (itemInMainHand.checkAir()) return@executor
                SakuraBindAPI.unBind(itemInMainHand)
                it.sendColorMessages("&a解绑成功!")
            }
        }
        node(
            "bindAll"
        ) {
            description = "绑定某玩家背包的物品"
            default = PermissionDefault.OP
            params = listOf(
                Param("<player>", suggestRuntime = ParamSuggestCache.playerParam),
                Param("[-noLore]", suggest = listOf("-noLore"))
            )
            executor {
                val player = getParam<Player>(0)
                val showLore = !"-noLore".equals(getOptionalParam<String>(1), true)
                for (itemStack in player.inventory) {
                    if (itemStack == null) continue
                    if (itemStack.checkAir()) continue
                    SakuraBindAPI.bind(itemStack, player, showLore)
                }
                player.updateInventory()
                it.sendColorMessages("&a绑定成功!")
            }
        }
        node(
            "unBindAll"
        ) {
            description = "解绑定某玩家背包的物品"
            default = PermissionDefault.OP
            params = listOf(Param("<player>", suggestRuntime = ParamSuggestCache.playerParam))
            executor {
                val player = getParam<Player>(0)
                for (itemStack in player.inventory) {
                    if (itemStack == null) continue
                    if (itemStack.checkAir()) continue
                    SakuraBindAPI.unBind(itemStack)
                }
                player.updateInventory()
                it.sendColorMessages("&a解绑成功!")
            }
        }
    }
    CommandHandler.updateCommands()
}