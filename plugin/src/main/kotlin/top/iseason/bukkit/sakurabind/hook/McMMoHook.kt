package top.iseason.bukkit.sakurabind.hook

import com.gmail.nossr50.events.skills.repair.McMMOPlayerRepairCheckEvent
import com.gmail.nossr50.events.skills.salvage.McMMOPlayerSalvageCheckEvent
import com.gmail.nossr50.events.skills.unarmed.McMMOPlayerDisarmEvent
import org.bukkit.event.EventHandler
import top.iseason.bukkit.sakurabind.SakuraBindAPI
import top.iseason.bukkit.sakurabind.config.Lang
import top.iseason.bukkit.sakurabind.utils.MessageTool
import top.iseason.bukkittemplate.hook.BaseHook
import top.iseason.bukkittemplate.utils.bukkit.EntityUtils.getHeldItem

object McMMoHook : BaseHook("mcMMO"), org.bukkit.event.Listener {

    @EventHandler
    fun onItemSalvage(event: McMMOPlayerSalvageCheckEvent) {
        val itemStack = event.salvageItem
        val owner = SakuraBindAPI.getOwner(itemStack) ?: return
        val player = event.player
        if (SakuraBindAPI.getItemSetting(itemStack).getBoolean("addons.mcmmo-salvage", owner.toString(), player)) {
            MessageTool.messageCoolDown(player, Lang.addons__mcmmo_salvage)
            event.isCancelled = true
        }
    }

    @EventHandler
    fun onItemRepair(event: McMMOPlayerRepairCheckEvent) {
        val itemStack = event.repairedObject
        val owner = SakuraBindAPI.getOwner(itemStack) ?: return
        val player = event.player
        if (SakuraBindAPI.getItemSetting(itemStack).getBoolean("addons.mcmmo-repair", owner.toString(), player)) {
            MessageTool.messageCoolDown(player, Lang.addons__mcmmo_repair)
            event.isCancelled = true
        }
    }

    @EventHandler
    fun onItemDisarm(event: McMMOPlayerDisarmEvent) {
        val player = event.defender
        val itemStack = player.getHeldItem() ?: return
        val owner = SakuraBindAPI.getOwner(itemStack) ?: return
        if (SakuraBindAPI.getItemSetting(itemStack).getBoolean("addons.mcmmo-disarm", owner.toString(), player)) {
            MessageTool.messageCoolDown(event.player, Lang.addons__mcmmo_disarm)
            event.isCancelled = true
        }
    }
}