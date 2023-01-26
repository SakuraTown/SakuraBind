package top.iseason.bukkit.sakurabind.command

import org.bukkit.block.Block
import org.bukkit.entity.Entity
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.permissions.PermissionDefault
import top.iseason.bukkit.sakurabind.SakuraBindAPI
import top.iseason.bukkit.sakurabind.config.Config
import top.iseason.bukkit.sakurabind.config.DefaultItemSetting
import top.iseason.bukkit.sakurabind.config.ItemSettings
import top.iseason.bukkit.sakurabind.config.Lang
import top.iseason.bukkit.sakurabind.listener.SelectListener
import top.iseason.bukkit.sakurabind.logger.BindType
import top.iseason.bukkittemplate.command.CommandNode
import top.iseason.bukkittemplate.command.CommandNodeExecutor
import top.iseason.bukkittemplate.command.Param
import top.iseason.bukkittemplate.command.ParamSuggestCache
import top.iseason.bukkittemplate.utils.bukkit.MessageUtils.sendColorMessage
import top.iseason.bukkittemplate.utils.other.submit

object SelectCommand : CommandNode(
    name = "select",
    description = "为玩家开启/关闭目标选择模式",
    default = PermissionDefault.OP,
    async = true,
    isPlayerOnly = true,
    params = listOf(
        Param("<player>", suggestRuntime = ParamSuggestCache.playerParam),
        Param("[bind]", listOf("bind"))
    )
) {
    override var onExecute: CommandNodeExecutor? = CommandNodeExecutor { params, _ ->
        val player = params.next<Player>()
        val silent = params.hasParma("-silent")
        val next = params.nextOrNull<String>()
        if ("bind" == next) {
            when (val any = SelectListener.selecting[player]) {
                is ItemStack -> {
                    SakuraBindAPI.bind(any, player, type = BindType.COMMAND_BIND_ITEM)
                }

                is Block -> {
                    SakuraBindAPI.bindBlock(
                        any,
                        player.uniqueId,
                        ItemSettings.getSetting(any.drops.first()),
                        BindType.COMMAND_BIND_BLOCK
                    )
                }

                is Entity -> {
                    SakuraBindAPI.bindEntity(any, player, DefaultItemSetting, BindType.COMMAND_BIND_ENTITY)
                }

                else -> {
                    player.sendColorMessage(Lang.command__select_no_selected)
                    return@CommandNodeExecutor
                }
            }
            SelectListener.selecting.remove(player)
            if (!silent) {
                player.sendColorMessage(Lang.command__select_bind)
            }
            return@CommandNodeExecutor
        }
        if (SelectListener.selecting.containsKey(player)) {
            if (!silent)
                player.sendColorMessage(Lang.command__select_off)
            SelectListener.selecting.remove(player)
        } else {
            if (!silent)
                player.sendColorMessage(Lang.command__select_on)
            SelectListener.selecting[player] = true
            submit(Config.command_select_timeout, async = true) {
                if (!SelectListener.selecting.containsKey(player)) return@submit
                SelectListener.selecting.remove(player)
                player.sendColorMessage(Lang.command__select_timeout)
            }
        }
    }
}