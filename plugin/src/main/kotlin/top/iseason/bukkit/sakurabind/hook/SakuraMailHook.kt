package top.iseason.bukkit.sakurabind.hook

import org.bukkit.Bukkit
import org.bukkit.inventory.ItemStack
import top.iseason.bukkit.sakurabind.config.Config
import top.iseason.bukkit.sakuramail.SakuraMail
import top.iseason.bukkit.sakuramail.config.SystemMailsYml
import java.time.Duration
import java.util.*

object SakuraMailHook {
    private val plugin = Bukkit.getPluginManager().getPlugin("SakuraMail")
    var hasHook = plugin != null

    /**
     * 将遗失的物品发送给玩家
     */
    fun sendMail(uuid: UUID, list: List<ItemStack>) {
        if (!hasHook) return
        val mailYml = SystemMailsYml.getMailYml(Config.mailId) ?: return
        val icon = mailYml.icon.clone()
        val iterator = mailYml.items.keys.iterator()
        val items = mutableMapOf<Int, ItemStack>()
        var remain = 0
        for ((index, itemStack) in list.withIndex()) {
            if (iterator.hasNext()) {
                items[iterator.next()] = itemStack
            } else {
                remain = index + 1
                break
            }
        }
        if (items.isEmpty()) return
        SakuraMail.sendTempMail(
            uuid,
            icon,
            mailYml.title,
            items,
            mailYml.commands,
            mailYml.expire ?: Duration.ofDays(30)
        )
        if (remain != 0) {
            sendMail(uuid, list.drop(remain))
        }
    }
}