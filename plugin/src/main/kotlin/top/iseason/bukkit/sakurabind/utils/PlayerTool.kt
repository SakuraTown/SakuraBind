package top.iseason.bukkit.sakurabind.utils


import de.tr7zw.nbtapi.utils.MinecraftVersion
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack

object PlayerTool {
    fun getOffHandItem(player: Player): ItemStack? {
        return if (MinecraftVersion.isAtLeastVersion(MinecraftVersion.MC1_9_R1)) player.inventory.itemInOffHand else null
    }
}