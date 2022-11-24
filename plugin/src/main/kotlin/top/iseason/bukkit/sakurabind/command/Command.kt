package top.iseason.bukkit.sakurabind.command

import org.bukkit.entity.Player
import org.bukkit.permissions.PermissionDefault
import org.jetbrains.exposed.sql.statements.api.ExposedBlob
import top.iseason.bukkit.sakurabind.SakuraBindAPI
import top.iseason.bukkit.sakurabind.config.Lang
import top.iseason.bukkit.sakurabind.dto.PlayerItem
import top.iseason.bukkit.sakurabind.dto.PlayerItems
import top.iseason.bukkittemplate.command.*
import top.iseason.bukkittemplate.config.dbTransaction
import top.iseason.bukkittemplate.utils.bukkit.ItemUtils.checkAir
import top.iseason.bukkittemplate.utils.bukkit.ItemUtils.toByteArray
import top.iseason.bukkittemplate.utils.bukkit.MessageUtils.formatBy
import top.iseason.bukkittemplate.utils.bukkit.MessageUtils.sendColorMessage
import top.iseason.bukkittemplate.utils.bukkit.MessageUtils.sendColorMessages
import top.iseason.bukkittemplate.utils.other.EasyCoolDown

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
            async = true
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
            async = true
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
            async = true
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
            async = true
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

        node(
            "get"
        ) {
            description = "获取暂存箱物品"
            default = PermissionDefault.TRUE
            isPlayerOnly = true
            executor {
                val player = it as Player
                var page = 0
                var isEmpty = true
                if (EasyCoolDown.check(player, 1000)) {
                    it.sendColorMessage(Lang.command_coolDown)
                    return@executor
                }
                val count = dbTransaction {
                    var count = 0
                    while (true) {
                        val items =
                            PlayerItem.find { PlayerItems.uuid eq player.uniqueId }.limit(10, (page * 10).toLong())
                                .toList()
                        if (items.isEmpty()) break
                        for (item in items) {
                            val itemStack = item.getItemStack()
                            val addItem = player.inventory.addItem(itemStack)
                            //放不下了
                            if (addItem.isNotEmpty()) {
                                val first = addItem.values.first()
                                if (itemStack.amount != first.amount) count++ else if (count == 0) {
                                    return@dbTransaction -1
                                }
                                item.item = ExposedBlob(first.toByteArray())
                                isEmpty = false
                                break
                            } else {
                                item.delete()
                                count++
                            }
                        }
                        page++
                    }
                    count
                }
                if (count == -1) {
                    it.sendColorMessage(Lang.get_full)
                } else if (count == 0) {
                    it.sendColorMessage(Lang.get_empty)
                } else if (isEmpty) {
                    it.sendColorMessage(Lang.get_all)
                } else {
                    it.sendColorMessage(Lang.get_item.formatBy(count))
                }
            }
        }
    }
    CommandHandler.updateCommands()
}