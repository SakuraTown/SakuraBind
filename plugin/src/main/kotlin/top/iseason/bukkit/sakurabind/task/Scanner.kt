package top.iseason.bukkit.sakurabind.task

import io.github.bananapuncher714.nbteditor.NBTEditor
import org.bukkit.Bukkit
import org.bukkit.inventory.ItemStack
import org.bukkit.scheduler.BukkitRunnable
import top.iseason.bukkit.sakurabind.SakuraBindAPI
import top.iseason.bukkit.sakurabind.config.Config
import top.iseason.bukkit.sakurabind.config.ItemSettings
import top.iseason.bukkit.sakurabind.config.Lang
import top.iseason.bukkit.sakurabind.logger.BindType
import top.iseason.bukkit.sakurabind.utils.MessageTool
import top.iseason.bukkittemplate.utils.bukkit.ItemUtils.checkAir
import top.iseason.bukkittemplate.utils.bukkit.MessageUtils.sendColorMessage
import java.util.*

class Scanner : BukkitRunnable() {
    private val sendBackMap = mutableMapOf<UUID, MutableList<ItemStack>>()
    override fun run() {

        Bukkit.getOnlinePlayers().forEach {
            if (!it.isOnline) return
            if (Config.checkByPass(it)) return@forEach
//                    info("正在检查 ${it.name} ${it.uniqueId} 的背包")
//                    info("送回物品功能: $auto_bind__scanner_send_back")
            var hasFound = false
            val inventory = it?.openInventory?.bottomInventory ?: return@forEach
            try {
                //为了兼容mod，获取到的格子数不一致
                for (i in 0 until inventory.size) {
                    val item = inventory.getItem(i) ?: continue
                    if (item.checkAir()) continue
                    val owner = SakuraBindAPI.getOwner(item)
                    val setting = ItemSettings.getSetting(item, owner != null)
                    if (setting.getBoolean(
                            "scanner-send-back",
                            owner.toString(),
                            it
                        ) && owner != null && owner != it.uniqueId
                    ) {
//                                info("找到一个违规物品${item.type} 属于 ${owner}")
                        sendBackMap.computeIfAbsent(owner) { mutableListOf() }.add(item)
                        inventory.setItem(i, null)
                        hasFound = true
                        continue
                    }
//                            println("${item.type} ${setting.getBoolean("auto-bind.enable", null, it)}")
                    if (owner == null &&
                        (setting.getBoolean("auto-bind.enable", null, it) || NBTEditor.contains(
                            item, Config.auto_bind_nbt
                        ))
                    ) {
//                                info("已绑定物品 ${item.type}")
                        MessageTool.bindMessageCoolDown(it, Lang.auto_bind__onScanner, setting, item)
                        SakuraBindAPI.bind(item, it, type = BindType.SCANNER_BIND_ITEM)
                    }
                }
            } catch (_: Exception) {
            }
            if (hasFound) it.sendColorMessage(Lang.scanner_item_send_back)
        }
        if (sendBackMap.isNotEmpty()) {
//                    info("正在送回物品")
            sendBackMap.forEach { (uid, list) ->
                SakuraBindAPI.sendBackItem(uid, list)
            }
            sendBackMap.clear()
        }
    }
}