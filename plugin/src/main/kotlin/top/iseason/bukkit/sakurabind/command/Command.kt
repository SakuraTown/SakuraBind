package top.iseason.bukkit.sakurabind.command

import io.github.bananapuncher714.nbteditor.NBTEditor
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.permissions.PermissionDefault
import org.jetbrains.exposed.sql.statements.api.ExposedBlob
import top.iseason.bukkit.sakurabind.SakuraBindAPI
import top.iseason.bukkit.sakurabind.config.Config
import top.iseason.bukkit.sakurabind.config.Lang
import top.iseason.bukkit.sakurabind.dto.PlayerItem
import top.iseason.bukkit.sakurabind.dto.PlayerItems
import top.iseason.bukkittemplate.command.*
import top.iseason.bukkittemplate.config.dbTransaction
import top.iseason.bukkittemplate.debug.SimpleLogger
import top.iseason.bukkittemplate.utils.bukkit.EntityUtils.getHeldItem
import top.iseason.bukkittemplate.utils.bukkit.ItemUtils.checkAir
import top.iseason.bukkittemplate.utils.bukkit.ItemUtils.toByteArray
import top.iseason.bukkittemplate.utils.bukkit.MessageUtils.formatBy
import top.iseason.bukkittemplate.utils.bukkit.MessageUtils.sendColorMessage
import top.iseason.bukkittemplate.utils.bukkit.MessageUtils.sendColorMessages
import top.iseason.bukkittemplate.utils.other.EasyCoolDown

fun mainCommand() {
    CommandNode.usageFooter = "&7所有命令加上 '-silent' 参数可以不显示提示消息\n "
    command("sakuraBind") {
        description = "樱花绑定根节点"
        alias = arrayOf("sBind", "sb", "sab", "bind")
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
                val showLore = !hasParma("-noLore")
                val itemInMainHand = player.inventory.itemInMainHand
                if (itemInMainHand.checkAir()) return@executor
                SakuraBindAPI.bind(itemInMainHand, player, showLore)
                if (!hasParma("-silent"))
                    it.sendColorMessages(Lang.command__bind)
            }
        }
        node(
            "bindTo"
        ) {
            description = "绑定手上的物品给某玩家"
            default = PermissionDefault.OP
            params = listOf(
                Param("<player>", suggestRuntime = ParamSuggestCache.playerParam),
                Param("[-noLore]", suggest = listOf("-noLore"))
            )
            isPlayerOnly = true
            async = true
            executor {
                val player = getParam<Player>(0)
                val showLore = !hasParma("-noLore")
                val itemInMainHand = (it as Player).getHeldItem()
                if (itemInMainHand.checkAir()) return@executor
                SakuraBindAPI.bind(itemInMainHand!!, player, showLore)
                if (!hasParma("-silent"))
                    it.sendColorMessages(Lang.command__bindTo.formatBy(player.name))
            }
        }
        node(
            "unBind"
        ) {
            description = "解绑定某玩家手上的物品"
            default = PermissionDefault.OP
            params = listOf(
                Param("<player>", suggestRuntime = ParamSuggestCache.playerParam)
            )
            async = true
            executor {
                val player = getParam<Player>(0)
                val itemInMainHand = player.inventory.itemInMainHand
                if (itemInMainHand.checkAir()) return@executor
                SakuraBindAPI.unBind(itemInMainHand)
                if (!hasParma("-silent"))
                    it.sendColorMessages(Lang.command__unbind)
            }
        }
        node(
            "bindAll"
        ) {
            description = "绑定某玩家背包里的所有物品"
            default = PermissionDefault.OP
            params = listOf(
                Param("<player>", suggestRuntime = ParamSuggestCache.playerParam),
                Param("[-noLore]", suggest = listOf("-noLore"))
            )
            async = true
            executor {
                val player = getParam<Player>(0)
                val showLore = !hasParma("-noLore")
                for (itemStack in player.inventory) {
                    if (itemStack == null) continue
                    if (itemStack.checkAir()) continue
                    SakuraBindAPI.bind(itemStack, player, showLore)
                }
                player.updateInventory()
                if (!hasParma("-silent"))
                    it.sendColorMessages(Lang.command__bindAll)
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
                if (!hasParma("-silent"))
                    it.sendColorMessages(Lang.command__unbindAll)
            }
        }

        node(
            "getLost"
        ) {
            description = "获取遗失物品"
            default = PermissionDefault.TRUE
            async = true
            isPlayerOnly = true
            executor {
                val player = it as Player
                var page = 0
                var isEmpty = true
                if (EasyCoolDown.check(player, 1000)) {
                    it.sendColorMessage(Lang.command__getLost_coolDown)
                    return@executor
                }
                var totalCount = 0
                dbTransaction {
                    while (true) {
                        val items =
                            PlayerItem.find { PlayerItems.uuid eq player.uniqueId }.limit(10, (page * 10).toLong())
                                .toList()
                        if (items.isEmpty()) break
                        for (item in items) {
                            val itemStacks = item.getItemStacks()
                            val release = mutableListOf<ItemStack>()
                            for (itemStack in itemStacks) {
                                val amount = itemStack.amount
                                val addItem = player.inventory.addItem(itemStack)
//                                println("size = ${addItem.size}")
                                //放不下了
                                if (addItem.isNotEmpty()) {
                                    val first = addItem.values.first()
                                    release.add(first)
                                    isEmpty = false
//                                    println("not empty ${amount} - ${first.amount}")
                                    totalCount += (amount - first.amount)
                                } else {
//                                    println("empty ${amount}")
                                    totalCount += amount
                                }
                            }
                            if (release.isEmpty()) {
                                item.delete()
                            } else {
                                item.item = ExposedBlob(release.toByteArray())
                                break
                            }
                        }
                        page++
                    }
                }
                if (hasParma("-silent")) return@executor
//                println(totalCount)
                if (totalCount == -1) {
                    it.sendColorMessage(Lang.command__getLost_full)
                } else if (totalCount == 0) {
                    it.sendColorMessage(Lang.command__getLost_empty)
                } else if (isEmpty) {
                    it.sendColorMessage(Lang.command__getLost_all.formatBy(totalCount))
                } else {
                    it.sendColorMessage(Lang.command__getLost_item.formatBy(totalCount))
                }
            }
        }
        node(
            "autoBind"
        ) {
            description = "给手上的物品添加自动绑定的NBT"
            default = PermissionDefault.OP
            isPlayerOnly = true
            async = true
            executor {
                val player = it as Player
                val heldItem = player.getHeldItem()
                if (heldItem.checkAir()) return@executor
                val autoBindNbt = Config.auto_bind_nbt.split('.').toTypedArray()
                heldItem!!.itemMeta = NBTEditor.set(heldItem, "", *autoBindNbt)!!.itemMeta
                player.updateInventory()
                if (!hasParma("-silent"))
                    it.sendColorMessage(Lang.command__autoBind.formatBy(Config.auto_bind_nbt))
            }
        }
        node(
            "debug"
        ) {
            description = "切换debug模式"
            default = PermissionDefault.OP
            async = true
            executor {
                SimpleLogger.isDebug = !SimpleLogger.isDebug
                if (!hasParma("-silent"))
                    it.sendColorMessage(Lang.command__debug.formatBy(SimpleLogger.isDebug))
            }
        }
    }
    CommandHandler.updateCommands()
}