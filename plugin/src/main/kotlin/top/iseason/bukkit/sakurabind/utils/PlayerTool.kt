package top.iseason.bukkit.sakurabind.utils

import io.github.bananapuncher714.nbteditor.NBTEditor
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack

object PlayerTool {
    fun getOffHandItem(player: Player): ItemStack? {
        return if (NBTEditor.getMinecraftVersion()
                .greaterThanOrEqualTo(NBTEditor.MinecraftVersion.v1_9)
        ) player.inventory.itemInOffHand else null
    }
}