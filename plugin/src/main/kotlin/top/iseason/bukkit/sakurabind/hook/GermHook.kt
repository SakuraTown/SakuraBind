package top.iseason.bukkit.sakurabind.hook

import com.germ.germplugin.api.GermSlotAPI
import io.github.bananapuncher714.nbteditor.NBTEditor
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import top.iseason.bukkit.sakurabind.SakuraBindAPI
import top.iseason.bukkit.sakurabind.command.CallbackCommand
import top.iseason.bukkit.sakurabind.config.Config
import top.iseason.bukkit.sakurabind.config.ItemSettings
import top.iseason.bukkit.sakurabind.config.Lang
import top.iseason.bukkit.sakurabind.utils.BindType
import top.iseason.bukkit.sakurabind.utils.MessageTool
import top.iseason.bukkittemplate.debug.debug
import top.iseason.bukkittemplate.hook.BaseHook
import top.iseason.bukkittemplate.utils.bukkit.ItemUtils.checkAir
import java.util.*

object GermHook : BaseHook("GermPlugin") {

    fun scanSlots(sendBackMap: MutableMap<UUID, MutableList<ItemStack>>, player: Player): Boolean {
        var hasFound = false
        try {
            val allGermSlotIdentity = GermSlotAPI.getAllGermSlotIdentity()
            //为了兼容mod，获取到的格子数不一致
            for (id in allGermSlotIdentity) {
                val item = GermSlotAPI.getItemStackFromIdentity(player, id)
                if (item.checkAir()) continue
                val owner = SakuraBindAPI.getOwner(item)
                val ownerStr = owner?.toString()
                val setting = ItemSettings.getSetting(item, owner != null)
                if (owner != null &&
                    setting.getBoolean("auto-unbind.enable", ownerStr, player) &&
                    setting.getBoolean("auto-unbind.onScanner", ownerStr, player)
                ) {
                    debug("萌芽: 解绑物品 ${item.type}")
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
                    debug("萌芽: ${player.name} 的槽中存在绑定物品 ${item.type} 属于 $owner")
                    sendBackMap.computeIfAbsent(owner) { mutableListOf() }.add(item)
                    GermSlotAPI.saveItemStackToIdentity(player, id, ItemStack(Material.AIR))
                    hasFound = true
                    continue
                }
                if (owner == null &&
                    ((setting.getBoolean("auto-bind.enable", null, player) && setting.getBoolean(
                        "auto-bind.onScanner",
                        null,
                        player
                    )) || NBTEditor.contains(item, *Config.autoBindNbt))
                ) {
                    debug("萌芽: 绑定物品 ${item.type}")
                    MessageTool.bindMessageCoolDown(player, Lang.auto_bind__onScanner, setting, item)
                    SakuraBindAPI.bind(item, player, type = BindType.SCANNER_BIND_ITEM)
                }
            }
        } catch (_: Exception) {
        }
        return hasFound
    }
}