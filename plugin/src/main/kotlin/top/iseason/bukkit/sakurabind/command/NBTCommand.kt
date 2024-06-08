package top.iseason.bukkit.sakurabind.command

import io.github.bananapuncher714.nbteditor.NBTEditor
import org.bukkit.entity.Player
import org.bukkit.permissions.PermissionDefault
import top.iseason.bukkittemplate.command.CommandNode
import top.iseason.bukkittemplate.command.CommandNodeExecutor
import top.iseason.bukkittemplate.utils.bukkit.EntityUtils.getHeldItem

object NBTCommand : CommandNode(
    name = "nbt",
    description = "在控制台输出手上物品的NBT",
    default = PermissionDefault.OP,
    async = true,
    isPlayerOnly = true
) {
    override var onExecute: CommandNodeExecutor? = CommandNodeExecutor { params, sender ->
        val player = sender as Player
        val heldItem = player.inventory.getHeldItem() ?: return@CommandNodeExecutor
        println(NBTEditor.getNBTCompound(heldItem).toJson())
    }

}