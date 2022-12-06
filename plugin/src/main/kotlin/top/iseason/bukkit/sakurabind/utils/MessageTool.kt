package top.iseason.bukkit.sakurabind.utils

import org.bukkit.entity.HumanEntity
import top.iseason.bukkit.sakurabind.config.Config
import top.iseason.bukkittemplate.utils.bukkit.MessageUtils.sendColorMessage
import top.iseason.bukkittemplate.utils.other.EasyCoolDown

object MessageTool {
    fun sendCoolDown(player: HumanEntity, message: String) {
        if (!EasyCoolDown.check(player.uniqueId, Config.message_coolDown))
            player.sendColorMessage(message)
    }
}