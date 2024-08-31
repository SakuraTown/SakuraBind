package top.iseason.bukkit.sakurabind.listener

import com.destroystokyo.paper.event.player.PlayerArmorChangeEvent
import com.destroystokyo.paper.event.player.PlayerArmorChangeEvent.SlotType.*

import org.bukkit.entity.LivingEntity
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.inventory.ItemStack
import top.iseason.bukkit.sakurabind.SakuraBindAPI
import top.iseason.bukkit.sakurabind.config.Config
import top.iseason.bukkit.sakurabind.config.ItemSettings
import top.iseason.bukkit.sakurabind.config.Lang
import top.iseason.bukkit.sakurabind.utils.BindType
import top.iseason.bukkit.sakurabind.utils.MessageTool
import top.iseason.bukkittemplate.utils.bukkit.ItemUtils.checkAir

object PaperListener : Listener {

    fun isPaper() =
        try {
            Class.forName("com.destroystokyo.paper.event.player.PlayerArmorChangeEvent")
            true
        } catch (_: Exception) {
            false
        }

    @EventHandler(ignoreCancelled = true)
    fun onArmorChange(event: PlayerArmorChangeEvent) {
        val item = event.newItem
        if (item == null || item.checkAir()) return
        if (Config.checkByPass(event.player)) return
        val owner = SakuraBindAPI.getOwner(item)?.toString()
        val player = event.player
        val setting = ItemSettings.getSetting(item)
        if (owner == null) {
            if (setting.getBoolean("auto-bind.enable", null, player) &&
                (setting.getBoolean("auto-bind.onEquipWear", null, player) ||
                        SakuraBindAPI.isAutoBind(item))
            ) {
                SakuraBindAPI.bind(item, player, type = BindType.EQUIP_BIND_ITEM)
                setEquip(event.slotType, player, item)
                MessageTool.messageCoolDown(player, Lang.auto_bind__onEquiped)
            }
        } else {
            if (setting.getBoolean("auto-unbind.enable", owner, player) &&
                setting.getBoolean("auto-unbind.onEquipWear", owner, player)
            ) {
                SakuraBindAPI.unBind(item, type = BindType.EQUIP_UNBIND_ITEM)
                setEquip(event.slotType, player, item)
                MessageTool.messageCoolDown(player, Lang.auto_unbind__onEquiped)
            }
        }
    }

    private fun setEquip(slotType: PlayerArmorChangeEvent.SlotType, player: LivingEntity, item: ItemStack) {
        when (slotType) {
            HEAD -> player.equipment?.helmet = item
            CHEST -> player.equipment?.chestplate = item
            LEGS -> player.equipment?.leggings = item
            FEET -> player.equipment?.boots = item
        }
    }
}