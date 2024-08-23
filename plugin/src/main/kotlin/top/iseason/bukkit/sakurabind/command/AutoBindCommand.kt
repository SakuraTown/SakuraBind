package top.iseason.bukkit.sakurabind.command

import io.github.bananapuncher714.nbteditor.NBTEditor
import org.bukkit.entity.Player
import org.bukkit.permissions.PermissionDefault
import top.iseason.bukkit.sakurabind.config.Config
import top.iseason.bukkit.sakurabind.config.Lang
import top.iseason.bukkittemplate.command.CommandNode
import top.iseason.bukkittemplate.command.CommandNodeExecutor
import top.iseason.bukkittemplate.command.Param
import top.iseason.bukkittemplate.command.ParmaException
import top.iseason.bukkittemplate.utils.bukkit.EntityUtils.getHeldItem
import top.iseason.bukkittemplate.utils.bukkit.MessageUtils.formatBy
import top.iseason.bukkittemplate.utils.bukkit.MessageUtils.sendColorMessage

object AutoBindCommand : CommandNode(
    name = "autoBind",
    description = "给手上的物品添加自动绑定的NBT",
    default = PermissionDefault.OP,
    isPlayerOnly = true,
    async = true,
    params = listOf(
        Param("[nbt]", suggestRuntime = { Config.auto_bind_nbt.split('.') }),
        Param("[value]"),
    )
) {
    override var onExecute: CommandNodeExecutor? = CommandNodeExecutor { params, sender ->
        val player = sender as Player
        val nbt = params.nextOrNull<String>()
        val value = params.nextOrNull<String>() ?: ""
        val heldItem = player.getHeldItem() ?: throw ParmaException("请拿着物品")
        if (nbt == null) {
            heldItem.itemMeta = NBTEditor.set(heldItem, value, *Config.autoBindNbt)!!.itemMeta
            if (!params.hasParma("-silent"))
                player.sendColorMessage(Lang.command__autoBind.formatBy(Config.auto_bind_nbt))
        } else {
            val autoBindNbt = nbt.split('.').toTypedArray()
            heldItem.itemMeta = NBTEditor.set(heldItem, value, NBTEditor.CUSTOM_DATA, *autoBindNbt)!!.itemMeta
            player.sendColorMessage(Lang.command__autoBind.formatBy(nbt))
        }
        player.updateInventory()

    }

}