package top.iseason.bukkit.sakurabind.task


import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.ItemStack
import org.bukkit.scheduler.BukkitRunnable
import top.iseason.bukkit.sakurabind.SakuraBindAPI
import top.iseason.bukkit.sakurabind.command.CallbackCommand
import top.iseason.bukkit.sakurabind.config.Config
import top.iseason.bukkit.sakurabind.config.ItemSettings
import top.iseason.bukkit.sakurabind.config.Lang
import top.iseason.bukkit.sakurabind.hook.BanItemHook
import top.iseason.bukkit.sakurabind.hook.GermHook
import top.iseason.bukkit.sakurabind.listener.SelectListener
import top.iseason.bukkit.sakurabind.utils.BindType
import top.iseason.bukkit.sakurabind.utils.MessageTool
import top.iseason.bukkit.sakurabind.utils.SendBackType
import top.iseason.bukkittemplate.debug.debug
import top.iseason.bukkittemplate.utils.bukkit.ItemUtils.checkAir
import top.iseason.bukkittemplate.utils.bukkit.MessageUtils.sendColorMessage
import java.util.*

class Scanner : BukkitRunnable() {
    private val sendBackMap = mutableMapOf<UUID, MutableList<ItemStack>>()
    override fun run() {
        val onlinePlayers = Bukkit.getOnlinePlayers()
//            debug { "开始扫描玩家背包,共 ${onlinePlayers.size} 人" }
        onlinePlayers
            .parallelStream()
            .filter {
                it.isValid && it.isOnline && !SelectListener.noScanning.contains(it.uniqueId) && !Config.checkByPass(it)
            }
            .forEach { player ->
                var hasFound = false
                val inventories = mutableListOf<Inventory>(player!!.inventory)
                if (BanItemHook.hasHooked) inventories.addAll(BanItemHook.getModInventories(player))
                if (GermHook.hasHooked) {
                    hasFound = GermHook.scanSlots(sendBackMap, player)
                }
                if (inventories.isNotEmpty()) {
                    for (inv in inventories) {
                        hasFound = scanInventory(inv, player)
                    }
                }
                if (hasFound) player.sendColorMessage(Lang.scanner_item_send_back)
            }
        if (sendBackMap.isNotEmpty()) {
            sendBackMap.forEach { (uid, list) ->
                SakuraBindAPI.sendBackItem(uid, list, type = SendBackType.SCANNER)
            }
            sendBackMap.clear()
        }
    }

    fun scanInventory(inv: Inventory, player: Player): Boolean {
        var hasFound = false
        try {
            //为了兼容mod，获取到的格子数不一致
            for (i in 0 until inv.size) {
                val item = inv.getItem(i) ?: continue
                if (item.checkAir()) continue
                val owner = SakuraBindAPI.getOwner(item)
                val ownerStr = owner?.toString()
                val setting = ItemSettings.getSetting(item)
                if (owner != null &&
                    setting.getBoolean("auto-unbind.enable", ownerStr, player) &&
                    setting.getBoolean("auto-unbind.onScanner", ownerStr, player)
                ) {
                    debug { "解绑物品 ${item.type}" }
                    SakuraBindAPI.unBind(item, BindType.SCANNER_UNBIND_ITEM)
                    MessageTool.messageCoolDown(player, Lang.auto_unbind__onScanner)
                    continue
                }
                if (owner != null && owner != player.uniqueId &&
                    (CallbackCommand.isCallback(owner) || setting.getBoolean(
                        "item.send-back-scanner",
                        ownerStr,
                        player
                    ))
                ) {
                    debug { "${player.name} 的背包中存在绑定物品 ${item.type} 属于 $owner" }
                    sendBackMap.computeIfAbsent(owner) { mutableListOf() }.add(item)
                    inv.setItem(i, null)
                    hasFound = true
                    continue
                }
                if (owner == null &&
                    (
                            (setting.getBoolean("auto-bind.enable", null, player) && setting.getBoolean(
                                "auto-bind.onScanner",
                                null,
                                player
                            ))
                                    || SakuraBindAPI.isAutoBind(item)
                            )
                ) {
                    debug { "绑定物品 ${item.type}" }
                    MessageTool.bindMessageCoolDown(player, Lang.auto_bind__onScanner, setting, item)
                    SakuraBindAPI.bind(item, player, type = BindType.SCANNER_BIND_ITEM)
                }
            }
        } catch (_: Exception) {
        }
        return hasFound
    }
}