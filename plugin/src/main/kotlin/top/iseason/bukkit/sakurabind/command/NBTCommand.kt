package top.iseason.bukkit.sakurabind.command


import org.bukkit.entity.Player
import org.bukkit.permissions.PermissionDefault
import top.iseason.bukkittemplate.command.CommandNode
import top.iseason.bukkittemplate.command.CommandNodeExecutor
import top.iseason.bukkittemplate.command.ParmaException
import top.iseason.bukkittemplate.utils.bukkit.EntityUtils.getHeldItem
import top.iseason.bukkittemplate.utils.bukkit.ItemUtils.toJson

object NBTCommand : CommandNode(
    name = "nbt",
    description = "在控制台输出手上物品的NBT",
    default = PermissionDefault.OP,
    async = true,
    isPlayerOnly = true
) {
    override var onExecute: CommandNodeExecutor? = CommandNodeExecutor { params, sender ->
        val player = sender as Player
        val heldItem = player.inventory.getHeldItem() ?: throw ParmaException("请拿着物品")
        println(heldItem.toJson())
    }

}