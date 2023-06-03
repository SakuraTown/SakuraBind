package top.iseason.bukkit.sakurabind.task

import io.github.bananapuncher714.nbteditor.NBTEditor
import org.bukkit.Bukkit
import org.bukkit.inventory.ItemStack
import org.bukkit.scheduler.BukkitRunnable
import top.iseason.bukkit.sakurabind.SakuraBindAPI
import top.iseason.bukkit.sakurabind.command.CallbackCommand
import top.iseason.bukkit.sakurabind.config.Config
import top.iseason.bukkit.sakurabind.config.ItemSettings
import top.iseason.bukkit.sakurabind.config.Lang
import top.iseason.bukkit.sakurabind.listener.SelectListener
import top.iseason.bukkit.sakurabind.utils.BindType
import top.iseason.bukkit.sakurabind.utils.MessageTool
import top.iseason.bukkittemplate.debug.debug
import top.iseason.bukkittemplate.utils.bukkit.ItemUtils.checkAir
import top.iseason.bukkittemplate.utils.bukkit.MessageUtils.sendColorMessage
import top.iseason.bukkittemplate.utils.other.runAsync
import java.util.*

class Scanner : BukkitRunnable() {
    private val sendBackMap = mutableMapOf<UUID, MutableList<ItemStack>>()
    override fun run() {
        runAsync {
            val onlinePlayers = Bukkit.getOnlinePlayers()
            debug("开始扫描玩家背包,共 ${onlinePlayers.size} 人")
            onlinePlayers.stream()
                .filter {
                    it.isOnline && !Config.checkByPass(it) &&
                            !SelectListener.noScanning.contains(it.uniqueId)
                }
                .forEach { player ->
                    var hasFound = false
                    val inventory = player?.openInventory?.bottomInventory ?: return@forEach
                    try {
                        //为了兼容mod，获取到的格子数不一致
                        for (i in 0 until inventory.size) {
                            val item = inventory.getItem(i) ?: continue
                            if (item.checkAir()) continue
                            val owner = SakuraBindAPI.getOwner(item)
                            val ownerStr = owner?.toString()
                            val setting = ItemSettings.getSetting(item, owner != null)
                            if (owner != null &&
                                setting.getBoolean("auto-unbind.enable", ownerStr, player) &&
                                setting.getBoolean("auto-unbind.onScanner", ownerStr, player)
                            ) {
                                debug("解绑物品 ${item.type}")
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
                                debug("${player.name} 的背包中存在绑定物品 ${item.type} 属于 $owner")
                                sendBackMap.computeIfAbsent(owner) { mutableListOf() }.add(item)
                                inventory.setItem(i, null)
                                hasFound = true
                                continue
                            }
                            if (owner == null &&
                                ((setting.getBoolean("auto-bind.enable", null, player) && setting.getBoolean(
                                    "auto-bind.onScanner",
                                    null,
                                    player
                                ))
                                        || NBTEditor.contains(
                                    item, Config.auto_bind_nbt
                                ))
                            ) {
                                debug("绑定物品 ${item.type}")
                                MessageTool.bindMessageCoolDown(player, Lang.auto_bind__onScanner, setting, item)
                                SakuraBindAPI.bind(item, player, type = BindType.SCANNER_BIND_ITEM)
                            }
                        }
                    } catch (_: Exception) {
                    }
                    if (hasFound) player.sendColorMessage(Lang.scanner_item_send_back)
                }
            if (sendBackMap.isNotEmpty()) {
                sendBackMap.forEach { (uid, list) ->
                    SakuraBindAPI.sendBackItem(uid, list)
                }
                sendBackMap.clear()
            }
        }
    }
}